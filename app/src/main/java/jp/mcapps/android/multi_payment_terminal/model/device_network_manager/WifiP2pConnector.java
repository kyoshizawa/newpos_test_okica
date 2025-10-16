package jp.mcapps.android.multi_payment_terminal.model.device_network_manager;

import android.net.NetworkInfo;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;

public abstract class WifiP2pConnector {
    public abstract void onStart();
    public abstract void onStop();
    public abstract void onNsdServiceResolved(NsdServiceInfo nsdServiceInfo);
    public abstract void onPeersChange(WifiP2pDeviceList peers);
    public abstract void onConnectionChanged(NetworkInfo netInfo);
    public abstract void onThisDeviceChange(WifiP2pDevice device);
    public abstract void onDiscoveryChanged(int state);

    protected abstract void onConnected();
    protected abstract void onDisconnected();

    protected final SharedObjects _so;

    public WifiP2pConnector(SharedObjects so) {
        _so = so;
    }

    protected void disconnect(WifiP2pManager.ActionListener listener) {
        _so.wifiP2pManager.removeGroup(_so.wifiP2pChannel, listener);
    }

    protected void connectCancel(WifiP2pManager.ActionListener listener) {
        final WifiP2pDevice device = _so.thisDevice.getValue();
        if (device == null || device.status == WifiP2pDevice.CONNECTED) {
            disconnect(listener);
        } else if (device.status == WifiP2pDevice.AVAILABLE || device.status == WifiP2pDevice.INVITED) {
            _so.wifiP2pManager.cancelConnect(_so.wifiP2pChannel, listener);
        }
    }
}