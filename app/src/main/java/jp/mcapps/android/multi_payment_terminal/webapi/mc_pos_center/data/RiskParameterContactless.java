package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data;

import com.google.gson.annotations.Expose;

public class RiskParameterContactless {
    public static class Request {
    }

    public static class Response {
        @Expose
        public Boolean result;              // 処理成否

        @Expose
        public String errorCode;            // エラーコード

        @Expose
        public RiskParam[] riskParams;
    }

    public static class RiskParam {
        @Expose
        public String brandSign;  // ブランド識別

        @Expose
        public String aid;  // AID

        @Expose
        public Integer floorLimit;  // フロアリミット

        @Expose
        public Integer maxTargetPercentage;  // 最大目標パーセンテージ

        @Expose
        public Integer targetPercentage;  // 目標パーセンテージ

        @Expose
        public Integer threshold;  // しきい値

        @Expose
        public Boolean brandEnableFlg;  // ブランド有効/無効フラグ

        @Expose
        public String defaultDDOL;  // デフォルトDDOL

        @Expose
        public Integer execPriority;  // 実行優先順位

        @Expose
        public String terminalActionCdDenial;  // 端末アクションコード・拒否

        @Expose
        public String terminalActionCdDefault;  // 端末アクションコード・デフォルト

        @Expose
        public String terminalActionCdOnline;  // 端末アクションコード・オンライン

        @Expose
        public String terminalActionCdDenialRefund;  // 端末アクションコード・拒否(取消)

        @Expose
        public String terminalActionCdDefaultRefund;  // 端末アクションコード・デフォルト(取消)

        @Expose
        public String terminalActionCdOnlineRefund;  // 端末アクションコード・オンライン(取消)

        @Expose
        public Integer acquirerOnlinePayType;  // アクワイアラ指定オンライン要求支払種別

        @Expose
        public String kameitenClassCd;  // 加盟店分類コード

        @Expose
        public String txnTypeCd;  // 取引分類コード

        @Expose
        public String chargeTypeCd;  // チャージタイプコード

        @Expose
        public String appPersonalInfo;  // AP個別情報

        @Expose
        public Integer kernelId;  // カーネル番号

        @Expose
        public Integer acquirerContactlessId;  // アクワイアラID

        @Expose
        public String combinationOptions;  // 端末オプション設定

        @Expose
        public Integer contactlessTransactionLimit;  // 非接触トランザクションリミット

        @Expose
        public Integer cvmRequiredLimit;  // 非接触CVMリミット

        @Expose
        public String merchantNameAndLocation;  // 加盟店名

        @Expose
        public Integer removableTimeout;  // 電文の戻り時間

        @Expose
        public String terminalCountryCode;  // 端末国コード

        @Expose
        public String terminalInterchangeProfile;  // 端末交換プロファイル

        @Expose
        public Integer terminalType;  // 端末タイプ

        @Expose
        public String transactionCurrencyCode;  // 取引国コード

        @Expose
        public Integer transactionCurrencyExponent;  // 取引通貨桁数

        @Expose
        public String defaultMdol;  // デフォルトMDOL
    }
}
