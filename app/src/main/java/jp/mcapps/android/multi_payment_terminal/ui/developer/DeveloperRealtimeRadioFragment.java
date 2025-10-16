package jp.mcapps.android.multi_payment_terminal.ui.developer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.data.CurrentRadio;
import jp.mcapps.android.multi_payment_terminal.database.history.radio.RadioData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentDeveloperRealtimeRadioBinding;

public class DeveloperRealtimeRadioFragment extends BaseFragment {
    private final String SCREEN_NAME = "電波状況リアルタイム確認";

    private DeveloperRealtimeRadioViewModel _viewModel;
    private BroadcastReceiver _receiver;
    private Handler _handler;
    private Runnable _run;

    public static DeveloperRealtimeRadioFragment newInstance() {
        return new DeveloperRealtimeRadioFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        FragmentDeveloperRealtimeRadioBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_developer_realtime_radio, container, false);
        binding.setHandlers(new DeveloperEventHandlersImpl());

        _viewModel = new ViewModelProvider(this).get(DeveloperRealtimeRadioViewModel.class);

        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setViewModel(_viewModel);

        _handler = new Handler(Looper.getMainLooper());
        _run = new Runnable() {
            @Override
            public void run() {
                //現在時刻 1秒間隔で更新
                _viewModel.setCurrentTime();
                _handler.postDelayed(this, 1000);
            }
        };

        //最新の値を取得
        RadioData latestData = CurrentRadio.getData();
        _viewModel.setRadioData(latestData);

        //電波情報の変更を受け取るレシーバ
        _receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                _viewModel.setRadioData(CurrentRadio.getData());
            }
        };

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }

    @Override
    public void onPause() {
        //BroadcastReceiverの解除
        LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(_receiver);
        _handler.removeCallbacks(_run);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter("CHANGE_RADIO_DATA");
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(_receiver, intentFilter);
        _handler.post(_run);
    }
}
