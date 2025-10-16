package jp.mcapps.android.multi_payment_terminal.ui.pin;

import androidx.databinding.ObservableArrayList;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.pos.device.SDKException;
import com.pos.device.beeper.Beeper;

import java.util.Collections;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import timber.log.Timber;

public abstract class PinInputViewModel extends ViewModel {
    protected int _pinDigits = 0;
    protected boolean _forceRandomDisable = false;
    public void setPinDigits(int digits) {
        _pinDigits = digits;
    }
    protected String _paymentType = "";
    public String getPaymentType() { return _paymentType; }
    public void setPaymentType(String type) { _paymentType = type; }

    protected final ObservableArrayList<String> _numbers = new ObservableArrayList<String>();
    public ObservableArrayList<String> getNumbers() { return _numbers; }

    protected String _pin = "";
    public String getPin() { return _pin; }

    protected final MutableLiveData<String> _displayedPin = new MutableLiveData<String>("");
    public MutableLiveData<String> getDisplayedPin() { return _displayedPin; }

    {
        for (int i = 0; i < 10; i++) {
            _numbers.add(String.format("%s", i));
        }

        //PIN番号の配置はランダムかどうかを選ばせる予定だったが、クレジットPINがランダム配置固定なのでそれに合わせる
        //if (AppPreference.isPinRandomEnabled()) Collections.shuffle(_numbers);

        // シャッフルするかどうかは継承先で決める
        // Collections.shuffle(_numbers);
    }

    public int getFixedAmount() {
        return Amount.getFixedAmount();
    }

    public void inputNumber(String number) {
        Timber.d("input number");
        if (_pin.length() >= _pinDigits) {
            beep(2500, 100);    //エラー音
            return;
        }

        _pin += number;
        beep(2700, 40); //アクセス音

        _displayedPin.setValue(_displayedPin.getValue() + MainApplication.getInstance().getString(
                R.string.text_credit_card_pin_input_value));
    }

    public void correct() {
        if (_pin.length() == 0) {
            beep(2500, 100);    //エラー音
        } else {
            beep(2700, 40);    //アクセス音
        }
        _pin = "";
        _displayedPin.setValue("");
    }

    //確定押下 継承先でenterBeepを呼び出して効果音鳴動
    public abstract boolean enter();

    public abstract void cancel();

    protected void beep(int frequency, int durationMs) {
        final Beeper beeper = Beeper.getInstance();
        try {
            beeper.beep(frequency, durationMs);
        } catch (SDKException e) {
            Timber.e(e);
        }
    }

    protected void enterBeep() {
        if (_pin.length() == _pinDigits) {
            beep(2700, 100);    //確定音
        }else {
            beep(2500, 100);    //エラー音
        }
    }
}
