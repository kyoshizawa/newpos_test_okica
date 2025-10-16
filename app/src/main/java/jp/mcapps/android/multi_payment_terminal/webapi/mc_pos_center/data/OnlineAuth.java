package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data;

import com.google.gson.annotations.Expose;

public class OnlineAuth {
    public static class Request {
        @Expose
        public String terminalNo;       // 端末番号

        @Expose
        public String deliveryCode;     // 仕向け先コード

        @Expose
        public int acquirerId;          // クレジットアクワイアラID

        @Expose
        public int moneyKbn;            // マネー区分

        @Expose
        public int msICKbn;             // 磁気・IC区分

        @Expose
        public int cardKbn;             // 使用ストライプ情報

        @Expose
        public String authDateTime;     // 操作日時

        @Expose
        public int fare;                // 売上金額

        @Expose
        public int terminalProcNo;      // 通番（伝票番号）

        @Expose
        public String payMethod;        // 支払方法

        @Expose
        public String productCd;        // 商品コード

        @Expose
        public int driverCd;            // 乗務員コード

        @Expose
        public String aid;              // AID

        @Expose
        public String posEntryMode;     // POSエントリモード

        @Expose
        public String panSeqNo;         // PANシーケンスナンバー

        @Expose
        public int icTerminalFlg;       // IC端末対応フラグ

        @Expose
        public String brandSign;        // ブランド識別

        @Expose
        public int keyType;             // 鍵種別

        @Expose
        public int keyVersion;          // 鍵バージョン

        @Expose
        public String rsaData;          // 暗号化データ
    }

    public static class Response {
        @Expose
        public Boolean result;          // 処理成否

        @Expose
        public String errorCode;        // エラーコード

        @Expose
        public String terminalNo;       // 端末番号

        @Expose
        public String cafisErrorCd;     // オーソリエラーコード

        @Expose
        public String authDateTime;     // 売上日時

        @Expose
        public String resCd;            // レスポンスコード

        @Expose
        public String iccData;          // ICC関連データ

        @Expose
        public String maskedMemberNo;   // マスクされたカード番号

        @Expose
        public String cafisApprovalNo;  // 承認番号

        @Expose
        public int terminalProcNo;      // 伝票番号

        @Expose
        public int fare;                // 売上金額

        @Expose
        public int transactionType;     // 取引内容

        @Expose
        public String payMethod;        // 支払方法

        @Expose
        public String productCd;        // 商品コード

        @Expose
        public String kid;              // KID
    }
}
