package jp.mcapps.android.multi_payment_terminal.database;

import com.google.gson.annotations.Expose;

/**
 * 暫定ビルド用
 */

public class DeviceClient {

    public static class Cancel {
        @Expose
        public String cancel;
    }
    public static class StatusSimple {
        @Expose
        public String status;
    }
    public static class Status {
        @Expose
        public String status;
        @Expose
        public String IDi;
        @Expose
        public String IDm;
        @Expose
        public String rem;
        @Expose
        public String value;
        @Expose
        public String sid;
        @Expose
        public String sprwid;
        @Expose
        public String time;
        @Expose
        public String bRem;
        @Expose
        public String statementID;
        @Expose
        public String ICsequence;
        @Expose
        public String SFLogID;
        @Expose
        public String oldstatementID;
        @Expose
        public String oldSFLogID;
    }

    public static class Display {
        @Expose
        public String[] display;
    }

    public static class Operation {
        @Expose
        public String inputRequest;
        @Expose
        public String value;
        @Expose
        public String timeout;
        @Expose
        public String keyId;
    }

    public static class RetryID {
        @Expose
        public String unFinRetryFlg;
        @Expose
        public ResultID unFinInfo;
    }

    public static class RetryWAON {
        @Expose
        public String unFinRetryFlg;
        @Expose
        public ResultWAON unFinInfo;
    }

    public static class RetryQUICPay {
        @Expose
        public String unFinRetryFlg;
        @Expose
        public ResultQUICPay unFinInfo;
    }

    public static class RetryEdy {
        @Expose
        public String unFinRetryFlg;
        @Expose
        public ResultEdy unFinInfo;
    }

    public static class Retrynanaco {
        @Expose
        public String unFinRetryFlg;
        @Expose
        public UnFinInfonanaco unFinInfo;
    }

    public static class UnFinInfonanaco {
        @Expose
        public String cancelButtonReDispFlg;
        @Expose
        public String dispMsg;
        @Expose
        public String notifyCode;
    }

    public static class Result {
        @Expose
        public String result;
        @Expose
        public String IDi;
        @Expose
        public String IDm;
        @Expose
        public String rem;
        @Expose
        public String value;
        @Expose
        public String sid;
        @Expose
        public String sprwid;
        @Expose
        public String time;
        @Expose
        public String code;
        @Expose
        public String bRem;
        @Expose
        public String statementID;
        @Expose
        public String ICsequence;
        @Expose
        public String SFLogID;
        @Expose
        public String oldstatementID;
        @Expose
        public String oldSFLogID;
    }

    public static class ResultID {
        @Expose
        public String result;
        @Expose
        public String resultForClient;
        @Expose
        public String operationStatus;
        @Expose
        public String operationResultCode;
        @Expose
        public String userMaskMembershipNum;
        @Expose
        public String memberMaskMembershipNum;
        @Expose
        public String tenantMaskMembershipNum;
        @Expose
        public String cardCompMaskMembershipNum;
        @Expose
        public String membershipNum;
        @Expose
        public String businessId;
        @Expose
        public String trade;
        @Expose
        public String payment;
        @Expose
        public String goodsCode;
        @Expose
        public String taxOther;
        @Expose
        public String effectiveTerm;
        @Expose
        public String sequenceNo;
        @Expose
        public String recognitionNum;
        @Expose
        public String value;
        @Expose
        public String sid;
        @Expose
        public String slipNo;
        @Expose
        public String oldSlipNo;
        @Expose
        public String termIdentId;
        @Expose
        public String dealingsThroughNum;
        @Expose
        public String reqTimeStamp;
        @Expose
        public String time;
        @Expose
        public String totalAmount;
        @Expose
        public String giftFlg;
        @Expose
        public String rem;
        @Expose
        public String todayTotalUseCnt;
        @Expose
        public String todayTotalTrAmount;
        @Expose
        public String thisMonTotalTrAmount;
        @Expose
        public String authErrCode;
        @Expose
        public String authErrMsg;
        @Expose
        public String code;
        @Expose
        public String unFinFlg;
        @Expose
        public String cancelFlg;
    }

