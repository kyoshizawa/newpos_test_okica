package jp.mcapps.android.multi_payment_terminal.ui.history;

import android.os.Handler;
import android.os.Looper;

import androidx.databinding.Observable;
import androidx.databinding.PropertyChangeRegistry;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorDao;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorData;
import jp.mcapps.android.multi_payment_terminal.database.history.operation.OperationData;

public class HistoryErrorViewModel extends HistoryViewModel {
    private final MutableLiveData<List<ErrorData>> _errorDataList = new MutableLiveData<>(Collections.emptyList());

    private final ErrorDao _dao;
    private final Handler _handler = new Handler(Looper.getMainLooper());

    public HistoryErrorViewModel (){
        super("yyyy年M月d日H時m分　から1時間", Calendar.HOUR_OF_DAY, -2);
        _dao = DBManager.getErrorDao();
    }

    public void getErrorHistory(Date start) {
        setShowDateTime(start);
        final Runnable run = () -> {
            //表示範囲の設定 指定した時刻～1時間後
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(start);
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            Date end = calendar.getTime();

            final List<ErrorData> errorDataList = _dao.getAllByDateTime(start, end);
            _handler.post(() -> {
                setHasNext(errorDataList.get(0) != null);
                errorDataList.remove(0);

                setHasPrev(errorDataList.get(errorDataList.size() - 1) != null);
                errorDataList.remove(errorDataList.size() - 1);

                setErrorDataList(errorDataList);
                setHasHistory(!errorDataList.isEmpty());

                if (errorDataList.isEmpty()) {
                    setErrorText("履歴がありません");
                }
            });
        };
        new Thread(run).start();
    }

    public MutableLiveData<List<ErrorData>> getErrorDataList() {
        return _errorDataList;
    }

    public void setErrorDataList(List<ErrorData> errorDataList) {
        _errorDataList.setValue(errorDataList);
    }
}
