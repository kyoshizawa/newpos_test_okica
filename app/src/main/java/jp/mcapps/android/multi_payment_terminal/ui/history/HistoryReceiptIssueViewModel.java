package jp.mcapps.android.multi_payment_terminal.ui.history;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HistoryReceiptIssueViewModel extends ViewModel {
    private final MutableLiveData<Boolean> _isDetail = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> getIsDetail() {
        return _isDetail;
    }
    public void setIsDetail(boolean b) {
        _isDetail.setValue(b);
    }
}
