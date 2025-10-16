package jp.mcapps.android.multi_payment_terminal.ui.history;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.MutableLiveData;

import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.radio.RadioDao;

public class HistoryRadioForUserViewModel extends HistoryViewModel {
    private PieDataSet _pieDataSet = new PieDataSet(Collections.emptyList(), "");
    private final MutableLiveData<PieData> _pieData = new MutableLiveData<>(new PieData());

    private final RadioDao _dao;
    private final Handler _handler = new Handler(Looper.getMainLooper());

    public HistoryRadioForUserViewModel() {
        super("yyyy年M月d日H時m分　から5分間", Calendar.MINUTE, -1);
        _dao = DBManager.getRadioDao();
    }

    public void getRadioLevelHistory(Date start) {
        setShowDateTime(start);
        final Runnable run = () -> {

            //表示範囲の設定 指定した時刻～1時間後
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(start);
            calendar.add(Calendar.MINUTE, 5);
            Date end = calendar.getTime();

            final List<Integer> levelList = _dao.getLevelByDateTime(start, end);
            _handler.post(() -> {

                setHasNext(levelList.get(0) != null);
                levelList.remove(0);

                setHasPrev(levelList.get(levelList.size() - 1) != null);
                levelList.remove(levelList.size() - 1);

                if (levelList.size() != 0) {
                    setHasHistory(true);
                } else {
                    setErrorText("履歴がありません");
                    setHasHistory(false);
                    return;
                }

                float[] values = {0.0f, 0.0f, 0.0f};
                for (int level : levelList) {
                    switch (level) {
                        case 0:
                        case 1:
                            values[2] += 1.0f;
                            break;
                        case 2:
                            values[1] += 1.0f;
                            break;
                        case 3:
                        case 4:
                            values[0] += 1.0f;
                            break;
                    }
                }

                String[] dimensions = {"強", "中", "弱"};//分割円の名称(String型)

                List<Integer> colors = new ArrayList<>();
                //Entryにデータ格納
                List<PieEntry> entryList = new ArrayList<>();
                for (int i = 0; i < values.length; i++) {
                    if (values[i] > 0f) {
                        int color = 0;
                        switch (i) {
                            case 0 :
                                color = MainApplication.getInstance().getResources().getColor(R.color.radio_level_high, MainApplication.getInstance().getTheme());
                                break;
                            case 1 :
                                color = MainApplication.getInstance().getResources().getColor(R.color.radio_level_middle, MainApplication.getInstance().getTheme());
                                break;
                            case 2 :
                                color = MainApplication.getInstance().getResources().getColor(R.color.radio_level_low, MainApplication.getInstance().getTheme());
                                break;
                        }
                        colors.add(color);
                        entryList.add(new PieEntry(values[i], dimensions[i]));
                    }
                }

                //PieDataSetにデータ格納
                PieDataSet pieDataSet = getPieDataSet();
                pieDataSet.setValues(entryList);
                pieDataSet.setColors(colors);
                setPieDataSet(pieDataSet);

                //PieDataにPieDataSet格納
                PieData pieData = getPieData().getValue();
                if (pieData == null) {
                    pieData = new PieData(pieDataSet);
                } else {
                    pieData.setDataSet(pieDataSet);
                }

                setPieData(pieData);
            });
        };
        new Thread(run).start();
    }

    public PieDataSet getPieDataSet() {
        return _pieDataSet;
    }

    public void setPieDataSet(PieDataSet pieDataSet) {
        _pieDataSet = pieDataSet;
    }

    public MutableLiveData<PieData> getPieData() {
        return _pieData;
    }

    public void setPieData(PieData pieData) {
        _pieData.setValue(pieData);
    }
}
