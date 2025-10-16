package jp.mcapps.android.multi_payment_terminal.error;

public class GmoErrorCodes {
    /* 限度額エラーです。 */
    public static final String S0011 = "S0011";

    /* 残高不足エラーです。 */
    public static final String S0012 = "S0012";

    /* お客様のカードが利用できません。 */
    public static final String S0013 = "S0013";

    /* 決済金額が 0 円以下です。 */
    public static final String S0014 = "S0014";

    /* 返金処理に失敗しました。 */
    public static final String S0015 = "S0015";

    /* 元取引が無効または見つかりません。 */
    public static final String S0016 = "S0016";

    /* 伝票番号が不正または重複しています。 */
    public static final String S0017 = "S0017";

    /* このコードは他社専用コードのため、ご利用頂けません。 */
    public static final String S0018 = "S0018";

    /* ユーザの状態が不明またはユーザが存在しません。 */
    public static final String S0019 = "S0019";

    /* 加盟店または店舗が、無効か未登録です。 */
    public static final String S0020 = "S0020";

    /* 取消処理に失敗しました。 */
    public static final String S0021 = "S0021";

    /* パラメータが不正です。（文字種、桁数、フォーマット等） */
    public static final String S0051 = "S0051";

    /* お支払いが既に完了しているか支払いコードが無効です。 */
    public static final String S0052 = "S0052";

    /* 対応できないコードです。支払方法を確認してください。 */
    public static final String S0053 = "S0053";

    /* メンテナンス中です。 */
    public static final String S0055 = "S0055";

    /* その他のエラーです。詳細は returnMessage（結果メッセージ）をご確認ください。 */
    public static final String S0059 = "S0059";

    /* ユーザ認証エラーです。 */
    public static final String S0097 = "S0097";

    /* 端末認証エラーです。（管理画面上で店舗が端末識別番号チェック有無を ON に設定されている場合に出るエラー） */
    public static final String S0098 = "S0098";

    /* システムエラーです。 */
    public static final String S0099 = "S0099";
}
