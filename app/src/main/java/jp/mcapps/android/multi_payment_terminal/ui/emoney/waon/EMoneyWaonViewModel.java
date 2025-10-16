package jp.mcapps.android.multi_payment_terminal.ui.emoney.waon;

import android.graphics.drawable.Drawable;

import androidx.databinding.ObservableBoolean;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputSeparationPayFDViewModel;

public class EMoneyWaonViewModel extends ViewModel {
    public enum Phases {
        WAITING,     // カードタッチ待ち
        CONNECTING,  // 処理中
        COMPLETED,   // 正常終了
        NOT_ENOUGH,  // 残高不足
        ERROR,       // エラー
    }
    private final MutableLiveData<Integer> _balance = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getBalance() { return _balance; }

    private final ObservableBoolean _isWaiting = new ObservableBoolean();
    public ObservableBoolean isWaiting() { return _isWaiting; }

    private final ObservableBoolean _isConnecting = new ObservableBoolean();
    public ObservableBoolean isConnecting() { return _isConnecting; }

    private final ObservableBoolean _isCompleted = new ObservableBoolean();
    public ObservableBoolean isCompleted() { return _isCompleted; }

    private final ObservableBoolean _isNotEnough = new ObservableBoolean();
    public ObservableBoolean isNotEnough() { return _isNotEnough; }

    private final ObservableBoolean _isError = new ObservableBoolean();
    public ObservableBoolean isError() { return _isError; }

    private final ObservableBoolean _showWithCashAndCancelBtn = new ObservableBoolean(false);
    public ObservableBoolean getShowWithCashAndCancelBtn() { return _showWithCashAndCancelBtn; }
    public void setShowWithCashAndCancelBtn(boolean b) { _showWithCashAndCancelBtn.set(b); }

    private final ObservableBoolean _cancelable = new ObservableBoolean(true);
    public ObservableBoolean getCancelable() {
        return _cancelable;
    }
    public void setCancelable(boolean b) {
        _cancelable.set(b);
    }

    private final MutableLiveData<String> _lcd1 = new MutableLiveData<>("");
    public MutableLiveData<String> getLcd1() { return _lcd1; }
    public void setLcd1(String message) { _lcd1.setValue(message); }

    private final MutableLiveData<String> _lcd2 = new MutableLiveData<>("");
    public MutableLiveData<String> getLcd2() { return _lcd2; }
    public void setLcd2(String message) { _lcd2.setValue(message); }

    private final MutableLiveData<String> _lcd3 = new MutableLiveData<>("");
    public MutableLiveData<String> getLcd3() { return _lcd3; }
    public void setLcd3(String message) { _lcd3.setValue(message); }

    private final MutableLiveData<String> _lcd4 = new MutableLiveData<>("");
    public MutableLiveData<String> getLcd4() { return _lcd4; }
    public void setLcd4(String message) { _lcd4.setValue(message); }

    private final MutableLiveData<Integer> _amount = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getAmount() { return _amount; }
    public void setAmount(Integer amount) { _amount.setValue(amount); }

    private final ObservableBoolean _showHistoryPrint = new ObservableBoolean(false);
    public ObservableBoolean getShowHistoryPrint() { return _showHistoryPrint; }
    public void setShowHistoryPrint(boolean b) { _showHistoryPrint.set(b); }

    private Integer _radioLevel = null;

    public Integer getRadioLevel() {
        return _radioLevel;
    }

    public void setRadioLevel(int radioLevel) {
        _radioLevel = radioLevel;
    }

    private final MutableLiveData<Integer> _radioLevelImage = new MutableLiveData<>(null);

    public MutableLiveData<Integer> getRadioLevelImage() {
        return _radioLevelImage;
    }

    public void setRadioLevelImage(String type) {
        int radioLevelImage;
        switch (type) {
            case "3G":
                if (_radioLevel == 0 || _radioLevel == 1) {
                    radioLevelImage =R.drawable.ic_radio_level_low;
                } else if (_radioLevel == 2 || _radioLevel == 3) {
                    radioLevelImage = R.drawable.ic_radio_level_middle;
                } else {
                    radioLevelImage = R.drawable.ic_radio_level_high;
                }
                break;
            case "LTE":
                if (_radioLevel == 0) {
                    radioLevelImage = R.drawable.ic_radio_level_low;
                } else if (_radioLevel == 1) {
                    radioLevelImage = R.drawable.ic_radio_level_middle;
                } else {
                    radioLevelImage = R.drawable.ic_radio_level_high;
                }
                break;
            case "WIFI":
                if (_radioLevel == 0) {
                    radioLevelImage = R.drawable.ic_radio_level_low;
                } else if (_radioLevel == 1 || _radioLevel == 2) {
                    radioLevelImage = R.drawable.ic_radio_level_middle;
                } else {
                    radioLevelImage = R.drawable.ic_radio_level_high;
                }
                break;
            default :
                radioLevelImage = R.drawable.ic_radio_level_low;
        }
        _radioLevelImage.setValue(radioLevelImage);
    }

    {
        setPhase(Phases.WAITING);
    }

    public void setPhase(Phases phase) {
        _isWaiting.set(phase == Phases.WAITING);
        _isConnecting.set(phase == Phases.CONNECTING);
        _isCompleted.set(phase == Phases.COMPLETED);
        _isNotEnough.set(phase == Phases.NOT_ENOUGH);
        _isError.set(phase == Phases.ERROR);
    }

    //ADD-S BMT S.Oyama 2024/09/19 フタバ双方向向け改修
    private MutableLiveData<AmountInputSeparationPayFDViewModel> _amountInputSeparationPayFDViewModel = new MutableLiveData<>(null);
    public MutableLiveData<AmountInputSeparationPayFDViewModel> getAmountInputSeparationPayFDViewModel() {
        return _amountInputSeparationPayFDViewModel;
    }
    public void setAmountInputSeparationPayFDViewModel(AmountInputSeparationPayFDViewModel viewModel) {
        _amountInputSeparationPayFDViewModel.setValue( viewModel);
    }
    //ADD-E BMT S.Oyama 2024/09/19 フタバ双方向向け改修

}
