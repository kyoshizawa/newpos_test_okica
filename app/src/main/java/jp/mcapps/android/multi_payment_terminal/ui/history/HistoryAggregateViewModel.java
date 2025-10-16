package jp.mcapps.android.multi_payment_terminal.ui.history;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Collections;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.aggregate.AggregateData;

public class HistoryAggregateViewModel extends ViewModel {
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<List<AggregateData>> _aggregateDataList = new MutableLiveData<>(Collections.emptyList());

    public HistoryAggregateViewModel() {
    }

    public void getAggregateHistory() {
        final Runnable run = () -> {
            List<AggregateData> aggregateDataList = DBManager.getAggregateDao().getAggregateHistory();
            handler.post(() -> _aggregateDataList.setValue(aggregateDataList));
        };
        new Thread(run).start();
    }

    public MutableLiveData<List<AggregateData>> getAggregateDataList() {
        return _aggregateDataList;
    }
}