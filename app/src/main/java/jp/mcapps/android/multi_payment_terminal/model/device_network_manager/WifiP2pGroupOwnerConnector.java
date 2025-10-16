package jp.mcapps.android.multi_payment_terminal.model.device_network_manager;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Build;
import android.provider.Settings;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.core.content.ContextCompat;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import jp.mcapps.android.multi_payment_terminal.data.DeviceServiceInfo;
import timber.log.Timber;

public class WifiP2pGroupOwnerConnector extends WifiP2pConnector {
    public WifiP2pGroupOwnerConnector(SharedObjects so) {
        super(so);
    }
    public List<Disposable> disposables = new ArrayList<>();

    private void createGroupOwner() {
        boolean isGranted = ContextCompat.checkSelfPermission(
                _so.appContext,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        if (!isGranted) {
            throw new IllegalStateException("Permission: Manifest.permission.ACCESS_FINE_LOCATION is required.");
        }

        WifiP2pExtension.createGroupSync(_so.wifiP2pManager, _so.wifiP2pChannel);
    }

    @Override
    public void onStart() {
        setWifiChannels();
    }

    @Override
    public void onStop() {
        // 接続キャンセル
        final WifiP2pDevice device = _so.thisDevice.getValue();
        if (device == null || device.status == WifiP2pDevice.CONNECTED) {
            WifiP2pExtension.removeGroupSync(_so.wifiP2pManager, _so.wifiP2pChannel);
        } else if (device.status == WifiP2pDevice.AVAILABLE || device.status == WifiP2pDevice.INVITED) {
            WifiP2pExtension.cancelConnectSync(_so.wifiP2pManager, _so.wifiP2pChannel);
        }

        for (Disposable disposable : disposables) {
            disposable.dispose();
        }
        disposables.clear();
    }

    @Override
    public void onNsdServiceResolved(NsdServiceInfo nsdServiceInfo) {
        try {
            final InetAddress serviceHost = nsdServiceInfo.getHost();
            final InetAddress ownerHost = _so.thisConnectionInfo.getValue() != null
                    ? _so.thisConnectionInfo.getValue().groupOwnerAddress
                    : null;

            if (ownerHost instanceof Inet4Address && serviceHost instanceof Inet4Address) {
                // IPアドレス
                final byte[] serviceAddress = serviceHost.getAddress();
                final byte[] ownerAddress = ownerHost.getAddress();

                // IPアドレス帯をチェック
                final NetworkInterface networkInterface = NetworkInterface.getByInetAddress(ownerHost);

                final boolean isReachableService = Observable.fromIterable(networkInterface.getInterfaceAddresses())
                        .filter(nic -> nic.getAddress().equals(ownerHost))
                        .any(nic -> {
                            final int subnetLength = nic.getNetworkPrefixLength();
                            boolean ret = true;
                            for (int i = 0; i < subnetLength; i++) {
                                final int bytesIndex = i / 8;
                                final int bitIndex = i % 8;
                                final int bitMast = 1 << (7 - bitIndex);
                                final int serviceMasked = (int) serviceAddress[bytesIndex] & bitMast;
                                final int ownerMasked = (int) ownerAddress[bytesIndex] & bitMast;
                                if (serviceMasked != ownerMasked) {
                                    ret = false;
                                    break;
                                }
                            }
                            return ret;
                        }).blockingGet();

                //Timber.d("Verify service network: %s, (%s, %s)", isReachableService, serviceHost, ownerHost);
                Timber.i("Verify service network: %s, (%s, %s)", isReachableService, serviceHost, ownerHost);
                if (isReachableService) {
                    _so.deviceServiceInfo.onNext(new DeviceServiceInfo(nsdServiceInfo));
                    Timber.i("NSD Service resolved: %s", nsdServiceInfo);
                }
            }
        } catch(Throwable t) {
            Timber.e(t, "nsdManager.resolveService onServiceResolved() Error");
        }
    }

    @Override
    public void onPeersChange(WifiP2pDeviceList peers) {
        // 処理なし
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
        // 処理なし
    }

    @Override
    public void onDiscoveryChanged(int state) {

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
        final WifiP2pGroup group =
                WifiP2pExtension.requestGroupInfoSync(_so.wifiP2pManager, _so.wifiP2pChannel);

        // 接続情報を取得
        final WifiP2pInfo info =
                WifiP2pExtension.requestConnectionInfoSync(_so.wifiP2pManager, _so.wifiP2pChannel);

        if (group == null || info == null) return;

        final String ssid = group.getNetworkName();
        final String pass = group.getPassphrase();
        Timber.tag(_so.TAG).i("WiFi P2P GroupInfo: SSID=%s PASS=%s", ssid, pass);
        _so.thisGroupInfo.onNext(group);

        Timber.tag(_so.TAG).i("WiFi P2P ConnectionInfo: %s", info);
        if (info.groupFormed && info.isGroupOwner) {
            // グループオーナー
        } else if (info.groupFormed) {
            // グループオーナーでない
        }
        _so.thisConnectionInfo.onNext(info);

        // Nsd
        if (!_so.nsdDiscoveryListener.isRegistered()) {
            _so.nsdDiscoveryListener.isRegistered(true);

            try {
                _so.nsdManager.discoverServices("_http._tcp.", NsdManager.PROTOCOL_DNS_SD, _so.nsdDiscoveryListener);
                Timber.tag(_so.TAG).i("Register NsdDiscoveryListener Success");
            } catch (Exception e) {
                Timber.tag(_so.TAG).e(e.getMessage(), "Register NsdDiscoveryListener Failure");
            }
        }
    }

    @Override
    protected void onDisconnected() {
        Timber.tag(_so.TAG).w("WiFi P2P Disconnected");

        if (_so.isWifiP2pEnabled.getValue() != null && _so.isWifiP2pEnabled.getValue()) {
            setWifiChannels();
        }
    }

    private void setWifiChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {  //
            final WifiP2pConfig config = new WifiP2pConfig.Builder()
                    .setNetworkName("DIRECT-pt750")  // DIRECT-以外はてきとうにきめた
                    .setPassphrase("8888888888")  //てきとうにきめた
                    .enablePersistentMode(true)
                    .setGroupOperatingFrequency(2437)  // 2.4GHz
                    .build();

            // 正直いらない
            config.groupOwnerIntent = 15;
            config.wps.setup = WpsInfo.PBC;

            WifiP2pExtension.createGroupSync(_so.wifiP2pManager, _so.wifiP2pChannel, config);
        } else {
            if (1 == Settings.System.getInt(_so.appContext.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0)) {
                Timber.tag(_so.TAG).i("機内モード");
                Observable.timer(10, TimeUnit.SECONDS).subscribe(l -> {
                    setWifiChannels();
                });
                return;
            } else {
            }
            // ESP32のチップは 2.4GHz のWiFiしか接続できない (802.11b/g/n)
            // 2.4GHz にするにはチャンネルを 1 ~ 11 に設定する必要がある（非公開メソッドでしか設定できない）
            // StackOverflowでは チャンネルを設定してから GroupOwnerを作成しないとできないとの記載あり
            // チャンネルを毎回変えるとESP32が接続できなくなるっぽいので固定にしておく
            final int lc = 0;
            final int oc = 6;

            if (WifiP2pExtension.setWifiChannelsSync(_so.wifiP2pManager, _so.wifiP2pChannel, lc, oc)) {
                createGroupOwner();
            }
        }
    }
}
