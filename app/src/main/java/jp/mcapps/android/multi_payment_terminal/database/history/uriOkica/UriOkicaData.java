package jp.mcapps.android.multi_payment_terminal.database.history.uriOkica;

import static jp.mcapps.android.multi_payment_terminal.data.okica.Constants.COMPANY_CODE_BUPPAN;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.nio.charset.StandardCharsets;

import fi.iki.elonen.NanoHTTPD;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.data.okica.ICMaster;
import jp.mcapps.android.multi_payment_terminal.data.okica.IDi;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.AmountParam;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.RefundParam;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.ResultParam;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.SurveyParam;
import jp.mcapps.android.multi_payment_terminal.database.Converters;
import jp.mcapps.android.multi_payment_terminal.thread.credit.data.CreditResult;
import jp.mcapps.android.multi_payment_terminal.thread.emv.utils.ISOUtil;
import jp.mcapps.android.multi_payment_terminal.ui.emoney.okica.BaseEMoneyOkicaViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.emoney.okica.CommonJudge;
import jp.mcapps.android.multi_payment_terminal.ui.emoney.okica.EMoneyOkicaViewModel;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.TerminalInfo;
import timber.log.Timber;

@Entity(tableName = "history_uri_okica")
public class UriOkicaData {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "okica_send")
    public Integer okicaSend;

    @ColumnInfo(name = "okica_card_idi", typeAffinity = ColumnInfo.BLOB)
    public byte[] okicaCardIdi;

    @ColumnInfo(name = "okica_detail_id")
    public Integer okicaDetailId;

    @ColumnInfo(name = "okica_process_code")
    public Integer okicaProcessCode;

    @ColumnInfo(name = "okica_trans_date")
    public String okicaTransDate;

    @ColumnInfo(name = "okica_functiion_type")
    public Integer okicaFunctionType;

    @ColumnInfo(name = "okica_control_code")
    public Integer okicaControlCode;

    @ColumnInfo(name = "okica_unfinished_flg")
    public Integer okicaUnfinishedFlg;

    @ColumnInfo(name = "okica_business_code")
    public Integer okicaBusinessCode;

    @ColumnInfo(name = "okica_model_code")
    public String okicaModelCode;

    @ColumnInfo(name = "okica_machine_id")
    public String okicaMachineId;

    @ColumnInfo(name = "okica_sequence")
    public Integer okicaSequence;

    @ColumnInfo(name = "okica_process_type")
    public Integer okicaProcessType;

    @ColumnInfo(name = "okica_sf_log_id")
    public Integer okicaSfLogId;

    @ColumnInfo(name = "okica_sf1_business_id")
    public Integer okicaSf1BusinessId;

    @ColumnInfo(name = "okica_sf1_amount")
    public Integer okicaSf1Amount;

    @ColumnInfo(name = "okica_sf1_balance")
    public Integer okicaSf1Balance;

    @ColumnInfo(name = "okica_sf1_category")
    public Integer okicaSf1Category;

    @ColumnInfo(name = "trans_amount")
    public Integer transAmount;

    @ColumnInfo(name = "trans_specified_amount")
    public Integer transSpecifiedAmount;

    @ColumnInfo(name = "trans_meter_amount")
    public Integer transMeterAmount;

    @ColumnInfo(name = "trans_adj_amount")
    public Integer transAdjAmount;

    @ColumnInfo(name = "trans_cash_together_amount")
    public Integer transCashTogetherAmount;

    @ColumnInfo(name = "trans_other_amount_one_type")
    public Integer transOtherAmountOneType;

    @ColumnInfo(name = "trans_other_amount_one")
    public Integer transOtherAmountOne;

    @ColumnInfo(name = "trans_other_amount_two_type")
    public Integer transOtherAmountTwoType;

    @ColumnInfo(name = "trans_other_amount_two")
    public Integer transOtherAmountTwo;

    @ColumnInfo(name = "trans_time")
    public Integer transTime;

    @ColumnInfo(name = "term_latitude")
    public String termLatitude;

    @ColumnInfo(name = "term_longitude")
    public String termLongitude;

    @ColumnInfo(name = "term_network_type")
    public String termNetworkType;

    @ColumnInfo(name = "term_radio_level")
    public Integer termRadioLevel;

    @ColumnInfo(name = "encrypt_type")
    public Integer encryptType;

    public UriOkicaData() {}

    /**
     * OKICA売上データ
     */
    public UriOkicaData(EMoneyOkicaViewModel.TransactionData transData, int encType, TerminalInfo.Response terminalInfoRes, int termSequence, ResultParam _resultParam, AmountParam amountParam, SurveyParam surveyParam) {
        BaseEMoneyOkicaViewModel.ReadData readData = transData.getReadData();
        int icType = transData.getIDi().getType();
        int businessCode = 0;

        // カードの活性事業者コードに該当する活性事業者データを、IC運用マスタから取得
        ICMaster.Activator activator = MainApplication.getInstance().getOkicaICMaster().getData().getActivator(readData.cardBasicInfoB3.getCompanyCode());

        // 沖縄IC(物販)の事業者コードを取得
        // ※カードの活性事業者コードに該当する活性事業者が、IC運用マスタに存在する事が条件
        ICMaster.Activator activatorBuppan = MainApplication.getInstance().getOkicaICMaster().getData().getActivator(COMPANY_CODE_BUPPAN);
        if (activator != null && activatorBuppan != null) {
            businessCode = activatorBuppan.getCompanyCode();
        }

        // OKICA送信状態
        okicaSend = 0;
        // カードIDi
        okicaCardIdi = transData.getIDi().getRawData();
        // 一件明細ID
        okicaDetailId = readData.getAccessControlInfo().getIkkenMeisaiId() + 1;
        // 取引日時
        String tDate = transData.getTransactionDate();
        tDate = tDate.replace("/", "");
        tDate = tDate.replace(":", "");
        tDate = tDate.replace(" ", "");
        okicaTransDate = tDate.substring(0, 14);
        // 機能種別
        okicaFunctionType = readData.getCardBasicInfoB3().getFuncType();
        // カード制御コード
        okicaControlCode = readData.accessControlInfo.getCardControlInfo();
        // 通信未了フラグ
        okicaUnfinishedFlg = (_resultParam.transResult == TransMap.RESULT_UNFINISHED) ? 1 : 0;
        // 機器事業者コード
        okicaBusinessCode = businessCode;
        // 機種コード
        byte hi = Integer.valueOf(terminalInfoRes.machineModelCd.substring(0, 2), 16).byteValue();
        byte lo = Integer.valueOf(terminalInfoRes.machineModelCd.substring(2, 4), 16).byteValue();
        int mModelCd = (ISOUtil.bcd2int((byte)(hi & 0x0F))) * 100 + ISOUtil.bcd2int(lo);
        if ((hi & 0x10) != 0) {
            mModelCd |= 0x80;
        }
        okicaModelCode = Integer.toHexString(mModelCd);
        // 機器ID
        okicaMachineId = terminalInfoRes.machineId;
        // IC取扱通番
        okicaSequence = termSequence;
        // SFログID
        okicaSfLogId = (_resultParam.transResult == TransMap.RESULT_NEGA_CHECK_ERROR) ? readData.sfLogInfo.getSFLogId() : readData.sfBalanceInfo.getExecId() + 1;
        // 事業者コード（SF1）
        okicaSf1BusinessId = businessCode;
        // 利用金額（SF1）
        int amount = transData.getCardAmount();
        okicaSf1Amount = (_resultParam.transType == TransMap.TYPE_SALES) ? -amount : amount;
        // 残額（SF1）
        if (_resultParam.transType == TransMap.TYPE_CACHE_CHARGE) {
            if (_resultParam.transResult == TransMap.RESULT_NEGA_CHECK_ERROR) {
                okicaSf1Balance = transData.getReadData().getSFLogInfo().getBalance();
            } else {
                okicaSf1Balance = transData.getWriteChargeData().getAccessControlInfo().getPurseAmount();
            }
        } else {
            if (_resultParam.transResult == TransMap.RESULT_NEGA_CHECK_ERROR) {
                okicaSf1Balance = transData.getReadData().getSFLogInfo().getBalance();
            } else {
                okicaSf1Balance = transData.getWriteData().getAccessControlInfo().getPurseAmount();
            }
        }
        // 入金区分（SF1） : チャージした場合は現金(0x00)を設定（現状では現金以外のチャージは想定外）
        okicaSf1Category = (_resultParam.transType == TransMap.TYPE_CACHE_CHARGE) ? 0 : readData.sfBalanceInfo.getDepositClass();
        // 取引金額
        transAmount = transData.getCardAmount();
        // 定額
        transSpecifiedAmount = amountParam.transSpecifiedAmount;
        // メーター金額
        transMeterAmount = amountParam.transMeterAmount;
        // 増減額
        transAdjAmount = amountParam.transAdjAmount;
        // 現金併用金額
        transCashTogetherAmount = amountParam.transCashTogetherAmount;
        // その他金額1種別（予備）
        transOtherAmountOneType = 0;
        // その他金額1（予備）
        transOtherAmountOne = 0;
        //その他金額2種別（予備）
        transOtherAmountTwoType = 0;
        //その他金額2（予備）
        transOtherAmountTwo = 0;
        //取引処理時間 このタイミングではNULLの場合あり
        transTime = surveyParam.transTime;
        // 位置情報（緯度）
        termLatitude = surveyParam.termLatitude;
        // 位置情報（経度）
        termLongitude = surveyParam.termLongitude;
        // ネットワーク種別
        termNetworkType = surveyParam.termNetworkType;
        // 電波状況（レベル）
        termRadioLevel = surveyParam.termRadioLevel;
        // 暗号化パターン
        encryptType = encType;

        // 処理コード
        okicaProcessCode = 0;
        // 処理種別
        okicaProcessType = 0;
        if (_resultParam.transType == TransMap.TYPE_SALES) {
            // 支払
            if ((icType == 0x00) || (icType == 0x06)) {    // IC複合券
                okicaProcessCode = (_resultParam.transResult == TransMap.RESULT_NEGA_CHECK_ERROR) ? 0x2100 : 0x2602;
            } else if ((icType == 0x03) || (icType == 0x07)) {    // ICSF券
                okicaProcessCode = (_resultParam.transResult == TransMap.RESULT_NEGA_CHECK_ERROR) ? 0x2110 : 0x2611;
            } else {
                Timber.w("OKICA一件明細（支払） 処理コードなし ICカード種別 : %d", icType);
            }
            okicaProcessType = 0x46;
        } else if (_resultParam.transType == TransMap.TYPE_CANCEL) {
            // 取消
            if ((icType == 0x00) || (icType == 0x06)) {    // IC複合券
                okicaProcessCode = (_resultParam.transResult == TransMap.RESULT_NEGA_CHECK_ERROR) ? 0x2100 : 0x2702;
            } else if ((icType == 0x03) || (icType == 0x07)) {    // ICSF券
                okicaProcessCode = (_resultParam.transResult == TransMap.RESULT_NEGA_CHECK_ERROR) ? 0x2110 : 0x2711;
            } else {
                Timber.w("OKICA一件明細（取消） 処理コードなし ICカード種別 : %d", icType);
            }
            okicaProcessType = 0x4C;
        } else if (_resultParam.transType == TransMap.TYPE_CACHE_CHARGE) {
            if ((icType == 0x00) || (icType == 0x06)) {    // IC複合券
                okicaProcessCode = (_resultParam.transResult == TransMap.RESULT_NEGA_CHECK_ERROR) ? 0x2100 : 0x0500;
            } else if ((icType == 0x03) || (icType == 0x07)) {    // ICSF券
                okicaProcessCode = (_resultParam.transResult == TransMap.RESULT_NEGA_CHECK_ERROR) ? 0x2110 : 0x0510;
            } else {
                Timber.w("OKICA一件明細（チャージ） 処理コードなし ICカード種別 : %d", icType);
            }
            okicaProcessType = 0x49;
        } else {
            Timber.w("OKICA一件明細 決済種別異常 : %d", _resultParam.transType);
        }
    }
}
