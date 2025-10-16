package jp.mcapps.android.multi_payment_terminal.webapi.tablet;

import android.net.Network;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.net.SocketFactory;

import jp.mcapps.android.multi_payment_terminal.webapi.McHttpClient;
import jp.mcapps.android.multi_payment_terminal.webapi.tablet.data.ConnectionInfo;
import jp.mcapps.android.multi_payment_terminal.webapi.tablet.data.SignedIn;
import jp.mcapps.android.multi_payment_terminal.webapi.tablet.data.Version;
import okhttp3.OkHttpClient;
import timber.log.Timber;

public class TabletApiImpl implements TabletApi {
    private final McHttpClient _httpClient = McHttpClient.getInstance();
    private final String API_VERSION = "v1";
    private OkHttpClient _client = null;

    private String _baseUrl = null;

    public String getBaseUrl() {
        return _baseUrl;
    }

    public void setBaseUrl(String address) {
        if (address == null) {
            Timber.d("clear tablet url");
            _baseUrl = null;
            return;
        }

        String baseUrl = String.format("http://%s", address);
        _baseUrl = baseUrl;

        Timber.d("set tablet url %s", baseUrl);
    }

    public void setBaseUrl(String host, int port) {
        String baseUrl = String.format("http://%s:%s", host, port);
        _baseUrl = baseUrl;

        Timber.d("set tablet url %s", baseUrl);
    }

    @Override
    public void setConnectTimeout(int time, TimeUnit timeUnit) {
        _client = _httpClient.getClientBuilder()
                .connectTimeout(time, timeUnit)
                .build();
    }

    public Version.Response getVersion() throws IOException {
        final String path = "/v1/version";

        final Version.Response version = _httpClient
                .get(makeUrl(path))
                .client(_client)
                .responseObject(Version.Response.class);

        return version;
    }

    public SignedIn.Response getSignedIn() throws IOException {
        final String path = "/v1/driver/signedin";

        final SignedIn.Response signedIn = _httpClient
                .get(makeUrl(path))
                .client(_client)
                .responseObject(SignedIn.Response.class);

        return signedIn;
    }

    @Override
    public ConnectionInfo[] postMyInfo(ConnectionInfo info) throws IOException {
        final String path = "/v1/device/pt750";

        final ConnectionInfo[] response = _httpClient
                .post(makeUrl(path))
                .setBody(info)
                .client(_client)
                .responseObject(ConnectionInfo[].class);

        return response;
    }

    @Override
    public ConnectionInfo getIfBoxConnectionInfo() throws IOException {
        final String path = "/v1/device/ima820";

        final ConnectionInfo response = _httpClient
                .get(makeUrl(path))
                .client(_client)
                .responseObject(ConnectionInfo.class);

        return response;
    }

    @Override
    public ConnectionInfo getTabletConnectionInfo(@Nullable Network network) throws IOException {
        final String path = "/v1/device/tapp";
        OkHttpClient client = _client;

        if (network != null) {
            client = _client.newBuilder()
                    .socketFactory(network.getSocketFactory())
                    .build();
        }

        final ConnectionInfo response = _httpClient
                .get(makeUrl(path))
                .client(client)
                .responseObject(ConnectionInfo.class);

        return response;
    }

    private String makeUrl(String path) {
        if (_baseUrl == null) {
            throw new IllegalStateException("Tablet baseURL is not set");
        }

        return _baseUrl + path;
    }
}