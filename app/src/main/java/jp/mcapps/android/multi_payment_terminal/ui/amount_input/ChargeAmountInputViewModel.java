package jp.mcapps.android.multi_payment_terminal.ui.amount_input;

import static java.lang.Integer.parseInt;

import static jp.mcapps.android.multi_payment_terminal.data.okica.Constants.COMPANY_CODE_BUPPAN;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.okica.ICMaster;
import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import timber.log.Timber;

@SuppressWarnings("ALL")
public class ChargeAmountInputViewModel extends ViewModel {

    private final MutableLiveData<Integer> _amount = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getAmount() { return _amount; }

    public void inputNumber(String stringNumber) {
        final int number = parseInt(stringNumber);
        final int currentVal = _amount.getValue();

        /* チャージ限度額 */
        int chargeMaxAmount;
        if (!AppPreference.isDemoMode()) {
            final ICMaster.Activator activator = MainApplication.getInstance().getOkicaICMaster().getData().getActivator(COMPANY_CODE_BUPPAN);
            chargeMaxAmount = activator.getPurseLimitAmount();
        } else {
            chargeMaxAmount = 30_000;
        }

        final int amount = currentVal == 0 ? number : (currentVal*10) + number ;

        if (!checkDigits(amount, chargeMaxAmount)) {
            Timber.d("Out of range amount");
            return;
        }

        _amount.setValue(amount);
    }

    public void correct() {
        _amount.setValue(0);
    }

    public void enter() {
    }

    public void cancel() {
    }

    private boolean checkDigits(int amount, int chargeMaxAmount) {
        return amount >= 1000 && amount <=chargeMaxAmount;
    }
}
