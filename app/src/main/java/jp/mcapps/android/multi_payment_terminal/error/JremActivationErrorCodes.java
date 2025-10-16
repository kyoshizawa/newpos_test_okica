package jp.mcapps.android.multi_payment_terminal.error;

public class JremActivationErrorCodes {
    /* 入力された申込通番・アクティベートID・パスワードに該当する証明書情報データがない */
    public static final String T_E0110 = "T_E0110";

    /* 有効な証明書情報が取得不可能な状態 */
    public static final String T_E0125 = "T_E0125";

    /* 証明書情報に登録されている端末固有番号が一致しない */
    public static final String T_E0126 = "T_E0126";

    /* 有効な端末情報が取得不可能な状態 */
    public static final String T_E0127 = "T_E0127";

    /* 端末交換時、交換元の証明書情報が取得不可能な状態 */
    public static final String T_E0128  = "T_E0128";

    /* 必須項目に値が入力されていない */
    public static final String T_E0215 = "T_E0215";

    /* 入力された値の桁数が不正 */
    public static final String T_E0216 = "T_E0216";

    /* 入力された値が数値でない */
    public static final String T_E0221 = "T_E0221";

    /* 入力された値が半角英数字でない */
    public static final String T_E0222 = "T_E0222";

    /* フォーマット変換する際に想定外な値、引数であった */
    public static final String T_E0225 = "T_E0225";

    /* 電子マネー認証エラー */
    public static final String E4902 = "4902";
}
