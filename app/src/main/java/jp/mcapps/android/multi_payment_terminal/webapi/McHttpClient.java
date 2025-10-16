package jp.mcapps.android.multi_payment_terminal.webapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.Primitives;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class McHttpClient {
    private static McHttpClient _instance;
    private final OkHttpClient _client;
    private final Gson _gson = new GsonBuilder().disableHtmlEscaping().create();
    private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

    private McHttpClient() {
        // Cookieの処理 永続化していないので一発目の通信はCookieがない
        CookieJar cookieJar = new CookieJar() {
            @Override
            public void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
                cookieStore.put(httpUrl.host(), list);
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl httpUrl) {
                List<Cookie> cookies = cookieStore.get(httpUrl.host());
                return cookies != null ? cookies : new ArrayList<Cookie>();
            }
        };

        _client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .cookieJar(cookieJar)
                .build();

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
    }

    public static McHttpClient getInstance() {
        if (_instance == null) {
            synchronized (McHttpClient.class) {
                if (_instance == null) {
                    _instance = new McHttpClient();
                }
            }
        }
        return _instance;
    }

    public OkHttpClient.Builder getClientBuilder() {
        return _client.newBuilder();
    }

    public Get get(String url) { return new Executor(url, "GET"); }
    public Post post(String url) { return new Executor(url, "POST"); }
    public Put put(String url) { return new Executor(url, "PUT"); }
    public Delete delete(String url) { return new Executor(url, "DELETE"); }

    public void download(String url, String filePath) throws IOException {
        final Executor executor = new Executor(url, "GET").client(_client.newBuilder()
                .readTimeout(0, TimeUnit.SECONDS)
                .build());

        final File tempFile = File.createTempFile("temp", "tmp");

        final ResponseBody responseBody = executor.responseBody();

        try {
            final BufferedInputStream input = new BufferedInputStream(responseBody.byteStream());
            final OutputStream output = new FileOutputStream(tempFile);

            final byte[] data = new byte[1024];
            int readByte = 0;

            while ((readByte = input.read(data)) != -1) {
                output.write(data, 0, readByte);
            }

            output.flush();
            output.close();

            tempFile.renameTo(new File(filePath));
        } catch (IOException ex) {
            throw ex;
        }
    }

    public void upload(String url, String filePath, String fieldName) throws IOException {
            final File file = new File(filePath);
            final String boundary = String.valueOf(System.currentTimeMillis());

            final MediaType mediaType = MediaType.parse("multipart/form-data");
            final RequestBody requestBody = RequestBody.create(file, mediaType);

            final MultipartBody multipartBody = new MultipartBody.Builder(boundary)
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(fieldName, file.getName(), requestBody)
                    .build();

        try {
            final Response response = new Executor(url, "POST").client(_client.newBuilder()
                    .writeTimeout(0, TimeUnit.SECONDS)
                    .readTimeout(0, TimeUnit.SECONDS)
                    .connectTimeout(360, TimeUnit.SECONDS)
                    .build())
                    .addHeader("Content-Type", "multipart/form-data")
                    .setBody(multipartBody)
                    .response();
        } catch (Exception ex) {
            throw ex;
        }
    }

    public interface Common {
        public okhttp3.Response response() throws IOException;
        public ResponseBody responseBody() throws IOException;
        public String responseString() throws IOException;
        public <T> T responseObject(Class<T> classOfT) throws IOException;
    }
    public interface Get extends Common {
        public Get client(OkHttpClient client);
        public Get addHeader(String field, String value);
        public Get addHeader(Map<String, String> headers);
        public Get addQueryParameter(String key, String value);
        public Get addQueryParameter(Map<String, String> query);
        public Get clearCookies();
    }
    public interface Post extends Common {
        public Post client(OkHttpClient client);
        public Post addHeader(String field, String value);
        public Post addHeader(Map<String, String> headers);
        public Post addQueryParameter(String key, String value);
        public Post addQueryParameter(Map<String, String> query);
        public Post setBody(RequestBody requestBody);
        public Post setBody(byte[] body);
        public Post setBody(String body);
        public Post setBody(Object body);
        public Post setBodyEmpty();
        public Post addFormData(String name, String value);
        public Post clearCookies();
    }
    public interface Put extends Common {
        public Put client(OkHttpClient client);
        public Put addHeader(String field, String value);
        public Put addHeader(Map<String, String> headers);
        public Put setBody(RequestBody requestBody);
        public Put setBody(byte[] body);
        public Put setBody(String body);
        public Put setBody(Object body);
        public Put setBodyEmpty();
        public Put addFormData(String name, String value);
        public Put clearCookies();
    }
    public interface Delete extends Common {
        public Delete client(OkHttpClient client);
        public Delete addHeader(String field, String value);
        public Delete addHeader(Map<String, String> headers);
        public Delete addQueryParameter(String key, String value);
        public Delete addQueryParameter(Map<String, String> query);
        public Delete setBody(RequestBody requestBody);
        public Delete setBody(byte[] body);
        public Delete setBody(String body);
        public Delete setBody(Object body);
        public Delete setBodyEmpty();
        public Delete clearCookies();
    }

    public interface Download extends Common {
    }
    public interface Upload extends Common {
    }

    public class Executor implements Get, Post, Put, Delete, Download, Upload {
        private final HttpUrl.Builder urlBuilder;
        private final String method;
        private final Headers.Builder headersBuilder = new Headers.Builder();
        private RequestBody body = null;
        private OkHttpClient customClient = null;
        private FormBody.Builder formBodyBuilder = null;
        private boolean cookieClearFlag = false;
        private Response response;

        private Executor(String url, String method) {
            urlBuilder = HttpUrl.parse(url).newBuilder();
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
            this.body = RequestBody.create(null, body);
            return this;
        }

        public Executor setBody(String body) {
            setBody(body.getBytes());
            return this;
        }

        public Executor setBodyEmpty() {
            setBody("{}");
            return this;
        }

        public Executor setBody(Object obj) {
            final String json = _gson.toJson(obj);
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

        public Executor client(OkHttpClient client) {
            if (client != null) {
                customClient = client;
            }
            return this;
        }

        public Executor clearCookies() {
            cookieClearFlag = true;
            return this;
        }

        public Response response() throws IOException {
//            _client.connectionPool().evictAll();
            if (!method.equals("GET")) {
                final String contentType = headersBuilder.get("Content-Type") != null
                        ? headersBuilder.get("Content-Type")
                        : "application/json";

                addHeader("Content-Type", contentType);

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
            }

            final HttpUrl httpUrl = urlBuilder.build();
            final Request request = new Request.Builder()
                    .url(httpUrl)
                    .headers(headersBuilder.build())
                    .method(method, body)
                    .build();
            try {
                final OkHttpClient client = customClient != null ? customClient : _client;

                if (cookieClearFlag) {
                    client.cookieJar().saveFromResponse(httpUrl, null);
                }
                final Response response = client.newCall(request).execute();
                this.response = response;
                if (400 <= response.code()) {
                    String s = response.body().string();
                    if (BuildConfig.DEBUG) {
                        Timber.e("Response: %s %s (StatusCode: %s) %s", request.method(), request.url(), response.code(), s);
                    } else {
                        Timber.e("StatusCode: %s", response.code());
                    }
                    throw new HttpStatusException(response.code() , s);
                }
                return response;
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
            final String s = responseBody.string();
            if (BuildConfig.DEBUG) {
                Request req = response.request();
                Timber.d("Response: %s %s (StatusCode: %s) %s", req.method(), req.url(), response.code(), s);
            }
            return s;
        }

        public <T> T responseObject(Class<T> classOfT) throws IOException {
            final String json = responseString();
            final Object object = _gson.fromJson(json, (Type) classOfT);
            return Primitives.wrap(classOfT).cast(object);
        }
    }
}
