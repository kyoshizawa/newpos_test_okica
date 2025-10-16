package jp.mcapps.android.multi_payment_terminal.ui.amount_input;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import timber.log.Timber;

import static java.lang.Integer.parseInt;

public class AmountInputLt27ViewModel extends ViewModel {
    private static final int MAX_AMOUNT_DIGITS = 6;

    private final IFBoxManager _ifBoxManager;

    public AmountInputLt27ViewModel(IFBoxManager ifBoxManager) {
        _ifBoxManager = ifBoxManager;
    }

    private final MutableLiveData<Integer> _changeAmount = new MutableLiveData<>(0); // 入力金額
    public MutableLiveData<Integer> getChangeAmount() {
        return _changeAmount;
    }
    public void setChangeAmount(int value) {
        _changeAmount.setValue(value);
    }

    private final MutableLiveData<Integer> _paymentAmount = new MutableLiveData<>(0); // 決済金額
    public MutableLiveData<Integer> getPaymentAmount() {
        return _paymentAmount;
    }
    public void setPaymentAmount(int value) {
        _paymentAmount.setValue(value);
    }

    private final MutableLiveData<Integer> _cashAmount = new MutableLiveData<>(0); //現金分割
    public MutableLiveData<Integer> getCashAmount() {
        return _cashAmount;
    }
    public void setCashAmount(int value) {
        _cashAmount.setValue(value);
    }

    {
        setPaymentAmount(Amount.getTotalAmount());
        setCashAmount(Amount.getCashAmount());
    }

    public void fetchMeterCharge() {
        _ifBoxManager.fetchMeter()
                .timeout(5, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    _paymentAmount.setValue(Amount.getTotalAmount());
                }, error -> {
                });
    }

    // 数字キータップ
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

    // ->タップ
    public void changeBack() {
        final int currentAmount = _changeAmount.getValue();
        final int amount = AppPreference.isInput1yenEnabled()
                ? currentAmount/10
                : (currentAmount/100 != 0) ? (currentAmount/100)*10 : 0;

        setChangeAmount(amount);
    }

    // 決済金額タップ
    public boolean paymentAmount() {
        final int currentAmount = _changeAmount.getValue();
        // 20240119 支払済金額対応
        int currentPayment = Amount.getMeterCharge() - Amount.getTicketAmount(); // チケット金額が変更されている可能性があるためここで再度チェック
        if( IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D) ) {// OKABE仕様
            currentPayment = currentPayment - Amount.getPaidAmount(); // 支払済み金額チェック
        }

        if ( currentAmount <= currentPayment ) {
            Amount.setCashAmount(currentPayment - currentAmount); // 変更する決済金額 <= 現在のメーター金額 - チケット分割 の場合のみ値をセット
            return true;
        } else {
            setPaymentAmount(Amount.getTotalAmount());
            return false;
        }
    }

    // 現金分割タップ
    public boolean cashAmount() {
        final int currentAmount = _changeAmount.getValue();
        // 20240119 支払済金額対応
        int currentPayment = Amount.getMeterCharge() - Amount.getTicketAmount(); // チケット金額が変更されている可能性があるためここで再度チェック

        if( IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D) ) {// OKABE仕様
            currentPayment = currentPayment - Amount.getPaidAmount(); // 支払済み金額チェック
        }

        if ( currentAmount < currentPayment ) {
            Amount.setCashAmount(currentAmount); // 変更する現金分割金額 < 現在のメーター金額 - チケット分割 の場合のみ値をセット
            return true;
        } else {
            setPaymentAmount(Amount.getTotalAmount());
            return false;
        }
    }

    // 分割取消タップ
    public void reset() {
        Amount.setCashAmount(0);
    }

    private boolean checkDigits(int amount) {
        return (amount / ((int) Math.pow(10, MAX_AMOUNT_DIGITS))) == 0;
    }
}
