package jp.mcapps.android.multi_payment_terminal.model;

import android.net.wifi.p2p.WifiP2pInfo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.data.DeviceServiceInfo;
import jp.mcapps.android.multi_payment_terminal.httpserver.events.EventBroker;
import jp.mcapps.android.multi_payment_terminal.model.device_network_manager.Constants;
import jp.mcapps.android.multi_payment_terminal.model.device_network_manager.DeviceNetworkManager;
import jp.mcapps.android.multi_payment_terminal.webapi.tablet.TabletApi;
import jp.mcapps.android.multi_payment_terminal.webapi.tablet.TabletApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.tablet.data.ConnectionInfo;
import jp.mcapps.android.multi_payment_terminal.webapi.tablet.data.SignedIn;
import jp.mcapps.android.multi_payment_terminal.webapi.tablet.data.Version;
import timber.log.Timber;

public class TabletLinker {
    private final MainApplication _app = MainApplication.getInstance();
    private final TabletApi _apiClient = new TabletApiImpl();
    private final DeviceNetworkManager _deviceNetworkManager;
    private final List<Disposable> _disposables = new ArrayList<>();
    private final Gson _gson = new GsonBuilder().disableHtmlEscaping().create();

    private boolean isConnected;
    private boolean isStarted;

    private final BehaviorSubject<SignedIn.Response> _driverSubject = BehaviorSubject.create();
    public BehaviorSubject getDriverSubject() {
        return _driverSubject;
    }

    private Disposable _postMyInfoDisposable;

    public TabletLinker(DeviceNetworkManager deviceNetworkManager) {
        _deviceNetworkManager = deviceNetworkManager;
        initialize();
    }

    public void start() {
        if (isStarted) return;
        else isStarted = true;

        _disposables.add(EventBroker.ima820.subscribe(connInfo -> {
            if (connInfo != null) {
                Timber.i("タブレット連動 IM-A820接続情報を受信 %s", connInfo);
                _deviceNetworkManager.getDeviceServiceInfo().onNext(new DeviceServiceInfo(connInfo));
            }
        }));

        Timber.d("start TabletLinker");
        _disposables.add(_deviceNetworkManager.getTabletServiceInfo().subscribe(info -> {
            if (info.getLost()) {
                isConnected = false;
                _apiClient.setBaseUrl(null);
            } else {
                if (info.isAvailable()) {
                    isConnected = true;
                    Timber.d("change tablet service info");

                    _apiClient.setBaseUrl(info.getAddress());
                } else {
                    isConnected = false;
                    _apiClient.setBaseUrl(null);
                }
            }
        }));
    }

    public void stop() {
        if (_postMyInfoDisposable != null && !_postMyInfoDisposable.isDisposed()) {
            _postMyInfoDisposable.dispose();
        }

        _postMyInfoDisposable = null;

        for (Disposable d : _disposables) {
            try { d.dispose(); } catch (Exception ignore) { }
        }
        _disposables.clear();
        initialize();
    }

    public void restart() {
        stop();
        start();
    }

    private void initialize() {
        _apiClient.setBaseUrl(null);
        isConnected = false;
        isStarted = false;
    }

    private void sleep(long millis) {
        try { Thread.sleep(millis); } catch (Exception ignore) {}
    }

    public boolean isConnected() {
        return isConnected;
    }

    public Single<Version.Response> getVersion() {
        return Single.create(emitter -> {
            if (!isConnected) {
                if (!emitter.isDisposed()) {
                    emitter.onError(new Throwable("Not connected."));
                }
            }

            try {
                final Version.Response version = _apiClient.getVersion();
                if (!emitter.isDisposed()) {
                    emitter.onSuccess(version);
                }
            } catch (Exception e) {
                if (!emitter.isDisposed()) {
                    emitter.onError(e);
                }
            }
        });
    }

    public Single<SignedIn.Response> getSignedIn() {
        return Single.create(emitter -> {
            if (!isConnected) {
                if (!emitter.isDisposed()) {
                    emitter.onError(new Throwable("Not connected."));
                }
            }

            try {
                final SignedIn.Response signedIn = _apiClient.getSignedIn();
                if (!emitter.isDisposed()) {
                    emitter.onSuccess(signedIn);
                }
            } catch (Exception e) {
                if (!emitter.isDisposed()) {
                    emitter.onError(e);
                }
            }
        });
    }
}
