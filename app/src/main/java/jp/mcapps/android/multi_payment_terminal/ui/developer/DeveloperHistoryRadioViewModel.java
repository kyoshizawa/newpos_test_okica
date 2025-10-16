package jp.mcapps.android.multi_payment_terminal.ui.developer;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.MutableLiveData;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.radio.RadioDao;
import jp.mcapps.android.multi_payment_terminal.database.history.radio.RadioData;
import jp.mcapps.android.multi_payment_terminal.ui.history.HistoryViewModel;

public class DeveloperHistoryRadioViewModel extends HistoryViewModel {
    private final MutableLiveData<List<RadioData>> _radioDataList = new MutableLiveData<>(Collections.emptyList());

    private final RadioDao _dao;
    private final Handler _handler = new Handler(Looper.getMainLooper());

    public DeveloperHistoryRadioViewModel() {
        super("yyyy年M月d日H時m分　から1時間", Calendar.HOUR_OF_DAY, -1);
        _dao = DBManager.getRadioDao();
    }

    public void getRadioHistory(Date start) {
        setShowDateTime(start);
        final Runnable run = () -> {
            //表示範囲の設定 指定した時刻～1時間後
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(start);
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            Date end = calendar.getTime();

            final List<RadioData> radioDataList = _dao.getAllByDateTime(start, end);
            _handler.post(() -> {
                setHasNext(radioDataList.get(0) != null);
                radioDataList.remove(0);

                setHasPrev(radioDataList.get(radioDataList.size() - 1) != null);
                radioDataList.remove(radioDataList.size() - 1);

                setRadioDataList(radioDataList);
                setHasHistory(!radioDataList.isEmpty());

                if (radioDataList.isEmpty()) {
                    setErrorText("履歴がありません");
                }
            });
        };
        new Thread(run).start();
    }

    public MutableLiveData<List<RadioData>> getRadioDataList() {
        return _radioDataList;
    }
    public void setRadioDataList(List<RadioData> radioDataList) {
        _radioDataList.setValue(radioDataList);
    }
}
