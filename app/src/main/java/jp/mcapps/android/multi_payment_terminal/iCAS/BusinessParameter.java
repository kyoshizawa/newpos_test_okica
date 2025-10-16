package jp.mcapps.android.multi_payment_terminal.iCAS;

import com.google.gson.annotations.Expose;

public class BusinessParameter {
    @Expose
    public String commonName;          // ユニークＩＤ(メッセージ承認コードの生成に必要）
    @Expose
    public String organizationalUnit;  // 組織名(メッセージ承認コードの生成に必要）

    @Expose
    public String businessId;      // 業務識別子
    @Expose
    public String vr;              // バージョン番号
    @Expose
    public String time;            // 処理日時
    @Expose
    public String sid;             // 決済ＩＤ
    @Expose
    public Object money;           // マネークラス（業務識別子により決定）
    @Expose
    public String message;         // メッセージ承認コード

    public static class Suica {
        @Expose
        public String value;           // 処理金額
        @Expose
        public String idi;             // IDi
        @Expose
        public String together;        // 現金併用フラグ ON:現金併用 OFF:併用しない
        @Expose
        public String oldSid;          // 旧決済ＩＤ
        @Expose
        public String uiGuideline;     // ＵＩガイドライン対応フラグ ON:対応版 OFF:未対応版
        @Expose
        public String inProgressUI;    // 処理中ＵＩフラグ ON:対応版 OFF:未対応版
    }

    public static class iD {
        @Expose
        public String value;           // 処理金額
        @Expose
        public String slipNo;          // 伝票番号
        @Expose
        public String oldTermIdentId;  // 旧物販端末Ｒ/Ｗコード
        @Expose
        public String membershipNum;   // 会員番号
        @Expose
        public String effectiveTerm;   // 有効期限
        @Expose
        public String oldSid;          // 旧決済ＩＤ
        @Expose
        public String taxOther;        // 税・その他
        @Expose
        public String goodsCode;       // 商品コード
        @Expose
        public String payment;         // 支払方法区分
        @Expose
        public String daTermFrom;      // 日計期間開始日時
        @Expose
        public String training;        // トレーニングフラグ
        @Expose
        public String uiGuideline;     // ＵＩガイドライン対応フラグ ON:対応版 OFF:未対応版
        @Expose
        public String inProgressUI;    // 処理中ＵＩフラグ ON:対応版 OFF:未対応版
    }

    public static class Waon {
        @Expose
        public String value;           // 処理金額
        @Expose
        public String together;         // 現金併用フラグ ON:現金併用 OFF:併用しない
        @Expose
        public String totalValue;       // 合計取引金額
        @Expose
        public String pointValue;       // ポイント対象金額
        @Expose
        public String chargePoint;      // チャージポイント
        @Expose
        public String nTimesPoint;      // 加盟店独自ポイント（N倍）
        @Expose
        public String nTimesOtherPoint; // 加盟店独自ポイント（N倍以外）
        @Expose
        public String slipNo;           // 伝票番号
        @Expose
        public String idm;              // 製造ＩＤ
        @Expose
        public String waonNum;          // ＷＡＯＮ番号
        @Expose
        public String cardThroughNum;   // カード通番
        @Expose
        public String oldTermIdentId;   // 旧物販端末Ｒ/Ｗコード
        @Expose
        public String oldSid;           // 旧決済ＩＤ
        @Expose
        public String daTermFrom;       // 日計期間開始日時
        @Expose
        public String training;         // トレーニングフラグ
        @Expose
        public String uiGuideline;      // ＵＩガイドライン対応フラグ
        @Expose
        public String inProgressUI;     // 処理中ＵＩフラグ
    }

    public static class nanaco {
        @Expose
        public String oldSid;           // 旧決済ＩＤ
        @Expose
        public String value;            // 処理金額
        @Expose
        public String totalAmountDischargeFlg;  // 全額引去フラグ
        @Expose
        public String otherCardUseFlg;  // 他nanacoカード利用フラグ
        @Expose
        public String daTermFrom;       // 日計期間開始日時
        @Expose
        public String training;         // トレーニングフラグ
        @Expose
        public String cancelStopFlg;    // キャンセル取消フラグ
        @Expose
        public String uiGuideline;      // UIガイドライン対応フラグ
        @Expose
        public String inProgressUI;     // 処理中UIフラグ
    }

    public static class Edy {
        @Expose
        public String value;            // 処理金額
        @Expose
        public String depositMeans;     // 入金手段
        @Expose
        public String retryFlg;         // リトライフラグ
        @Expose
        public String totalAmountDischargeFlg;  // 全額引去フラグ
        @Expose
        public String otherCardUseFlg;  // 他Edyカード利用フラグ
        @Expose
        public String oldSid;           // 旧決済ＩＤ
        @Expose
        public String daTermFrom;       // 日計期間開始日時
        @Expose
        public String training;         // トレーニングフラグ
        @Expose
        public String uiGuideline;      // UIガイドライン対応フラグ
        @Expose
        public String inProgressUI;     // 処理中UIフラグ
    }

    public static class QUICPay {
        @Expose
        public String value;            // 処理金額
        @Expose
        public String slipNo;           // 伝票番号
        @Expose
        public String oldTermIdentId;   // 旧物販端末R/Wコード
        @Expose
        public String membershipNum;    // 会員番号
        @Expose
        public String effectiveTerm;    // 有効期限
        @Expose
        public String oldSid;           // 旧決済ＩＤ
        @Expose
        public String daTermForm;       // 日計期間開始日時
        @Expose
        public String training;         // トレーニングフラグ
        @Expose
        public String uiGuideline;      // UIガイドライン対応フラグ
        @Expose
        public String inProgressUI;     // 処理中UIフラグ
    }
}
