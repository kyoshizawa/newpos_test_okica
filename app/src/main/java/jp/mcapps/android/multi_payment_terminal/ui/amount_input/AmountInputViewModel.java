package jp.mcapps.android.multi_payment_terminal.ui.amount_input;

import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

import androidx.databinding.ObservableBoolean;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
//import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import timber.log.Timber;

import static java.lang.Integer.parseInt;

@SuppressWarnings("ALL")
public class AmountInputViewModel extends ViewModel {
    private static final int MAX_AMOUNT_DIGITS = 6;
    private static final int MAX_AMOUNT_DIGITS_LARGE = 7;

    public static class InputModes {
        public static final String INCREASE = "INCREASE";
        public static final String DECREASE = "DECREASE";
        public static final String FLAT_RATE = "FLAT_RATE";
    };

    public static class AmountInputConverters {
        public static Boolean isSelectedIncrease(String mode) {
            return mode == InputModes.INCREASE;
        }

        public static Boolean isSelectedDecrease(String mode) {
            return mode == InputModes.DECREASE;
        }

        public static Boolean isSelectedFlatRate(String mode) {
            return mode == InputModes.FLAT_RATE;
        }

        public static Boolean isNoSelected(String mode) {
            return mode == null;
        }
    }


    public AmountInputViewModel() {

    }

    private MutableLiveData<String> _inputMode = new MutableLiveData<>(InputModes.INCREASE);

    public MutableLiveData<String> getInputMode() {
        return _inputMode;
    }

    public void setInputMode(String mode) {
        _inputMode.setValue(mode);
    }

    private final MutableLiveData<Integer> _totalChangeAmount = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getTotalChangeAmount() { return _totalChangeAmount; }

    public void setTotalChangeAmount(int amount) {
        _totalChangeAmount.setValue(amount);
    }
    public void addTotalChange(int amount) {
        _totalChangeAmount.setValue(_totalChangeAmount.getValue() + amount);
    }

    private final MutableLiveData<Integer> _meterCharge = new MutableLiveData<>(Amount.getMeterCharge());
    public MutableLiveData<Integer> getMeterCharge() { return _meterCharge; }
    public void fetchMeterCharge() {
//        _ifBoxManager.fetchMeter()
//                .timeout(5, TimeUnit.SECONDS)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(() -> {
//                    _meterCharge.setValue(Amount.getMeterCharge());
//                }, error -> {
//                });
    }

    private final MutableLiveData<Integer> _flatRateAmount = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getFlatRateAmount() { return _flatRateAmount; }
    public void setFlatRateAmount(int amount) {
        _flatRateAmount.setValue(amount);
    }

    private final MutableLiveData<Integer> _changeAmount = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getChangeAmount() { return _changeAmount; }
    public void setChangeAmount(int amount) {
        _changeAmount.setValue(amount);
    }

    private final MutableLiveData<String> _changeHistory = new MutableLiveData<String>("");
    public MutableLiveData<String> getChangeHistory() { return _changeHistory; }
    public void setChangeHistory(String value) {
        _changeHistory.setValue(value);
    }
    public void addChangeHistory(String prefix, int value) {
        final String history =
                _changeHistory.getValue() +  " " + prefix + NumberFormat.getNumberInstance().format(value);

        _changeHistory.setValue(history);
    }

    { // initializer
        setFlatRateAmount(Amount.getFlatRateAmount());
        setTotalChangeAmount(Amount.getTotalChangeAmount());

        if (Amount.getTotalChangeAmount() != 0) {
            addChangeHistory(Amount.getTotalChangeAmount() > 0 ? "+" : "-", Amount.getTotalChangeAmount());
        }
    }

    public void inputNumber(String stringNumber) {
        final int number = parseInt(stringNumber);
        final int currentVal = _changeAmount.getValue();

        final int shift = (int) Math.pow(10, stringNumber.length());

        final int changeAmount = AppPreference.isInput1yenEnabled()
                ? (currentVal*shift) + number
                : currentVal != 0 ? (currentVal + number)*shift : number * shift;

        if (!checkDigits(changeAmount)) {
            Timber.e("Out of range changeAmount");
            return;
        }

        setChangeAmount(changeAmount);
    }

    public void correct() {
        setChangeAmount(0);
    }

    public void increase() {
        final int changeAmount = _changeAmount.getValue();
        if (changeAmount <= 0) return;

        final int baseAmount = _flatRateAmount.getValue() <= 0
                ? Amount.getMeterCharge()
                : _flatRateAmount.getValue();

        final int currentTotal = baseAmount + _totalChangeAmount.getValue();

        if (!checkDigits(currentTotal + changeAmount)) {
            Timber.e("Out of range totalAmount");
            return;
        }

        addTotalChange(+changeAmount);
        addChangeHistory("+", changeAmount);
        setChangeAmount(0);
    }

    public void decrease() {
        final int changeAmount = _changeAmount.getValue();
        if (changeAmount <= 0) return;

        final int baseAmount = _flatRateAmount.getValue() <= 0
                ? Amount.getMeterCharge()
                : _flatRateAmount.getValue();

        final int currentTotal = baseAmount + _totalChangeAmount.getValue();

        if (currentTotal - changeAmount < 0) {
            Timber.e("Out of range totalAmount");
            return;
        }

        addTotalChange(-changeAmount);
        addChangeHistory("-", changeAmount);
        setChangeAmount(0);
    }

    public void flatRate() {
        final int changeAmount = _changeAmount.getValue();

        setFlatRateAmount(changeAmount);
        setChangeHistory("");
        setTotalChangeAmount(0);
        setChangeAmount(0);
    }

    public void enter() {
        Amount.setTotalChangeAmount(_totalChangeAmount.getValue());
        Amount.setFlatRateAmount(_flatRateAmount.getValue());
    }

    public void cancel() {
    }

    public void reset() {
        setChangeHistory("");
        setFlatRateAmount(0);
        setTotalChangeAmount(0);
    }

    public void changeBack() {
        final int currentAmount = _changeAmount.getValue();
        final int amount = AppPreference.isInput1yenEnabled()
                ? currentAmount/10
                : (currentAmount/100 != 0) ? (currentAmount/100)*10 : 0;

        setChangeAmount(amount);
    }

    public void apply() {
        final String mode = _inputMode.getValue();

        if (mode == null) return;
        else if (mode.equals(InputModes.INCREASE))  increase();
        else if (mode.equals(InputModes.DECREASE))  decrease();
        else if (mode.equals(InputModes.FLAT_RATE)) flatRate();

//        _inputMode.setValue(null);
    }

    private boolean checkDigits(int amount) {
        if (AppPreference.getMaxAmountType() == AppPreference.MaxAmountType.LARGE) {
            return (amount / ((int) Math.pow(10, MAX_AMOUNT_DIGITS_LARGE))) == 0;
        }

        return (amount / ((int) Math.pow(10, MAX_AMOUNT_DIGITS))) == 0;
    }
}
