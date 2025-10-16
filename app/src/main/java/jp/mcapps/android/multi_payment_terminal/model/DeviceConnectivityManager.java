package jp.mcapps.android.multi_payment_terminal.model;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;

import jp.mcapps.android.multi_payment_terminal.MainApplication;

public class DeviceConnectivityManager {
    public static void setProcessMobileNetwork(Network defaultNetwork) {
        ConnectivityManager connectivityManager = (ConnectivityManager) MainApplication.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
        for (Network network: connectivityManager.getAllNetworks()) {
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
            if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE && networkInfo.isAvailable()) {
                // 利用可能なモバイルネットワークが見つかれば、それを設定する
                ConnectivityManager.setProcessDefaultNetwork(network);
                return;
            }
        }
        // 利用可能なモバイルネットワークが見つからなかったので、渡されたネットワークを設定する
        ConnectivityManager.setProcessDefaultNetwork(defaultNetwork);
    }
}