    public static class ResultWAON {
        @Expose
        public String result;
        @Expose
        public String resultForClient;
        @Expose
        public String operationStatus;
        @Expose
        public String operationResultCode;
        @Expose
        public String code;
        @Expose
        public String authErrCode;
        @Expose
        public String authErrMsg;
        @Expose
        public String reqTimeStamp;
        @Expose
        public String time;
        @Expose
        public String waonNum;
        @Expose
        public String businessId;
        @Expose
        public String tradeTypeCode;
        @Expose
        public String sid;
        @Expose
        public String termIdentId;
        @Expose
        public String idm;
        @Expose
        public String cardThroughNum;
        @Expose
        public String slipNo;
        @Expose
        public String oldSlipNo;
        @Expose
        public String value;
        @Expose
        public String chargeType;
        @Expose
        public String autoChargeValue;
        @Expose
        public String cashTogetherValue;
        @Expose
        public String totalValue;
        @Expose
        public String beforeBalance;
        @Expose
        public String balance;
        @Expose
        public String pointTradeValue;
        @Expose
        public String pointGrantType;
        @Expose
        public String pointGrantMessage1;
        @Expose
        public String pointGrantMessage2;
        @Expose
        public String point;
        @Expose
        public String memberStorePointNTimes;
        @Expose
        public String memberStorePointOther;
        @Expose
        public String memberStorePointTotal;
        @Expose
        public String addPointTotal;
        @Expose
        public String validDate1;
        @Expose
        public String point1;
        @Expose
        public String validDate2;
        @Expose
        public String point2;
        @Expose
        public String totalPoint;
        @Expose
        public String paymentPossibleFrom;
        @Expose
        public String paymentPossibleTo;
        @Expose
        public String chargePointFrom;
        @Expose
        public String chargePointTo;
        @Expose
        public String cashTogetherFlg;
        @Expose
        public String balanceInsufficient;
        @Expose
        public String balanceExcess;
        @Expose
        public String unFinFlg;
        @Expose
        public String autoChargeFlg;
        @Expose
        public String autoChargeStatus;
        @Expose
        public String cashChargeFlg;
        @Expose
        public String sentBackFlg;
        @Expose
        public String pointChargeFlg;
        @Expose
        public String cancelFlg;
        @Expose
        public AddInfo addInfo;

        // レギュレーション変更により、WAONカード番号はRASから通知される
        // マスク無しのカード番号情報を用いて、クライアント端末側でマスク処理を行う
        public String userMaskWaonNum() {
            if (waonNum == null || waonNum.length() != 16) {
                return null;
            } else {
                // 先頭12桁をマスクしたwaonNumを返却（お客様控えWAON番号）
                return "************" + waonNum.substring(12);
            }
        }

        public String memberMaskWaonNum() {
            if (waonNum == null || waonNum.length() != 16) {
                return null;
            } else {
                // 先頭4桁をマスクしたwaonNumを返却（加盟店控えWAON番号）
                return "****" + waonNum.substring(4, 16);
            }
        }

        public String tenantMaskWaonNum() {
            if (waonNum == null || waonNum.length() != 16) {
                return null;
            } else {
                // 先頭4桁と9桁目以降をマスクしないwaonNumを返却（加盟店本部控えWAON番号）
                // 例：1234****567890123456
                return waonNum.substring(0, 4) + "****" + waonNum.substring(8, 16);
            }
        }
    }

    public static class AddInfo {
        @Expose
        public HistoryData[] historyData;
    }
    public static class HistoryData {
        @Expose
        public String terminalId;
        @Expose
        public String cardThroughNum;
        @Expose
        public String terminalThroughNum;
        @Expose
        public String tradeTypeCode;
        @Expose
        public String historyDate;
        @Expose
        public String historyTime;
        @Expose
        public String balance;
        @Expose
        public String value;
        @Expose
        public String chargeValue;
        @Expose
        public String chargeType;

