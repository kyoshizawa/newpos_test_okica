package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TicketIssueViewModel extends ViewModel {

    private final Handler _handler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<Boolean> _isProcessing = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isProcessing() {
        return _isProcessing;
    }
    public void setProcessing(boolean b) {
        _handler.post(() -> {
            _isProcessing.setValue(b);
        });
    }

    // 結果
    private final MutableLiveData<Boolean> _issueResult = new MutableLiveData<Boolean>(true);
    public MutableLiveData<Boolean> isIssueResult() { return _issueResult; }
    public void setIssueResult(boolean value) {
        _issueResult.setValue(value);
    }

    // チケット名
    private final MutableLiveData<String> _ticketName = new MutableLiveData<String>("");
    public MutableLiveData<String> getTicketName() { return _ticketName; }
    public void setTicketName(String ticketName) {
        _ticketName.setValue(ticketName);
    }

    // チケット便情報
    private final MutableLiveData<String> _ticketTripInfo = new MutableLiveData<String>("");
    public MutableLiveData<String> getTicketTripInfo() { return _ticketTripInfo; }
    public void setTicketTripInfo(String embarkName, String departureTime) {
        String ticketName = String.format("%s %s発",embarkName ,departureTime.substring(0,5));
        _ticketTripInfo.setValue(ticketName);
    }

    // 残席
    private final MutableLiveData<String> _remainingSeats = new MutableLiveData<String>("");
    public MutableLiveData<String> getRemainingSeats() { return _remainingSeats; }
    public void setRemainingSeats(int Seats) {
        String vacantSeats = String.format("残%d",Seats);
        _remainingSeats.setValue(vacantSeats);
    }

    // エラーコード
    private final MutableLiveData<String> _errorCode = new MutableLiveData<String>("");
    public MutableLiveData<String> getErrorCode() { return _errorCode; }
    public void setErrorCode(String msg) {
        _errorCode.setValue("コード：" + msg);
    }

    // エラーメッセージ
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<String>("");
    public MutableLiveData<String> getErrorMessage() { return _errorMessage; }
    public void setErrorMessage(String msg) {
        _errorMessage.setValue(msg);
    }

    // エラーメッセージ(補足)
    private final MutableLiveData<String> _errorMessageInformation = new MutableLiveData<String>("");
    public MutableLiveData<String> getErrorMessageInformation() { return _errorMessageInformation; }
    public void setErrorMessageInformation(String msg) {
        _errorMessageInformation.setValue(msg);
    }

    // 取消ボタン
    private final MutableLiveData<Boolean> _isCancel = new MutableLiveData<Boolean>(false);
    public MutableLiveData<Boolean> isCancel() { return _isCancel; }
    public void setCancel(boolean b) {
        _handler.post(() -> {
            _isCancel.setValue(b);
        });
    }
}