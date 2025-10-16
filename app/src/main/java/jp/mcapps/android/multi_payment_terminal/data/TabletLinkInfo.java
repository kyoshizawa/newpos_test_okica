package jp.mcapps.android.multi_payment_terminal.data;

import com.google.gson.annotations.Expose;

public class TabletLinkInfo {
    @Expose
    public String deviceName;
    @Expose
    public String deviceAddress;
    @Expose
    public int port;
    @Expose
    public String ssid;
    @Expose
    public String passphrase;
    @Expose
    public Integer version;
}
