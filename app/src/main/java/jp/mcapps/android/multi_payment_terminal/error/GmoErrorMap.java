package jp.mcapps.android.multi_payment_terminal.error;

import android.app.Application;

import java.util.HashMap;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;

// GMOからのエラーコードをMCのエラーコードに変換する

public class GmoErrorMap {
    public static final String UNDEFINED_ERROR_CODE = "3319";
    public static final String INTERNAL_ERROR_CODE = UNDEFINED_ERROR_CODE + "@@@端末内部エラー@@@";
    public static Application _app = MainApplication.getInstance();

    private static final HashMap<String, String> _errorMap = new HashMap<>();

    public static String get(String gmoErrorCode) {
        if (gmoErrorCode != null) {
            String value = _errorMap.get(gmoErrorCode);
            return value != null ? value : UNDEFINED_ERROR_CODE + "@@@" + gmoErrorCode + "@@@";
        } else {
            return UNDEFINED_ERROR_CODE + "@@@@@@";
        }
    }

    static {
        _errorMap.put(GmoErrorCodes.S0011, _app.getString(R.string.error_type_qr_3301));
        _errorMap.put(GmoErrorCodes.S0012, _app.getString(R.string.error_type_qr_3302));
        _errorMap.put(GmoErrorCodes.S0013, _app.getString(R.string.error_type_qr_3303));
        _errorMap.put(GmoErrorCodes.S0014, null);
        _errorMap.put(GmoErrorCodes.S0015, _app.getString(R.string.error_type_qr_3306));
        _errorMap.put(GmoErrorCodes.S0016, _app.getString(R.string.error_type_qr_3307));
        _errorMap.put(GmoErrorCodes.S0017, null);
        _errorMap.put(GmoErrorCodes.S0018, _app.getString(R.string.error_type_qr_3304));
        _errorMap.put(GmoErrorCodes.S0019, _app.getString(R.string.error_type_qr_3310));
        _errorMap.put(GmoErrorCodes.S0020, _app.getString(R.string.error_type_qr_3311));
        _errorMap.put(GmoErrorCodes.S0021, null);
        _errorMap.put(GmoErrorCodes.S0051, null);
        _errorMap.put(GmoErrorCodes.S0052, _app.getString(R.string.error_type_qr_3312));
        _errorMap.put(GmoErrorCodes.S0053, _app.getString(R.string.error_type_qr_3305));
        _errorMap.put(GmoErrorCodes.S0055, _app.getString(R.string.error_type_qr_3308));
        _errorMap.put(GmoErrorCodes.S0059, _app.getString(R.string.error_type_qr_3313));
        _errorMap.put(GmoErrorCodes.S0097, null);
        _errorMap.put(GmoErrorCodes.S0098, _app.getString(R.string.error_type_qr_3314));
        _errorMap.put(GmoErrorCodes.S0099, _app.getString(R.string.error_type_qr_3309));
    }
}
