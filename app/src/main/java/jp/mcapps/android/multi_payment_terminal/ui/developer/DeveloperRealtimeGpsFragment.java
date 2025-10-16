package jp.mcapps.android.multi_payment_terminal.ui.developer;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.database.history.gps.GpsData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentDeveloperRealtimeGpsBinding;
import jp.mcapps.android.multi_payment_terminal.receiver.GpsDataReceiver;
import timber.log.Timber;

public class DeveloperRealtimeGpsFragment extends BaseFragment {
    private final String SCREEN_NAME = "GPSリアルタイム確認";

    private DeveloperRealtimeGpsViewModel _viewModel;
    private GpsDataReceiver _receiver;
    private Handler _handler;
    private Runnable _run;

    public static DeveloperRealtimeGpsFragment newInstance() {
        return new DeveloperRealtimeGpsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        FragmentDeveloperRealtimeGpsBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_developer_realtime_gps, container, false);
        binding.setHandlers(new DeveloperEventHandlersImpl());

        _viewModel = new ViewModelProvider(this).get(DeveloperRealtimeGpsViewModel.class);

        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setViewModel(_viewModel);

        _handler = new Handler(Looper.getMainLooper());
        _run = new Runnable() {
            @Override
            public void run() {
                _viewModel.setCurrentTime();
                _handler.postDelayed(this, 1000);
            }
        };

        //履歴から最新の値を取得
        _viewModel.getLatestGpsData();

        _receiver = new GpsDataReceiver() {
            @Override
            protected void onGpsDataReceive(GpsData gpsData) {
                if (gpsData != null) {
                    _viewModel.setGpsData(gpsData);
                }
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
        IntentFilter intentFilter = new IntentFilter("SEND_GPS_DATA");
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(_receiver, intentFilter);
        _handler.post(_run);
    }
}
