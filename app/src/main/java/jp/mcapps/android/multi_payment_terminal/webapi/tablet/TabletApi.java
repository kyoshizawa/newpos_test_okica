package jp.mcapps.android.multi_payment_terminal.webapi.tablet;

import android.net.Network;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import jp.mcapps.android.multi_payment_terminal.webapi.tablet.data.ConnectionInfo;
import jp.mcapps.android.multi_payment_terminal.webapi.tablet.data.SignedIn;
import jp.mcapps.android.multi_payment_terminal.webapi.tablet.data.Version;

public interface TabletApi {
    String getBaseUrl();
    void setBaseUrl(String address);

    void setBaseUrl(String host, int port);
    void setConnectTimeout(int time, TimeUnit timeUnit);
    Version.Response getVersion() throws IOException;
    SignedIn.Response getSignedIn() throws IOException;
    ConnectionInfo[] postMyInfo(ConnectionInfo info) throws IOException;
    ConnectionInfo getIfBoxConnectionInfo() throws IOException;
    ConnectionInfo getTabletConnectionInfo(Network network) throws IOException;
}
