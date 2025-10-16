package jp.mcapps.android.multi_payment_terminal.service;

import android.app.Notification;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import timber.log.Timber;

public class WifiP2pService extends LifecycleService implements ViewModelStoreOwner {
    private WifiP2pServiceViewModel _viewModel;
    private ViewModelStore _viewModelStore = new ViewModelStore();

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("create WifiP2pService");

        // ForegroundService対応：通知付きで昇格（API 26以降は必須）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification notification = NotificationUtil.Create(
                    this,
                    "wifi_p2p_channel",
                    "WiFi P2P サービス",
                    R.drawable.ic_notification_blank
            );
            startForeground(NotificationUtil.SERVICE_ID_WIFIP2P, notification);
        }

        _viewModel = new ViewModelProvider(this, MainApplication.getViewModelFactory()).get(WifiP2pServiceViewModel.class);
        _viewModel.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.d("destroy WifiP2pService");
        _viewModel.stop();
    }

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        return _viewModelStore;
    }
}
