package jp.mcapps.android.multi_payment_terminal.data;

import android.bluetooth.BluetoothClass;
import android.net.nsd.NsdServiceInfo;
import android.text.TextUtils;

import java.lang.IllegalStateException;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import jp.mcapps.android.multi_payment_terminal.webapi.tablet.data.ConnectionInfo;
import timber.log.Timber;

public class DeviceServiceInfo implements Cloneable {
    private static final Gson _gson = new Gson();

    private String _serviceName;
    private String getServiceName() {
        return _serviceName;
    }

    private String _serviceType;
    private String getServiceType() {
        return _serviceType;
    }

    private String _hostAddress;
    private String getHostAddress() {
        return _hostAddress;
    }

    private int _port;
    private int getPort() {
        return _port;
    }

    private boolean _lost;
    public boolean getLost() {
        return _lost;
    }

    public void setLost(boolean b) {
        _lost = b;
    }

    public DeviceServiceInfo(NsdServiceInfo service, boolean lost) {
        _serviceName = service.getServiceName();
        _serviceType = service.getServiceType();
        _hostAddress = service.getHost().getHostAddress();
        _port = service.getPort();
        _lost = lost;
    }

    public DeviceServiceInfo(
            String serviceName,
            String serviceType,
            String hostAddress,
            int port,
            boolean lost
    ) {
        _serviceName = serviceName;
        _serviceType = serviceType;
        _hostAddress = hostAddress;
        _port = port;
        _lost = lost;
    }

    public DeviceServiceInfo(ConnectionInfo connInfo, boolean lost) {
        _serviceName = connInfo.serviceName;
        _serviceType = connInfo.serviceType;
        _hostAddress = connInfo.hostAddress;
        _port = connInfo.port;
        _lost = lost;
    }

    public DeviceServiceInfo(ConnectionInfo connInfo) {
        this(connInfo, false);
    }

    public DeviceServiceInfo(NsdServiceInfo service) {
        this(service, false);
    }

    public DeviceServiceInfo() {
    }

    public DeviceServiceInfo(String storedJson) {
        try {
            final Stored stored = _gson.fromJson(storedJson, Stored.class);
            _serviceName = stored.serviceName;
            _serviceType = stored.serviceType;
            _hostAddress = stored.hostAddress;
            _port = stored.port;
            _lost = false;

            Timber.d("restore %s", storedJson);
        } catch (Exception ignore) {
            Timber.e("invalid json format: %s", storedJson);
        }
    }

    public boolean isAvailable() {
        boolean isAvailable = !TextUtils.isEmpty(_hostAddress)
                && _port != 0;

        return isAvailable;
    }

    public String getAddress() {
//        if (!isAvailable()) {
//            throw new IllegalStateException("Property: service is null");
//        }

        return String.format("%s:%s", _hostAddress, _port);
    }

    // ポート81固定なのでこれを呼べるのはIM-A820だけ タブレットは非対応
    public String getWebSocketAddress() {
//        if (!isAvailable()) {
//            throw new IllegalStateException("Property: service is null");
//        }

        return String.format("%s:81", _hostAddress);
    }

    public DeviceServiceInfo copy (boolean lost) {
        try {
           final DeviceServiceInfo clone = (DeviceServiceInfo) super.clone();
           clone.setLost(lost);

           return clone;
        } catch (Exception ignore) { }

        return null;
    }

    public String toJson() {
        final Stored stored = new Stored();
        stored.serviceName = _serviceName;
        stored.serviceType = _serviceType;
        stored.hostAddress = _hostAddress;
        stored.port = _port;

        return _gson.toJson(stored);
    }

    private class Stored {
        @Expose
        @SerializedName("service_name")
        public String serviceName;

        @Expose
        @SerializedName("service_type")
        public String serviceType;

        @Expose
        @SerializedName("host_address")
        public String hostAddress;

        @Expose
        @SerializedName("port")
        public int port;
    }
}

