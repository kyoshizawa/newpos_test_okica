package jp.mcapps.android.multi_payment_terminal.error;

import android.app.Application;

import java.util.HashMap;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;

// MCセンターからのエラーコードを端末のエラーコードに変換する

public class McPosCenterErrorMap {
    private static final Application _app = MainApplication.getInstance();
    public static final String UNDEFINED_ERROR_CODE = _app.getString(R.string.error_type_payment_system_undefined_error);
    public static final String INTERNAL_ERROR_CODE = _app.getString(R.string.error_type_payment_system_internal_error);
    private static final HashMap<String, String> _errorMap = new HashMap<>();

    public static String get(String mcPosCenterErrorCode) {
        if (mcPosCenterErrorCode != null) {
            String value = _errorMap.get(mcPosCenterErrorCode);
            return value != null ? value : UNDEFINED_ERROR_CODE + "@@@" + mcPosCenterErrorCode + "@@@";
        } else {
            return UNDEFINED_ERROR_CODE + "@@@@@@";
        }
    }

    static {

        /* 共通エラー */
        _errorMap.put(McPosCenterErrorCodes.E0098, _app.getString(R.string.error_type_payment_system_2018));
        _errorMap.put(McPosCenterErrorCodes.E0099, _app.getString(R.string.error_type_payment_system_2019));

        /* 認証用公開鍵取得 */
        _errorMap.put(McPosCenterErrorCodes.E0101, _app.getString(R.string.error_type_payment_system_2030));
        _errorMap.put(McPosCenterErrorCodes.E0102, _app.getString(R.string.error_type_payment_system_2031));

        /* 相互認証1 */
        _errorMap.put(McPosCenterErrorCodes.E0201, _app.getString(R.string.error_type_payment_system_2032));
        _errorMap.put(McPosCenterErrorCodes.E0202, _app.getString(R.string.error_type_payment_system_2033));
        _errorMap.put(McPosCenterErrorCodes.E0203, _app.getString(R.string.error_type_payment_system_2034));

        /* 相互認証2 */
        _errorMap.put(McPosCenterErrorCodes.E0301, _app.getString(R.string.error_type_payment_system_2035));
        _errorMap.put(McPosCenterErrorCodes.E0302, _app.getString(R.string.error_type_payment_system_2036));
        _errorMap.put(McPosCenterErrorCodes.E0303, _app.getString(R.string.error_type_payment_system_2037));

        /* 疎通確認 */
        _errorMap.put(McPosCenterErrorCodes.E0401, null);

        /* センター設定情報取得 */
        _errorMap.put(McPosCenterErrorCodes.E0501, _app.getString(R.string.error_type_payment_system_2050));
        _errorMap.put(McPosCenterErrorCodes.E0502, _app.getString(R.string.error_type_payment_system_2051));
        _errorMap.put(McPosCenterErrorCodes.E0503, _app.getString(R.string.error_type_payment_system_2052));
        _errorMap.put(McPosCenterErrorCodes.E0504, _app.getString(R.string.error_type_payment_system_2053));

        /* 端末稼働情報連携 */
        _errorMap.put(McPosCenterErrorCodes.E0601, null);

        /* 機器番号設定 */
        _errorMap.put(McPosCenterErrorCodes.E0701, null);

        /* 係員設定 */
        _errorMap.put(McPosCenterErrorCodes.E0801, _app.getString(R.string.error_type_payment_system_2012));
        _errorMap.put(McPosCenterErrorCodes.E0802, _app.getString(R.string.error_type_payment_system_2012));

        /* 売り上げ情報連携 */
        _errorMap.put(McPosCenterErrorCodes.E0901, _app.getString(R.string.error_type_payment_system_2019));
        _errorMap.put(McPosCenterErrorCodes.E0902, null);
        _errorMap.put(McPosCenterErrorCodes.E0903, null);
        _errorMap.put(McPosCenterErrorCodes.E0904, null);
        _errorMap.put(McPosCenterErrorCodes.E0905, null);

        /* クレジットCA公開鍵DL */
        _errorMap.put(McPosCenterErrorCodes.E1001, _app.getString(R.string.error_type_credit_3012));
        _errorMap.put(McPosCenterErrorCodes.E1002, _app.getString(R.string.error_type_credit_3012));
        _errorMap.put(McPosCenterErrorCodes.E1003, _app.getString(R.string.error_type_credit_3012));
        _errorMap.put(McPosCenterErrorCodes.E1004, _app.getString(R.string.error_type_credit_3012));
        _errorMap.put(McPosCenterErrorCodes.E1005, _app.getString(R.string.error_type_credit_3012));

        /* カードデータ保護用公開鍵取得 */
        _errorMap.put(McPosCenterErrorCodes.E1101, null);
        _errorMap.put(McPosCenterErrorCodes.E1102, null);
        _errorMap.put(McPosCenterErrorCodes.E1103, null);
        _errorMap.put(McPosCenterErrorCodes.E1104, null);
        _errorMap.put(McPosCenterErrorCodes.E1105, null);

        /* クレジットカード判定 */
        _errorMap.put(McPosCenterErrorCodes.E1201, null);
        _errorMap.put(McPosCenterErrorCodes.E1202, null);
        _errorMap.put(McPosCenterErrorCodes.E1203, null);
        _errorMap.put(McPosCenterErrorCodes.E1204, null);
        _errorMap.put(McPosCenterErrorCodes.E1205, null);

        /* IC・MSクレジットオンラインオーソリ */
        _errorMap.put(McPosCenterErrorCodes.E1301, null);
        _errorMap.put(McPosCenterErrorCodes.E1302, null);
        _errorMap.put(McPosCenterErrorCodes.E1303, null);
        _errorMap.put(McPosCenterErrorCodes.E1304, null);

        /* IC・MSクレジットオンラインオーソリ取消 */
        _errorMap.put(McPosCenterErrorCodes.E1401, null);
        _errorMap.put(McPosCenterErrorCodes.E1402, null);
        _errorMap.put(McPosCenterErrorCodes.E1403, null);
        _errorMap.put(McPosCenterErrorCodes.E1404, null);
        _errorMap.put(McPosCenterErrorCodes.E1405, null);
    }
}
