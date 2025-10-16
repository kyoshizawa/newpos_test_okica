package jp.mcapps.android.multi_payment_terminal.ui.setup;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.reactivex.Observable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.data.DeviceServiceInfo;
import jp.mcapps.android.multi_payment_terminal.data.FirmWareInfo;
//import jp.mcapps.android.multi_payment_terminal.model.device_network_manager.DeviceNetworkManager;
//import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.model.Updater;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.IFBoxApi;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.IFBoxApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Version;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Wifi;
import timber.log.Timber;

@SuppressWarnings("ALL")
public class IFBoxSetupViewModel extends ViewModel {
//    private final DeviceNetworkManager _deviceNetworkManager;
//    private final IFBoxManager _ifBoxManager;
    private final Application _app = MainApplication.getInstance();
    private final WifiManager _wifiManager = (WifiManager) _app.getSystemService(Context.WIFI_SERVICE);
    private final Updater _updater;
    private IFBoxApi _apiClient = new IFBoxApiImpl();
    private Handler _handler = new Handler(Looper.getMainLooper());
    public final void cleanup() {
        CommonClickEvent.RecordClickOperation("戻る", "IM-A820設定画面", false);
        Timber.d("cleanup");

        final List<WifiConfiguration> configuredNetworks = _wifiManager.getConfiguredNetworks();

        if (configuredNetworks != null) {
            for (WifiConfiguration config : configuredNetworks) {
                final String __ssid = config.SSID.replace("\"", "");
                if (checkSSID(__ssid)) {
                    String result = deleteCompletlyWifiConnection(config.networkId) ? "Success" : "Failure";
                    Timber.d("delete Wifi Connection %s SSID: %s, networkId: %s", result, __ssid, config.networkId);
                }
            }
        }

        if (!AppPreference.isIFBoxSetupFinished()) {
//            _deviceNetworkManager.deletePersistentGroup()
//                    .subscribeOn(Schedulers.io())
//                    .subscribe(()->{},e->{});
        }
    };

    public enum DisplayTypes {
        TOP_MENU,
        DEVICE_MENU,
        PARAMETER_MENU,
        DEVICE_CONNECTING,
        FIRMWARE_SELECT,
        FIRMWARE_UPLOADING,
    }

    public static class ConnectionStatus {
        public static final int NONE = 0;
        public static final int FIEMWARE_INFO_CHECKED = 1;
        public static final int WIFI_CONNECTED = 2;
        public static final int CONFIG_UPLOADED = 3;
        public static final int WIFI_P2P_CONNECTED = 4;
    }

    public static class UploadStatus {
        public static final int NONE = 0;
        public static final int DOWNLOADED = 1;
        public static final int UPLOADED = 2;
        public static final int VERSION_CHECKED = 3;
    }

    public IFBoxSetupViewModel(Updater updater) {
//        _deviceNetworkManager = deviceNetworkManager;
//        _ifBoxManager = ifBoxManager;
        _updater = updater;
    }

    private final BroadcastReceiver _wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> results = ((WifiManager) _app.getSystemService(
                    Context.WIFI_SERVICE)).getScanResults();

            final List<ScanResult> targets = Observable.fromIterable(results)
                    .filter(r -> checkSSID(r.SSID))
                    .toList()
                    .blockingGet();

            _accessPoints.setValue(targets);

            Timber.i("wifi scan success and target num: %s", targets.size());

            for (ScanResult target : targets) {
                Timber.i("SSID: %s, BSSID: %s", target.SSID, target.BSSID);
            }

