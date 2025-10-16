package jp.mcapps.android.multi_payment_terminal.error;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// JREMからのエラーコードをMCのエラーコードに変換する

public class JremRasErrorMap {
    public static final String EMONEY_CERTIFICATE_ERROR_CODE = "4901";      // 証明書エラー
    public static final String EMONEY_OPENING_ERROR_CODE = "4903";          // 開局エラー
    public static final String UNDEFINED_ERROR_CODE = "4089";
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
    // ここでは登録されて"いない"コードを判別するために使用する
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

    // E110のエラーだけは状態でエラーコード分けないといけないので定義不可
    static {
        _errorMap.put(JremRasErrorCodes.E82,  "4010");
        _errorMap.put(JremRasErrorCodes.E83,  "4011");
        _errorMap.put(JremRasErrorCodes.E10083,  "14011");
        _errorMap.put(JremRasErrorCodes.E90,  "4012");
        _errorMap.put(JremRasErrorCodes.E91,  "4013");
        _errorMap.put(JremRasErrorCodes.E92,  "4014");
        _errorMap.put(JremRasErrorCodes.E10092, "14014");
        _errorMap.put(JremRasErrorCodes.E93,  "4015");
        _errorMap.put(JremRasErrorCodes.E111, "4016");
        _errorMap.put(JremRasErrorCodes.E305, "4017");
        _errorMap.put(JremRasErrorCodes.E88,  "4018");
        _errorMap.put(JremRasErrorCodes.E10088,  "14018");
        _errorMap.put(JremRasErrorCodes.E96,  "4019");
        _errorMap.put(JremRasErrorCodes.E110, "4020");
        _errorMap.put(JremRasErrorCodes.E95,  "4021");
        _errorMap.put(JremRasErrorCodes.E10095, "14021");
        _errorMap.put(JremRasErrorCodes.E353, "4022");
        _errorMap.put(JremRasErrorCodes.E10353, "14022");
        _errorMap.put(JremRasErrorCodes.E256, "4023");
        _errorMap.put(JremRasErrorCodes.E497, "4024");
        _errorMap.put(JremRasErrorCodes.E498, "4025");
        _errorMap.put(JremRasErrorCodes.E289, "4026");
        _errorMap.put(JremRasErrorCodes.E290, "4027");
        _errorMap.put(JremRasErrorCodes.E385, "4028");
        _errorMap.put(JremRasErrorCodes.E409, "4029");
        _errorMap.put(JremRasErrorCodes.E511, "4030");
        _errorMap.put(JremRasErrorCodes.E772, "4031");
        _errorMap.put(JremRasErrorCodes.E774, "4032");
        _errorMap.put(JremRasErrorCodes.E784, "4033");
        _errorMap.put(JremRasErrorCodes.E785, "4034");
        _errorMap.put(JremRasErrorCodes.E786, "4035");
        _errorMap.put(JremRasErrorCodes.E787, "4036");
        _errorMap.put(JremRasErrorCodes.E788, "4037");
        _errorMap.put(JremRasErrorCodes.E1032,"4038");
        _errorMap.put(JremRasErrorCodes.E1044,"4039");
        _errorMap.put(JremRasErrorCodes.E1281,"4040");
        _errorMap.put(JremRasErrorCodes.E522, "4041");
        _errorMap.put(JremRasErrorCodes.E817, "4042");
        _errorMap.put(JremRasErrorCodes.E1043,"4043");
        _errorMap.put(JremRasErrorCodes.E1282,"4044");
        _errorMap.put(JremRasErrorCodes.E514, "4045");
        _errorMap.put(JremRasErrorCodes.E791, "4046");
//        _errorMap.put(JremRasErrorCodes.E522, "4047");
        _errorMap.put(JremRasErrorCodes.E87,  "4048");
        _errorMap.put(JremRasErrorCodes.E10087,  "14048");
        _errorMap.put(JremRasErrorCodes.E836, "4049");
        _errorMap.put(JremRasErrorCodes.E837, "4050");
        _errorMap.put(JremRasErrorCodes.E504, "4051");
        _errorMap.put(JremRasErrorCodes.E506, "4052");
        _errorMap.put(JremRasErrorCodes.E512, "4053");
        _errorMap.put(JremRasErrorCodes.E816, "4054");
        _errorMap.put(JremRasErrorCodes.E524, "4055");
        _errorMap.put(JremRasErrorCodes.E505, "4056");
        _errorMap.put(JremRasErrorCodes.E519, "4057");
        _errorMap.put(JremRasErrorCodes.E768, "4058");
        _errorMap.put(JremRasErrorCodes.E769, "4059");
        _errorMap.put(JremRasErrorCodes.E770, "4060");
        _errorMap.put(JremRasErrorCodes.E771, "4061");
        _errorMap.put(JremRasErrorCodes.E801, "4062");
        _errorMap.put(JremRasErrorCodes.E876, "4063");
        _errorMap.put(JremRasErrorCodes.E804, "4064");
        _errorMap.put(JremRasErrorCodes.E508, "4065");
        _errorMap.put(JremRasErrorCodes.E509, "4066");
        _errorMap.put(JremRasErrorCodes.E510, "4067");
        _errorMap.put(JremRasErrorCodes.E854, "4068");
        _errorMap.put(JremRasErrorCodes.E855, "4069");
        _errorMap.put(JremRasErrorCodes.E856, "4070");
        _errorMap.put(JremRasErrorCodes.E857, "4071");
        _errorMap.put(JremRasErrorCodes.E869, "4072");
        _errorMap.put(JremRasErrorCodes.E1037,"4073");
        _errorMap.put(JremRasErrorCodes.E1038,"4074");
        _errorMap.put(JremRasErrorCodes.E1039,"4075");
        _errorMap.put(JremRasErrorCodes.E1040,"4076");
        _errorMap.put(JremRasErrorCodes.E386, "4077");
        _errorMap.put(JremRasErrorCodes.E501, "4078");
        _errorMap.put(JremRasErrorCodes.E838, "4079");
        _errorMap.put(JremRasErrorCodes.E839, "4080");
        _errorMap.put(JremRasErrorCodes.E1042,"4081");
        _errorMap.put(JremRasErrorCodes.E503, "4082");
        _errorMap.put(JremRasErrorCodes.E835, "4083");
        _errorMap.put(JremRasErrorCodes.E1027,"4084");
        _errorMap.put(JremRasErrorCodes.E1280,"4085");
        _errorMap.put(JremRasErrorCodes.E517, "4086");
        _errorMap.put(JremRasErrorCodes.E1031,"4087");
        _errorMap.put(JremRasErrorCodes.E1030,"4088");
        _errorMap.put(JremRasErrorCodes.E4901,"4901");
        _errorMap.put(JremRasErrorCodes.E4903,"4903");
    }
}
