package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.HttpModule;

import com.google.gson.Gson;
import com.google.gson.internal.Primitives;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class FeliCaHttpClient {
    private static FeliCaHttpClient _instance;
    private OkHttpClient _defaultClient;
    private final Gson _gson = new Gson();
    private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

    /***********************************************************************************/
    /*** TCAP試験用コード TLAM-N-016用 https接続確認のため、証明書を無視する ***/
    public static class MyX509TrustManager implements X509TrustManager {
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
        }

        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
        }

        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[]{};
        }
    }
    /***********************************************************************************/

    private FeliCaHttpClient() {
        // Cookieの処理 永続化していないので一発目の通信はCookieがない
        CookieJar cookieJar = new CookieJar() {
            @Override
            public void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
                cookieStore.put(httpUrl.host(), list);
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl httpUrl) {
                List<Cookie> cookies = cookieStore.get(httpUrl.host());
                return cookies != null ? cookies : new ArrayList<>();
            }
        };
        _defaultClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .cookieJar(cookieJar)
                .build();
        // CookieManager の生成
        CookieManager cookieManager = new CookieManager();
        // すべての Cookie を受けいれる
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        // CookieHandler のデフォルトに設定
        CookieHandler.setDefault(cookieManager);
    }

    public static FeliCaHttpClient get_instance() {
        if (_instance == null) {
            synchronized (FeliCaHttpClient.class) {
                if (_instance == null) {
                    _instance = new FeliCaHttpClient();
                }
            }
        }
        return _instance;
    }

    public Get get(String url)   { return new Executor(url, "GET"); }
    public Post post(String url) { return new Executor(url, "POST"); }

    public interface Common {
        Response response() throws IOException;
//        public ResponseBody responseBody() throws IOException;
//        public String responseString() throws IOException;
//        public <T> T responseObject(Class<T> classOfT) throws IOException;
    }
    public interface Get extends Common {
        Get addHeader(String field, String value);
//        public Get addHeader(Map<String, String> headers);
        Get addQueryParameter(String key, String value);
//        public Get addQueryParameter(Map<String, String> query);
//        public Get setConnectTimeioutMilliseconds(int timeioutMilliseconds);
        Get sslSocketFactory(SSLSocketFactory socketFactory);
        Get setHostnameVerifier(HostnameVerifier hostnameVerifier);
    }

    public interface Post extends Common {
        Post addHeader(String field, String value);
//        public Post addHeader(Map<String, String> headers);
        Post addQueryParameter(String key, String value);
//        public Post addQueryParameter(Map<String, String> query);
//        public Post setBody(RequestBody requestBody);
        Post setBody(byte[] body);
//        public Post setBody(String body);
//        public Post setBody(Object body);
//        public Post addFormData(String name, String value);
        Post setConnectTimeioutMilliseconds(int timeioutMilliseconds);
    }

    public <T> T getResponseObject(Response response, Class<T> classOfT) throws IOException {
        final ResponseBody responseBody = response.body();
        final String json = responseBody.string();
        final Object object = _gson.fromJson(json, (Type) classOfT);
        return Primitives.wrap(classOfT).cast(object);
    }

    public class Executor implements Get, Post {
        private final HttpUrl.Builder urlBuilder;
        private final String method;
        private final Headers.Builder headersBuilder = new Headers.Builder();
        private RequestBody body = null;
        private OkHttpClient customClient = null;
        private FormBody.Builder formBodyBuilder = null;

        private Executor(String url, String method) {
            urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
            this.method = method;
        }

        public Executor addHeader(String field, String value) {
            headersBuilder.set(field, value);
            return this;
        }

        public Executor addHeader(Map<String, String> headers) {
            for (final Map.Entry<String, String> pair : headers.entrySet()) {
                headersBuilder.set(pair.getKey(), pair.getValue());
            }
            return this;
        }

        public Executor addQueryParameter(String key, String value) {
            urlBuilder.addQueryParameter(key, value);
            return this;
        }

        public Executor addQueryParameter(Map<String, String> query) {
            for (final Map.Entry<String, String> pair : query.entrySet()) {
                urlBuilder.addQueryParameter(pair.getKey(), pair.getValue());
            }
            return this;
        }

        public Executor setBody(RequestBody requestBody) {
            this.body = requestBody;
            return this;
        }

        public Executor setBody(byte[] body) {
            this.body = RequestBody.create(body, null);
            return this;
        }

        public Executor setBody(String body) {
            setBody(body.getBytes());
            return this;
        }

        public Executor setBody(Object obj) {
            final Gson gson = new Gson();
            final String json = gson.toJson(obj);
            setBody(json);
            return this;
        }

        public Executor addFormData(String name, String value) {
            if (formBodyBuilder == null) {
                formBodyBuilder = new FormBody.Builder();
            }
            formBodyBuilder.add(name, value);

            return this;
        }

        public Executor setConnectTimeioutMilliseconds(int timeioutMilliseconds) {
            if(customClient == null) {
                _defaultClient = _defaultClient.newBuilder().connectTimeout(timeioutMilliseconds, TimeUnit.MILLISECONDS).build();
            } else {
                customClient = customClient.newBuilder().connectTimeout(timeioutMilliseconds, TimeUnit.MILLISECONDS).build();
            }
            return this;
        }

        public Get sslSocketFactory(SSLSocketFactory socketFactory) {
            if(customClient == null) {
                _defaultClient = _defaultClient.newBuilder().sslSocketFactory(socketFactory, new MyX509TrustManager()).build();
            } else {
                customClient = customClient.newBuilder().sslSocketFactory(socketFactory, new MyX509TrustManager()).build();
            }
            return this;
        }

        public Get setHostnameVerifier(HostnameVerifier hostnameVerifier) {
            if(customClient == null) {
                _defaultClient = _defaultClient.newBuilder().hostnameVerifier(hostnameVerifier).build();
            } else {
                customClient = customClient.newBuilder().hostnameVerifier(hostnameVerifier).build();
            }
            return this;
        }

        public Executor client(OkHttpClient client) {
            customClient = client;
            return this;
        }

        public Response response() throws IOException {
            final String contentType = headersBuilder.get("Content-Type") != null
                    ? headersBuilder.get("Content-Type")
                    : "application/json";

            addHeader("Content-Type", contentType);

/*
            final boolean useFormData = contentType == "application/x-www-form-urlencoded"
                    || contentType == "multipart/form-data";

            if (useFormData) {
                if (body != null && body instanceof MultipartBody) {

                } else if (formBodyBuilder != null) {
                    body = formBodyBuilder.build();
                } else {
                    throw new IllegalStateException("form data is not set");
                }
            }
*/

            final Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .headers(headersBuilder.build())
                    .method(method, body)
                    .build();
            try {
                final OkHttpClient client = customClient != null ? customClient : _defaultClient;

/*
                if (400 <= response.code()) {
                    throw new HttpStatusException(response.code());
                }
*/

                return client.newCall(request).execute();
            } catch (IOException ex) {
                throw ex;
            }
        }

        public ResponseBody responseBody() throws IOException {
            final Response response = response();
            return response.body();
        }

        public String responseString() throws IOException {
            final ResponseBody responseBody = responseBody();
            return responseBody.string();
        }

        public <T> T responseObject(Class<T> classOfT) throws IOException {
            final String json = responseString();
            final Object object = _gson.fromJson(json, (Type) classOfT);
            return Primitives.wrap(classOfT).cast(object);
        }
    }
}
