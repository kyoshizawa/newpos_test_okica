package jp.mcapps.android.multi_payment_terminal.model.device_network_manager;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;

import java.util.Collection;
import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Wifi;
import timber.log.Timber;

/*
 * メインスレッドからの呼び出し不可
 * WifiP2pManagerの各コールバックがメインスレッドで実行されるため
 * 呼び出し側もメインスレッドで動いているとデッドロックがかかってしまうので例外を返す
 */
public class WifiP2pExtension {
    public static String TAG = "";

    public static boolean connectSync(WifiP2pManager manager, WifiP2pManager.Channel channel, WifiP2pConfig config) {
        checkThread();
        try {
            return (boolean) Single.create(emitter -> {
                Timber.d("start connect");
                //manager.connect(channel, config, boolActionListener(emitter, "connect"));
            }).blockingGet();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean clearServiceRequestsSync(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        checkThread();
        try {
            return (boolean) Single.create(emitter -> {
                manager.clearServiceRequests(channel, boolActionListener(emitter, "clearServiceRequests"));
            }).blockingGet();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean createGroupSync(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        checkThread();
        try {
            return (boolean) Single.create(emitter -> {
                //manager.createGroup(channel, boolActionListener(emitter, "createGroup"));
            }).blockingGet();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean createGroupSync(WifiP2pManager manager, WifiP2pManager.Channel channel, WifiP2pConfig config) {
        checkThread();
        try {
            return (boolean) Single.create(emitter -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    //manager.createGroup(channel, config, boolActionListener(emitter, "createGroup"));
                }
            }).blockingGet();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean deletePersistentGroupSync(WifiP2pManager manager, WifiP2pManager.Channel channel, int networkId) {
        checkThread();
        try {
            return (boolean) Single.create(emitter -> {
                WifiP2pReflections.deletePersistentGroup(manager, channel, networkId, boolActionListener(emitter, "deletePersistentGroup"));
            }).blockingGet();
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static Collection<WifiP2pGroup> requestPersistentGroupInfoSync(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        checkThread();
        try {
            return (Collection<WifiP2pGroup>) Single.create(emitter -> {
                WifiP2pReflections.requestPersistentGroupInfo(manager, channel, emitter::onSuccess);
            }).blockingGet();
        } catch (Exception ignore) {
            return null;
        }
    }

    public static boolean stopPeerDiscoverySync(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        checkThread();
        try {
            return (boolean) Single.create(emitter -> {
                //manager.stopPeerDiscovery(channel, boolActionListener(emitter, "stopPeerDiscovery"));
            }).blockingGet();
        } catch (Exception ignore) {
            return false;
        }
    }

    public static boolean cancelConnectSync(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        checkThread();
        return (boolean) Single.create(emitter -> {
            //manager.cancelConnect(channel, boolActionListener(emitter, "cancelConnect"));
        }).blockingGet();
    }

    public static boolean removeGroupSync(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        checkThread();
        try {
            return (boolean) Single.create(emitter -> {
                manager.removeGroup(channel, boolActionListener(emitter, "removeGroup"));
            }).blockingGet();
        } catch (Exception ignore) {
            return false;
        }
    }

    public static boolean discoverPeersSync(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        checkThread();
        try {
            return (boolean) Single.create(emitter -> {
                manager.discoverPeers(channel, boolActionListener(emitter, "discoverPeers"));
            }).blockingGet();
        } catch (Exception ignore) {
            return false;
        }
    }

    public static WifiP2pDeviceList requestPeersSync(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        checkThread();
        try {
            return (WifiP2pDeviceList) Single.create(emitter -> {
                manager.requestPeers(channel, emitter::onSuccess);
            }).blockingGet();
        } catch (Exception ignore) {
            return null;
        }
    }

    public static WifiP2pGroup requestGroupInfoSync(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        checkThread();
        try {
            return (WifiP2pGroup) Single.create(emitter -> {
                manager.requestGroupInfo(channel, emitter::onSuccess);
            }).blockingGet();
        } catch (Exception e) {
            return null;
        }
    }

    public static WifiP2pInfo requestConnectionInfoSync(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        checkThread();
        try {
            return (WifiP2pInfo) Single.create(emitter -> {
                manager.requestConnectionInfo(channel, emitter::onSuccess);
            }).blockingGet();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean setWifiChannelsSync(
            WifiP2pManager manager,
            WifiP2pManager.Channel channel,
            int listeningChannel,
            int operatingChannel
    ) {
        checkThread();
        try {
            return (boolean) Single.create(emitter -> {
                WifiP2pReflections.setWifiChannels(
                        manager,
                        channel,
                        listeningChannel,
                        operatingChannel,
                        boolActionListener(emitter, "setWifiChannels"));
            }).blockingGet();
        } catch (Exception ignore) {
            return false;
        }
    }

    private static void checkThread() {
        if (Thread.currentThread().getId() == 1) {
            throw new IllegalThreadStateException("メインスレッドからの実行不可");
        }
    }

    @SuppressWarnings("all")
    private static WifiP2pManager.ActionListener boolActionListener(@NonNull SingleEmitter emitter, String methodName) {
        return new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Timber.tag(TAG).v("%s Success", methodName);
                emitter.onSuccess(true);
            }

            @Override
            public void onFailure(int reason) {
                final String msg = reason == WifiP2pManager.BUSY
                        ? "BUSY"
                        : reason == WifiP2pManager.ERROR
                        ? "ERROR"
                        : reason == WifiP2pManager.NO_SERVICE_REQUESTS
                        ? "NO_SERVICE_REQUESTS"
                        : reason == WifiP2pManager.P2P_UNSUPPORTED
                        ? "P2P_UNSUPPORTED"
                        : "UNKNOWN";

                Timber.tag(TAG).i("%s Failure reason: %s", methodName, msg);
                emitter.onSuccess(false);
            }
        };
    }
}
