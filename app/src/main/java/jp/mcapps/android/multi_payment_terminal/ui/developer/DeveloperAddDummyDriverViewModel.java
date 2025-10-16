package jp.mcapps.android.multi_payment_terminal.ui.developer;

import java.util.Date;

import androidx.databinding.Bindable;
import androidx.databinding.PropertyChangeRegistry;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DeveloperAddDummyDriverViewModel extends ViewModel {
    private MutableLiveData<String> _driverCode = new MutableLiveData<String>("");
    private MutableLiveData<String> _driverName = new MutableLiveData<String>("");
    private MutableLiveData<Date> _createdAt = new MutableLiveData<Date>(new Date());

    public MutableLiveData<String> getDriverCode() {
        return _driverCode;
    }

    public void setDriverCode(String value) {
        _driverCode.setValue(value);
    }

    public MutableLiveData<String> getDriverName() {
        return _driverName;
    }

    public void setDriverName(String value) {
        _driverName.setValue(value);
    }

    public MutableLiveData<Date> getCreatedAt() {
        return _createdAt;
    }

    public void setCreatedAt(Date value) {
        _createdAt.setValue(value);
    }
}
