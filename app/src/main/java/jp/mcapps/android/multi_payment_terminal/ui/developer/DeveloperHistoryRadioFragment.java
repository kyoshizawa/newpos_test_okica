package jp.mcapps.android.multi_payment_terminal.ui.developer;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.Date;

import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentDeveloperHistoryRadioBinding;
import jp.mcapps.android.multi_payment_terminal.receiver.PickedDateTimeReceiver;

public class DeveloperHistoryRadioFragment extends BaseFragment {
    private final String SCREEN_NAME = "開発者向け電波履歴";

    private DeveloperHistoryRadioViewModel _viewModel;
    private PickedDateTimeReceiver _receiver;

    public static DeveloperHistoryRadioFragment newInstance() {
        return new DeveloperHistoryRadioFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        FragmentDeveloperHistoryRadioBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_developer_history_radio, container, false);
        binding.setHandlers(new DeveloperEventHandlersImpl());

        _viewModel = new ViewModelProvider(this).get(DeveloperHistoryRadioViewModel.class);

        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setViewModel(_viewModel);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        DeveloperHistoryRadioAdapter adapter = new DeveloperHistoryRadioAdapter();
        _viewModel.getRadioDataList().observe(getViewLifecycleOwner(), adapter::submitList);

        RecyclerView.LayoutManager rLayoutManager = new LinearLayoutManager(getContext());
        binding.listRadioHistory.setLayoutManager(rLayoutManager);
        binding.listRadioHistory.setAdapter(adapter);

        //初期値の設定 1時間前～現在の時刻
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, -1);
        Date date = calendar.getTime();

        _viewModel.getRadioHistory(date);

        _receiver = new PickedDateTimeReceiver() {
            @Override
            protected void onPickedDateTimeReceive(Date pickedDateTime) {
                if (pickedDateTime != null) {
                    _viewModel.getRadioHistory(pickedDateTime);
                }
            }
        };

        return binding.getRoot();
    }

    @Override
    public void onPause() {
        //BroadcastReceiverの解除
        LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(_receiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter("SEND_PICKED_DATE");
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(_receiver, intentFilter);
    }
}
