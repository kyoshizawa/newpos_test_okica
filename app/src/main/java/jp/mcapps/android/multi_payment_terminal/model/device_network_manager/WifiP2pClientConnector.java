package jp.mcapps.android.multi_payment_terminal.model.device_network_manager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.text.TextUtils;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.data.DeviceServiceInfo;
import jp.mcapps.android.multi_payment_terminal.data.TabletLinkInfo;
import jp.mcapps.android.multi_payment_terminal.httpserver.HttpServer;
import jp.mcapps.android.multi_payment_terminal.webapi.tablet.TabletApi;
import jp.mcapps.android.multi_payment_terminal.webapi.tablet.TabletApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.tablet.data.ConnectionInfo;
import jp.mcapps.android.multi_payment_terminal.webapi.tablet.data.Version;
import timber.log.Timber;

public class WifiP2pClientConnector extends WifiP2pConnector {
    protected enum ConnectionStatus {
        Disconnected("未接続"),
        ConnectStart("接続開始"),
        Connecting("接続中"),
        Connected("接続完了");

        ConnectionStatus(String message) {
            _message = message;
        }

        // メッセージの定義はロギング用
        private final String _message;
        public String getMessage() {
            return _message;
        }

        private boolean eq(ConnectionStatus status) { return this == status; }
        private boolean ne(ConnectionStatus status) { return this != status; }
        private boolean lt(ConnectionStatus status) { return this.ordinal() <  status.ordinal(); }
        private boolean le(ConnectionStatus status) { return this.ordinal() <= status.ordinal(); }
        private boolean gt(ConnectionStatus status) { return this.ordinal() >  status.ordinal(); }
        private boolean ge(ConnectionStatus status) { return this.ordinal() >= status.ordinal(); }
    }

    // タブレットのシャットダウン・再起動で誤動作しないようにするための保護時間
    private long RECOVERY_PROTECT_TIME = 5 * 1000;

    private final MainApplication _app = MainApplication.getInstance();
    private final List<Disposable> disposables = new ArrayList<>();
    private final TabletApi tabletApi = new TabletApiImpl();
    private final Object _lock = new Object();
    private ConnectionStatus _connectionStatus;
    private ConnectionInfo _myConnInfo = null;
    private long _disconectedAt = -1;

    // ログ出力するためセッター経由で値をセットする
    private void setConnectionStatus(ConnectionStatus status) {
        // アプリ起動前にすでにWiFi Directに接続されている場合は初期状態 -> 接続完了となる
        Timber.tag(_so.TAG).i("タブレット連動状態: %s", status.getMessage());
        _connectionStatus = status;
    }

    public WifiP2pClientConnector(SharedObjects so) {
        super(so);
        setConnectionStatus(ConnectionStatus.Disconnected);
        // Todo 正常時に頻繁にタイムアウトするようであれば見直す
        tabletApi.setConnectTimeout(5, TimeUnit.SECONDS);
    }

    // discoverPeersに失敗すると何もできなくなるのでQueueに入れる
    private final Runnable discoverPeersUntilSuccess = new Runnable() {
        @Override
        public void run() {
            _so.retryQueue.remove(this);

            final int wifiState = _so.wifiManager.getWifiState();
            if (wifiState != WifiManager.WIFI_STATE_ENABLED && wifiState != WifiManager.WIFI_STATE_ENABLING) {
                _so.wifiManager.setWifiEnabled(true);
            }

            if (!WifiP2pExtension.discoverPeersSync(_so.wifiP2pManager, _so.wifiP2pChannel)) {
                Timber.tag(_so.TAG).i("discoverPeersに失敗 リトライキューに追加");
                _so.retryQueue.add(this);
            }
        }
    };