        //ADD-S BMT S.Oyama 2024/09/03 フタバ双方向向け改修
        @Expose
        public String sprw_id;
//ADD-E BMT S.Oyama 2024/09/03 フタバ双方向向け改修
    }
    public static class ResultEdy {
        @Expose
        public String result;
        @Expose
        public String sid;
        @Expose
        public String businessId;
        @Expose
        public String termIdentId;
        @Expose
        public String time;
        @Expose
        public String value;
        @Expose
        public String cashTogetherValue;    // 現金併用時端末内部で使用
        @Expose
        public String totalValue;           // 現金併用時端末内部で使用
        @Expose
        public String shortfall;
        @Expose
        public SaleHistoryEdy[] saleHistories;
        @Expose
        public SaleHistory2Edy[] saleHistories2;
        @Expose
        public String trade;
        @Expose
        public String userMaskMembershipNum;
        @Expose
        public String balance;
        @Expose
        public String autoRetryFlg;
        @Expose
        public String forcedBalanceFlg;
        @Expose
        public String otherCardUsePossibleFlg;
        @Expose
        public String nearfullFlg;
        @Expose
        public String code;
    }

    public static class SaleHistoryEdy {
        @Expose
        public String cardResultCode;
        @Expose
        public String edyTransactionNo;
        @Expose
        public String cardTransactionNo;
        @Expose
        public String userMaskMembershipNum;
        @Expose
        public String memberMaskMembershipNum;
        @Expose
        public String transactionMoney;
        @Expose
        public String beforeBalance;
        @Expose
        public String afterBalance;
    }

    public static class SaleHistory2Edy {
        @Expose
        public String edyTrade;
        @Expose
        public String transactionDate;
        @Expose
        public String transactionMoney;
        @Expose
        public String balance;
    }

    public static class Resultnanaco {
        @Expose
        public String result;
        @Expose
        public String sid;
        @Expose
        public String businessId;
        @Expose
        public String termIdentId;
        @Expose
        public String time;
        @Expose
        public String value;
        @Expose
        public String shortfall;
        @Expose
        public String paidTotalValue;
        @Expose
        public String trade;
        @Expose
        public String userMaskNanacNum;
        @Expose
        public String balance;
        @Expose
        public String otherCardUsePossibleFlg;
        @Expose
        public String nearfullFlg;
        @Expose
        public String logfullFlg;
        @Expose
        public SaleHistorynanaco[] saleHistories;
        @Expose
        public String code;
    }

    public static class SaleHistorynanaco {
        @Expose
        public String cardResultCode;
        @Expose
        public String slipNo;
        @Expose
        public String cardTransactionNo;
        @Expose
        public String userMaskNanacoNum;
        @Expose
        public String memberMaskNanacoNum;
        @Expose
        public String tenantMaskNanacoNum;
        @Expose
        public String cardCompMaskNanacoNum;
        @Expose
        public String transactionMoney;
        @Expose
        public String beforeBalance;
        @Expose
        public String afterBalance;
    }

    public static class ResultQUICPay {
        @Expose
        public String result;
        @Expose
        public String resultForClient;
        @Expose
        public String operationStatus;
        @Expose
        public String operationResultCode;
        //ADD-S BMT S.Oyama 2025/01/23 フタバ双方向向け改修
        @Expose
        public String acquierName;
        //ADD-E BMT S.Oyama 2024/01/23 フタバ双方向向け改修
        @Expose
        public String userMaskMembershipNum;
        @Expose
        public String memberMaskMembershipNum;
        @Expose
        public String tenantMaskMembershipNum;
        @Expose
        public String cardCompMaskMembershipNum;
        @Expose
        public String membershipNum;
        @Expose
        public String businessId;
        @Expose
        public String trade;
        @Expose
        public String payment;
        @Expose
        public String goodsCode;
        @Expose
        public String taxOther;
        @Expose
        public String effectiveTerm;
        @Expose
        public String sequenceNo;
        @Expose
        public String recognitionNum;
        @Expose
        public String value;
        @Expose
        public String sid;
        @Expose
        public String slipNo;
        @Expose
        public String oldSlipNo;
        @Expose
        public String termIdentId;
        @Expose
        public String dealingsThroughNum;
        @Expose
        public String reqTimeStamp;
        @Expose
        public String time;
        @Expose
        public String totalAmount;
        @Expose
        public String partRtFlg;
        @Expose
        public String saleTrade;
        @Expose
        public String saleSlipNo;
        @Expose
        public String saleValue;
        @Expose
        public String saleDealingsThroughNum;
        @Expose
        public String authErrCode;
        @Expose
        public String authErrMsg;
        @Expose
        public String code;
        @Expose
        public String cancelFlg;
        @Expose
        public AddInfoQUICPay addInfo;
    }

