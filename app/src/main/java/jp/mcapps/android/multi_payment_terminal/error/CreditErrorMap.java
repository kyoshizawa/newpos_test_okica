package jp.mcapps.android.multi_payment_terminal.error;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// MCセンターからのエラーコードを端末のエラーコードに変換する

public class CreditErrorMap {
    public static final String UNDEFINED_ERROR_CODE = "3099";
    private static String _internalErrorCode = null;
    private static final HashMap<String, String> _errorMap = new HashMap<>();

    public static String get(String mcPosCenterErrorCode) {
//        String value = _errorMap.get(mcPosCenterErrorCode);
//
//        if(value == null) {
//            value = UNDEFINED_ERROR_CODE;
//            _internalErrorCode = mcPosCenterErrorCode;
//        } else {
//            _internalErrorCode = null;
//        }
//        return value;

        if (mcPosCenterErrorCode != null) {
            String value = _errorMap.get(mcPosCenterErrorCode);
            return (value != null ? value : UNDEFINED_ERROR_CODE) + "@@@" + mcPosCenterErrorCode + "@@@";
        } else {
            return UNDEFINED_ERROR_CODE + "@@@@@@";
        }
    }

    public static String getKeyByValue(String value) {
        for(Map.Entry entry : _errorMap.entrySet()) {
            if(Objects.equals(value, entry.getValue())) {
                return (String)entry.getKey();
            }
        }
        if(value.equals(UNDEFINED_ERROR_CODE)) {
            // クレジットの未定義エラーの場合は内部エラーコードを返却
            return _internalErrorCode;
        } else {
            // その他マネーのエラーの場合はnullを返却
            return null;
        }
    }