    @Override
    public void onStart() {
        final Version.Response version = AppPreference.getTabletVersionInfo();
        final String tabletDeviceId = version != null ? version.deviceId : "UNKNOWN";
        Timber.tag(_so.TAG).i("タブレットWiFi-Direct接続処理開始 deviceId %s, port %s",
                tabletDeviceId, AppPreference.getTabletLinkInfo().port);

        discoverPeersUntilSuccess.run();

        _so.serverPort = getFreePort();
        HttpServer.startServer(_so.serverPort);

        /*
         * 接続状態の時は定期的に通信を行う
         * 無通信でグループを外されるのを防止するためと
         * ネットワーク切断を検知できない場合があるため
         */
        disposables.add(Observable
                .interval(1, TimeUnit.MINUTES)
                .subscribe(ignore -> {
                    if (_connectionStatus.eq(ConnectionStatus.Connected) && _myConnInfo != null) {
                        if (!TextUtils.isEmpty(tabletApi.getBaseUrl())) {
                            try {
                                //Timber.tag(_so.TAG).d("タブレット連動 接続情報送信開始 %s", _myConnInfo);
                                final ConnectionInfo[] devices = tabletApi.postMyInfo(_myConnInfo);
                                //Timber.tag(_so.TAG).d("タブレット連動 接続情報送信成功 %s", _myConnInfo);

                                for (ConnectionInfo ci : devices) {
                                    if (ci.serviceName.equals(Constants.Devices.IFBox.getServiceName())) {
                                        Timber.tag(_so.TAG).i("(定期的)タブレット連動 IF-BOXの接続情報の取得成功 %s", ci);
                                        final DeviceServiceInfo dsi = new DeviceServiceInfo(ci);
                                        AppPreference.setIFBoxServiceInfo(dsi.toJson());
                                        _so.deviceServiceInfo.onNext(dsi);
                                    }
                                }
                            } catch (SocketTimeoutException e) {
                                Timber.tag(_so.TAG).e("(定期的)タブレット連動 接続情報送信タイムアウト発生 再接続実行");
                                onDisconnected();
                            } catch (Exception e) {
                                Timber.tag(_so.TAG).e(e);
                                onDisconnected();
                            }
                        }
                    }
                })
        );
    }

    @Override
    public void onStop() {
        _so.serverPort = 0;
        HttpServer.stopServer();

        // 接続キャンセル
        final WifiP2pDevice device = _so.thisDevice.getValue();
        if (device == null || device.status == WifiP2pDevice.CONNECTED) {
            WifiP2pExtension.removeGroupSync(_so.wifiP2pManager, _so.wifiP2pChannel);
        } else if (device.status == WifiP2pDevice.AVAILABLE || device.status == WifiP2pDevice.INVITED) {
            WifiP2pExtension.cancelConnectSync(_so.wifiP2pManager, _so.wifiP2pChannel);
        }

        tabletApi.setBaseUrl(null);
        for (Disposable disposable : disposables) {
            disposable.dispose();
        }

        disposables.clear();
    }

    @Override
    public void onNsdServiceResolved(NsdServiceInfo nsdServiceInfo) {
        // タブレット連動時はNSDは使用しない
    }

