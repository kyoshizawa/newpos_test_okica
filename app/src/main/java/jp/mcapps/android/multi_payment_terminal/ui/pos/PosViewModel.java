package jp.mcapps.android.multi_payment_terminal.ui.pos;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import timber.log.Timber;

public class PosViewModel extends ViewModel {

    public PosViewModel() {
        super();

        Timber.d("PosViewModel is instantiated.");
    }

    // ホームボタン
    private final MutableLiveData<Boolean> _homeVisible = new MutableLiveData<Boolean>(false);
    public LiveData<Boolean> getHomeVisible() { return _homeVisible; }
    public void setHomeVisible(boolean b) { _homeVisible.setValue(b); }

    // フラグメント自体を戻る用のボタン
    private final MutableLiveData<Boolean> _backVisible = new MutableLiveData<Boolean>(false);
    public LiveData<Boolean> getBackVisible() { return _backVisible; }
    public void setVisible(boolean b) { _backVisible.setValue(b); }

    // スキャンボタン
    private final MutableLiveData<Boolean> _qrScanVisible = new MutableLiveData<Boolean>(false);
    public LiveData<Boolean> getQrScanVisible() { return _qrScanVisible; }
    public void setQRScanVisible(boolean b) { _qrScanVisible.setValue(b); }

    // 検索ボタン
    private final MutableLiveData<Boolean> _searchVisible = new MutableLiveData<Boolean>(false);
    public LiveData<Boolean> getSearchVisible() { return _searchVisible; }
    public void setSearchVisible(boolean b) { _searchVisible.setValue(b); }

    private final MutableLiveData<Boolean> _cartConfirmVisible = new MutableLiveData<Boolean>(false);
    public LiveData<Boolean> getCartConfirmVisible() { return _cartConfirmVisible; }
    public void setCartConfirmVisible(boolean b) { _cartConfirmVisible.setValue(b); }

    private final MutableLiveData<Boolean> _navigateUpVisible = new MutableLiveData<Boolean>(false);
    public LiveData<Boolean> getNavigateUpVisible() { return _navigateUpVisible; }
    public void setNavigateUpVisible(boolean b) { _navigateUpVisible.setValue(b); }

    //戻るボタン、決済画面用、onCreateにて生成
    private final MutableLiveData<Boolean> _paymentSelectVisible = new MutableLiveData<>(false);
    public LiveData<Boolean> getPaymentSelectVisible() {
        return _paymentSelectVisible;
    }
    public void setPeymentSelectVisible(Boolean b) {
        _paymentSelectVisible.setValue(b);
    }
}
