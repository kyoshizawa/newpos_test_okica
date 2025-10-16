package jp.mcapps.android.multi_payment_terminal.ui.developer;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.MutableLiveData;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.gps.GpsDao;
import jp.mcapps.android.multi_payment_terminal.database.history.gps.GpsData;
import jp.mcapps.android.multi_payment_terminal.ui.history.HistoryViewModel;

public class DeveloperHistoryGpsViewModel extends HistoryViewModel {
    private final MutableLiveData<List<GpsData>> _gpsDataList = new MutableLiveData<>(Collections.emptyList());

    private final GpsDao _dao;
    private final Handler _handler = new Handler(Looper.getMainLooper());

    public DeveloperHistoryGpsViewModel() {
        super("yyyy年M月d日H時m分　から1時間", Calendar.HOUR_OF_DAY, -1);
        _dao = DBManager.getGpsDao();
    }

    public void getGpsHistory(Date start) {
        setShowDateTime(start);
        final Runnable run = () -> {
            //表示範囲の設定 指定した時刻～1時間後
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(start);
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            Date end = calendar.getTime();

            final List<GpsData> gpsDataList = _dao.getAllByDateTime(start, end);
            _handler.post(() -> {
                setHasNext(gpsDataList.get(0) != null);
                gpsDataList.remove(0);

                setHasPrev(gpsDataList.get(gpsDataList.size() - 1) != null);
                gpsDataList.remove(gpsDataList.size() - 1);

                setGpsDataList(gpsDataList);
                setHasHistory(!gpsDataList.isEmpty());

                if (gpsDataList.isEmpty()) {
                    setErrorText("履歴がありません");
                }
            });
        };
        new Thread(run).start();
    }

    public MutableLiveData<List<GpsData>> getGpsDataList() {
        return _gpsDataList;
    }
    public void setGpsDataList(List<GpsData> gpsDataList) {
        _gpsDataList.setValue(gpsDataList);
    }
}