package jp.mcapps.android.multi_payment_terminal.model.device_network_manager;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.data.DeviceServiceInfo;
import timber.log.Timber;

/*
    ファイルを分割する前に元々DeviceNetworkManagerにあったフィールドをまとめたクラス
    分割したクラス間でオブジェクトが変わってしまわないために使用
    jp.mcapps.android.multi_payment_terminal.model.device_network_managerにあるファイル以外には公開しないため
    classのアクセス修飾子には何もつけない
*/

class SharedObjects {
    // ログをフィルタをするときにタグが異なると読みにくいのでDeviceNetworkManagerで統一する
    public final String TAG = DeviceNetworkManager.class.getSimpleName();

    public final Context appContext = MainApplication.getInstance();

    public final DeviceNetworkManager deviceNetworkManager;

    public final ExecutorService pool = Executors.newFixedThreadPool(1);

    public SharedObjects(DeviceNetworkManager deviceNetworkManager) {
        this.deviceNetworkManager = deviceNetworkManager;
    }

    public final WifiP2pManager wifiP2pManager = (WifiP2pManager) appContext.getSystemService(Context.WIFI_P2P_SERVICE);
    public final WifiP2pManager.ChannelListener wifiP2pChannelListener = () -> {
        // onChannelDisconnected
        Timber.tag(TAG).e("WifiP2pChannel Disconnected");
        if (wifiP2pManager != null) {
        }
    };
    public WifiP2pManager.Channel wifiP2pChannel = wifiP2pManager.initialize(
            appContext,
            Looper.getMainLooper(),
            wifiP2pChannelListener);
    public WifiP2pConnector connector;
    public final WifiP2pBroadcastReceiver wifiP2pReceiver = new WifiP2pBroadcastReceiver(this);

    public final NsdManager nsdManager = (NsdManager) appContext.getSystemService(Context.NSD_SERVICE);
    public final NsdListeners.DiscoveryListener nsdDiscoveryListener = new NsdListeners.DiscoveryListener(this);
    public final NsdListeners.RegistrationListener nsdRegistrationListener = new NsdListeners.RegistrationListener(this);

    public final WifiManager wifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);

    public final BehaviorSubject<Boolean> isWifiP2pEnabled = BehaviorSubject.createDefault(false);

    public final BehaviorSubject<List<WifiP2pDevice>> peers = BehaviorSubject.createDefault(new ArrayList<>());

    public final BehaviorSubject<WifiP2pInfo> thisConnectionInfo = BehaviorSubject.create();

    public final BehaviorSubject<WifiP2pDevice> thisDevice = BehaviorSubject.create();

    public final BehaviorSubject<WifiP2pGroup> thisGroupInfo = BehaviorSubject.create();

    public final BehaviorSubject<DeviceServiceInfo> deviceServiceInfo = BehaviorSubject.createDefault(new DeviceServiceInfo());

    public final BehaviorSubject<DeviceServiceInfo> tabletServiceInfo = BehaviorSubject.createDefault(new DeviceServiceInfo());

    public final PublishSubject<Boolean> postMyConnectionResult = PublishSubject.create();

    public final List<Disposable> disposables = new ArrayList<>();

    public final List<Runnable> retryQueue = new ArrayList<>();

    public final Gson gson = new Gson();

    public int serverPort = 0;
}
