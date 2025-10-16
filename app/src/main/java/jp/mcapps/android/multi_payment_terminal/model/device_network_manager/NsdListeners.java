package jp.mcapps.android.multi_payment_terminal.model.device_network_manager;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import jp.mcapps.android.multi_payment_terminal.data.DeviceServiceInfo;
import timber.log.Timber;

class NsdListeners {
    public static class RegistrationListener implements NsdManager.RegistrationListener {
        public final SharedObjects _so;

        public RegistrationListener(SharedObjects so) {
            _so = so;
        }

        @Override
        public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
            Timber.tag(_so.TAG).d("onServiceRegistered");
        }

        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Timber.tag(_so.TAG).e("onRegistrationFailed");
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo arg0) {
            Timber.tag(_so.TAG).d("onServiceUnregistered");
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Timber.tag(_so.TAG).e("onUnregistrationFailed");
        }
    };

    public static class DiscoveryListener implements  NsdManager.DiscoveryListener {
        private final SharedObjects _so;

        public DiscoveryListener(SharedObjects so) {
            _so = so;
        }

        private boolean _isRegistered = false;
        public boolean isRegistered() {
            return _isRegistered;
        }
        public void isRegistered(boolean b) {
            _isRegistered = b;
        }

        @Override
        public void onStartDiscoveryFailed(String s, int i) {
            Timber.tag(_so.TAG).e("Discovery failed (start): %s, %s", s, i);
            _so.nsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onStopDiscoveryFailed(String s, int i) {
            Timber.tag(_so.TAG).e("Discovery failed (stop): %s, %s", s, i);
            _so.nsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onDiscoveryStarted(String s) {
            Timber.tag(_so.TAG).d("Discovery started: %s", s);
        }

        @Override
        public void onDiscoveryStopped(String s) {
            Timber.tag(_so.TAG).d("Discovery stopped: %s", s);
        }

        @Override
        public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
            Timber.tag(_so.TAG).d("Service found: %s", nsdServiceInfo);

            if (nsdServiceInfo != null) {
                String serviceName = nsdServiceInfo.getServiceName();
                if (Constants.Devices.findServiceName(serviceName)) {
                    _so.nsdManager.resolveService(nsdServiceInfo, new NsdManager.ResolveListener() {
                        @Override
                        public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {
                            /*
                                Todo タイマーの多重起動問題あり
                                     ログが大量に出て端末の起動/業務終了ができなくなるため一旦コメントアウト
                             */
//                            Timber.tag(_so.TAG).e("Resolve failed: %s, %s",
//                                    nsdServiceInfo != null ? nsdServiceInfo.getServiceName() : null, i);
                            Observable.timer(5, TimeUnit.SECONDS)
                                    .subscribe(t -> {
                                        _so.nsdManager.resolveService(nsdServiceInfo, this);
                                    });
                        }

                        @Override
                        public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
                            Timber.tag(_so.TAG).d("Resolve service: %s", nsdServiceInfo);
                            if (nsdServiceInfo == null) return;
                            _so.connector.onNsdServiceResolved(nsdServiceInfo);
                        }
                    });
                }
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
            Timber.tag(_so.TAG).d("Service lost: %s", nsdServiceInfo != null ? nsdServiceInfo.getServiceName() : null);

            if (nsdServiceInfo != null) {
                final DeviceServiceInfo resolved = _so.deviceServiceInfo.getValue();
//                if (resolved.isAvailable() && resolved.getService().getServiceName().equals(nsdServiceInfo.getServiceName())) {
                if (resolved.isAvailable()) {
                    _so.deviceServiceInfo.onNext(resolved.copy(true));
                    Timber.tag(_so.TAG).i("NSD Service lost: %s", nsdServiceInfo);
                }
            }
        }
    }
}
