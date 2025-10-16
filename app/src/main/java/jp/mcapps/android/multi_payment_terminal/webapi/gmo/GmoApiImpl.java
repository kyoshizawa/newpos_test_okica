package jp.mcapps.android.multi_payment_terminal.webapi.gmo;

import java.io.IOException;
import java.util.Map;

import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.McHttpClient;
import jp.mcapps.android.multi_payment_terminal.webapi.gmo.data.*;

public class GmoApiImpl implements GmoApi {
    private final McHttpClient _httpClient = McHttpClient.getInstance();

    @Override
    public PostLogin.Response postLogin(PostLogin.Request request) throws IOException, HttpStatusException {
        final String path = "/gateway/api/v1/login";

        final PostLogin.Response response = _httpClient
                .post(makeUrl(path))
                .setBody(request)
                .responseObject(PostLogin.Response.class);

        return response;
    }

    @Override
    public PutOrders.Response putOrders(Map<String, String> header, PutOrders.Request request) throws IOException, HttpStatusException {
        final String path = "/gateway/api/v1/qr/orders";

        final PutOrders.Response response = _httpClient
                .put(makeUrl(path))
                .addHeader(header)
                .setBody(request)
                .responseObject(PutOrders.Response.class);

        return response;
    }

    @Override
    public GetCheckOrder.Response getCheckOrder(Map<String, String> header, Map<String, String> queryParams) throws IOException, HttpStatusException {
        final String path = "/gateway/api/v1/qr/checkorder";

        final GetCheckOrder.Response response = _httpClient
                .get(makeUrl(path))
                .addHeader(header)
                .addQueryParameter(queryParams)
                .responseObject(GetCheckOrder.Response.class);

        return response;
    }

    @Override
    public PutRefunds.Response putRefunds(Map<String, String> header, PutRefunds.Request request) throws IOException, HttpStatusException {
        final String path = "/gateway/api/v1/qr/refunds";

        final PutRefunds.Response response = _httpClient
                .put(makeUrl(path))
                .addHeader(header)
                .setBody(request)
                .responseObject(PutRefunds.Response.class);

        return response;
    }

    @Override
    public GetCheckRefunds.Response getCheckRefunds(Map<String, String> header, Map<String, String> queryParams) throws IOException, HttpStatusException {
        final String path = "/gateway/api/v1/qr/checkrefunds";

        final GetCheckRefunds.Response response = _httpClient
                .get(makeUrl(path))
                .addHeader(header)
                .addQueryParameter(queryParams)
                .responseObject(GetCheckRefunds.Response.class);

        return response;
    }

    @Override
    public GetOrders.Response getOrders(Map<String, String> header, Map<String, String> queryParams) throws IOException, HttpStatusException {
        final String path = "/gateway/api/v1/qr/orders";

        final GetOrders.Response response = _httpClient
                .get(makeUrl(path))
                .addHeader(header)
                .addQueryParameter(queryParams)
                .responseObject(GetOrders.Response.class);

        return response;
    }

    // これは多分端末から呼ぶものではない
    @Override
    public PostOrdersForPartner.Response postOrdersForPartner(Map<String, String> header, PostOrdersForPartner.Request request) throws IOException, HttpStatusException {
        final String path = "/portal/queryapi/v1/qr/ordersforpartner";

        final PostOrdersForPartner.Response response = _httpClient
                .post(makeUrl(path))
                .addHeader(header)
                .setBody(request)
                .responseObject(PostOrdersForPartner.Response.class);

        return response;
    }

    @Override
    public PutCreateQRCode.Response putCreateQRCode(Map<String, String> header, PutCreateQRCode.Request request) throws IOException, HttpStatusException {
        final String path = "/gateway/api/v1/qr/createqrcode";

        final PutCreateQRCode.Response response = _httpClient
                .post(makeUrl(path))
                .addHeader(header)
                .setBody(request)
                .responseObject(PutCreateQRCode.Response.class);

        return response;
    }

    @Override
    public PutCancelOrders.Response PutCancelOrders(Map<String, String> header, PutCancelOrders.Request request) throws IOException, HttpStatusException {
        final String path = "/gateway/api/v1/qr/cancelorders";

        final PutCancelOrders.Response response = _httpClient
                .post(makeUrl(path))
                .addHeader(header)
                .setBody(request)
                .responseObject(PutCancelOrders.Response.class);

        return response;
    }

    private String makeUrl(String path){
        return BuildConfig.GMO_CENTER_BASE_URL + path;
    }
}
