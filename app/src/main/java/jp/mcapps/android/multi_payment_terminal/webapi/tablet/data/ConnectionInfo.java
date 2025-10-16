package jp.mcapps.android.multi_payment_terminal.webapi.tablet.data;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ConnectionInfo {
    @SerializedName("service_name")
    @Expose
    public String serviceName;

    @SerializedName("service_type")
    @Expose
    public String serviceType;

    @SerializedName("host_address")
    @Expose
    public String hostAddress;

    @SerializedName("port")
    @Expose
    public int port;

    @SerializedName("device_name")
    @Expose
    public String deviceName;
    @SerializedName("device_address")
    @Expose
    public String deviceAddress;

    @SerializedName("wifi_ap")
    @Expose
    public WifiAPInfo wifiAP;

    @SerializedName("os")
    @Expose
    public OSInfo os;

    @NonNull
    @Override
    public String toString() {
        return String.format("ConnectionInfo: {" +
                "\"serviceName\": \"%s\"" +
                ", \"serviceType\": \"%s\"" +
                ", \"hostAddress\": \"%s\"" +
                ", \"deviceAddress\": \"%s\"" +
                ", \"port\": %s" +
                ", \"wifiAP\": %s +" +
                ", \"os\": %s}", serviceName, serviceType, hostAddress, deviceAddress, port, wifiAP, os);
    }

    public static class WifiAPInfo {
        @SerializedName("ssid")
        @Expose
        public String ssid;

        @SerializedName("passphrase")
        @Expose
        public String passphrase;

        @NonNull
        @Override
        public String toString() {
            return String.format("{\"ssid\": \"%s\", \"passphrase\": \"%s\"}", ssid, passphrase);
        }
    }

    public static class OSInfo {
        @SerializedName("type")
        @Expose
        public String type;

        @SerializedName("version")
        @Expose
        public int version;

        @NonNull
        @Override
        public String toString() {
            return String.format("{\"type\": \"%s\", \"version\": \"%s\"}", type, version);
        }
    }
}
