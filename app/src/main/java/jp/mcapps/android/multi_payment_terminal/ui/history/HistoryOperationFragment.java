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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.Date;

import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentHistoryOperationBinding;
import jp.mcapps.android.multi_payment_terminal.receiver.PickedDateTimeReceiver;

public class HistoryOperationFragment extends BaseFragment {
    private final String SCREEN_NAME = "操作履歴";

    private HistoryOperationViewModel _viewModel;
    private PickedDateTimeReceiver _receiver;

    public static HistoryOperationFragment newInstance() {
        return new HistoryOperationFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        FragmentHistoryOperationBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_history_operation, container, false);
        binding.setHandlers(new HistoryEventHandlersImpl(this));

        _viewModel = new ViewModelProvider(this).get(HistoryOperationViewModel.class);

        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setViewModel(_viewModel);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        HistoryOperationAdapter adapter = new HistoryOperationAdapter();
        _viewModel.getOperationDataList().observe(getViewLifecycleOwner(), adapter::submitList);

        RecyclerView.LayoutManager rLayoutManager = new LinearLayoutManager(getContext());
        binding.listOperationHistory.setLayoutManager(rLayoutManager);
        binding.listOperationHistory.setAdapter(adapter);

        //初期値の設定 1時間前～現時刻の履歴
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, -1);
        _viewModel.getOperationHistory(calendar.getTime());

        _receiver = new PickedDateTimeReceiver() {
            @Override
            protected void onPickedDateTimeReceive(Date pickedDateTime) {
                if (pickedDateTime != null) {
                    _viewModel.getOperationHistory(pickedDateTime);
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
        IntentFilter intentFilter = new IntentFilter("SEND_PICKED_DATE");
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(_receiver, intentFilter);
        super.onResume();
    }
}