    public static class AddInfoQUICPay {
        @Expose
        public HistoryDataQUICPay[] historyData;
    }

    public static class HistoryDataQUICPay {
        @Expose
        public String date;
        @Expose
        public String trade;
        @Expose
        public String termIdentId;
        @Expose
        public String dealingsThroughNum;
        @Expose
        public String value;
    }

    // 業務処理状態応答 Suica
    public static class BusinessStatusResponse {
        @Expose
        public String result;
        @Expose
        public String sid;
        @Expose
        public String sprwid;
        @Expose
        public ResultData resultData;
        @Expose
        public String code;
    }

    public static class ResultData {
        @Expose
        public String businessId;
        @Expose
        public String operationStatus;
        @Expose
        public String result;
        @Expose
        public String IDi;
        @Expose
        public String IDm;
        @Expose
        public String rem;
        @Expose
        public String value;
        @Expose
        public String sid;
        @Expose
        public String sprwid;
        @Expose
        public String time;
        @Expose
        public String code;
        @Expose
        public String bRem;
        @Expose
        public String statementID;
        @Expose
        public String ICsequence;
        @Expose
        public String SFLogID;
        @Expose
        public String oldstatementID;
        @Expose
        public String oldSFLogID;
        @Expose
        public String operationResultCode;
    }

    // 業務処理状態応答 iD
    public static class BusinessStatusResponseID {
        @Expose
        public String result;
        @Expose
        public String sid;
        @Expose
        public String termIdentId;
        @Expose
        public ResultID resultData;
        @Expose
        public String code;
    }

    // 業務処理状態応答 WAON
    public static class BusinessStatusResponseWAON {
        @Expose
        public String result;
        @Expose
        public String sid;
        @Expose
        public String termIdentId;
        @Expose
        public ResultWAON resultData;
        @Expose
        public String code;
    }

    // 業務処理状態応答 QUICPay
    public static class BusinessStatusResponseQUICPay {
        @Expose
        public String result;
        @Expose
        public String sid;
        @Expose
        public String termIdentId;
        @Expose
        public ResultQUICPay resultData;
        @Expose
        public String code;
    }

    // 業務処理状態応答 Edy
    public static class BusinessStatusResponseEdy {
        @Expose
        public String result;
        @Expose
        public String sid;
        @Expose
        public String termIdentId;
        @Expose
        public ResultEdy resultData;
        @Expose
        public String code;
    }

    // 業務処理状態応答 nanaco
    public static class BusinessStatusResponsenanaco {
        @Expose
        public String result;
        @Expose
        public String sid;
        @Expose
        public String termIdentId;
        @Expose
        public Resultnanaco resultData;
        @Expose
        public String code;
    }
    // 日計 Edy (撤去時に使用）
    public static class JournalEdy {
        @Expose
        public String result;
        @Expose
        public String sid;
        @Expose
        public String time;
        @Expose
        public String termFrom;
        @Expose
        public String termTo;
        @Expose
        public String businessId;
        @Expose
        public EdySummary summary;
        @Expose
        public EdyDetail[] details;
        @Expose
        public String code;
    }

    public static class EdySummary {
        @Expose
        public EdyCharge discharge;
        @Expose
        public EdyCharge charge;
    }

    public static class EdyCharge {
        @Expose
        public String compCount;
        @Expose
        public String compAmount;
        @Expose
        public String incompCount;
        @Expose
        public String incompAmount;
    }

    public static class EdyDetail {
        @Expose
        public String reqTimeStamp;
        @Expose
        public String trade;
        @Expose
        public String userMaskMembershipNum;
        @Expose
        public String cardTransactionNo;
        @Expose
        public String payValue;
    }

    // 日計 nanaco
    public static class Journalnanaco {
        @Expose
        public String result;
        @Expose
        public String sid;
        @Expose
        public String time;
        @Expose
        public String termFrom;
        @Expose
        public String termTo;
        @Expose
        public String businessId;
        @Expose
        public ReportSales reportSales;
        @Expose
        public ReportCashCharge reportCashCharge;
        @Expose
        public String code;
    }

