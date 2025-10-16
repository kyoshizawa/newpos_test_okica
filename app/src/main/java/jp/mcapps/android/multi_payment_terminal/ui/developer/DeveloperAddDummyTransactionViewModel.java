package jp.mcapps.android.multi_payment_terminal.ui.developer;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DeveloperAddDummyTransactionViewModel extends ViewModel {
    private final MutableLiveData<Integer> _transBrand = new MutableLiveData<>(0);
    private final MutableLiveData<String > _transCnt = new MutableLiveData<>(null);

    public MutableLiveData<Integer> getTransBrand() {
        return _transBrand;
    }

    public MutableLiveData<String> getTransCnt() {
        return _transCnt;
    }
}
