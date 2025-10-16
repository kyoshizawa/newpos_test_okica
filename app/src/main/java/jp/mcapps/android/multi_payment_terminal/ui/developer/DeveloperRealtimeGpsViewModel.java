package jp.mcapps.android.multi_payment_terminal.ui.developer;

import android.os.Handler;
import android.os.Looper;

import androidx.databinding.Observable;
import androidx.databinding.PropertyChangeRegistry;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Date;

import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.gps.GpsDao;
import jp.mcapps.android.multi_payment_terminal.database.history.gps.GpsData;

public class DeveloperRealtimeGpsViewModel extends ViewModel implements Observable {
    private final MutableLiveData<GpsData> _gpsData = new MutableLiveData<>(new GpsData());
    private final MutableLiveData<Date> _currentTime = new MutableLiveData<>(new Date());

    private final PropertyChangeRegistry callbacks = new PropertyChangeRegistry();
    private final GpsDao _dao;
    private final Handler _handler = new Handler(Looper.getMainLooper());

    public DeveloperRealtimeGpsViewModel() {
        _dao = DBManager.getGpsDao();
    }

    public void getLatestGpsData() {
        final Runnable run = () -> {
            final GpsData gpsData = _dao.getLatestOne();
            _handler.post(() -> {
                if (gpsData != null) {
                    setGpsData(gpsData);
                }
            });
        };
        new Thread(run).start();
    }

    public MutableLiveData<GpsData> getGpsData() {
        return _gpsData;
    }

    public void setGpsData(GpsData gpsData) {
        _gpsData.setValue(gpsData);
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

