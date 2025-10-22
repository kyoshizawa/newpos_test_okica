package jp.mcapps.android.multi_payment_terminal;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.pos.device.config.DevConfig;

import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.okica.ICMaster;
import jp.mcapps.android.multi_payment_terminal.databinding.ActivityStartBinding;
//import jp.mcapps.android.multi_payment_terminal.devices.DiscoverDevice;
//import jp.mcapps.android.multi_payment_terminal.devices.GloryCashChanger;
//import jp.mcapps.android.multi_payment_terminal.model.DeviceConnectivityManager;
import jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl;
import jp.mcapps.android.multi_payment_terminal.service.GetGpsService;
import jp.mcapps.android.multi_payment_terminal.service.LogSendService;
//import jp.mcapps.android.multi_payment_terminal.service.WifiP2pService;
import jp.mcapps.android.multi_payment_terminal.logger.EventLogger;
import jp.mcapps.android.multi_payment_terminal.ui.menu.MenuHomeFragment;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Version;
import timber.log.Timber;

public class StartActivity extends AppCompatActivity {
    private final int MESSAGE_CONNECTIVITY_TIMEOUT = 1;
    private final int MESSAGE_WAIT_NETAVAILABLE_TIMEOUT = 2;
    private final int NETWORK_CONNECTIVITY_TIMEOUT_MS = 30 * 1000;
    private final int WAIT_NETAVAILABLE_TIMEOUT_MS = 30 * 1000;
    private final int INIT_WAIT = 1;
    private final int INIT_RUN = 2;
    private final String SCREEN_NAME = "スタート画面";

    private final NetworkHandler _handler = new NetworkHandler();
    private StartViewModel _startViewModel;
    private Runnable _restartAction = null;

    private ConnectivityManager.NetworkCallback _networkCallback = null;

    private ConnectivityManager mConnectivityManager = null;

    private static int _mInitPhase = 0;
    private static Network _mPrevNetwork = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("start activity");
        super.onCreate(savedInstanceState);

        final ActivityStartBinding binding = DataBindingUtil.setContentView(
                this, R.layout.activity_start);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        _startViewModel = new ViewModelProvider(this, MainApplication.getViewModelFactory()).get(StartViewModel.class);

        _startViewModel.appStart();

        binding.setActivity(this);
        binding.setViewModel(_startViewModel);
        binding.setLifecycleOwner(this);
        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

//        if (Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            // これはIFBoxキッティング後に必要な処理なので許可されている前提
//            // アップデート処理を行うためにはWiFi-Directの接続が必要なため
//            Intent wifiP2pService = new Intent(this, WifiP2pService.class);
//            startService(wifiP2pService);
//        }
//
//        Intent logSendService = new Intent(this, LogSendService.class);
//        startService(logSendService);
        _StartService();

        if (AppPreference.isDemoMode()) {
            Timber.i("デモモード設定:有効");
            startMainActivity(null);
            return;
        }

        // 起動時にフタバ双方向手動モードの場合は戻す
        Version.Response ifboxVersionInfo = AppPreference.getIFBoxVersionInfo();
        if (ifboxVersionInfo != null && ifboxVersionInfo.appModel.equals(IFBoxAppModels.FUTABA_D_MANUAL)) {
            ifboxVersionInfo.appModel = IFBoxAppModels.FUTABA_D;
            AppPreference.setIFBoxVersionInfo(ifboxVersionInfo);
        }

        Timber.i("FW: %s", DevConfig.getFirmwareVersion());

        final ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        _startViewModel.isEnd().observe(this, b -> {
            if (b) {
                startMainActivity(_startViewModel.getErrors());
            }
        });

        _networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Timber.i("ネットワーク状態:正常");
                if (AppPreference.getIsOnCradle()) {//cashdrawer対応
                    super.onAvailable(network);
                    Timber.i("setProcessDefaultNetwork: network==" + network);
                    if (mConnectivityManager != null) {
                        mConnectivityManager.bindProcessToNetwork(network);
                        NetworkCapabilities capabilities = mConnectivityManager.getNetworkCapabilities(network);
                        if (capabilities != null) {
                            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                                Timber.d("networkInfoType==WIFI");
                            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                                Timber.d("networkInfoType==CELLULAR");
                            } else {
                                Timber.d("networkInfoType==OTHER");
                            }
                        }
                    }
                    _handler.removeMessages(MESSAGE_CONNECTIVITY_TIMEOUT);

