package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.Locale;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.data.TransactionResults;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.pos.TaxCalcDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TaxCalcData;
import jp.mcapps.android.multi_payment_terminal.model.TransLogger;

public class CashConfirmViewModel extends ViewModel {
    TaxCalcDao _taxCalcDao = DBManager.getTaxCalcDao();
    private final MutableLiveData<Integer> _deposit = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> _over = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> _totalPrice = new MutableLiveData<>(0);

    public LiveData<Integer> getDeposit(){
        return _deposit;
    }
    public LiveData<String> getDepositString(){
        return Transformations.map(_deposit, input -> String.format(Locale.JAPANESE, "%,d", input));
    }
    public LiveData<Integer> getOver(){
        return _over;
    }
    public LiveData<String> getOverString(){
        return Transformations.map(_over, input -> String.format(Locale.JAPANESE, "%,d", input));
    }
    public LiveData<Integer> getTotalPrice(){
        return _totalPrice;
    }
    public LiveData<String> getTotalPriceString(){
        return Transformations.map(_totalPrice, input -> String.format(Locale.JAPANESE, "%,d", input));
    }

    public void setDeposit(Integer deposit){
        _deposit.setValue(deposit);
    }
    public void setOver(Integer over){
        _over.setValue(over);
    }
    public void setTotalPrice(Integer totalPrice){
        _totalPrice.setValue(totalPrice);
    }

    private MutableLiveData<Boolean> _isRepay = new MutableLiveData<Boolean>(false);
    public MutableLiveData<Boolean> isRepay() {
        return _isRepay;
    }
    public void isRepay(boolean b) {
        _isRepay.setValue(b);
    }

    private MutableLiveData<String> _finishedMessage = new MutableLiveData<>("");
    public MutableLiveData<String> getFinishedMessage() {
        return _finishedMessage;
    }
    public void setFinishedMessage(String message) {
        _finishedMessage.setValue(message);
    }

    private MutableLiveData<Boolean> _isFinished = new MutableLiveData<Boolean>(false);
    public MutableLiveData<Boolean> isFinished() {
        return _isFinished;
    }
    public void isFinished(boolean b) {
        _isFinished.setValue(b);
    }
    private MutableLiveData<TransactionResults> _result = new MutableLiveData<>();
    public MutableLiveData<TransactionResults> getResult() {
        return _result;
    }
    public void setResult(TransactionResults result) {
        _result.setValue(result);
    }

    public void fetchTotalPrice(){
        new Thread(() -> {
            TaxCalcData taxCalcData =  _taxCalcDao.getData();
            Log.d("taxCalcCount" , String.valueOf(_taxCalcDao.getCount()));
            _totalPrice.postValue(taxCalcData.total_amount);
        }).start();
    }
}
