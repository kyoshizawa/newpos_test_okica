package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data;

import com.google.gson.annotations.Expose;

public class CardAnalyze {
    public static class Request {

        @Expose
        public int keytype;                     // 鍵種別

        @Expose
        public int keyVersion;                  // 鍵バージョン

        @Expose
        public String rsaData;                  // 暗号化データ
    }

    public static class Response {
        @Expose
        public Boolean result;                  // 処理成否

        @Expose
        public String errorCode;                // エラーコード

        @Expose
        public String acquirerName;             // アクワイアラ名

        @Expose
        public String issuerName;               // イシュア名

        @Expose
        public String printText;                // 印字名

        @Expose
        public String deliveryCode;             // 仕向け先コード

        @Expose
        public String deliveryName;             // 仕向け先名

        @Expose
        public int acquirerId;                  // クレジットアクワイアラID

        @Expose
        public String brandSign;                // ブランド識別

        @Expose
        public String aid;                      // AID

        @Expose
        public int cardKbn;                     // カード区分

        @Expose
        public int pinBypassInstruction;        // PINバイパス表示

        @Expose
        public int pinSkipFBFlg;                // PIN不知FB有効フラグ

        @Expose
        public int pinLessFlg;                  // PINレス取扱い可否

        @Expose
        public int pinLessLimit;                // PINレス限度額

        @Expose
        public int floorLimit;                  // フロアリミット

        @Expose
        public int maxTargetPercentage;         // 最大目標パーセンテージ

        @Expose
        public int targetPercentage;            // 目標パーセンテージ

        @Expose
        public int threshold;                   // しきい値

        @Expose
        public Boolean msAvailableFlg;          // MS移行フラグ

        @Expose
        public Boolean brandEnableFlg;          // ブランド有効/無効フラグ

        @Expose
        public String defaultDDOL;              // デフォルトDDOL

        @Expose
        public int execPriority;                // 実行優先順位

        @Expose
        public String terminalActionCdDenial;   // 端末アクションコード・拒否

        @Expose
        public String terminalActionCdDefault;  // 端末アクションコード・デフォルト

        @Expose
        public String terminalActionCdOnline;   // 端末アクションコード・オンライン

        @Expose
        public int acquirerOnlinePayType;       // アクワイアラ指定オンライン要求支払種別

        @Expose
        public String kameitenClassCd;          // 加盟店分類コード

        @Expose
        public String txnTypeCd;                // 取引分類コード

        @Expose
        public String chargeTypeCd;             // チャージタイプコード

        @Expose
        public String appPersonalInfo;          // AP個別情報
    }
}
