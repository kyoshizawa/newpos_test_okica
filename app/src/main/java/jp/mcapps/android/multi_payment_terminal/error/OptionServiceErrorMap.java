package jp.mcapps.android.multi_payment_terminal.error;

import android.app.Application;

import java.util.HashMap;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;

// オプションサービスからのエラーコードを端末のエラーコードに変換する

public class OptionServiceErrorMap {
    private static final Application _app = MainApplication.getInstance();
    public static final String UNDEFINED_ERROR_CODE = _app.getString(R.string.error_type_option_service_5109);
    public static final String UNDEFINED_CONNECTION_ERROR_CODE = _app.getString(R.string.error_type_option_service_5099);
    private static final HashMap<String, String> _errorMap = new HashMap<>();

    public static String get(String errorCode) {
        if (errorCode != null) {
            String value = _errorMap.get(errorCode);
            return value != null ? value : UNDEFINED_ERROR_CODE + "@@@" + errorCode + "@@@";
        } else {
            return UNDEFINED_ERROR_CODE;
        }
    }

    // 明示的に未定義エラーがほしいとき用
    public static String getUndefinedErrorCode(String errorCode) {
            return UNDEFINED_ERROR_CODE + "@@@" + errorCode + "@@@";
    }

    public static String getCommunicationError(String errorCode) {
        if (errorCode != null) {
            String value = _errorMap.get(errorCode);
            return value != null ? value : UNDEFINED_CONNECTION_ERROR_CODE + "@@@" + errorCode + "@@@";
        } else {
            return UNDEFINED_CONNECTION_ERROR_CODE;
        }
    }

    public static String getCommunicationError(int statusCode) {
        switch (statusCode) {
            case 400:
                return _app.getString(R.string.error_type_option_service_5001);
            case 401:
                return _app.getString(R.string.error_type_option_service_5002);
            case 403:
                return _app.getString(R.string.error_type_option_service_5003);
            case 404:
                return _app.getString(R.string.error_type_option_service_5004);
            case 500:
                return _app.getString(R.string.error_type_option_service_5005);
            default:
                return UNDEFINED_CONNECTION_ERROR_CODE + "@@@" + statusCode + "@@@";
        }
    }

    static {

        /* 共通エラー */
        _errorMap.put(OptionServiceErrorCodes.A5098, _app.getString(R.string.error_type_option_service_5020));
        _errorMap.put(OptionServiceErrorCodes.A5099, _app.getString(R.string.error_type_option_service_5021));
    }
}
