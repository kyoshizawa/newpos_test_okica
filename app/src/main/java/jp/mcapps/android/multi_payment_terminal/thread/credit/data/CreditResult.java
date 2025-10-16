package jp.mcapps.android.multi_payment_terminal.thread.credit.data;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement;

public class CreditResult {
    public static class Result {
        // 有効フラグ（売上・印刷では未使用）
        // 本フラグがtrueであれば、売上データとして保存する
        public boolean validFlg;
        // オーソリエラーコード（売上・印刷では未使用）
        public String cafisErrorCd;
        // エラー要因（売上・印刷では未使用）
        public CreditSettlement.AuthErrorReason errorReason;
        // アプリケーションリスト（売上・印刷では未使用）
        public String[] appList;
        // アクワイアラID
        @Expose
        public int acquireId;
        // イシュア名
        @Expose
        public String issuerName;
        // 印字名
        @Expose
        public String printText;
        // 仕向け先コード
        @Expose
        public String deliveryCode;
        // 仕向け先名
        @Expose
        public String deliveryName;
        // クレジットアクワイアラID
        @Expose
        public int acquirerId;
        // ブランド識別
        @Expose
        public String brandSign;
        // AID
        @Expose
        public String aid;
        // カード区分
        @Expose
        public int cardKbn;
        // マネー区分
        @Expose
        public int moneyKbn;
        // 磁気・IC区分
        @Expose
        public int msICKbn;
        // 乗務員コード
        @Expose
        public int driverCd;
        // POSエントリモード
        @Expose
        public String posEntryMode;
        // PANシーケンスナンバー
        @Expose
        public int panSeqNo;
        // IC端末対応フラグ
        @Expose
        public int icTerminalFlg;
        // 鍵種別
        @Expose
        public int keyType;
        // 鍵バージョン
        @Expose
        public int keyVersion;
        // 暗号化データ
        @Expose
        public String rsaData;
        // 処理成否
        @Expose
        public boolean result;
        // 端末番号
        @Expose
        public String terminalNo;
        // 売上日時
        @Expose
        public String authDateTime;
        // レスポンスコード
        @Expose
        public String resCd;
        // ICC関連データ
        @Expose
        public String iccData;
        // マスクされたカード番号
        @Expose
        public String maskedMemberNo;
        // 承認番号
        @Expose
        public String cafisApprovalNo;
        // 伝票番号
        @Expose
        public int terminalProcNo;
        // 売上金額
        @Expose
        public int fare;
        // 取引内容
        @Expose
        public int transactionType;
        // 支払方法
        @Expose
        public String payMethod;
        // 商品コード
        @Expose
        public String productCd;
        // KID
        @Expose
        public String kid;
        // オンオフ区分
        @Expose
        public int onOffKbn;
        // チップコンディションコード
        @Expose
        public String chipCC;
        // 強制オンライン
        @Expose
        public int forcedOnline;
        // 強制承認
        @Expose
        public int forcedApproval;
        // カード有効期限
        @Expose
        public String cardExpDate;
        // アプリケーションラベル
        @Expose
        public String applicationLabel;
        // サイン欄印字フラグ
        @Expose
        public int signatureFlag;

        //ADD-S BMT S.Oyama 2024/10/03 フタバ双方向向け改修
        //ATC
        @Expose
        public String atc;

        //PIN存在フラグ Pin有り時はtrue
        @Expose
        public Boolean pinExistFL;
        //ADD-E BMT S.Oyama 2024/10/03 フタバ双方向向け改修

    }
}
