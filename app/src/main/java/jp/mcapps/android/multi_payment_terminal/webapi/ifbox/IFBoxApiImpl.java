package jp.mcapps.android.multi_payment_terminal.webapi.ifbox;

import java.io.IOException;

import javax.net.SocketFactory;

import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.webapi.McHttpClient;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Chip;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Ems;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Meter;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Version;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Wifi;
import okhttp3.OkHttpClient;
import timber.log.Timber;

public class IFBoxApiImpl implements IFBoxApi {
    private final McHttpClient _httpClient = McHttpClient.getInstance();
    private final String API_VERSION = "v1";

    private String _baseUrl = null;

    public void setBaseUrl(String url) {
        _baseUrl = url;
    }

    public Version.Response getVersion() throws IOException {
        final String path = "/version";

        final Version.Response version = _httpClient
                .get(makeUrl(path))
                .responseObject(Version.Response.class);

        return version;
    }

    public String getHeep() throws IOException {
        final String path = "/heep";

        final String heep = _httpClient
                .get(makeUrl(path))
                .responseString();

        return heep;
    }

    public Chip.Response getChip() throws IOException {
        final String path = "/chip";

        final Chip.Response chip = _httpClient
                .get(makeUrl(path))
                .responseObject(Chip.Response.class);

        return chip;
    }

    public Meter.Response getMeter() throws IOException {
        String path = "/meter/" + API_VERSION;
        if (IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D)) {
            path = "/meter/v2" ;
        } else if (IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) {
            path = "/meter/v3" ;
        }

        final Meter.Response meter = _httpClient
                .get(makeUrl(path))
                .responseObject(Meter.Response.class);

        return meter;
    }

    public Meter.ResponseStatusFutabaD getMeterFutabaD() throws IOException {
        String path = path = "/meter/v4";

        final Meter.ResponseStatusFutabaD meter = _httpClient
                .get(makeUrl(path))
                .responseObject(Meter.ResponseStatusFutabaD.class);

        return meter;
    }

    public Ems.Response getEms() throws IOException {
        final String path = "/ems/" + API_VERSION;

        final Ems.Response ems = _httpClient
                .get(makeUrl(path))
                .responseObject(Ems.Response.class);

        return ems;
    }

    public void postWifi(Wifi.Request request, SocketFactory factory) throws IOException {
        final String path = "/wifi/" + API_VERSION;

        final OkHttpClient client = _httpClient.getClientBuilder().socketFactory(factory).build();

        _httpClient.post(makeUrl(path))
                .addQueryParameter(request.toMap())
                .setBodyEmpty()
                .client(client)
                .response();
    }

    public void postUpdate(String filePath) throws IOException {
        final String path = "/update/" + API_VERSION;

        _httpClient.upload(makeUrl(path), filePath, "update");
    }

    private String makeUrl(String path) {
        return (_baseUrl != null ? _baseUrl : BuildConfig.IF_BOX_BASE_URL) + path;
    }
}