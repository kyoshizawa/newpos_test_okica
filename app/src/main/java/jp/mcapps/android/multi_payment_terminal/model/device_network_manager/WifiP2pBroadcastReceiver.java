package jp.mcapps.android.multi_payment_terminal.model.device_network_manager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;

import java.util.ArrayList;
import java.util.Collection;

import timber.log.Timber;

public class WifiP2pBroadcastReceiver extends BroadcastReceiver {
    private final SharedObjects _so;

    public WifiP2pBroadcastReceiver(SharedObjects so) {
        _so = so;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        _so.pool.submit(new BackgroundTask(context, intent, _so));
    }

    private static class BackgroundTask implements Runnable {
        private final Context _context;
        private final Intent _intent;
        private final SharedObjects _so;

        public BackgroundTask(Context context, Intent intent, SharedObjects so) {
            _context = context;
            _intent = intent;
            _so = so;
        }

        @Override
        public void run() {
            if (_intent == null) return;

            final String action = _intent.getAction();

            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // Check to see if Wi-Fi is enabled and notify appropriate activity
                final int state = _intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                _so.isWifiP2pEnabled.onNext(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
                //Timber.tag(_so.TAG).v("Receive WIFI_P2P_STATE_CHANGED_ACTION: %s", state);
                Timber.tag(_so.TAG).i("Receive WIFI_P2P_STATE_CHANGED_ACTION: %s", state);
                Timber.tag(_so.TAG).i("WiFi P2P Enabled: %s", _so.isWifiP2pEnabled.getValue());
            }
            else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                final Collection<WifiP2pDevice> devices =
                        ((WifiP2pDeviceList) _intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST)).getDeviceList();
                if (devices != null) {
                    //Timber.tag(_so.TAG).v("Receive WIFI_P2P_PEERS_CHANGED_ACTION: %s", devices.size());
                    Timber.tag(_so.TAG).i("Receive WIFI_P2P_PEERS_CHANGED_ACTION: %s", devices.size());
                }

                WifiP2pDeviceList peers =
                        WifiP2pExtension.requestPeersSync(_so.wifiP2pManager, _so.wifiP2pChannel);

                updatePeers(peers);
                Timber.i("Thread id %s", Thread.currentThread().getId());
                _so.connector.onPeersChange(peers);
            }
            else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                final NetworkInfo netInfo =
                        (NetworkInfo) _intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                //Timber.tag(_so.TAG).v("Receive WIFI_P2P_CONNECTION_CHANGED_ACTION: %s", netInfo);
                Timber.tag(_so.TAG).i("Receive WIFI_P2P_CONNECTION_CHANGED_ACTION: %s", netInfo);

                _so.connector.onConnectionChanged(netInfo);
            }
            else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                // Respond to this device's wifi state changing
                final WifiP2pDevice device =
                        (WifiP2pDevice) _intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                //Timber.tag(_so.TAG).v("Receive WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: %s", device);
                Timber.tag(_so.TAG).i("Receive WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: %s", device);
                _so.thisDevice.onNext(device);
                _so.connector.onThisDeviceChange(device);
            }
            else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
                int state = _intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
                _so.connector.onDiscoveryChanged(state);
            }
            else {
                if (_intent.getExtras() != null && _intent.getExtras().keySet() != null) {
                    for (String key : _intent.getExtras().keySet()) {
                        //Timber.tag(_so.TAG).v("Receive action: %s extra: %s", _intent.getAction(), key);
                        Timber.tag(_so.TAG).i("Receive action: %s extra: %s", _intent.getAction(), key);
                    }
                } else {
                    //Timber.tag(_so.TAG).v("Receive action: %s", _intent.getAction());
                    Timber.tag(_so.TAG).i("Receive action: %s", _intent.getAction());
                }
            }
        }

        protected void updatePeers(WifiP2pDeviceList peers) {
            final Collection<WifiP2pDevice> devices = peers.getDeviceList();

            for (WifiP2pDevice device : devices) {
                //Timber.tag(_so.TAG).v("Update Peers: %s, %s, %s",
                Timber.tag(_so.TAG).i("Update Peers: %s, %s, %s",
                        device.deviceName, device.deviceAddress, device.isGroupOwner());
            }

            _so.peers.onNext(new ArrayList<>(devices));
        }
    }
}
