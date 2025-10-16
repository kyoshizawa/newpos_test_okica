package jp.mcapps.android.multi_payment_terminal.model.device_network_manager;

import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;

import java.lang.reflect.Proxy;
import java.util.Collection;

// 非公開メソッドをまとめたクラス
@SuppressWarnings("all")
public class WifiP2pReflections {
    // 2.4GHz にするにはチャンネルを 1 ~ 11 に設定する必要がある
    public static void setWifiChannels(
            WifiP2pManager instance,
            WifiP2pManager.Channel channel,
            int listeningChannel,
            int operatingChannel,
            WifiP2pManager.ActionListener listener
    ) {
        try {
            WifiP2pManager.class.getMethod(
                    "setWifiP2pChannels",
                    WifiP2pManager.Channel.class,
                    int.class,
                    int.class,
                    WifiP2pManager.ActionListener.class
            ).invoke(instance, channel, listeningChannel, operatingChannel, listener);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    // WiFi-Directの接続履歴一覧を取得する
    // リスナーが非公開クラスになっているため自作したリスナーを介してインスタンス化する
    public static void requestPersistentGroupInfo(WifiP2pManager instance, WifiP2pManager.Channel channel, GroupListListener listener) {
        try {
            WifiP2pManager.class.getMethod(
                    "requestPersistentGroupInfo",
                    WifiP2pManager.Channel.class,
                    getPersistentGroupInfoListenerClass()
            ).invoke(instance, channel, persistentGroupInfoListenerFactory(listener));
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    // WiFi-Directの接続履歴を削除する
    // 履歴を削除する以外にWiFi-DirectのSSID・パスワードを変更する方法がない
    // システム設定画面から削除できるがアプリケーションから行う場合は非公開メソッドで削除するしかない
    public static void deletePersistentGroup(WifiP2pManager instance, WifiP2pManager.Channel channel, int networkId, WifiP2pManager.ActionListener listener) {
        try {
            WifiP2pManager.class.getMethod(
                    "deletePersistentGroup",
                    WifiP2pManager.Channel.class,
                    int.class,
                    WifiP2pManager.ActionListener.class
            ).invoke(instance, channel, networkId, listener);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    // APIレベル29以前ではメソッドが公開されていない
    // WiFi-Directの情報を削除するには対象のnetworkIdが必要
    public static int getNetworkId(WifiP2pGroup instance) {
        try {
            return (Integer) WifiP2pGroup.class.getMethod("getNetworkId").invoke(instance);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    // WifiP2pGroupListクラスが非公開で引数の型に指定できないのでインスタンスはObject型で受け取る
    public static Collection<WifiP2pGroup> getGroupList(Object instance) {
        try {
            return (Collection<WifiP2pGroup>) Class.forName("android.net.wifi.p2p.WifiP2pGroupList").getMethod("getGroupList").invoke(instance);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    @FunctionalInterface
    public interface GroupListListener {
        void run(Collection<WifiP2pGroup> groups);
    }

    public static Object persistentGroupInfoListenerFactory(GroupListListener listener) {
        try {
            final Class<?> PersistentGroupInfoListener = getPersistentGroupInfoListenerClass();

            return PersistentGroupInfoListener.cast(Proxy.newProxyInstance(
                    PersistentGroupInfoListener.getClassLoader(),
                    new Class[] { PersistentGroupInfoListener },
                    (proxy, method, args) -> {
                        Collection<WifiP2pGroup> groups = getGroupList(args[0]);
                        listener.run(groups);

                        return null;
                    }
            ));
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    private static Class<?> getPersistentGroupInfoListenerClass() {
        try {
            return Class.forName(WifiP2pManager.class.getName() + "$PersistentGroupInfoListener");
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }
}
