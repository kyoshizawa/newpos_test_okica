package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TicketGateQrScanResultsViewModel extends ViewModel {

    // 結果
    private final MutableLiveData<Boolean> _qrScanResult = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> getResult() { return _qrScanResult; }
    public void setResult(Boolean result) { _qrScanResult.setValue(result); }

    /* 成功時 */
    // 合計人数
    private final MutableLiveData<Integer> _totalPeoples = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getTotalPeoples() { return _totalPeoples; }
    public void setTotalPeoples(Integer totalPeoples) { _totalPeoples.setValue(totalPeoples); }

    // 大人(人数)
    private final MutableLiveData<Integer> _adultNumber = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getAdultNumber() { return _adultNumber; }
    public void setAdultNumber(Integer adultNumber) { _adultNumber.setValue(adultNumber); }

    // 小人(人数)
    private final MutableLiveData<Integer> _childNumber = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getChildNumber() { return _childNumber; }
    public void setChildNumber(Integer childNumber) { _childNumber.setValue(childNumber); }

    // 乳幼児(人数)
    private final MutableLiveData<Integer> _babyNumber = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getBabyNumber() { return _babyNumber; }
    public void setBabyNumber(Integer babyNumber) { _babyNumber.setValue(babyNumber); }

    // 障がい者 大人(人数)
    private final MutableLiveData<Integer> _adultDisabilityNumber = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getAdultDisabilityNumber() { return _adultDisabilityNumber; }
    public void setAdultDisabilityNumber(Integer adultDisabilityNumber) { _adultDisabilityNumber.setValue(adultDisabilityNumber); }

    // 障がい者 小人(人数)
    private final MutableLiveData<Integer> _childDisabilityNumber = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getChildDisabilityNumber() { return _childDisabilityNumber; }
    public void setChildDisabilityNumber(Integer childDisabilityNumber) { _childDisabilityNumber.setValue(childDisabilityNumber); }

    // 介助者(人数)
    private final MutableLiveData<Integer> _caregiverNumber = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getCaregiverNumber() { return _caregiverNumber; }
    public void setCaregiverNumber(Integer caregiverNumber) { _caregiverNumber.setValue(caregiverNumber); }

    /* 失敗時 */
    // エラーコード
    private final MutableLiveData<String> _errorCode = new MutableLiveData<>("");
    public MutableLiveData<String> getErrorCode() {
        return _errorCode;
    }
    public void setErrorCode(String code) {
        _errorCode.setValue(code);
    }

    // エラーメッセージ
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>("");
    public MutableLiveData<String> getErrorMessage() {
        return _errorMessage;
    }
    public void setErrorMessage(String msg) { _errorMessage.setValue(msg); }

    // エラーメッセージ（英語）
    private final MutableLiveData<String> _errorMessageEnglish = new MutableLiveData<>("");
    public MutableLiveData<String> getErrorMessageEnglish() {
        return _errorMessageEnglish;
    }
    public void setErrorMessageEnglish(String msg) { _errorMessageEnglish.setValue(msg); }
}
