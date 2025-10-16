package jp.mcapps.android.multi_payment_terminal.ui.history;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.MutableLiveData;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.operation.OperationDao;
import jp.mcapps.android.multi_payment_terminal.database.history.operation.OperationData;

public class HistoryOperationViewModel extends HistoryViewModel {
    private final MutableLiveData<List<OperationData>> _operationDataList = new MutableLiveData<>(Collections.emptyList());

    private final OperationDao _dao;
    private final Handler _handler = new Handler(Looper.getMainLooper());

    public HistoryOperationViewModel() {
        super("yyyy年M月d日H時m分　から1時間", Calendar.HOUR_OF_DAY, -2);
        _dao = DBManager.getOperationDao();
    }

    public void getOperationHistory(Date start) {
        setShowDateTime(start);
        final Runnable run = () -> {
            //表示範囲の設定 指定した時刻～1時間後
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(start);
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            Date end = calendar.getTime();

            final List<OperationData> operationDataList = _dao.getAllByDateTime(start, end);
            _handler.post(() -> {
                setHasNext(operationDataList.get(0) != null);
                operationDataList.remove(0);

                setHasPrev(operationDataList.get(operationDataList.size() - 1) != null);
                operationDataList.remove(operationDataList.size() - 1);

                setOperationDataList(operationDataList);
                setHasHistory(!operationDataList.isEmpty());

                if (operationDataList.isEmpty()) {
                    setErrorText("履歴がありません");
                }
            });
        };
        new Thread(run).start();
    }

    public MutableLiveData<List<OperationData>> getOperationDataList() {
        return _operationDataList;
    }
    public void setOperationDataList(List<OperationData> operationDataList) {
        _operationDataList.setValue(operationDataList);
    }
}
