package jp.mcapps.android.multi_payment_terminal.data;

import jp.mcapps.android.multi_payment_terminal.database.history.radio.RadioData;
import jp.mcapps.android.multi_payment_terminal.service.GetRadioService;

public class CurrentRadio {
    private static RadioData _radioData = null;
    public static RadioData getData() {
        return _radioData;
    }
    public static void setData(RadioData data) {
        _radioData = data;
    }

    private static Integer _imageLevel = 0;

    public static Integer getImageLevel() {
        return _imageLevel;
    }
    public static void setImageLevel(int resourceId) {
        _imageLevel = resourceId;
    }

    public static void setAirplaneMode() {
        if (_imageLevel != GetRadioService.AIRPLANE_MODE) {
            _imageLevel = GetRadioService.AIRPLANE_MODE;
        }
    }

    public static void cancelAirplaneMode() {
        if (_imageLevel == GetRadioService.AIRPLANE_MODE) {
            _imageLevel = 0;
        }
    }
}