    // 日計 QUICPay
    public static class JournalQUICPay {
        @Expose
        public String result;
        @Expose
        public String termFrom;
        @Expose
        public String termTo;
        @Expose
        public String totalCount;
        @Expose
        public String totalAmount;
        @Expose
        public QUICPaySummary summary;
        @Expose
        public QUICPayDetails[] details;
        @Expose
        public String code;
    }

    public static class QUICPayDetails {
        @Expose
        public String reqTimeStamp;
        @Expose
        public String membershipNum;
        @Expose
        public String trade;
        @Expose
        public String slipNo;
        @Expose
        public String value;
        @Expose
        public String dealingsThroughNum;
    }

    public static class QUICPaySummary {
        @Expose
        public QUICPaySummaryDetail sale;
        @Expose
        public QUICPaySummaryDetail cancel;
    }

    public static class QUICPaySummaryDetail {
        @Expose
        public String compCount;
        @Expose
        public String compAmount;
        @Expose
        public String incompCount;
        @Expose
        public String incompAmount;
    }

    public static class ReportSales {
        @Expose
        public String outPutDate;
        @Expose
        public String termIdentId;
        @Expose
        public String compCount;
        @Expose
        public String compAmount;
        @Expose
        public String incompCount;
        @Expose
        public String incompAmount;
        @Expose
        public DayDetail[] dayDetail;
    }

    public static class DayDetail {
        @Expose
        public String date;
        @Expose
        public String compCount;
        @Expose
        public String compAmount;
        @Expose
        public String incompCount;
        @Expose
        public String incompAmount;
        @Expose
        public Detail[] detail;
    }

    public static class Detail {
        @Expose
        public String dealDate;
        @Expose
        public String cardResultCode;
        @Expose
        public String slipNo;
        @Expose
        public String memberMaskNanacoNum;
        @Expose
        public String cardTransactionNo;
        @Expose
        public String payValue;
    }

    public static class ReportCashCharge {
        @Expose
        public String outPutDate;
        @Expose
        public String termIdentId;
        @Expose
        public String compCount;
        @Expose
        public String compAmount;
        @Expose
        public String incompCount;
        @Expose
        public String incompAmount;
        @Expose
        public DayDetail[] dayDetail;
    }

    public static class Confirm {
        @Expose
        public String confirm;
        @Expose
        public String waittime;
    }

    public static class RWParam {

        /*
            ring(bar)[0] : control
            ring(bar)[1] : color
            ring(bar)[2] : time
            sound[0] : type
            sound[1] : control
            lcd[0] : no
            lcd[1] : message
            lcd[2] : time
        */

        public static final int RING_BAR_CTRL_OFF     = 0x00;
        public static final int RING_BAR_CTRL_ON      = 0x01;
        public static final int RING_BAR_CTRL_BLINK   = 0x02;     // 点滅周期を1000msとし、500ms点灯-500ms消灯
        public static final int RING_BAR_COLOR_NONE   = 0x00;
        public static final int RING_BAR_COLOR_BLUE   = 0x01;
        public static final int RING_BAR_COLOR_RED    = 0x02;
        public static final int SOUND_TYPE_SUICA      = 0x01;
        public static final int SOUND_CTRL_ERROR      = 0x03;     // 回復不能エラーまたは残額不足
        public static final int SOUND_CTRL_SUCCESS1   = 0x04;     // 正常終了 ピピッ
        public static final int SOUND_CTRL_SUCCESS2   = 0x05;     // 正常終了（残額が1,000円以下）ﾋﾟﾋﾟﾋﾟｯ
        public static final int SOUND_CTRL_NOEND      = 0x06;     // 処理未了 ピピﾋﾟﾋﾟﾋﾟｯ
        public static final int SOUND_CTRL_SUCCESS3   = 0x07;     // 正常終了 ピッ
        public static final int SOUND_CTRL_STOP       = 0x63;     // 鳴動停止

        @Expose
        public Integer[] ring;
        @Expose
        public Integer[] bar;
        @Expose
        public Integer[] sound;
        @Expose
        public String[] lcd1;
        @Expose
        public String[] lcd2;
        @Expose
        public String[] lcd3;
        @Expose
        public String[] lcd4;
    }
    public static class EdyFirstComm {
        @Expose
        public String result;
        @Expose
        public String businessId;
        @Expose
        public String termIdentId;
        @Expose
        public String code;
    }
}