            isWifiScanCompleted(true);
            _app.unregisterReceiver(this);
        }
    };


    private MutableLiveData<DisplayTypes> _displayType = new MutableLiveData<>(DisplayTypes.TOP_MENU);
    public MutableLiveData<DisplayTypes> getDisplayType() {
        return _displayType;
    }
    public void setDisplayType(DisplayTypes displayType) {
        if (displayType == DisplayTypes.DEVICE_MENU) { Timber.i("デバイス一覧画面が表示されました。"); };
        if (displayType == DisplayTypes.FIRMWARE_SELECT) { Timber.i("ファームウェア一覧画面が表示されました。"); };
        _displayType.setValue(displayType);
    }

    public void setDisplay_DEVICE_MENU() {
        CommonClickEvent.RecordClickOperation("デバイス設定「詳細」", "IM-A820設定画面", false);
        setDisplayType(DisplayTypes.DEVICE_MENU);
    }

    public void setDisplay_FIRMWARE_SELECT() {
        CommonClickEvent.RecordClickOperation("ファームウェア設定", "接続設定完了画面", false);
        setDisplayType(DisplayTypes.FIRMWARE_SELECT);
    }

    private MutableLiveData<List<ScanResult>> _accessPoints = new MutableLiveData<>(new ArrayList<>());
    public MutableLiveData<List<ScanResult>> getAccessPoints() {
        return _accessPoints;
    }

    private MutableLiveData<Integer> _connectionStatus = new MutableLiveData<>(ConnectionStatus.NONE);
    public MutableLiveData<Integer> getConnectionStatus() {
        return _connectionStatus;
    }
    public void setConnectionStatus(int status) {
        Timber.d("デバイス接続状態:%d => %d", getConnectionStatus().getValue(), status);
        _handler.post(() -> {
            _connectionStatus.setValue(status);
        });
    }

    private MutableLiveData<Boolean> _isWifiScanCompleted = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isWifiScanCompleted() {
        return _isWifiScanCompleted;
    }
    public void isWifiScanCompleted(Boolean b) {
        _handler.post(() -> {
            _isWifiScanCompleted.setValue(b);
        });
    }


    private MutableLiveData<Integer> _uploadStatus = new MutableLiveData<>(UploadStatus.NONE);
    public MutableLiveData<Integer> getUploadStatus() {
        return _uploadStatus;
    }
    public void setUploadStatus(int status) {
        _handler.post(() -> {
            _uploadStatus.setValue(status);
        });
    }

    private MutableLiveData<Boolean> _isExecuteFailure = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isExecuteFailure() {
        return _isExecuteFailure;
    }
    public void isExecuteFailure(boolean b) {
        _handler.post(() -> {
            _isExecuteFailure.setValue(b);
        });
    }

    public void resume() {
//        final IntentFilter intentFilter = new IntentFilter();
//        // Wi-Fi ONにしておかないとスキャンできない
//        _wifiManager.setWifiEnabled(true);
//        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
//        _app.registerReceiver(_wifiScanReceiver, intentFilter);
//
//        wifiScan();
    }

    public void pause() {
//        _app.unregisterReceiver(_wifiScanReceiver);
    }

