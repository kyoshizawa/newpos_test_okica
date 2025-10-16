package jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.data.AuthTest;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.data.CreateManyTransaction;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.data.GetTenant;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.data.ListTenantProduct;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.data.ListTenantProductCategory;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.data.Status;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.type.Tenant;
import jp.mcapps.android.multi_payment_terminal.webapi.utils.TokenAuthenticator;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

public class PaypfPosApiImpl implements PaypfPosApi {

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final PaypfPosApiImpl instance = new PaypfPosApiImpl();

    public static PaypfPosApi getInstance() {
        return instance;
    }

    private final OkHttpClient _client; // singletonにしたいやつはこれ
    private final Gson _gson = new GsonBuilder().disableHtmlEscaping().create();

    private PaypfPosApiImpl() {

        final PaypfAuthenticationRepository repository = new PaypfAuthenticationRepository();
        final TokenAuthenticator auth = new TokenAuthenticator(repository);

        _client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor((chain) -> {
                    Request req = chain.request().newBuilder()
                            .addHeader("Accept", "application/json")
                            .build();
                    return chain.proceed(req);
                })
                .addInterceptor(auth) // リクエスト時にAuthorizationヘッダをつけます
                .authenticator(auth) // レスポンスエラー(401)時にトークンを更新を行います
                .build();
    }

    @Override
    public AuthTest.Response authTest() throws IOException, HttpStatusException {

        String url = makeUrl("/auth/test");

        // request
        Request req = new Request.Builder()
                .url(url)
                .get()
                .build();

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            return handleResponse(resp, AuthTest.Response.class);
        }
    }

    @Override
    public Tenant getTenantByCustomerCode(@NotNull String serviceInstanceID, @NotNull String customerCode) throws IOException, HttpStatusException, PaypfStatusException {

        String url = makeUrl(String.format("/instances/%s/tenant-by-customer-code/%s", serviceInstanceID, customerCode));

        // query
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();

        // request
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            GetTenant.Response r = handleResponse(resp, GetTenant.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r.data;
        }
    }

    @Override
    public ListTenantProductCategory.ResponseData listTenantProductCategories(@NotNull String serviceInstanceID, long tenantID, int limit, int offset) throws IOException, HttpStatusException, PaypfStatusException {

        String url = makeUrl(String.format("/instances/%s/tenants/%s/categories", serviceInstanceID, tenantID));

        // query
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        urlBuilder.addQueryParameter("limit", Integer.toString(limit));
        urlBuilder.addQueryParameter("offset", Integer.toString(offset));

        // request
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            ListTenantProductCategory.Response r = handleResponse(resp, ListTenantProductCategory.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r.data;
        }
    }

    @Override
    public ListTenantProduct.ResponseData listTenantProducts(@NotNull String serviceInstanceID, long tenantID, int limit, int offset) throws IOException, HttpStatusException, PaypfStatusException {

        String url = makeUrl(String.format("/instances/%s/tenants/%s/products", serviceInstanceID, tenantID));

        // query
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        urlBuilder.addQueryParameter("limit", Integer.toString(limit));
        urlBuilder.addQueryParameter("offset", Integer.toString(offset));

        // request
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            ListTenantProduct.Response r = handleResponse(resp, ListTenantProduct.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r.data;
        }
    }

    @Override
    public CreateManyTransaction.Response createTransactions(@NotNull CreateManyTransaction.Request request) throws IOException, HttpStatusException {

        String url = makeUrl("/pos-product-transactions");

        // request body
        String bodyJson = _gson.toJson(request);
        RequestBody body = RequestBody.create(bodyJson, JSON);

        // request
        Request req = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s %s", req.method(), req.url().toString(), bodyJson);
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            CreateManyTransaction.Response r = handleResponse(resp, CreateManyTransaction.Response.class);

            return r;
        }
    }

    private <T> T handleResponse(final Response resp, Class<T> clazz) throws IOException {
        assert resp.body() != null;
        String s = resp.body().string();
        Request req = resp.request();
        if (BuildConfig.DEBUG) {
            Timber.d("Response: %s %s (StatusCode: %s) %s", req.method(), req.url().toString(), resp.code(), s);
        }
        return _gson.fromJson(s, clazz);
    }

    private void handleErrorStatus(final Response resp, final Status st) throws PaypfStatusException {
        if (st != null) {
            Request req = resp.request();
            Timber.e("Domain error on api request: %s %s (code: %s message: '%s')", req.method(), req.url().toString(), st.code, st.message);
            throw new PaypfStatusException(st.code, st.message);
        }
    }

    private void handleHttpStatus(final Response resp) throws HttpStatusException, IOException {
        if (!resp.isSuccessful()) {
            Request req = resp.request();
            String s = resp.body() != null ? resp.body().string() : null;
            Timber.e("StatusCode error on api request: %s %s (StatusCode: %s) %s", req.method(), req.url().toString(), resp.code(), s);
            throw new HttpStatusException(resp.code(), s);
        }
    }

    private String makeUrl(@NotNull String path) {
        return BuildConfig.POS_ACTIVATE_BASE_URL + path;
    }
}
