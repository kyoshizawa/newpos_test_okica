package jp.mcapps.android.multi_payment_terminal.error;

public class JremRasErrorCodes {
    /* ID判定異常 */
    public static final int E82 = 82;

    /* ネガチェック異常、又は　Suica10年失効判定 */
    public static final int E83 = 83;

    /* ネガチェック異常（ＷＡＯＮ）*/
    public static final int E10083 = 10083;

    /* カード有効終了年月日異常 */
    public static final int E111 = 111;

    /* カード活性化コード判定異常 */
    public static final int E90 = 90;

    /* 機能種別コード判定異常 */
    public static final int E91 = 91;

    /* カード制御コード判定異常 */
    public static final int E92 = 92;

    /* ネガ化カード（ＷＡＯＮ）*/
    public static final int E10092 = 10092;

    /* パース合計金額判定異常 */
    public static final int E93 = 93;

    /* その他読込判定エラー */
    public static final int E305 = 305;

    /* チャージ不可事業者エラー */
    public static final int E321 = 321;

    /* カード認証異常 */
    public static final int E85 = 85;

    /* 読出し異常 */
    public static final int E86 = 86;

    /* FeliCa処理エラー */
    public static final int E417 = 417;

    /* ネガ書き込み処理でFeliCa処理エラー */
    public static final int E354 = 354;

    /* 書き込み異常 */
    public static final int E87 = 87;

    /* 書き込み異常（iD） */
    public static final int E10087 = 10087;

    /* Pollingでタイムアウト検知 */
    public static final int E95 = 95;

    /* カードエラーによりpollingでタイムアウト検知 */
    public static final int E10095 = 10095;

    /* FeliCa処理エラー */
    public static final int E353 = 353;

    /* FeliCa処理エラー（iD） */
    public static final int E10353 = 10353;

    /* FeliCa処理エラー */
    public static final int E355 = 355;

    /* 引去り不能（残額不足） */
    public static final int E88 = 88;

    /* 引去り不能（利用限度（額、回数）超過）iD */
    public static final int E10088 = 10088;

    /* 積み増し不能（積み増し限度額超過） */
    public static final int E89 = 89;

    /* 払戻判定異常(カードID) */
    public static final int E96 = 96;

    /* 払戻判定異常(キャッシュバック) */
    public static final int E97 = 97;

    /* 払戻判定異常(SFログ時間) */
    public static final int E98 = 98;

    /* 払戻判定異常(SFログ端末ID) */
    public static final int E99 = 99;

    /* 払戻判定異常(一件明細処理コード) */
    public static final int E104 = 104;

    /* 直近チャージ金額異常 */
    public static final int E107 = 107;

    /* 指定ＩDi異常（かざされたカードIDiがサービスで意図していたカードIDiと一致しない） */
    public static final int E110 = 110;

    /* 二重起動エラー */
    public static final int E337 = 337;

    /* フロアリミット金額超過（残りフロアリミット金額が不足している） */
    public static final int E108 = 108;

    /* クライアント証明書エラー */
    public static final int E257 = 257;

    /* クライアント端末未登録エラー */
    public static final int E289 = 289;

    /* クライアント端末状態エラー */
    public static final int E290 = 290;

    /* クライアント端末認証エラー */
    public static final int E291 = 291;

    /* パラメータエラー */
    public static final int E273 = 273;

    /* HMAC改ざん検知エラー */
    public static final int E274 = 274;

    /* R/Wデバイス操作エラー */
    public static final int E369 = 369;

    /* ステータス設定エラー */
    public static final int E371 = 371;

    /* 処理キャンセル */
    public static final int E370 = 370;

    /* DBアクセスエラー */
    public static final int E385 = 385;

    /* 電子マネー中継サーバ接続エラー */
    public static final int E497 = 497;

    /* 電子マネー中継サーバ通信エラー */
    public static final int E498 = 498;

    /* 通信エラー（サーバ側通信タイムアウト） */
    public static final int E256 = 256;

    /* その他エラー */
    public static final int E409 = 409;

    /* QUICPay カードのネガチェック異常 */
    public static final int E511 = 511;

    /* WAON独自セキュリティチェックエラー WAON鍵情報が存在しない */
    public static final int E772 = 772;

    /* WAON独自セキュリティチェックエラー MAC不一致 */
    public static final int E774 = 774;

    /* WAON カード内容不正 フォーマット不正 */
    public static final int E784 = 784;

    /* WAON カード内容不正 カード番号不正 */
    public static final int E785 = 785;

    /* WAON カード内容不正 テストカード種別不正 */
    public static final int E786 = 786;

