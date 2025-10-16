package jp.mcapps.android.multi_payment_terminal.encoding.pos.el_qr;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import jp.mcapps.android.multi_payment_terminal.Result;
import jp.mcapps.android.multi_payment_terminal.encoding.ErrorDetail;

public class QRCodeParser {
    // eL-QRのコード体系については以下のURLを参照:
    // https://paymentsjapan.or.jp/wp-content/uploads/2022/11/MPM_Guideline_3.0.pdf

    public Result<QRCodeData, ErrorDetail> parseString(String code) {
        if (code == null || code.length() != 255) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_LENGTH));
        }

        // TLV分割
        List<TLV.Part> parts;
        try {
            parts = TLV.splitString(code);
        } catch (Exception e) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_LENGTH));
        }
        if (parts.size() != 7) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_LENGTH));
        }
        TLV.Part payloadFormatIndicator = parts.get(0);
        TLV.Part pointOfInitiationMethod = parts.get(1);
        TLV.Part merchantAccountInformation = parts.get(2);
        TLV.Part transactionAmount = parts.get(3);
        TLV.Part additionalDataFieldTemplate = parts.get(4);
        TLV.Part unreservedTemplates = parts.get(5);
        TLV.Part crcValue = parts.get(6);

        // 仕様バージョン
        if (!payloadFormatIndicator.tag.equals("00") || !payloadFormatIndicator.value.equals("01")) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_SYSTEM, "00", payloadFormatIndicator.value));
        }
        // 静的/動的フラグ
        if (!pointOfInitiationMethod.tag.equals("01") || !pointOfInitiationMethod.value.equals("12")) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_SYSTEM, "01", pointOfInitiationMethod.value));
        }
        // 契約店情報
        if (!merchantAccountInformation.tag.equals("27") || merchantAccountInformation.length != 96) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_SYSTEM, "27", merchantAccountInformation.value));
        }
        // 取引金額
        if (!transactionAmount.tag.equals("54") || transactionAmount.length != 11) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_SYSTEM, "54", transactionAmount.value));
        }
        // 付加情報１
        if (!additionalDataFieldTemplate.tag.equals("62") || additionalDataFieldTemplate.length != 26) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_SYSTEM, "62", additionalDataFieldTemplate.value));
        }
        // 付加情報２
        if (!unreservedTemplates.tag.equals("80") || unreservedTemplates.length != 85) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_SYSTEM, "80", unreservedTemplates.value));
        }
        // CRC
        if (!crcValue.tag.equals("63") || crcValue.length != 5) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_SYSTEM, "63", crcValue.value));
        }
        try {
            // CRCチェック
            int crc = Integer.parseInt(crcValue.value, 10);
            int computedCrc = new CRCGenerator(code.substring(0, code.length() - 5)).generate();
            if (crc != (computedCrc % 100000)) {
                return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_CHECK_DIGIT, "63", crcValue.value));
            }
        } catch (NumberFormatException e) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_SYSTEM, "63", crcValue.value));
        }

        // 契約店情報を分割 -------------------------------------------------------------------------------------------
        List<TLV.Part> merchantAccountInformationParts;
        try {
            merchantAccountInformationParts = TLV.splitString(merchantAccountInformation.value);
        } catch (Exception e) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_SYSTEM, "27", merchantAccountInformation.value));
        }
        if (merchantAccountInformationParts.size() != 2) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_SYSTEM, "27", merchantAccountInformation.value));
        }
        TLV.Part merchantAccountIdentifier = merchantAccountInformationParts.get(0);
        TLV.Part mpnInformation = merchantAccountInformationParts.get(1);

        // 地方税共同機構識別コード
        if (!merchantAccountIdentifier.tag.equals("00") || !merchantAccountIdentifier.value.equals("13800")) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_SYSTEM, "27/00", merchantAccountIdentifier.value));
        }
        // MPN情報
        if (!mpnInformation.tag.equals("01") || mpnInformation.length != 83) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_SYSTEM, "27/01", mpnInformation.value));
        }

        // MPN情報を分割 -------------------------------------------------------------------------------------------
        String checkDigit01 = mpnInformation.value.substring(0, 2);             // [ 2] チェックディジット
        String bankAccountNumber = mpnInformation.value.substring(2, 13);       // [11] 地方税共同機構の口座番号
        String paymentAmount = mpnInformation.value.substring(13, 24);          // [11] 払込金額
        String feeHandling = mpnInformation.value.substring(24, 25);            // [ 1] 手数料負担区分 (2: 加入者負担)
        String agencyID = mpnInformation.value.substring(25, 30);               // [ 5] 機関ID "13800"
        String stampDuty = mpnInformation.value.substring(30, 31);              // [ 1] 印紙税要否区分 (0: 不要)
        String taxItemNumber = mpnInformation.value.substring(31, 34);          // [ 3] 税目・料金番号
        String reservedArea1 = mpnInformation.value.substring(34, 39);          // [ 5] 拡張領域 (未使用)
        String checkDigit02 = mpnInformation.value.substring(39, 41);           // [ 2] チェックディジット
        String projectNumber = mpnInformation.value.substring(41, 61);          // [20] 案件特定キー番号
        String confirmationNumber = mpnInformation.value.substring(61, 67);     // [ 6] 確認番号
        String elTaxUsageArea = mpnInformation.value.substring(67, 68);         // [ 1] eL TAX 利用領域 (0: 固定)
        String commonTaxOfficeCode = mpnInformation.value.substring(68, 73);    // [ 5] 共通納税機関コード
        String taxOfficeCode = mpnInformation.value.substring(73, 76);          // [ 3] 税務事務所コード
        String reservedArea2 = mpnInformation.value.substring(76, 83);          // [ 7] 拡張領域 (未使用)

        // 付加情報１を分割 -------------------------------------------------------------------------------------------
        String levyYear = additionalDataFieldTemplate.value.substring(0, 4);                // [ 4] 賦課年度 yyyy形式
        String taxYear = additionalDataFieldTemplate.value.substring(4, 8);                 // [ 4] 課税年度 yyyy形式
        String periodCode = additionalDataFieldTemplate.value.substring(8, 10);             // [ 2] 期別コード
        String filingDueDate = additionalDataFieldTemplate.value.substring(10, 18);         // [ 8] 納期限 yyyyMMdd形式
        String paymentDueDate = additionalDataFieldTemplate.value.substring(18, 26);        // [ 8] 支払期限 yyyyMMdd形式

        // 結果を作成する -------------------------------------------------------------------------------------------
        QRCodeData data = new QRCodeData();
        data.taxItemNumber = taxItemNumber;

        // 納期限
        try {
            final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.JAPANESE);
            data.filingDueDate = format.parse(filingDueDate);
        } catch (ParseException ignored) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_FORMAT, "62[10:18]", paymentDueDate));
        }
        // 支払期限
        try {
            final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.JAPANESE);
            data.paymentDueDate = format.parse(paymentDueDate);
        } catch (ParseException ignored) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_FORMAT, "62[18:26]", paymentDueDate));
        }
        // 支払金額
        try {
            data.paymentAmount = Integer.parseUnsignedInt(transactionAmount.value); // paymentAmountとtransactionAmountは同じ
        } catch (NumberFormatException ignored) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_FORMAT, "54", transactionAmount.value));
        }

        return Result.ok(data);
    }
}
