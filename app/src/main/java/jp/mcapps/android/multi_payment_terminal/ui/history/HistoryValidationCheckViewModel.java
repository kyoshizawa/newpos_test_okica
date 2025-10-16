package jp.mcapps.android.multi_payment_terminal.ui.history;

import android.os.Handler;
import android.os.Looper;

import androidx.databinding.Observable;
import androidx.databinding.PropertyChangeRegistry;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Collections;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.validation_check.ValidationCheckDao;
import jp.mcapps.android.multi_payment_terminal.database.validation_check.ValidationCheckHistoryData;

public class HistoryValidationCheckViewModel extends ViewModel implements Observable {
    private final MutableLiveData<List<ValidationCheckHistoryData>> _historyList = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String> _title = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> _unsentCnt = new MutableLiveData<>(0);

    private final ValidationCheckDao _dao;
    private final Handler _handler;

    public HistoryValidationCheckViewModel() {
        _dao = DBManager.getValidationCheckDao();
        _handler = new Handler(Looper.getMainLooper());
    }

    public void init() {
        new Thread(() -> {
            final List<ValidationCheckHistoryData> history = _dao.getAllHistory();
            final int cnt = _dao.getUnsentCnt();
            _handler.post(() -> {
                _historyList.setValue(history);
                _unsentCnt.setValue(cnt);
            });
        }).start();
    }

    public MutableLiveData<List<ValidationCheckHistoryData>> getHistory() {
        return _historyList;
    }

    public MutableLiveData<String> getTitle() {
        return _title;
    }
    public void setTitle(String title) {
        _title.setValue(title);
    }

    public MutableLiveData<Integer> getUnsentCnt() {
        return _unsentCnt;
    }

    private final PropertyChangeRegistry callbacks = new PropertyChangeRegistry();
    @Override
    public void addOnPropertyChangedCallback(Observable.OnPropertyChangedCallback callback) {
        callbacks.add(callback);
    }
    @Override
    public void removeOnPropertyChangedCallback(Observable.OnPropertyChangedCallback callback) {
        callbacks.remove(callback);
    }
}