    /* WAON カード内容不正 残高限度額不正 */
    public static final int E787 = 787;

    /* WAON カード内容不正 出荷前カード有効期限不正 */
    public static final int E788 = 788;

    /* Edy サイバー取引ステータス不正 */
    public static final int E1032 = 1032;

    /* Edy 鍵不正異常 */
    public static final int E1044 = 1044;

    /* nanaco 読み込み残高上限値超過 */
    public static final int E1281 = 1281;

    /* 該当マネー以外のカードがかざされた */
    public static final int E522 = 522;

    /* WAON マネー残高不足発生 */
    public static final int E817 = 817;

    /* Edy 残高不足（残高0円 または丸めにより0円） */
    public static final int E1043 = 1043;

    /* nanaco 残高不足（残高0円） */
    public static final int E1282 = 1282;

    /* 処理未了中の別カードタッチ */
    public static final int E514 = 514;

    /* WAON 別カードタッチ（チャージ・現金併用） */
    public static final int E791 = 791;

    /* TCAP通信関連エラー発生 */
    public static final int E836 = 836;

    /* FeliCaコマンドのレスポンスデータ不正 */
    public static final int E837 = 837;

    /* iD 暗証番号入力回数超過 */
    public static final int E504 = 504;

    /* iD 暗証番号入力タイムアウト */
    public static final int E506 = 506;

    /* 売上不能（売上上限金額超過） */
    public static final int E512 = 512;

    /* WAON ポイント残高上限値超過 */
    public static final int E816 = 816;

    /* カード有効期限切れ */
    public static final int E524 = 524;

    /* iD モバイル固有ロック（プライバシー設定） */
    public static final int E505 = 505;

    /* iD モバイル固有ロック（PIN設定） */
    public static final int E519 = 519;

    /* WAON 他端末でロックされたカード（ブランドアプリステータス不正） */
    public static final int E768 = 768;

    /* WAON 他端末でロックされたカード（カードイシュアアプリステータス不正） */
    public static final int E769 = 769;

    /* WAON 他端末でロックされたカード（バリューイシュアアプリステータス不正） */
    public static final int E770 = 770;

    /* WAON 他端末でロックされたカード（ポイントイシュア1アプリステータス不正） */
    public static final int E771 = 771;

    /* WAON 取消可能期間超過 */
    public static final int E801 = 801;

    /* WAON 取消後、ポイント残高下限値を下回る */
    public static final int E876 = 876;

    /* WAON 取消後、残高限度額超過 */
    public static final int E804 = 804;

    /* オーソリ異常 */
    public static final int E508 = 508;

    /* 外接オンラインサーバ接続エラー */
    public static final int E509 = 509;

    /* 外接オンラインサーバ通信エラー */
    public static final int E510 = 510;

    /* 通信エラー */
    public static final int E854 = 854;

    /* WAONセンタ接続エラー */
    public static final int E855 = 855;

    /* WAON クレジットカードネットワークエラー */
    public static final int E856 = 856;

    /* WAONセンタ通信タイムアウト */
    public static final int E857 = 857;

    /* WAON iCASコマンド生成エラー */
    public static final int E869 = 869;

    /* センタ通信エラー（異常応答） */
    public static final int E1037 = 1037;

    /* センタ通信エラー（通信失敗） */
    public static final int E1038 = 1038;

    /* センタ通信エラー（応答電文不正） */
    public static final int E1039 = 1039;

    /* センタ通信エラー（要求電文不正） */
    public static final int E1040 = 1040;

    /* nanaco DBエラー */
    public static final int E386 = 386;

    /* 利用不可 */
    public static final int E501 = 501;

    /* WAON 対象データなし */
    public static final int E838 = 838;

    /* WAON 対象データ重複 */
    public static final int E839 = 839;

    /* Edy 強制残高照会カード更新NG */
    public static final int E1042 = 1042;

    /* iD 暗証番号入力情報存在チェックエラー */
    public static final int E503 = 503;

    /* 端末利用可能期間外 */
    public static final int E835 = 835;

    /* 業務実行不可 */
    public static final int E1027 = 1027;

    /* nanaco ネガ情報更新不可 */
    public static final int E1280 = 1280;

    /* 未了中に別端末で取引実施 */
    public static final int E517 = 517;

    /* Edy 他端末での業務実行による未了復旧不可 */
    public static final int E1031 = 1031;

    /* 電子マネー証明書エラー */
    public static final int E4901 = 4901;

    /* 電子マネー開局エラー */
    public static final int E4903 = 4903;

    /* nanaco ログフルエラー */
    public static final int E1030 = 1030;
}