    @Override
    public void onPeersChange(WifiP2pDeviceList peers) {
        synchronized (_lock) {
            if (_connectionStatus.ne(ConnectionStatus.Disconnected)) {
                return;
            }

            TabletLinkInfo info = AppPreference.getTabletLinkInfo();
            if (info == null) return;

            WifiP2pDevice targetPeer = null;
            boolean sameDeviceName = false;

            for (WifiP2pDevice peer : peers.getDeviceList()) {
                // WiFI接続でのリカバリ処理ように同じデバイス名の端末があるかを確認しておく
                // 別の端末を拾ってしまう可能性もあるのでこれではデバイス検出を確定しない
                if (peer.deviceName.equals(info.deviceName)) {
                    sameDeviceName = true;
                }

                if (peer.deviceAddress.equals(info.deviceAddress)) {
                    targetPeer = peer;
                    break;
                }
            }

            if (targetPeer != null) {
                final WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = targetPeer.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                config.groupOwnerIntent = 0;

                if (_connectionStatus.ne(ConnectionStatus.Disconnected) || !targetPeer.isGroupOwner()) {
                    return;
                }

                if (WifiP2pExtension.connectSync(_so.wifiP2pManager, _so.wifiP2pChannel, config)) {
                    setConnectionStatus(ConnectionStatus.Connecting);
                }
            }
            else {
                final boolean tryWifiRecovery = sameDeviceName
                        && info.ssid != null
                        && info.passphrase != null
                        && info.version != null && info.version >= Build.VERSION_CODES.R;  // Android11以降のタブレットを対象とする

                if (tryWifiRecovery) {
                    /*
                     * 近くに同一端末がある状態でタブレットを再起動すると再起動実行直後にここの処理に入ってしまう
                     * WiFi-Directのピアデバイスとしては見えなくなるがWiFiのアクセスポイントが削除されるまでラグがあるため
                     * WiFi接続処理が実行されてしまう
                     * WiFi-Direct切断から一定時間経過するまではリカバリ処理は行わない
                     */
                    final long elapsed  = new Date().getTime() - _disconectedAt;

                    if (RECOVERY_PROTECT_TIME - elapsed > 0) {
                        Timber.tag(_so.TAG).i("WiFi-Direct接続リカバリ 切断から%s秒以内のためリカバリ処理を行わない", RECOVERY_PROTECT_TIME / 1000);
                        return;
                    }

                    Timber.tag(_so.TAG).i("WiFi-Direct接続リカバリ開始 deviceName: %s", info.deviceName);

                    final List<ScanResult> results = scanWifiAp();
                    if (results != null) {
                        try {
                            final ScanResult result = Observable.fromIterable(results)
                                    .filter(x -> x.SSID.equals(info.ssid))
                                    .blockingFirst();

                            if (!info.deviceAddress.equals(result.BSSID)) {
                                info.deviceAddress = result.BSSID;
                                AppPreference.setTabletLinkInfo(info);
                            }
                        } catch (Exception ignore) {
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onConnectionChanged(NetworkInfo netInfo) {
        if (netInfo.isConnected()) {
            // connected
            onConnected();
        } else {
            // disconnected
            onDisconnected();
        }
    }

    @Override
    public void onThisDeviceChange(WifiP2pDevice device) {
        if (device.status == WifiP2pDevice.CONNECTED) {
            // onConnectedでもrequestGroupInfoが呼ばれるがタブレット連動の成功可否の確認のためここでも呼ぶ
            WifiP2pGroup group = WifiP2pExtension.requestGroupInfoSync(_so.wifiP2pManager, _so.wifiP2pChannel);
            if (group != null && AppPreference.getTabletLinkInfo().deviceAddress.equals(group.getOwner().deviceAddress)) {
                /*
                 * 接続済みの状態でアプリが再起動した場合(アプリが落ちた等)
                 * 通信可能な状態か不明なので一度切断する
                 */
                if (_connectionStatus.eq(ConnectionStatus.Disconnected)) {
                    setConnectionStatus(ConnectionStatus.Connected);
                    onDisconnected();
                }
                setConnectionStatus(ConnectionStatus.Connected);
                onConnected();
            }
        }
    }

    @Override
    public void onDiscoveryChanged(int state) {
        if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
            Timber.tag(_so.TAG).v("WIFI_P2P_DISCOVERY_STARTED");
        }
        else if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {
            Timber.tag(_so.TAG).v("WIFI_P2P_DISCOVERY_STOPPED");
            if (_connectionStatus == ConnectionStatus.Disconnected) {
                discoverPeersUntilSuccess.run();
            }
        } else {
            Timber.tag(_so.TAG).v("Unknown WIFI_P2P_DISCOVERY_CHANGED_ACTION %s", state);
        }
    }

    @Override
    protected void onConnected() {
        Timber.tag(_so.TAG).i("WiFi P2P Connected");

        boolean isGranted = ContextCompat.checkSelfPermission(
                _so.appContext,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        if (!isGranted) {
            throw new IllegalStateException("Permission: Manifest.permission.ACCESS_FINE_LOCATION is required.");
        }

        // グループオーナー情報を取得
        final WifiP2pGroup group = WifiP2pExtension.requestGroupInfoSync(_so.wifiP2pManager, _so.wifiP2pChannel);

        // 接続情報を取得
        final WifiP2pInfo info =
                WifiP2pExtension.requestConnectionInfoSync(_so.wifiP2pManager, _so.wifiP2pChannel);

        if (group == null || info == null) return;

        setTabletDeviceInfo(info);
        setMyConnInfo(info);

        disposables.add(Completable.create(emitter -> {
            try {
                final Version.Response version = tabletApi.getVersion();
                AppPreference.setTabletVersionInfo(version);
                Timber.tag(_so.TAG).i("タブレット連動: バージョン情報 %s", _so.gson.toJson(version));
            } catch (Exception e) {
                Timber.tag(_so.TAG).e("タブレット連動: バージョン情報取得に失敗");
            }
        }).observeOn(Schedulers.io()).subscribe());

        final String ssid = group.getNetworkName();
        final String pass = group.getPassphrase();
        Timber.tag(_so.TAG).i("WiFi P2P GroupInfo: SSID=%s PASS=%s", ssid, pass);
        _so.thisGroupInfo.onNext(group);

        Timber.tag(_so.TAG).i("WiFi P2P ConnectionInfo: %s", info);
        if (info.groupFormed && info.isGroupOwner) {
            // グループオーナー
            // Todo グループオーナーではないはずなのにグループオーナーになってしまった場合何かしらのリカバリが必要
        } else if (info.groupFormed) {
            // グループオーナーでない
        }
        _so.thisConnectionInfo.onNext(info);

        if (!_so.deviceServiceInfo.getValue().isAvailable()) {
            final String storedJson = AppPreference.getIFBoxServiceInfo();
            if (!TextUtils.isEmpty(storedJson)) {
                _so.deviceServiceInfo.onNext(new DeviceServiceInfo(storedJson));
            }
        }
    }

    @Override
    protected void onDisconnected() {
        Timber.tag(_so.TAG).w("WiFi P2P Disconnected");

        // 起動時に意図的に1度切断処理をコールしているため2回目以降の呼び出しでで切断時間を保存するようにする
        if (_disconectedAt < 0) {
            _disconectedAt = 0;
        } else {
            _disconectedAt = new Date().getTime();
        }

        final DeviceServiceInfo tsi = _so.tabletServiceInfo.getValue().copy(true);
        _so.tabletServiceInfo.onNext(tsi);
        _myConnInfo = null;

        // WiFi Direct 再設定
        if (_so.isWifiP2pEnabled.getValue() != null && _so.isWifiP2pEnabled.getValue()) {
            if (_connectionStatus.ne(ConnectionStatus.Disconnected)) {
                setConnectionStatus(ConnectionStatus.Disconnected);
                Timber.tag(_so.TAG).i("reconnect");
                _so.deviceNetworkManager.restart();
            }
        }
    }

    private void setTabletDeviceInfo(WifiP2pInfo info) {
        final InetAddress ownerAddress = info.groupOwnerAddress;
        final DeviceServiceInfo serviceInfo = new DeviceServiceInfo(
                Constants.Devices.Tablet.getServiceName(),
                Constants.ServiceTypes.HTTP_TCP.get(),
                ownerAddress.getHostAddress(),
                AppPreference.getTabletLinkInfo().port,
                false
        );

        tabletApi.setBaseUrl(serviceInfo.getAddress());
        _so.tabletServiceInfo.onNext(serviceInfo);
    }

    private int getFreePort() {
        try {
            final ServerSocket socket = new ServerSocket(0);
            final int port = socket.getLocalPort();
            socket.close();
            return port;
        } catch (Exception e) {
            Timber.tag(_so.TAG).e(e);
            return 0;
        }
    }

    private void setMyConnInfo(WifiP2pInfo info) {
        try {
            final InetAddress ownerHost = info.groupOwnerAddress;

            // IPアドレス
            final byte[] ownerAddress = ownerHost.getAddress();

            List<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

            String myAddress = null;

            for (NetworkInterface networkInterface : networkInterfaces) {
                if (myAddress != null) break;

                for (InterfaceAddress nic : networkInterface.getInterfaceAddresses()) {
                    if (myAddress != null) break;

                    final int subnetLength = nic.getNetworkPrefixLength();
                    final byte[] targetAddress = nic.getAddress().getAddress();

                    boolean ret = true;

                    for (int i = 0; i < subnetLength; i++) {
                        final int bytesIndex = i / 8;
                        final int bitIndex = i % 8;
                        final int bitMast = 1 << (7 - bitIndex);
                        final int myMasked = (int) targetAddress[bytesIndex] & bitMast;
                        final int ownerMasked = (int) ownerAddress[bytesIndex] & bitMast;
                        if (myMasked != ownerMasked) {
                            ret = false;
                            break;
                        }
                    }

                    if (ret) myAddress = nic.getAddress().getHostAddress();
                }
            }

            if (myAddress == null) {
                Timber.tag(_so.TAG).e("タブレット連動 PT-750のIPアドレス不明");
                return;
            }

            ConnectionInfo myInfo = new ConnectionInfo();
            myInfo.serviceName = Constants.Devices.Self.getServiceName();
            myInfo.serviceType = Constants.ServiceTypes.HTTP_TCP.get();
            myInfo.hostAddress = myAddress;
            myInfo.port = _so.serverPort;

            _myConnInfo = myInfo;

            //Timber.tag(_so.TAG).d("タブレット連動 接続情報送信開始 %s", _myConnInfo);
            final ConnectionInfo[] devices = tabletApi.postMyInfo(_myConnInfo);
            _so.postMyConnectionResult.onNext(true);
            //Timber.tag(_so.TAG).d("タブレット連動 接続情報送信成功 %s", _myConnInfo);

            for (ConnectionInfo ci : devices) {
                if (ci.serviceName.equals(Constants.Devices.IFBox.getServiceName())) {
                    Timber.tag(_so.TAG).i("タブレット連動 IF-BOXの接続情報の取得成功 %s", ci);
                    final DeviceServiceInfo dsi = new DeviceServiceInfo(ci);
                    AppPreference.setIFBoxServiceInfo(dsi.toJson());
                    _so.deviceServiceInfo.onNext(dsi);
                }
                if (ci.serviceName.equals(Constants.Devices.Tablet.getServiceName())) {
                    final TabletLinkInfo tabletLinkInfo = AppPreference.getTabletLinkInfo();

                    if (ci.deviceName != null) {
                        tabletLinkInfo.deviceName = ci.deviceName;
                    }

                    if (ci.wifiAP != null) {
                        tabletLinkInfo.ssid = ci.wifiAP.ssid;
                        tabletLinkInfo.passphrase = ci.wifiAP.passphrase;
                    }

                    if (ci.os != null) {
                        tabletLinkInfo.version = ci.os.version;
                    }

                    AppPreference.setTabletLinkInfo(tabletLinkInfo);
                }
            }
        } catch(Throwable t) {
            Timber.tag(_so.TAG).e(t,"タブレット連動 接続情報のセットに失敗");
            _so.postMyConnectionResult.onNext(false);
        }
    }

    /**
     * WiFi-Directで接続が出来なくなった時のリカバリ処理
     * 通常のWiFiで接続を行う
     *
     * @return 成否
     */
    private Network connectWifiAP() {
        final TabletLinkInfo info = AppPreference.getTabletLinkInfo();

        if (info == null || info.ssid == null || info.passphrase == null) {
            Timber.tag(_so.TAG).i("WiFi-Direct接続リカバリ 接続情報なし");
            return null;
        }

        final List<ScanResult> scanResults = scanWifiAp();

        if (scanResults == null) {
            Timber.e("WiFiスキャンに失敗");
            return null;
        }

        try {
            // 例外が発生しない確認するだけなので変数は使用しない
            final ScanResult ignore = Observable.fromIterable(scanResults)
                    .filter(x -> x.SSID.equals(info.ssid))
                    .blockingFirst();
            Timber.tag(_so.TAG).i("WiFi-Direct接続リカバリ WiFiアクセスポイント検出: %s", info.ssid);
        } catch (Exception ignore) {
            return null;
        }

        WifiConfiguration savedConfig = null;

        final List<WifiConfiguration> configuredNetworks = _so.wifiManager.getConfiguredNetworks();

        if (configuredNetworks != null) {
            for (WifiConfiguration config : configuredNetworks) {
                if (config.SSID.replace("\"", "").equals(info.ssid)) {
                    savedConfig = config;
                } else {
                    _so.wifiManager.disableNetwork(config.networkId);
                }
            }
        }

        int networkId = savedConfig != null ? savedConfig.networkId : -1;

        if (networkId < 0) {
            final WifiConfiguration config = new WifiConfiguration();
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.SSID = "\"" + info.ssid + "\"";
            config.preSharedKey = "\"" + info.passphrase + "\"";
            networkId = _so.wifiManager.addNetwork(config);
        }

        Timber.tag(_so.TAG).d("networkId: %s", networkId);

        if (networkId < 0) {
            Timber.tag(_so.TAG).e("WiFi-Direct接続リカバリ WiFi接続失敗 networkId=%s", networkId);
            return null;
        }

        if (!_so.wifiManager.enableNetwork(networkId, true)) {
            Timber.tag(_so.TAG).e("WiFi-Direct接続リカバリ enableNetwork失敗 networkId=%s", networkId);
            return null;
        }

        final ConnectivityManager connectivityManager =
                (ConnectivityManager) _app.getSystemService(Context.CONNECTIVITY_SERVICE);


        final AtomicReference<Network> wifiNetworkReference = new AtomicReference<>(null);

        Completable.create(emitter -> {
            final ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    wifiNetworkReference.set(network);
                    Timber.tag(_so.TAG).d("WiFi Network: %s", network);
                    connectivityManager.unregisterNetworkCallback(this);
                    emitter.onComplete();
                }

                @Override
                public void onUnavailable() {
                    super.onUnavailable();
                    Timber.tag(_so.TAG).d("WiFi Network unavailable");
                    emitter.onComplete();
                }
            };

            Timber.tag(_so.TAG).d("start requestNetwork");

            final NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build();

            connectivityManager.requestNetwork(request, callback);
        }).blockingAwait();

        final Network wifiNetwork = wifiNetworkReference.get();

        if (wifiNetwork == null) {
            return null;
        }

        final NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (!networkInfo.isAvailable()) {
            disconnectWifiAP();
            return null;
        }

        Timber.tag(_so.TAG).i("WiFi-Direct接続リカバリ WiFi接続成功");
        return wifiNetwork;
    }

    private void disconnectWifiAP() {
        final List<WifiConfiguration> configuredNetworks = _so.wifiManager.getConfiguredNetworks();

        if (configuredNetworks != null) {
            for (WifiConfiguration config : configuredNetworks) {
                _so.wifiManager.disableNetwork(config.networkId);
            }
        }
    }

    private List<ScanResult> scanWifiAp() {
        final AtomicReference<List<ScanResult>> scanResult = new AtomicReference<>();

        // WiFiアクセスポイントのスキャン開始失敗かスキャン結果が返るまで待つ
        Completable.create(emitter -> {
            final BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    List<ScanResult> results = ((WifiManager) _app.getSystemService(
                            Context.WIFI_SERVICE)).getScanResults();

                    scanResult.set(results);
                    _app.unregisterReceiver(this);
                    emitter.onComplete();
                }
            };

            _so.wifiManager.setWifiEnabled(true);
            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

            _app.registerReceiver(receiver, intentFilter);
            boolean success = _so.wifiManager.startScan();

            if (!success) {
                _app.unregisterReceiver(receiver);
                emitter.onComplete();
            }
        }).blockingAwait();

        return scanResult.get();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignore) {
        }
    }
}