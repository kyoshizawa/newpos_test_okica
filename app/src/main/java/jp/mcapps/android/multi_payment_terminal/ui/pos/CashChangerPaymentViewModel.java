package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.util.Log;

import androidx.annotation.RawRes;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.Locale;
import java.util.Map;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.TransactionResults;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.pos.TaxCalcDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TaxCalcData;
import jp.mcapps.android.multi_payment_terminal.devices.DepositAmountListener;
import jp.mcapps.android.multi_payment_terminal.devices.DepositErrorListener;
import jp.mcapps.android.multi_payment_terminal.devices.GloryCashChanger;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import timber.log.Timber;

public class CashChangerPaymentViewModel extends ViewModel implements LifecycleObserver {
    // TODO: Implement the ViewModel
    private SoundManager _soundManager = SoundManager.getInstance();

    public void start(int paymentAmount, boolean isRepay) {
        // 念のため入力金額とお釣りを初期化（初期化しなくても問題なさそう）
        setCashAmount(0);
        setChangeAmount(0);

        // 支払金額の設定
        setPaymentAmount(paymentAmount);

        GloryCashChanger gloryCashChanger = GloryCashChanger.getInstance();
        if (gloryCashChanger == null) {
            // これ以降の処理は何もできない
            return;
        }

        gloryCashChanger.setDepositAmountListener(_depositAmountListener);
//        gloryCashChanger.setCashCountListener(_cashCountListener);
        if (gloryCashChanger.connect() == false) {
            return;
        }
        if (isRepay) {

        }else {
            makeSound(R.raw.money_request);
            // 入金開始
            gloryCashChanger.beginDeposit();
        }
    }

    private final MutableLiveData<Integer> _paymentAmount = new MutableLiveData<>(0);   // 支払額
    public MutableLiveData<Integer> getPaymentAmount() {
        return _paymentAmount;
    }
    public void setPaymentAmount(int value) {
        _paymentAmount.postValue(value);
    }

    private final MutableLiveData<Integer> _cashAmount = new MutableLiveData<>(0);  // 入金額
    public MutableLiveData<Integer> getCashAmount() {
        return _cashAmount;
    }
    public void setCashAmount(int value) {
        _cashAmount.postValue(value);
    }
    public LiveData<String> getCashAmountString(){
        return Transformations.map(_cashAmount, input -> String.format(Locale.JAPANESE, "%,d", input));
    }

    private final MutableLiveData<Integer> _changeAmount = new MutableLiveData<>(0);    // 釣銭額
    public MutableLiveData<Integer> getChangeAmount() {
        return _changeAmount;
    }
    public void setChangeAmount(int value) {
        _changeAmount.postValue(value);
    }

    private int _cashAmountPay = 0;
    public void setCashAmountPay(int value){
        _cashAmountPay = value;
    }
    public int getCashAmountPay(){return _cashAmountPay;}

    private int _cashChangePay = 0;
    public void setCashChangePay(int value){
        _cashChangePay = value;
    }
    public int getCashChangePay(){return _cashChangePay;}

//    private final MutableLiveData<Integer> _connectStatus = new MutableLiveData<>(0);
//    public MutableLiveData<Integer> getConnectStatus() {
//        return _connectStatus;
//    }
//    public void setConnectStatus(int eventType) {
//        _connectStatus.postValue(eventType);
//    }

    private final MutableLiveData<Map<String, Integer>> _cashCount = new MutableLiveData<>(null);    // 残金
    public MutableLiveData<Map<String, Integer>> getCashCount() {
        return _cashCount;
    }
    public void setCashCount(Map<String, Integer> data) {
        _cashCount.postValue(data);
    }

    private final DepositAmountListener _depositAmountListener = new DepositAmountListener() {
        @Override
        public void onCChangerDeposit(int amount) {
            // 入金額を更新
            setCashAmount(amount);
            setCashAmountPay(amount);
            // お釣りを更新
            int change = amount - getPaymentAmount().getValue();
            if (change < 0) {
                change = 0;
            }
            setChangeAmount(change);
            setCashChangePay(change);
        }
    };

//    private final CashChangerConnectionListener _cashChangerConnectionListener = new CashChangerConnectionListener() {
//        @Override
//        public void onConnection(int eventType) {
//            setConnectStatus(eventType);
//        }
//    };

//    private final CashCountListener _cashCountListener = new CashCountListener() {
//        @Override
//        public void onCChangerCashCount(Map<String, Integer> data) {
//            setCashCount(data);
//        }
//    };

    private final DepositErrorListener _depositErrorListener = new DepositErrorListener() {
        @Override
        public void onErrorDeposit(int errorCode, int extendErrorCode) {
            //
        }
    };
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
    private void makeSound(@RawRes int soundId) {
        // 決済音の音量を設定
        float volume = AppPreference.getSoundPaymentVolume() / 10f;
        _soundManager.load(MainApplication.getInstance(), soundId, 1);

        _soundManager.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            _soundManager.play(sampleId, volume, volume, 1, 0, 1);
        });
    }
}