package jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.Arrays;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.McHttpClient;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningEmoney;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningSuica;
import okhttp3.OkHttpClient;

public class JremRasApiImpl implements JremRasApi {
    private static final String SCHEME_TYPE = "H";
    private final McHttpClient _httpClient = McHttpClient.getInstance();
    private OkHttpClient _okHttpClient;

    public JremRasApiImpl() throws IllegalStateException {
        final File file = new File(
                MainApplication.getInstance().getFilesDir(), BuildConfig.JREM_CLIENT_CERTIFICATE);

        // クライアント証明書がダウンロードされていない
        if (!file.exists()) {
            throw new IllegalStateException("jrem client certificate file not exists!!");
        }

        try {
            FileInputStream inputStream = new FileInputStream(file);
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(inputStream, AppPreference.getJremPassword().toCharArray());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());

            trustManagerFactory.init(keyStore);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager))
            {
                throw new IllegalStateException("Unexpected default trust managers:"
                        + Arrays.toString(trustManagers));
            }

            X509TrustManager trustManager = (X509TrustManager)trustManagers[0];

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
            keyManagerFactory.init(keyStore, AppPreference.getJremPassword().toCharArray());
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            _okHttpClient = _httpClient.getClientBuilder()
                    .sslSocketFactory(sslSocketFactory, trustManager)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // 交通系の開局
    @Override
    public OpeningSuica.Response openingSuica() throws IOException, HttpStatusException {
        final String path = "/pos/start.do";

        try {
            final OpeningSuica.Response response = _httpClient
                    .get(makeUrl(path))
                    .client(_okHttpClient)
                    .addQueryParameter("businessId", "145")
                    .responseObject(OpeningSuica.Response.class);

            return response;
        } catch (IOException ex) {
            throw ex;
        }
    }

    // 交通系の開局
    @Override
    public OpeningEmoney.Response openingEmoney() throws IOException, HttpStatusException {
        final String path = BuildConfig.JREM_RAS_OPENING_OTHERS_PATH;

        try {
            final OpeningEmoney.Response response = _httpClient
                    .get(makeUrl(path))
                    .client(_okHttpClient)
                    .addQueryParameter("businessId", "245")
                    .responseObject(OpeningEmoney.Response.class);


            return response;
        } catch (IOException ex) {
            throw ex;
        }
    }

    private String makeUrl(String path){
        return BuildConfig.JREM_RAS_BASE_URL + path;
    }
}
