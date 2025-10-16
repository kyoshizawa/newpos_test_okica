package jp.mcapps.android.multi_payment_terminal.ui.history;

import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.util.Calendar;
import java.util.Date;

import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentHistoryRadioForUserBinding;
//import jp.mcapps.android.multi_payment_terminal.receiver.PickedDateTimeReceiver;
import timber.log.Timber;

public class HistoryRadioForUserFragment extends BaseFragment {
    private final String SCREEN_NAME = "電波履歴";

    private HistoryRadioForUserViewModel _viewModel;
    //private PickedDateTimeReceiver _receiver;

    public static HistoryRadioForUserFragment newInstance() {
        return new HistoryRadioForUserFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FragmentHistoryRadioForUserBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_history_radio_for_user, container, false);
        binding.setHandlers(new HistoryEventHandlersImpl(this));

        _viewModel = new ViewModelProvider(this).get(HistoryRadioForUserViewModel.class);

        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setViewModel(_viewModel);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        PieChart pieChart = binding.chartRadioLevel;
        //グラフのスタイル
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setEntryLabelColor(R.color.black);

        Legend l = pieChart.getLegend();
        //凡例のスタイル
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setTextSize(16f);
        l.setXEntrySpace(7f);
        l.setDrawInside(true);

        PieDataSet pieDataSet = _viewModel.getPieDataSet();
        //DataSetのスタイル
        pieDataSet.setDrawValues(true);
        pieDataSet.setSliceSpace(5f);
        _viewModel.setPieDataSet(pieDataSet);

        //PieDataにPieDataSet格納
        PieData pieData = new PieData(pieDataSet);
        pieData.setValueFormatter(new PercentFormatter());
        pieData.setValueTextSize(14f);
        _viewModel.setPieData(pieData);

        _viewModel.getPieData().observe(getViewLifecycleOwner(), newData -> {
            pieChart.setData(newData);
            //PieChart更新
            _viewModel.setHasHistory(true);
            pieChart.animateY(1400, Easing.EasingOption.EaseInOutQuad);
            pieChart.invalidate();
        });

        //初期表示 5分前～現在時刻
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -5);
        Date date = calendar.getTime();

        _viewModel.getRadioLevelHistory(date);
//
//        _receiver = new PickedDateTimeReceiver() {
//            @Override
//            protected void onPickedDateTimeReceive(Date pickedDateTime) {
//                if (pickedDateTime != null) {
//                    _viewModel.getRadioLevelHistory(pickedDateTime);
//                }
//            }
//        };

        return binding.getRoot();
    }

    @Override
    public void onPause() {
        //BroadcastReceiverの解除
//        LocalBroadcastManager.getInstance(requireContext())
//                .unregisterReceiver(_receiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter("SEND_PICKED_DATE");
//        LocalBroadcastManager.getInstance(requireContext())
//                .registerReceiver(_receiver, intentFilter);
    }
}