//    public Completable removeWifiP2pGroup() {
//        return _deviceNetworkManager.deletePersistentGroup();
//    }

    public void wifiScan() {
        isWifiScanCompleted(false);

        final IntentFilter intentFilter = new IntentFilter();

        // Wi-Fi ONにしておかないとスキャンできない

        //_wifiManager.setWifiEnabled(true);

        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        _app.registerReceiver(_wifiScanReceiver, intentFilter);

        boolean success = _wifiManager.startScan();
        if (!success) {
            isWifiScanCompleted(true);
        }
    }

    // とりあえずエラー内容をToastで表示する
    /*
    public Single<String> connectWifiP2p(String ssid) {
        Single<String> single = Single.create(emitter -> {
            _wifiManager.setWifiEnabled(true);
            AppPreference.clearWifiP2pDeviceInfo();
            _deviceNetworkManager.restart();

            final int oldNetworkId = _wifiManager.getConnectionInfo().getNetworkId();

            WifiConfiguration savedConfig = null;

            final List<WifiConfiguration> configuredNetworks = _wifiManager.getConfiguredNetworks();

            if (configuredNetworks != null) {
                for (WifiConfiguration config : configuredNetworks) {
                    if (config.SSID.replace("\"", "").equals(ssid)) {
                        savedConfig = config;
                    } else {
                        _wifiManager.disableNetwork(config.networkId);
                    }
                }
            }

            int networkId = savedConfig != null ? savedConfig.networkId : -1;


            if (networkId < 0) {
                final WifiConfiguration config = new WifiConfiguration();
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                config.SSID = "\"" + ssid + "\"";
                config.preSharedKey = "\"8888888888\"";  // Todo ハードコーディング
                networkId = _wifiManager.addNetwork(config);
            }

            Timber.d("networkId: %s", networkId);

            if (networkId < 0) {
                Timber.e("Wifi接続失敗");
                if (!emitter.isDisposed()) {
                    emitter.onError(new Throwable("IM-A820への接続エラー (再実行)"));
                } else {
                }
                return;
            }

            final int targetNetworkId = networkId;

            if (!_wifiManager.enableNetwork(networkId, true)) {
                Timber.e("Wifi接続失敗");
                if (!emitter.isDisposed()) {
                    emitter.onError(new Throwable("IM-A820への接続エラー (再実行)"));
                }
                return;
            }

            final ConnectivityManager connectivityManager =
                    (ConnectivityManager) _app.getSystemService(Context.CONNECTIVITY_SERVICE);

            final AtomicReference<Network> wifiNetwork = new AtomicReference<>(null);

            final NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build();

            final ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    wifiNetwork.set(network);
                    Timber.d("WiFi Network: %s", network);
                    connectivityManager.unregisterNetworkCallback(this);
                }
            };

            Timber.d("start requestNetwork");
            connectivityManager.requestNetwork(request, callback);

            while (true) {
                if (emitter.isDisposed()) {
                    return;
                }

                sleep(2000);

                if (wifiNetwork.get() == null) continue;

                NetworkInfo info = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (info == null) continue;

                if (info.isAvailable()) {
//                    final WifiInfo connectionInfo = _wifiManager.getConnectionInfo();
//                    Timber.d("Connected SSID: %s", connectionInfo.getSSID());
                    break;
                }
            }

            Timber.i("接続確認成功");
            setConnectionStatus(ConnectionStatus.WIFI_CONNECTED);
            Timber.i("設定書き込み開始");
            while (_deviceNetworkManager.getThisGroupInfo().getValue() == null) {
                if (emitter.isDisposed()) {
                    return;
                }
                sleep(1000);
            }

            final WifiP2pGroup wifiP2pGroup = _deviceNetworkManager.getThisGroupInfo().getValue();

            final Wifi.Request wifiRequest = new Wifi.Request() {{
                ssid = wifiP2pGroup.getNetworkName();
                passphrase = wifiP2pGroup.getPassphrase();
            }};

            try {
                _apiClient.setBaseUrl(null);
                _apiClient.postWifi(wifiRequest, wifiNetwork.get().getSocketFactory());
                _wifiManager.disableNetwork(networkId);
            } catch (Exception e) {
                Timber.e(e);
                if (!emitter.isDisposed()) {
                    emitter.onError(new Throwable("IM-A820へのWiFi設定送信エラー"));
                }
                return;
            }

            DeviceServiceInfo deviceServiceInfo;
            while (true) {
                if (emitter.isDisposed()) {
                    return;
                }
                sleep(3000);
                deviceServiceInfo = _deviceNetworkManager.getDeviceServiceInfo().getValue();

                if (deviceServiceInfo.isAvailable()) {
                    break;
                }
            }

            Timber.i("設定書き込み成功");
            setConnectionStatus(ConnectionStatus.CONFIG_UPLOADED);
            Timber.i("設定完了確認開始");
            _apiClient.setBaseUrl("http://" + deviceServiceInfo.getAddress());

            AtomicReference<Version.Response> version = new AtomicReference<>(null);
            Runnable wifiP2pVersionCheck = () -> {
                try {
                    Version.Response v = _apiClient.getVersion();
                    Timber.i("現行 name: %s, model: %s, version: %s", v.appName, v.appModel, v.appVersion);

                    version.set(v);
                } catch (Exception e) {
                    Timber.e(e);
                }
            };

            // WiFi Direct接続直後は通信に失敗することがあるのでリトライする
            for (int i = 0; i < 3; i++) {
                wifiP2pVersionCheck.run();
                if (version.get() != null) break;
                sleep(500);
            }

            if (version.get() == null) {
                if (!emitter.isDisposed()) {
                    emitter.onError(new Throwable("WiFi Direct疎通確認失敗 (再実行)"));
                }
                Timber.e("設定完了確認失敗");
                return;
            }

            Timber.i("設定完了確認成功");
            setConnectionStatus(ConnectionStatus.WIFI_P2P_CONNECTED);

            Timber.i("接続完了しました。タップするとファームウェア設定に進みます。");

            if (!emitter.isDisposed()) {
                emitter.onSuccess("success");
            }

        });

        return single.subscribeOn(Schedulers.io())
                .timeout(120, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread());
    }
    */

    /**
     *
     * @param ssid
     * @return
     */
    public Single<String> connectWifiP2p(String ssid) {
        String passphrase = "8888888888";

        return connectWifiP2pAsync(ssid, passphrase)
                .doOnError(err -> {
                    Timber.e(err, "【connectWifiP2p】SSID 接続失敗: %s", ssid);
                })
                // flatMap で後続の “接続確認成功” 以降に流し込む
                .flatMap(network -> Single.<String>create(emitter -> {
//                    // --- ここから元の「接続確認成功」以降 ---
//                    Timber.i("接続確認成功");
//                    setConnectionStatus(ConnectionStatus.WIFI_CONNECTED);
//
//                    Timber.i("設定書き込み開始");
//                    // グループ情報到着待ち
//                    while (_deviceNetworkManager.getThisGroupInfo().getValue() == null) {
//                        if (emitter.isDisposed()) return;
//                        sleep(1000);
//                    }
//
//                    WifiP2pGroup wifiP2pGroup = _deviceNetworkManager.getThisGroupInfo().getValue();
//                    Wifi.Request wifiRequest = new Wifi.Request() {{
//                        ssid = wifiP2pGroup.getNetworkName();
//                        passphrase = wifiP2pGroup.getPassphrase();
//                    }};
//
//                    // ここで network を使ってソケットを取得
//                    try {
//                        _apiClient.setBaseUrl(null);
//                        _apiClient.postWifi(
//                                wifiRequest,
//                                network.getSocketFactory()     // ← ここに Network を渡す！
//                        );
//                    } catch (Exception e) {
//                        emitter.onError(new RuntimeException("IM-A820へのWiFi設定送信エラー", e));
//                        return;
//                    }
//
//                    // 設定書き込み後の待機
//                    DeviceServiceInfo deviceServiceInfo;
//                    do {
//                        if (emitter.isDisposed()) return;
//                        sleep(3000);
//                        deviceServiceInfo = _deviceNetworkManager.getDeviceServiceInfo().getValue();
//                    } while (deviceServiceInfo == null || !deviceServiceInfo.isAvailable());
//
//                    Timber.i("設定書き込み成功");
//                    setConnectionStatus(ConnectionStatus.CONFIG_UPLOADED);
//
//                    // バージョン取得（リトライ込み）
//                    Timber.i("設定完了確認開始");
//                    _apiClient.setBaseUrl("http://" + deviceServiceInfo.getAddress());
//                    Version.Response version = null;
//                    for (int i = 0; i < 3; i++) {
//                        try {
//                            version = _apiClient.getVersion();
//                            Timber.i("現行 name:%s model:%s version:%s",
//                                    version.appName, version.appModel, version.appVersion);
//                            break;
//                        } catch (Exception ex) {
//                            Timber.e("バージョン取得失敗 #%d", i);
//                            sleep(500);
//                        }
//                    }
//                    if (version == null) {
//                        emitter.onError(new RuntimeException("WiFi Direct疎通確認失敗"));
//                        return;
//                    }
//
//                    Timber.i("設定完了確認成功");
//                    setConnectionStatus(ConnectionStatus.WIFI_P2P_CONNECTED);
//                    Timber.i("接続完了しました。タップするとファームウェア設定に進みます。");

                    emitter.onSuccess("success");
                    // --- ここまで ---
                }))
                .onErrorResumeNext(err -> {
                    String msg = err.getMessage();
                    // 元メッセージがそのまま使える場合もあれば、
                    // 接続段階でのエラーは「再実行」メッセージに揃える
                    if (msg != null && msg.contains("接続エラー")) {
                        return Single.error(new Throwable("IM-A820への接続エラー (再実行)", err));
                    }
                    return Single.error(err);
                })
                // すべて IO で実行、結果を UI で受け取りやすく
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(120, TimeUnit.SECONDS)
                ;
    }

    /**
     * API33以上・以下ともに Wi-Fi 接続を非同期で実行し、
     * 成功時に取得した Network オブジェクトを返す
     */
    private Single<Network> connectWifiP2pAsync(String ssid, String passphrase) {
        return Single.<Network>create((SingleEmitter<Network> emitter) -> {
                    // １）P2P グループ情報をクリア
                    AppPreference.clearWifiP2pDeviceInfo();
                    //_deviceNetworkManager.restart();

                    // ２）インフラWi-Fi or NetworkSpecifier で接続
                    if (Build.VERSION.SDK_INT >= 33) {
                        // 13+ は NetworkSpecifier 版
                        WifiNetworkSpecifier spec = new WifiNetworkSpecifier.Builder()
                                .setSsid(ssid)
                                .setWpa2Passphrase(passphrase)
                                .build();

                        NetworkRequest req = new NetworkRequest.Builder()
                                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                                .setNetworkSpecifier(spec)
                                .build();

                        ConnectivityManager cm = _app.getSystemService(ConnectivityManager.class);
                        ConnectivityManager.NetworkCallback cb = new ConnectivityManager.NetworkCallback() {
                            @Override
                            public void onAvailable(@NonNull Network network) {
                                cm.unregisterNetworkCallback(this);
                                Timber.i("【NetworkSpecifier】Wi-Fi接続成功: %s", ssid);
                                if (!emitter.isDisposed()) emitter.onSuccess(network);  // ← Network を返す
                            }
                            @Override
                            public void onUnavailable() {
                                cm.unregisterNetworkCallback(this);
                                if (!emitter.isDisposed()) emitter.onError(new RuntimeException("Wi-Fi接続失敗"));
                            }
                        };
                        cm.requestNetwork(req, cb);

                    } else {
                        // 12以下は従来版 + requestNetwork で Network を拾う
                        // (既存の addNetwork/enableNetwork 部分は省略)
                        // ... 省略: targetConfig を作成し enableNetwork() まで実施 ...
                        ConnectivityManager cm = _app.getSystemService(ConnectivityManager.class);
                        NetworkRequest req = new NetworkRequest.Builder()
                                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                                .build();

                        AtomicReference<Network> wrappedNet = new AtomicReference<>(null);
                        ConnectivityManager.NetworkCallback cb = new ConnectivityManager.NetworkCallback() {
                            @Override
                            public void onAvailable(@NonNull Network network) {
                                cm.unregisterNetworkCallback(this);
                                wrappedNet.set(network);
                                Timber.i("【Legacy】Wi-Fi接続成功: %s", ssid);
                                if (!emitter.isDisposed()) emitter.onSuccess(network);
                            }
                            @Override
                            public void onUnavailable() {
                                cm.unregisterNetworkCallback(this);
                                if (!emitter.isDisposed()) emitter.onError(new RuntimeException("Wi-Fi接続失敗"));
                            }
                        };
                        cm.requestNetwork(req, cb);
                    }
                })
                .subscribeOn(Schedulers.io())
                .timeout(60, TimeUnit.SECONDS)
                .doOnError(err -> Timber.e(err, "connectWifiP2pAsync error"))
                ;
    }

    public Single<List<FirmWareInfo>> getFirmwares() {
        setConnectionStatus(ConnectionStatus.NONE);
        return  _updater.getAllLatestIFBoxVersion()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable firmwareUpdate(FirmWareInfo firmWareInfo) {
        //ADD-S BMT S.Oyama 2024/12/16 フタバ双方向向け改修
        Timber.i("[FUTABA-D]IFBoxSetupViewModel::firmwareUpdate() in");
        PrinterManager printerManager = PrinterManager.getInstance();
        //ADD-E BMT S.Oyama 2024/12/16 フタバ双方向向け改修

        setUploadStatus(UploadStatus.NONE);
        setDisplayType(DisplayTypes.FIRMWARE_UPLOADING);
        // 接続処理スキップしてアップデートかける場合があるのでここでもセットする
        //_apiClient.setBaseUrl("http://" + _deviceNetworkManager.getDeviceServiceInfo().getValue().getAddress());

        return Completable.create(emitter -> {
            Timber.i("ファイルダウンロード開始");
            _updater.downloadIFBox(firmWareInfo.binKey, firmWareInfo.modelName).observeOn(Schedulers.io()).subscribe(downloadProgress -> {
            }, throwable -> {
                if (!emitter.isDisposed()) {
                    Timber.e("ファイルダウンロード失敗");
                    emitter.onError(throwable);
                }
            }, () -> {
                Timber.i("ファイルダウンロード成功");
                setUploadStatus(UploadStatus.DOWNLOADED);
                Timber.i("IM-A820に書き込み開始");
                final File localDir = _app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                final String targetFilename = localDir.toString() + File.separator + firmWareInfo.modelName;

                //ADD-S BMT S.Oyama 2024/12/16 フタバ双方向向け改修
                Timber.i("[FUTABA-D]IFBoxSetupViewModel::firmwareUpdate() update start");
                //printerManager.getIFBoxManager().updateFirm820_Start();             //820ファームのアップデート開始　820側の/alive 停止
                //ADD-E BMT S.Oyama 2024/12/16 フタバ双方向向け改修

                try {
                    _apiClient.postUpdate(targetFilename);
                    Timber.i("IM-A820に書き込み成功");
                    //ADD-S BMT S.Oyama 2024/12/16 フタバ双方向向け改修
                    //printerManager.getIFBoxManager().updateFirm820_Ended();             //820ファームのアップデート開始　820側の/alive 再開
                    Timber.i("[FUTABA-D]IFBoxSetupViewModel::firmwareUpdate() update Success");
                    //ADD-E BMT S.Oyama 2024/12/16 フタバ双方向向け改修
                    setUploadStatus(UploadStatus.UPLOADED);
                    Timber.i("バージョン確認開始");
                    Completable.create(recoonectEmmiter -> {
//                        // 再接続を待つ
//                        AtomicBoolean isLost = new AtomicBoolean(false);
//                        _deviceNetworkManager.getDeviceServiceInfo().subscribe(info -> {
//                            /**/ if (info.getLost()) isLost.set(true);  // 切断
//                            else if (isLost.get() && info.getService() != null) {
//                                recoonectEmmiter.onComplete();  // 切断後の再接続
//                                _apiClient.setBaseUrl("http://" + info.getAddress());
//                            }
//                        });

                        Thread.sleep(5000);
//                        _deviceNetworkManager.getDeviceServiceInfo().subscribe(info -> {
//                            if (info.isAvailable()) {
//                                recoonectEmmiter.onComplete();  // 切断後の再接続
//                                _apiClient.setBaseUrl("http://" + info.getAddress());
//                            }
//                        });
                    }).subscribe(() -> {
                        AtomicReference<Version.Response> version = new AtomicReference<>(null);
                        Runnable wifiP2pVersionCheck = () -> {
                            try {
                                Version.Response v = _apiClient.getVersion();
                                Timber.i("現行 name: %s, model: %s, version: %s", v.appName, v.appModel, v.appVersion);

                                version.set(v);
                            } catch (Exception e) {
                                Timber.e("バージョン取得失敗");
                                Timber.e(e);
                            }
                        };

                        // WiFi Direct接続直後は通信に失敗することがあるのでリトライする
                        for (int i = 0; i < 3; i++) {
                            wifiP2pVersionCheck.run();
                            if (version.get() != null) break;
                            sleep(500);
                        }

                        if (version.get() == null) {
                            Timber.e("バージョン確認失敗");
                            return;
                        }

                        Timber.i("バージョン確認成功");
                        setUploadStatus(UploadStatus.VERSION_CHECKED);
                        AppPreference.setIFBoxOTAInfo(firmWareInfo);
//                        AppPreference.setIFBoxVersionInfo(version.get());
                        //_ifBoxManager.restart();

                        if (!emitter.isDisposed()) {
                            emitter.onComplete();
                        }
                    }, e -> {
                        Timber.e(e);
                        if (!emitter.isDisposed()) {
                            emitter.onError(e);
                        }
                    });
                } catch (Exception e) {
                    Timber.e("IM-A820に書き込み失敗");
                    //ADD-S BMT S.Oyama 2024/12/16 フタバ双方向向け改修
                    //printerManager.getIFBoxManager().updateFirm820_Ended();             //820ファームのアップデート開始　820側の/alive 再開
                    Timber.i("[FUTABA-D]IFBoxSetupViewModel::firmwareUpdate() update Failed");
                    //ADD-E BMT S.Oyama 2024/12/16 フタバ双方向向け改修
                    Timber.e(e);
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }
            });
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

//    public String getIFBoxUrl() {
//         DeviceServiceInfo service = _deviceNetworkManager.getDeviceServiceInfo().getValue();
//         if (service == null) return null;
//         try {
//             return "http://" + service.getAddress();
//         } catch(Exception e) {
//             return null;
//         }
//    }

    private void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException ignore) { }
    }

    private boolean checkSSID(String ssid) {
        return ssid.matches("^IM-A820.*");
    }

    private boolean deleteCompletlyWifiConnection(int netId) {
        try {
            return (Boolean) Single.create(emitter -> {
                final Class<?> cls = Class.forName(WifiManager.class.getName() + "$ActionListener");
                final Object actionListener = cls.cast(Proxy.newProxyInstance(
                        cls.getClassLoader(),
                        new Class[] { cls },
                        (proxy, method, args) -> {
                            Timber.d("Listener Method: %s", method.getName());

                            if (method.getName().equals("onSuccess")) {
                                Timber.i("Wifi接続情報の削除成功 networkId: %s", netId);
                                if (!emitter.isDisposed()) {
                                    emitter.onSuccess(true);
                                }
                            } else {
                                Timber.e("Wifi接続情報の削除失敗 networkId: %s", netId);
                                if (!emitter.isDisposed()) {
                                    emitter.onSuccess(false);
                                }
                            }

                            return null;
                        }
                ));

                Timber.d("actionListener class : %s", cls.getClass().getName());

                WifiManager.class.getMethod(
                        "forget",
                        int.class,
                        cls
                ).invoke(_wifiManager, netId, actionListener);
            }).blockingGet();
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }
}
