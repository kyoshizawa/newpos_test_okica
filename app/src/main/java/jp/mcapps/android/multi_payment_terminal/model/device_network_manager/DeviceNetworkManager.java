package jp.mcapps.android.multi_payment_terminal.model.device_network_manager;

import android.Manifest;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import java.net.Socket;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.core.content.ContextCompat;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.data.DeviceServiceInfo;
import jp.mcapps.android.multi_payment_terminal.httpserver.HttpServer;
import timber.log.Timber;

public class DeviceNetworkManager {
    private final SharedObjects _so = new SharedObjects(this);

    public BehaviorSubject<Boolean> isWifiP2pEnabled() { return  _so.isWifiP2pEnabled; }
    public BehaviorSubject<List<WifiP2pDevice>> getPeers() {return _so.peers; }
    public BehaviorSubject<WifiP2pInfo> getThisConnectionInfo() { return _so.thisConnectionInfo; }
    public BehaviorSubject<WifiP2pDevice> getThisDevice() { return _so.thisDevice; }
    public BehaviorSubject<WifiP2pGroup> getThisGroupInfo() { return _so.thisGroupInfo; }
    public BehaviorSubject<DeviceServiceInfo> getDeviceServiceInfo() { return _so.deviceServiceInfo; }
    public BehaviorSubject<DeviceServiceInfo> getTabletServiceInfo() { return _so.tabletServiceInfo; }
    public PublishSubject<Boolean> postMyConnectionResult() { return _so.postMyConnectionResult; }
    public int getServerPort() { return _so.serverPort;  }

    public void start() {
        _so.pool.submit(() -> {
            boolean isGranted = ContextCompat.checkSelfPermission(
                    _so.appContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED;

            if (!isGranted) {
                throw new IllegalStateException("Permission: Manifest.permission.ACCESS_FINE_LOCATION is required.");
            }

            setConnector();

            // 失敗した処理に関しては1分毎にリトライする
            // Todo 1度に1つのリトライ処理していないけどそれで問題ないかはおいおい考える
            Observable.interval(1, TimeUnit.MINUTES)
                    .subscribeOn(Schedulers.newThread())
                    .doOnSubscribe(_so.disposables::add)
                    .doFinally(_so.retryQueue::clear)
                    .subscribe(t -> {
                        if (_so.retryQueue.size() > 0) {
                            _so.retryQueue.get(0).run();
//                            _so.retryQueue.remove(0);
                        }
                    });


            // BroadcastReceiver登録
            final IntentFilter intentFilter = new IntentFilter();

            intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);             // WIFI Direct利用可否が変わった
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);             // 接続できるデバイスを検知した
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);        // 接続状態が変更された
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);       // 自身の状態が変更された
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
            _so.appContext.registerReceiver(_so.wifiP2pReceiver, intentFilter);

            _so.connector.onStart();
        });
    }

    public void stop() {
        boolean isGranted = ContextCompat.checkSelfPermission(
                _so.appContext,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        if (!isGranted) {
            throw new IllegalStateException("Permission: Manifest.permission.ACCESS_FINE_LOCATION is required.");
        }

        // BroadcastReceiver登録解除
        try {
            //startさせないでStopすると
            //java.lang.IllegalArgumentException: Receiver not registered
            //が発生するため、try catchしておく
            _so.appContext.unregisterReceiver(_so.wifiP2pReceiver);
        } catch(Throwable t) {
            Timber.tag(_so.TAG).e("unregisterReceiver(wifiP2pReceiver) error");
        }

        // WiFi P2P デバイス捜索の停止
        _so.wifiP2pManager.stopPeerDiscovery(_so.wifiP2pChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // success
            }
            @Override

            public void onFailure(int i) {
                Timber.tag(_so.TAG).w("Failed on WifiP2pManager.stopPeerDiscovery: %s", i);
            }
        });

        _so.deviceServiceInfo.onNext(new DeviceServiceInfo());
        _so.tabletServiceInfo.onNext(new DeviceServiceInfo());

        _so.connector.onStop();

        for (Disposable disposable : _so.disposables) {
            disposable.dispose();
        }
    }

    public void restart() {
        stop();
        start();
    }

    // 保存されているWifi Directの接続履歴を全て削除する
    @SuppressWarnings("all")
    public Completable deletePersistentGroup() {
        AppPreference.clearWifiP2pDeviceInfo();

        Timber.tag(_so.TAG).i("WiFi Direct接続情報削除開始");
        return Completable.create(emitter -> {
            try {
                Collection<WifiP2pGroup> groups =
                        WifiP2pExtension.requestPersistentGroupInfoSync(_so.wifiP2pManager, _so.wifiP2pChannel);

                int successCount = 0;

                for (WifiP2pGroup group : groups) {
                    int netId = WifiP2pReflections.getNetworkId(group);
                    if (WifiP2pExtension.deletePersistentGroupSync(_so.wifiP2pManager, _so.wifiP2pChannel, netId)) {
                        successCount++;
                    }
                }

                _so.deviceServiceInfo.onNext(new DeviceServiceInfo());
                _so.tabletServiceInfo.onNext(new DeviceServiceInfo());

                WifiP2pExtension.cancelConnectSync(_so.wifiP2pManager, _so.wifiP2pChannel);
                WifiP2pExtension.removeGroupSync(_so.wifiP2pManager, _so.wifiP2pChannel);

                Timber.tag(_so.TAG).i("WiFi Direct接続情報削除終了 成功 %s件, 失敗 %s件",
                        successCount, groups.size() - successCount);

                if (!emitter.isDisposed()) {
                    emitter.onComplete();
                }
            } catch(Exception e) {
                Timber.tag(_so.TAG).e(e);
                if (!emitter.isDisposed()) {
                    emitter.onError(e);
                }
            }
        });
    }

    private void setConnector() {
        boolean isGroupOwner = AppPreference.getTabletLinkInfo() == null;

        if (_so.connector == null) {
            if (isGroupOwner) {
                Timber.tag(_so.TAG).i("グループオーナーモード");
                _so.connector = new WifiP2pGroupOwnerConnector(_so);
            } else {
                Timber.tag(_so.TAG).i("クライアントモード");
                _so.connector = new WifiP2pClientConnector(_so);
            }
        } else {
            if (isGroupOwner && !_so.connector.getClass().equals(WifiP2pGroupOwnerConnector.class)) {
                Timber.tag(_so.TAG).i("グループオーナーモード");
                _so.connector = new WifiP2pGroupOwnerConnector(_so);
            }
            else if (!isGroupOwner && !_so.connector.getClass().equals(WifiP2pClientConnector.class)) {
                Timber.tag(_so.TAG).i("クライアントモード");
                _so.connector = new WifiP2pClientConnector(_so);
            }
        }
    }
}