package jp.mcapps.android.multi_payment_terminal.error;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import jp.mcapps.android.multi_payment_terminal.iCAS.iCASClient;

// iCASからのエラーコードをMCのエラーコードに変換する

public class iCASErrorMap {
    public static final String UNDEFINED_ERROR_CODE = "4099";
    private static final HashMap<Integer, String> _errorMap = new HashMap<>();
    private static int _internalErrorCode = 0;

    public static String get(Integer jremErrorCode) {
//        String value = _errorMap.get(jremErrorCode);
//        if(value == null) {
//            value = UNDEFINED_ERROR_CODE;
//            _internalErrorCode = jremErrorCode;
//        } else {
//            _internalErrorCode = 0;
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
                return entry.getKey().toString();
            }
        }

        if(value.equals(UNDEFINED_ERROR_CODE)) {
            // Ras応答の未定義エラーの場合は内部エラーコードを返却
            String erCode = String.valueOf(_internalErrorCode);
            return erCode;
        } else {
            // その他マネーのエラーの場合はnullを返却
            return null;
        }
    }

    static {
        _errorMap.put(iCASClient.ERROR_CLIENT_HTTP, "4090");
        _errorMap.put(iCASClient.ERROR_CLIENT_PROTOCOL, "4091");
        _errorMap.put(iCASClient.ERROR_CLIENT_SEQUENCE, "4092");
        _errorMap.put(iCASClient.ERROR_CLIENT_MSG_UNEXPECTED, "4093");
        _errorMap.put(iCASClient.ERROR_CLIENT_MSG_FORMAT, "4094");
        _errorMap.put(iCASClient.ERROR_CLIENT_MSG_ILLEGAL, "4095");
        _errorMap.put(iCASClient.ERROR_CLIENT_MSG_SERVER, "4096");
        _errorMap.put(iCASClient.ERROR_CLIENT_MSG_TCAP_VERSION, "4097");
    }
}
