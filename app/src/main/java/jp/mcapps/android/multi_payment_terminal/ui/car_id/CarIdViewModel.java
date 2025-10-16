package jp.mcapps.android.multi_payment_terminal.ui.car_id;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.model.McTerminal;
import timber.log.Timber;

public class CarIdViewModel extends ViewModel {
    private static final int MAX_CAR_ID_DIGITS = 4;
    private McTerminal _terminal = new McTerminal();

    private final MutableLiveData<String> _carId = new MutableLiveData<String>("");
    public MutableLiveData<String> getCarId() { return _carId; }

    public void inputNumber(String number) {
        if (_carId.getValue().length() >= MAX_CAR_ID_DIGITS) {
            Timber.d("桁数オーバー");
            return;
        }

        final String carNumber = _carId.getValue() + number;
        _carId.setValue(carNumber);
    }

    public void correct() {
        _carId.setValue("");
    }

    public String enter() {
        // デモモードの場合、号機番号入力処理は行わない
        if (AppPreference.isDemoMode()) {
            return null;
        }

        final String errorCode = _terminal.setCar(Integer.parseInt(_carId.getValue()));

        if (errorCode == null) {
            //売上情報連携
            _terminal.postPayment();
        }

        return errorCode;
    }
}