                    // つり銭機連動では、クレードルを介して有線LAN接続されており、initialize()実施中にnetworkが切り替わることがある
                    // この場合、initialize()に失敗するため、次の対策を行う
                    // ・networkが切り替わってからinitialize()を実施
                    // ・networkが切り替わらない場合、一定時間後にinitialize()を実施
                    if (_mInitPhase == 0) {
                        _mInitPhase = INIT_WAIT;
                        _startViewModel.isBooting(true);
                        _handler.sendMessageDelayed(
                                _handler.obtainMessage(MESSAGE_WAIT_NETAVAILABLE_TIMEOUT), WAIT_NETAVAILABLE_TIMEOUT_MS);
                    } else if (_mInitPhase == INIT_WAIT && !_mPrevNetwork.equals(network)) {
                        Timber.i("run initialize in onAvailable");
                        _mInitPhase = INIT_RUN;
                        _handler.removeMessages(MESSAGE_WAIT_NETAVAILABLE_TIMEOUT);
                        _startViewModel.isBooting(false);
                        _startViewModel.initialize();
                    }
                    _mPrevNetwork = network;
                } else {
                    _handler.removeMessages(MESSAGE_CONNECTIVITY_TIMEOUT);
                    _startViewModel.initialize();
                    connectivityManager.unregisterNetworkCallback(this);
                }
            }
        };

        final NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.requestNetwork(request, _networkCallback);

        _handler.sendMessageDelayed(
                _handler.obtainMessage(MESSAGE_CONNECTIVITY_TIMEOUT), NETWORK_CONNECTIVITY_TIMEOUT_MS);

        //request permissions
        int CODE_WRITE_SETTINGS_PERMISSION = 6789;
        boolean permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permission = Settings.System.canWrite(this);
        } else {
            permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
        }
        if (permission) {
            //Write allowed
        } else {
            //Write not allowed
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                this.startActivityForResult(intent, CODE_WRITE_SETTINGS_PERMISSION);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_SETTINGS}, CODE_WRITE_SETTINGS_PERMISSION);
            }
        }
    }

    /**
     * 後継機対応： onCreate から移動
     * さらに Android 8 から バックグラウンド状態中に startService を呼ぶとアプリが強制終了される対策で
     * startForegroundService での起動としている
     */
    protected void _StartService() {
        if (Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // これはIFBoxキッティング後に必要な処理なので許可されている前提
            // アップデート処理を行うためにはWiFi-Directの接続が必要なため
//            Intent wifiP2pService = new Intent(this, WifiP2pService.class);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                ContextCompat.startForegroundService(this, wifiP2pService);
//            } else {
//                startService(wifiP2pService); // Android 7.1 以下はこれでOK
//            }
        }

        Intent logSendService = new Intent(this, LogSendService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, logSendService);
        } else {
            startService(logSendService);
        };
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        if (_restartAction != null) {
            _restartAction.run();
            _restartAction = null;
        }
    }

    @Override
    public void onBackPressed() {
        hideSystemUI();
    }

    public void onUpdateClick(View view) {
//        String type = _startViewModel.getUpdateType().getValue() == StartViewModel.UpdateTypes.THIS ? "アプリアップデート" : "IM-A820アップデート";
//        CommonClickEvent.RecordClickOperation("アップデート", type, true);
        CommonClickEvent.RecordClickOperation("アップデート", "アップデート", true);
        _startViewModel.update();
        _restartAction = () -> {
            _startViewModel.cancel();
        };
    }

    public void onCancelClick(View view) {
//        String type = _startViewModel.getUpdateType().getValue() == StartViewModel.UpdateTypes.THIS ? "アプリアップデート" : "IM-A820アップデート";
        CommonClickEvent.RecordClickOperation("キャンセル", "アップデート", true);
        _startViewModel.cancel();
    }

    private void hideSystemUI() {
        Window window = getWindow();
        WindowInsetsController insetsController = window.getInsetsController();
        if (insetsController != null) {
            insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    private void showSystemUI() {
        Window window = getWindow();
        WindowInsetsController insetsController = window.getInsetsController();
        if (insetsController != null) {
            insetsController.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        }
    }

    @Override
    protected void finalize() throws Throwable {
        Timber.d("release StartActivity");
        super.finalize();
    }

    private void startMainActivity(String[] errors) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        if (errors != null) {
            intent.putExtra("errors", errors);
        }
        startActivity(intent);
        finish();
    }

    private class NetworkHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (AppPreference.getIsOnCradle()) {
                if (msg.what == MESSAGE_CONNECTIVITY_TIMEOUT) {
                    Timber.e("ネットワーク状態:異常");

                    // オフラインでもOKICAマスタデータの保持期限をチェックする
                    OkicaMasterControl _okicaMasterCtrl = new OkicaMasterControl();
                    _okicaMasterCtrl.okicaCheckMasterTimeLimit();

                    startMainActivity(null);
                    if (_networkCallback != null) {
                        final ConnectivityManager connectivityManager =
                                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                        connectivityManager.unregisterNetworkCallback(_networkCallback);
                    }
                } else if (msg.what == MESSAGE_WAIT_NETAVAILABLE_TIMEOUT) {
                    if (_mInitPhase == INIT_WAIT) {
                        Timber.i("run initialize in timeout");
                        _mInitPhase = INIT_RUN;
                        _startViewModel.isBooting(false);
                        _startViewModel.initialize();
                    }
                }
            } else {
                Timber.e("ネットワーク状態:異常");

                // オフラインでもOKICAマスタデータの保持期限をチェックする
                OkicaMasterControl _okicaMasterCtrl = new OkicaMasterControl();
                _okicaMasterCtrl.okicaCheckMasterTimeLimit();

                if (msg.what == MESSAGE_CONNECTIVITY_TIMEOUT) {
                    startMainActivity(null);
                    if (_networkCallback != null) {
                        final ConnectivityManager connectivityManager =
                                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                        connectivityManager.unregisterNetworkCallback(_networkCallback);
                    }
                }
            }
        }
    }
}