    static {
        /* 読み込み待ちタイムアウト */
        _errorMap.put(CreditErrorCodes.T01, "3001");
        /* カード読み取り失敗（MS） */
        _errorMap.put(CreditErrorCodes.T02, "3002");
        /* カード読み取り失敗（IC） */
        _errorMap.put(CreditErrorCodes.T03, "3003");
        /* ＩＣチップ付カードの磁気をスワイプ */
        _errorMap.put(CreditErrorCodes.T04, "3004");
        /* 圏外（疎通確認の送信時） */
        _errorMap.put(CreditErrorCodes.T05, "3005");
        /* 圏外（未送信売上の送信時） */
        _errorMap.put(CreditErrorCodes.T06, "3006");
        /* 圏外（クレジットＣＡ公開鍵要求時） */
        _errorMap.put(CreditErrorCodes.T07, "3007");
        /* 圏外（カードデータ保護用公開鍵要求時） */
        _errorMap.put(CreditErrorCodes.T08, "3008");
        /* 圏外（カード判定要求時） */
        _errorMap.put(CreditErrorCodes.T09, "3009");
        /* 圏外（オンラインオーソリ要求時） */
        _errorMap.put(CreditErrorCodes.T10, "3010");
        /* 圏外（オンラインオーソリ取消要求時） */
        _errorMap.put(CreditErrorCodes.T11, "3011");
        /* 公開鍵異常応答 */
        _errorMap.put(CreditErrorCodes.T12, "3012");
        /* カード判定異常応答 */
        _errorMap.put(CreditErrorCodes.T13, "3013");
        /* オンラインオーソリ異常応答 */
        _errorMap.put(CreditErrorCodes.T14, "3014");
        /* オンラインオーソリ取消異常応答 */
        _errorMap.put(CreditErrorCodes.T15, "3015");
        /* EMV処理：初期化 */
        _errorMap.put(CreditErrorCodes.T16, "3016");
        /* EMV処理：アプリケーション選択 */
        _errorMap.put(CreditErrorCodes.T17, "3017");
        /* EMV処理：ICカードデータ読み込み */
        _errorMap.put(CreditErrorCodes.T18, "3018");
        /* EMV処理：オフラインデータ認証 */
        _errorMap.put(CreditErrorCodes.T19, "3019");
        /* EMV処理：処理制限 */
        _errorMap.put(CreditErrorCodes.T20, "3020");
        /* EMV処理：本人確認 */
        _errorMap.put(CreditErrorCodes.T21, "3021");
        /* EMV処理：端末リスク管理 */
        _errorMap.put(CreditErrorCodes.T22, "3022");
        /* EMV処理：端末アクション分析 */
        _errorMap.put(CreditErrorCodes.T23, "3023");
        /* EMV処理：オンラインオーソリ応答検証 */
        _errorMap.put(CreditErrorCodes.T24, "3024");
        /* 暗証番号入力エラー */
        _errorMap.put(CreditErrorCodes.T25, "3025");
        /* 暗証番号入力回数エラー（残り１回） */
        _errorMap.put(CreditErrorCodes.T26, "3026");
        /* 暗証番号入力回数エラー（回数超過） */
        _errorMap.put(CreditErrorCodes.T27, "3027");
        /* ICチップのないカードを挿入 */
        _errorMap.put(CreditErrorCodes.T28, "3047");
        /* 疎通確認異常応答 */
        _errorMap.put(CreditErrorCodes.T29, "3093");
        /* カードデータ保護用公開鍵取得失敗 */
        _errorMap.put(CreditErrorCodes.T91,  "3091");
        /* 暗証番号入力タイムアウト */
        _errorMap.put(CreditErrorCodes.T30,  "3092");
        /* Try Another Interface */
        _errorMap.put(CreditErrorCodes.T31,  "3060");
        /* 非対応カード */
        _errorMap.put(CreditErrorCodes.T32,  "3061");
        /* CDCVM未実行 */
        _errorMap.put(CreditErrorCodes.T33,  "3062");
        /* END APPLICATION */
        _errorMap.put(CreditErrorCodes.T34,  "3063");
        /* EMV処理：PINバイパス不可 */
        _errorMap.put(CreditErrorCodes.T35,  "3064");
        /* 暗証番号エラー（G42） */
        _errorMap.put(CreditErrorCodes.G42,  "3028");
        /* 限度額オーバー（G55） */
        _errorMap.put(CreditErrorCodes.G55,  "3029");
        /* 有効期限エラー（G83） */
        _errorMap.put(CreditErrorCodes.G83,  "3030");
        /* 取扱不可（G12） */
        _errorMap.put(CreditErrorCodes.G12,  "3031");
        /* 保留判定（G30） */
        _errorMap.put(CreditErrorCodes.G30,  "3032");
        /* 取り扱い不可（G54） */
        _errorMap.put(CreditErrorCodes.G54,  "3033");
        /* カード取り込み（G56） */
        _errorMap.put(CreditErrorCodes.G56,  "3034");
        /* 事故カード（G60） */
        _errorMap.put(CreditErrorCodes.G60,  "3035");
        /* 無効カード（G61） */
        _errorMap.put(CreditErrorCodes.G61,  "3036");
        /* 会員番号エラー（G65） */
        _errorMap.put(CreditErrorCodes.G65,  "3037");
        /* 金額エラー（G68） */
        _errorMap.put(CreditErrorCodes.G68,  "3038");
        /* ボーナス月エラー（G71） */
        _errorMap.put(CreditErrorCodes.G71,  "3039");
        /* 分割回数エラー（G74） */
        _errorMap.put(CreditErrorCodes.G74,  "3040");
        /* 支払区分エラー（G78） */
        _errorMap.put(CreditErrorCodes.G78,  "3041");
        /* オンライン終了（G95） */
        _errorMap.put(CreditErrorCodes.G95,  "3042");
        /* 取扱不可（G97） */
        _errorMap.put(CreditErrorCodes.G97,  "3043");
        /* 取扱不可（G98） */
        _errorMap.put(CreditErrorCodes.G98,  "3044");
        /* 取扱不可（G99） */
        _errorMap.put(CreditErrorCodes.G99,  "3045");
        /* ボーナス月不正（H71） */
        _errorMap.put(CreditErrorCodes.H71,  "3046");
    }
}
