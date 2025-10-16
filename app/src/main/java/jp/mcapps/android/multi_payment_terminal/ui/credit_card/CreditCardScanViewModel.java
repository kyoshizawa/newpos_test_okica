package jp.mcapps.android.multi_payment_terminal.ui.credit_card;

import android.os.Handler;
import android.os.Looper;

import java.util.Collections;

import androidx.lifecycle.MutableLiveData;

import io.reactivex.rxjava3.disposables.Disposable;
import jp.mcapps.android.multi_payment_terminal.data.TransactionResults;
import jp.mcapps.android.multi_payment_terminal.thread.emv.Constants.*;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputSeparationPayFDViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.pin.PinInputViewModel;

// PinInputViewModelを継承しているのはデモモードで使うため
// 実際の決済時にはSDKのPIN画面を使う
public class CreditCardScanViewModel extends PinInputViewModel {
    {
        _pinDigits = 4;
        Collections.shuffle(_numbers);
    }

    public Handler _handler = new Handler(Looper.getMainLooper());

    private String _businessType;
    public String getBusinessType() { return _businessType; }
    public void setBusinessType(String type) { _businessType = type; }

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

    private MutableLiveData<CLState> _clState = new MutableLiveData<>(CLState.None);
    public MutableLiveData<CLState> getCLState() {
        return _clState;
    }
    public void setCLState(CLState state) {
        _handler.post(() -> {
            _clState.setValue(state);
        });
    }

    private MutableLiveData<ActivateIF> _activateIF = new MutableLiveData<>(ActivateIF.None);
    public MutableLiveData<ActivateIF> getActivateIF() {
        return _activateIF;
    }
    public void setActivateIF(ActivateIF activateIF) {
        _handler.post(() -> {
            _activateIF.setValue(activateIF);
        });
    }

    @Override
    public boolean enter() {
        enterBeep();    //効果音
        _isFinished.setValue(true);
        return false;
    }

    @Override
    public void cancel() {
    }

    private final MutableLiveData<Boolean> _isDemoPinInput = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isDemoPinInput() {
        return _isDemoPinInput;
    }

    public void isDemoPinInput(boolean b) {
        new Handler(Looper.getMainLooper()).post(() -> {
            _isDemoPinInput.setValue(b);
        });
    }

    private final MutableLiveData<String> _demoPinInputDisplay = new MutableLiveData<>("暗証番号を入力してください。");
    public final MutableLiveData<String> getDemoPinInputDisplay() {
        return _demoPinInputDisplay;
    }

    @Override
    public void correct() {
        if (_pin.length() == 0) {
            beep(2500, 100);    //エラー音
        } else {
            beep(2700, 40);    //アクセス音
        }
        _pin = _pin.replaceFirst(".$", "");
        _displayedPin.setValue(_displayedPin.getValue().replaceFirst(".$", ""));
    }

    private MutableLiveData<AmountInputSeparationPayFDViewModel> _amountInputSeparationPayFDViewModel = new MutableLiveData<>(null);
    public MutableLiveData<AmountInputSeparationPayFDViewModel> getAmountInputSeparationPayFDViewModel() {
        return _amountInputSeparationPayFDViewModel;
    }
    public void setAmountInputSeparationPayFDViewModel(AmountInputSeparationPayFDViewModel viewModel) {
        _amountInputSeparationPayFDViewModel.setValue( viewModel);
    }
    //ADD-E BMT S.Oyama 2024/09/19 フタバ双方向向け改修

}
