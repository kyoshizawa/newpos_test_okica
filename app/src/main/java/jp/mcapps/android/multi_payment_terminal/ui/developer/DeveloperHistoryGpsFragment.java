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
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentDeveloperHistoryGpsBinding;
import jp.mcapps.android.multi_payment_terminal.receiver.PickedDateTimeReceiver;
import timber.log.Timber;

public class DeveloperHistoryGpsFragment extends BaseFragment {
    private final String SCREEN_NAME = "GPS履歴";

    private DeveloperHistoryGpsViewModel _viewModel;
    private PickedDateTimeReceiver _receiver;

    public static DeveloperHistoryGpsFragment newInstance() {
        return new DeveloperHistoryGpsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        FragmentDeveloperHistoryGpsBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_developer_history_gps, container, false);
        binding.setHandlers(new DeveloperEventHandlersImpl());

        _viewModel = new ViewModelProvider(this).get(DeveloperHistoryGpsViewModel.class);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setViewModel(_viewModel);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        DeveloperHistoryGpsAdapter adapter = new DeveloperHistoryGpsAdapter();
        _viewModel.getGpsDataList().observe(getViewLifecycleOwner(), adapter::submitList);

        RecyclerView.LayoutManager rLayoutManager = new LinearLayoutManager(getContext());
        binding.listGpsHistory.setLayoutManager(rLayoutManager);
        binding.listGpsHistory.setAdapter(adapter);

        //初期値の設定 1時間前～現在の時刻
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, -1);
        Date date = calendar.getTime();

        _viewModel.getGpsHistory(date);

        _receiver = new PickedDateTimeReceiver() {
            @Override
            protected void onPickedDateTimeReceive(Date pickedDateTime) {
                if (pickedDateTime != null) {
                    _viewModel.getGpsHistory(pickedDateTime);
                }
            }
        };

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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