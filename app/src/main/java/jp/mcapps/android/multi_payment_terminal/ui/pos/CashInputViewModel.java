package jp.mcapps.android.multi_payment_terminal.ui.pos;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.pos.TaxCalcDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TaxCalcData;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import timber.log.Timber;

public class CashInputViewModel extends ViewModel {
    TaxCalcDao _taxCalcDao = DBManager.getTaxCalcDao();
    private MutableLiveData<Integer> _totalPrice = new MutableLiveData<>(0);

    public void setTotalPrice(Integer totalPrice) {
        _totalPrice.setValue(totalPrice);
    }

    public LiveData<Integer> getTotalPrice() {
        return _totalPrice;
    }

    private MutableLiveData<Boolean> _isFixedAmountPostalOrder = new MutableLiveData<>(false);

    public void setIsFixedAmountPostalOrder(Boolean isFixedAmountPostalOrder) {
        _isFixedAmountPostalOrder.setValue(isFixedAmountPostalOrder);
    }

    public LiveData<Boolean> getIsFixedAmountPostalOrder() {
        return _isFixedAmountPostalOrder;
    }

    public LiveData<String> getTotalPriceText() {
        return Transformations.map(_totalPrice, input -> String.format(Locale.JAPANESE, "%,d", input));
    }

    private final MutableLiveData<Integer> _deposit = new MutableLiveData<>(0);

    public LiveData<Integer> getDeposit() {
        return _deposit;
    }

    public LiveData<String> getDepositText() {
        return Transformations.map(_deposit, input -> {
            if (input == null || input <= 0) {
                return "0";
            }
            return String.format(Locale.JAPANESE, "%,d", input);
        });
    }

    public LiveData<Integer> getOver() {
        return Transformations.map(_deposit, input -> {
            if (input == null || input <= 0) {
                return 0;
            }
            int value = input - _totalPrice.getValue();
            return Math.max(value, 0);
        });
    }

    public LiveData<String> getOverText() {
        return Transformations.map(getOver(), input -> String.format(Locale.JAPANESE, "%,d", input));
    }

    public void fetch() {
        Observable.fromCallable(() -> {
                    TaxCalcData taxCalcData = _taxCalcDao.getData();
                    return taxCalcData;
                })
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {

                            // データ取得が成功した場合の処理
                            _totalPrice.setValue(result.total_amount);
                        },
                        error -> {
                            // エラーハンドリングの処理
                            Timber.e(error);
                        }
                );
    }

    public void addDeposit(String deposit) {
        String depositTmp = _deposit.getValue() + deposit;
        int deposit_i = 0;
        try {
            deposit_i = Integer.parseInt(depositTmp);
        } catch (Exception ex) {
            Timber.e(ex);
            return;
        }
        if (deposit_i < 0 || !McUtils.isCheckMaxAmount(deposit_i)) {
            Timber.e("桁数入力オーバー: 入力値無視（%s）",deposit);
            return;
        }
        _deposit.setValue(deposit_i);
    }

    public void backDeleteDeposit() {
        Integer deposit = _deposit.getValue();
        if (deposit == null || deposit <= 0) {
            return;
        }
        deposit = deposit / 10;
        _deposit.setValue(deposit);
    }

    public void clearDeposit() {
        _deposit.setValue(0);
    }
}
