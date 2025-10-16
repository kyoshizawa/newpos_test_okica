package jp.mcapps.android.multi_payment_terminal.ui.history;

import androidx.databinding.Observable;
import androidx.databinding.PropertyChangeRegistry;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryViewModel extends ViewModel implements Observable {
    private final PropertyChangeRegistry callbacks = new PropertyChangeRegistry();

    private final MutableLiveData<String> _showDateTime = new MutableLiveData<>("");
    private final MutableLiveData<String> _errorText = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> _hasHistory = new MutableLiveData<>(true);
    private final MutableLiveData<Date> _pickedDate = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> _hasNext = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _hasPrev = new MutableLiveData<>(false);

    private final String _formatPattern;
    private final int _calendarField;
    private final int _minMonth;

    public HistoryViewModel (String formatPattern, int calendarField, int month) {
        _formatPattern = formatPattern;
        _calendarField = calendarField;
        _minMonth = month;
    }

    public int getType() {
        return _calendarField;
    }

    public int getMinMonth() {
        return _minMonth;
    }

    public MutableLiveData<String> getShowDateTime() {
        return _showDateTime;
    }
    public void setShowDateTime(Date date) {
        SimpleDateFormat dateTimeTextFormat = new SimpleDateFormat(_formatPattern, Locale.JAPANESE);
        String showDateTime = dateTimeTextFormat.format(date);

        _showDateTime.setValue(showDateTime);
        setPickedDate(date);
    }

    public MutableLiveData<String> getErrorText() {
        return _errorText;
    }
    public void setErrorText(String text) {
        _errorText.setValue(text);
    }

    public MutableLiveData<Boolean> hasHistory() {
        return _hasHistory;
    }
    public void setHasHistory(boolean b) {
        _hasHistory.setValue(b);
    }

    public MutableLiveData<Date> getPickedDate() {
        return _pickedDate;
    }
    public void setPickedDate(Date date) {
        _pickedDate.setValue(date);
    }

    public MutableLiveData<Boolean> hasNext() {
        return _hasNext;
    }
    public void setHasNext(boolean b) {
        _hasNext.setValue(b);
    }

    public MutableLiveData<Boolean> hasPrev() {
        return _hasPrev;
    }
    public void setHasPrev(boolean b) {
        _hasPrev.setValue(b);
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
