package jp.mcapps.android.multi_payment_terminal.ui.setup;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.data.TabletLinkInfo;
import jp.mcapps.android.multi_payment_terminal.model.device_network_manager.DeviceNetworkManager;
import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.model.TabletLinker;
import jp.mcapps.android.multi_payment_terminal.model.device_network_manager.WifiP2pExtension;
import timber.log.Timber;

public class TabletLinkSetupViewModel extends ViewModel {
    private final DeviceNetworkManager _deviceNetworkManager;
    private final IFBoxManager _ifBoxManager;
    private final TabletLinker _tabletLinker;
    private final Context _appContext = MainApplication.getInstance();
    private final WifiManager _wifiManager = (WifiManager) _appContext.getSystemService(Context.WIFI_SERVICE);
    private int _deviceStatus  = -1;
    private Handler _handler = new Handler(Looper.getMainLooper());

    public enum Status {
        NONE,
        STARTED,
        PEER_DETECTED,
        CONNECTED,
        COMPLETED,
    }

    private final IntentFilter intentFilter = new IntentFilter() {
        {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);             // WIFI Direct利用可否が変わった
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);             // 接続できるデバイスを検知した
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);        // 接続状態が変更された
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);       // 自身の状態が変更された
            addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        }
    };

    private BroadcastReceiver receiver = null;
    private WifiP2pManager.Channel channel;
    private WifiP2pManager manager;
    private List<WifiP2pDevice> peers = new ArrayList<>();

    private MutableLiveData<Status> _status = new MutableLiveData<>(Status.NONE);
    public MutableLiveData<Status> getStatus() {
        return _status;
    }
    public void setStatus(Status status) {
        _handler.post(() -> {
            _status.setValue(status);
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

    public TabletLinkSetupViewModel(DeviceNetworkManager deviceNetworkManager, IFBoxManager ifBoxManager, TabletLinker tabletLinker) {
        _deviceNetworkManager = deviceNetworkManager;
        _ifBoxManager = ifBoxManager;
        _tabletLinker = tabletLinker;

        manager = (WifiP2pManager) _appContext.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(_appContext, _appContext.getMainLooper(), null);
    }

    public Completable connectP2p(String deviceAddress, int port) {
        // BroadcastReceiverをメインスレッドで実行しないためにスレッドプールを使う
        // doFinallyでshutdownするのでCompletableの外に定義する
        final ExecutorService pool = Executors.newFixedThreadPool(1);
        final List<Disposable> disposables = new ArrayList<>();

        return Completable.create(emitter -> {
            if (ActivityCompat.checkSelfPermission(_appContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (!emitter.isDisposed()) {
                    Timber.e("Not permitted");
                    emitter.onError(new Throwable("Not permitted"));
                }
                return;
            }

            Timber.i("デバイス検出開始");
            setStatus(Status.STARTED);

            if(!_wifiManager.isWifiEnabled()){
                if (!emitter.isDisposed()) {
                    Timber.e("Wifi not enabled");
                    emitter.onError(new WifiNotEnabledException());
                }
                return;
            }

            // 接続履歴の削除
            _deviceNetworkManager.deletePersistentGroup().blockingAwait();

            _deviceNetworkManager.stop();
            _ifBoxManager.stop();
            _tabletLinker.stop();

            final WifiP2pManager.PeerListListener peerListListener = peerList -> {

            };

            final AtomicBoolean discoveryStartFlg = new AtomicBoolean(false);

            receiver = new BroadcastReceiver() {
                @SuppressLint("CheckResult")
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void onReceive(Context context, Intent intent) {
                    pool.submit(() -> {
                        String action = intent.getAction();
                        if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                            Timber.d("WIFI_P2P_PEERS_CHANGED_ACTION");

                            final WifiP2pDeviceList peers = WifiP2pExtension.requestPeersSync(manager, channel);

                            // スレッドプールを1つしか用意していないので排他制御は不要
                            if (_status.getValue() == Status.STARTED) {
                                for (WifiP2pDevice d : peers.getDeviceList()) {
                                    if (d.deviceAddress.equals(deviceAddress)) {
                                        Timber.i("デバイス検出成功");
                                        setStatus(Status.PEER_DETECTED);
                                        Timber.i("デバイス接続開始");
                                        final WifiP2pConfig config = new WifiP2pConfig();
                                        config.deviceAddress = deviceAddress;
                                        config.wps.setup = WpsInfo.PBC;
                                        config.groupOwnerIntent = 0;

                                        if (!WifiP2pExtension.connectSync(manager, channel, config)) {
                                            if (!emitter.isDisposed()) {
                                                Timber.e("デバイス接続失敗");
                                                emitter.onError(new Throwable("接続処理に失敗"));
                                            }
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                            Timber.d("WIFI_P2P_STATE_CHANGED_ACTION");
                            final int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                            if (state == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {
                                if (!emitter.isDisposed()) {
                                    Timber.e("WiFi-Directが無効");
                                    emitter.onError(new Throwable("WiFi-Directが無効"));
                                }
                            }
                        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                            Timber.d("WIFI_P2P_CONNECTION_CHANGED_ACTION");

                            boolean isConnected = false;
                            ConnectivityManager cm = (ConnectivityManager) _appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                            Network nw = cm.getActiveNetwork();
                            if (nw != null) {
                                NetworkCapabilities actNw = cm.getNetworkCapabilities(nw);
                                if(actNw != null && actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                                    isConnected = true;
                            }

                            if (_status.getValue() == Status.PEER_DETECTED && !isConnected) {
                                if (!emitter.isDisposed()) {
                                    Timber.e("接続タイムアウト");
                                    emitter.onError(new Throwable("接続タイムアウト"));
                                }
                            }
                        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                            final WifiP2pDevice device =
                                    intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE, WifiP2pDevice.class);
                            Timber.d("Receive WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: %s", device);
                            _deviceStatus = device.status;

                            if (_status.getValue() == Status.PEER_DETECTED && device.status == WifiP2pDevice.CONNECTED) {
                                if (!emitter.isDisposed()) {
                                    Timber.i("デバイス接続成功");
                                    setStatus(Status.CONNECTED);
                                    final TabletLinkInfo info = new TabletLinkInfo();
                                    info.deviceAddress = deviceAddress;
                                    info.port = port;
                                    AppPreference.setTabletLinkInfo(info);
                                    _deviceNetworkManager.start();
                                    _ifBoxManager.start();
                                    _tabletLinker.start();

                                    final AtomicReference<Disposable> disposableReference = new AtomicReference<>(null);

                                    // _deviceNetworkManager接続が完了した時点でWiFiアクセスポイント等のタブレット連動の情報は更新される
                                    _deviceNetworkManager.postMyConnectionResult()
                                            .doOnSubscribe(disposableReference::set)
                                            .subscribe(result -> {
                                                if (result) {
                                                    setStatus(Status.COMPLETED);

                                                    emitter.onComplete();
                                                    Timber.i("タブレットとの通信成功");
                                                    disposableReference.get().dispose();
                                                } else {
                                                    Timber.i("タブレットとの通信失敗");
                                                    disposableReference.get().dispose();
                                                }
                                            });
                                }
                            }
                        } else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
                            int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
                            Timber.d("Receive WIFI_P2P_DISCOVERY_CHANGED_ACTION: %s", state);

                            /*
                             * STARTEDを受け取る直前にSTOPPEDを受け取って失敗になってしまうので
                             * STARTEDを受け取る前のSTOPPEDを無視するためにフラグをセットする
                             */
                            if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
                                discoveryStartFlg.set(true);
                            }

                            if (discoveryStartFlg.get() && _status.getValue() == Status.STARTED && state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {
                                if (!emitter.isDisposed()) {
                                    Timber.e("デバイス検索失敗");
                                    emitter.onError(new Throwable("デバイス検索に失敗"));
                                }
                            }
                        }
                    });
                }
            };

            _appContext.registerReceiver(receiver, intentFilter);

            if (!WifiP2pExtension.discoverPeersSync(manager, channel)) {
                if (!emitter.isDisposed()) {
                    Timber.e("Can't start discover peers");
                    emitter.onError(new Throwable("Can't start discover peers"));
                }
            }
        }).doFinally(() -> {
            pool.submit(() -> {
                for (Disposable d : disposables) {
                    if (!d.isDisposed()) {
                        d.dispose();
                    }
                }
                disposables.clear();

                if (_deviceStatus == WifiP2pDevice.AVAILABLE || _deviceStatus == WifiP2pDevice.INVITED) {
                    WifiP2pExtension.cancelConnectSync(manager, channel);
                }

                pool.shutdownNow();
                try { _appContext.unregisterReceiver(receiver); } catch(Exception ignore) { }
            });
        });
    }

    @SuppressLint("CheckResult")
    public Completable connectP2pBySSID(String ssid, int port) {
        setStatus(Status.STARTED);
        return Completable.create(emitter -> {
            scanDeviceAddress(ssid).subscribeOn(Schedulers.io()).subscribe(deviceAddress -> {
                connectP2p(deviceAddress, port).subscribeOn(Schedulers.io()).subscribe(emitter::onComplete, emitter::onError);
            }, emitter::onError);
        });
    }

    private Single<String> scanDeviceAddress(String ssid) {
        return Single.create(emitter -> {

            final Application app = MainApplication.getInstance();

            final AtomicReference<ScanResult> scanResultReference = new AtomicReference<>(null);
            final WifiManager wifiManager = (WifiManager) app.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            // WiFiアクセスポイントのスキャン開始失敗かスキャン結果が返るまで待つ
            Timber.i("WiFiアクセスポイント検出開始: %s", ssid);
            final BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Timber.i("アクセスポイント取得");
                    List<ScanResult> results = ((WifiManager) app.getApplicationContext().getSystemService(
                            Context.WIFI_SERVICE)).getScanResults();
                    Timber.i("WiFiアクセスポイントスキャン成功");
                    try {
                        final ScanResult result = Observable.fromIterable(results)
                                .filter(x -> x.getWifiSsid() != null && x.getWifiSsid().toString().equals(ssid))
                                .blockingFirst();

                        Timber.i("WiFiアクセスポイント検出: %s", ssid);

                        emitter.onSuccess(result.BSSID);

                    } catch (Exception ignore) {
                        if (!emitter.isDisposed()) {
                            emitter.onError(new Throwable("タブレットのSSID未検出"));
                        }
                    }

                    app.unregisterReceiver(this);
                }
            };

            if(!wifiManager.isWifiEnabled()){
                if (!emitter.isDisposed()) {
                    Timber.e("Wifi not enabled");
                    emitter.onError(new WifiNotEnabledException());
                }
                return;
            }

            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

            app.registerReceiver(receiver, intentFilter);
            @SuppressWarnings("deprecation")
            boolean success = wifiManager.startScan();

            if (!success) {
                app.unregisterReceiver(receiver);
                if (!emitter.isDisposed()) {
                    emitter.onError(new Throwable("WiFIアクセスポイントのスキャン開始失敗"));
                }
            }
        });
    }

    private void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException ignore) { }
    }

    public static class WifiNotEnabledException extends Throwable {
        public WifiNotEnabledException() {
            super("Wifi not enabled");
        }
    }
}
