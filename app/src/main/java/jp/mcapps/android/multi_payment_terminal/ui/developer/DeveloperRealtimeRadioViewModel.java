package jp.mcapps.android.multi_payment_terminal.ui.developer;

import androidx.databinding.Observable;
import androidx.databinding.PropertyChangeRegistry;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Date;

import jp.mcapps.android.multi_payment_terminal.database.history.radio.RadioData;

public class DeveloperRealtimeRadioViewModel extends ViewModel implements Observable {
    private final MutableLiveData<RadioData> _radioData = new MutableLiveData<>(new RadioData());
    private final MutableLiveData<Date> _currentTime = new MutableLiveData<>(new Date());

    private final PropertyChangeRegistry callbacks = new PropertyChangeRegistry();

    public MutableLiveData<RadioData> getRadioData() {
        return _radioData;
    }

    public void setRadioData(RadioData radioData) {
        _radioData.setValue(radioData);
    }

    public MutableLiveData<Date> getCurrentTime() {
        return _currentTime;
    }

    public void setCurrentTime() {
        _currentTime.setValue(new Date());
    }

    @Override
    public void addOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        callbacks.add(callback);
    }

    @Override
    public void removeOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        callbacks.remove(callback);
    }
}

