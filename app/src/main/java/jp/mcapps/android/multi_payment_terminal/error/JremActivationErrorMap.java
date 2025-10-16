package jp.mcapps.android.multi_payment_terminal.error;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// JREMからのエラーコードをMCのエラーコードに変換する

public class JremActivationErrorMap {
    public static final String EMONEY_ACTIVATE_ERROR_CODE = "4902";
    public static final String UNDEFINED_ERROR_CODE = "4009";
    private static final HashMap<String, String> _errorMap = new HashMap<>();
    private static String _internalErrorCode = null;

    public static String get(String jremErrorCode) {
//        String value = _errorMap.get(jremErrorCode);
//
//        if(value == null) {
//            value = UNDEFINED_ERROR_CODE;
//            _internalErrorCode = jremErrorCode;
//        } else {
//            _internalErrorCode = null;
//        }
//        return value;
        if (jremErrorCode != null) {
            String value = _errorMap.get(jremErrorCode);
            return value != null ? value : UNDEFINED_ERROR_CODE + "@@@" + jremErrorCode + "@@@";
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
            // 未定義エラーの場合は内部エラーコードを返却
            return _internalErrorCode;
        } else {
            // その他マネーのエラーの場合はnullを返却
            return null;
        }
    }

    static {
        _errorMap.put(JremActivationErrorCodes.T_E0110, "4001");
        _errorMap.put(JremActivationErrorCodes.T_E0125, "4002");
        _errorMap.put(JremActivationErrorCodes.T_E0128, "4003");
        _errorMap.put(JremActivationErrorCodes.T_E0215, "4004");
        _errorMap.put(JremActivationErrorCodes.T_E0216, "4005");
        _errorMap.put(JremActivationErrorCodes.T_E0221, "4006");
        _errorMap.put(JremActivationErrorCodes.T_E0222, "4007");
        _errorMap.put(JremActivationErrorCodes.T_E0225, "4008");
        _errorMap.put(JremActivationErrorCodes.E4902, "4902");
    }
}
