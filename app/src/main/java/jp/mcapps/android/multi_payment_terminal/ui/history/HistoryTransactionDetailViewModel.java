package jp.mcapps.android.multi_payment_terminal.ui.history;

import androidx.databinding.ObservableBoolean;
import androidx.lifecycle.ViewModel;

import io.reactivex.rxjava3.disposables.Disposable;

public class HistoryTransactionDetailViewModel extends ViewModel {
    private ObservableBoolean _isSuccess = new ObservableBoolean(false);
    public ObservableBoolean getIsSuccess() { return _isSuccess; }
    public void setIsSuccess(boolean b) { _isSuccess.set(b); }

    private final ObservableBoolean _cancelable = new ObservableBoolean(true);
    public ObservableBoolean getCancelable() { return _cancelable; }
    public void setCancelable(boolean b) { _cancelable.set(b); }

    private final ObservableBoolean _isCredit = new ObservableBoolean(false);
    public ObservableBoolean getIsCredit() { return _isCredit; }
    public void setIsCredit(boolean b) { _isCredit.set(b); }

    private final ObservableBoolean _isQr = new ObservableBoolean(false);
    public ObservableBoolean getIsQr() { return _isQr; }
    public void setIsQr(boolean b) { _isQr.set(b); }

    private final ObservableBoolean _isWatari = new ObservableBoolean(false);
    public ObservableBoolean getIsWatari() { return _isWatari; }
    public void setIsWatari(boolean b) { _isWatari.set(b); }

    private final ObservableBoolean _isNoQrBrandName = new ObservableBoolean(false);
    public ObservableBoolean getIsNoQrBrandName() { return _isNoQrBrandName; }
    public void setIsNoQrBrandName(boolean b) { _isNoQrBrandName.set(b); }

    private final ObservableBoolean _hasBalance = new ObservableBoolean(true);
    public ObservableBoolean getHasBalance() { return _hasBalance; }
    public void setHasBalance(boolean b) { _hasBalance.set(b); }

    private final ObservableBoolean _isUnFinished = new ObservableBoolean(false);
    public ObservableBoolean getIsUnFinished() { return _isUnFinished; }
    public void setIsUnFinished(boolean b) { _isUnFinished.set(b); }

    private final ObservableBoolean _isVisibility = new ObservableBoolean(false);
    public ObservableBoolean getIsVisibility() { return _isVisibility; }
    public void setIsVisibility(boolean b) { _isVisibility.set(b); }

    private final ObservableBoolean _isCash = new ObservableBoolean(false);
    public ObservableBoolean getIsCash() { return _isCash; }
    public void setIsCash(boolean b) { _isCash.set(b); }

    private final ObservableBoolean _isPostalOrder = new ObservableBoolean(false);
    public ObservableBoolean getIsPostalOrder() { return _isPostalOrder; }
    public void setIsPostalOrder(boolean b) { _isPostalOrder.set(b); }

    private final ObservableBoolean _isManual = new ObservableBoolean(false);
    public ObservableBoolean getIsManual() { return _isManual; }
    public void setIsManual(boolean b) { _isManual.set(b); }

    //ADD-S BMT S.Oyama 2024/11/18 フタバ双方向向け改修
    public static Disposable _meterDataV4InfoDisposable = null;
    public static Disposable _meterDataV4ErrorDisposable = null;
    //ADD-E BMT S.Oyama 2024/11/18 フタバ双方向向け改修

}
