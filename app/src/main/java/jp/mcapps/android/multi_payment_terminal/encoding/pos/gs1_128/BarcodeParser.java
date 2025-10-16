package jp.mcapps.android.multi_payment_terminal.encoding.pos.gs1_128;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import jp.mcapps.android.multi_payment_terminal.Result;
import jp.mcapps.android.multi_payment_terminal.encoding.ErrorDetail;

public class BarcodeParser {
    // GS1-128のコード体系については以下のURLを参照:
    // https://www.gs1jp.org/assets/img/pdf/guideline202403.pdf

    public Result<BarcodeData, ErrorDetail> parseString(String code) {
        if (code == null || code.length() != 44) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_LENGTH));
        }

        // 固定長で分割
        String appID = code.substring(0, 2);                    // 2
        String companyCode = code.substring(2, 8);              // 6
        String freeField = code.substring(8, 29);               // 21
        String reissueCountString = code.substring(29, 30);     // 1
        String dueDateString = code.substring(30, 36);          // 6
        String stampFlagString = code.substring(36, 37);        // 1
        String paymentAmountString = code.substring(37, 43);    // 6
        String checkDigitString = code.substring(43, 44);       // 1

        // GS1アプリケーション識別子は 91
        if (!appID.equals("91")) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_SYSTEM, "app[0:2]", appID));
        }

        // チェックディジットを確認する
        int checkDigit = new CheckDigitGenerator(code).generate();
        if (!String.valueOf(checkDigit).equals(checkDigitString)) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_CHECK_DIGIT, "checkDigit[43:44]", appID));
        }

        // 結果を生成
        BarcodeData data = new BarcodeData();
        data.invoiceCompanyCode = companyCode;
        data.freeField = freeField;

        // 再発行回数
        try {
            data.reissueCount = Integer.parseUnsignedInt(reissueCountString);
        } catch (NumberFormatException ignored) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_FORMAT, "reissueCount[29:30]", reissueCountString));
        }

        // 支払期限日
        try {
            final SimpleDateFormat format = new SimpleDateFormat("yyMMdd", Locale.JAPANESE);
            data.dueDate = format.parse(dueDateString);
        } catch (ParseException ignored) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_FORMAT, "dueDate[30:36]", dueDateString));
        }

        // 印刷フラグ
        try {
            data.stampFlag = Integer.parseUnsignedInt(stampFlagString);
        } catch (NumberFormatException ignored) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_FORMAT, "stampFlag[36:37]", stampFlagString));
        }

        // 支払金額
        try {
            data.paymentAmount = Integer.parseUnsignedInt(paymentAmountString);
        } catch (NumberFormatException ignored) {
            return Result.err(new ErrorDetail(ErrorCodes.INVALID_CODE_FORMAT, "paymentAmount[37:43]", paymentAmountString));
        }

        return Result.ok(data);
    }
}
