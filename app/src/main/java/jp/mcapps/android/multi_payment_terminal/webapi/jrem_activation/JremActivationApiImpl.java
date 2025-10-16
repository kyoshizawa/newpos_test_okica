package jp.mcapps.android.multi_payment_terminal.webapi.jrem_activation;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import jp.mcapps.android.multi_payment_terminal.webapi.McHttpClient;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_activation.data.Activation;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_activation.data.Authentication;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_activation.data.Download;
import timber.log.Timber;

public class JremActivationApiImpl implements JremActivationApi {
    private static final String SCHEME_TYPE = "H";
    private final McHttpClient _httpClient = McHttpClient.getInstance();

    // アクティベート認証
    public Authentication.Response authenticate(String activateId, String activatePassword) throws IOException {
        final String path = BuildConfig.JREM_ACTIVATION_OTHERS_PATH;
        try {
            final Authentication.Request request = new Authentication.Request();
            request.activateID = activateId;
            request.oneTimePassword = activatePassword;
            request.schemeType = SCHEME_TYPE;


            final Authentication.Response response = _httpClient
                    .post(makeUrl(path))
                    .addHeader("Content-Type", "application/json; charset=UTF-8")
                    .addQueryParameter("oP0619_1", "aaa")
                    .setBody(request)
                    .clearCookies()
                    .responseObject(Authentication.Response.class);

            return response;
        } catch (IOException ex) {
            throw ex;
        }
    }

    // 証明書ダウンロード
    public byte[] download(String activateId, String activatePassword) throws IOException {
        final String path = BuildConfig.JREM_ACTIVATION_OTHERS_PATH;

        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");

            final Download.Request request = new Download.Request();
            request.activateID = activateId;
            request.oneTimePassword = activatePassword;
            // request.termPrimaryNo = UUID.randomUUID().toString().replace("-", "");
            request.termPrimaryNo = McUtils.bytesToHexString(md5.digest(activateId.getBytes()));
            request.schemeType = SCHEME_TYPE;

            final byte[] responseBytes = _httpClient
                    .post(makeUrl(path))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept-Type", "application/json")
                    .addQueryParameter("oP0619_3", "aaa")
                    .setBody(request)
                    .responseBody().bytes();

            return responseBytes;

        } catch (IOException ex) {
            throw ex;
        } catch (NoSuchAlgorithmException e) {
            Timber.d(e);
            return null;
        }
    }

    // アクティベート(ユニークIDチェック)
    public Activation.Response activate(String uniqueId) throws IOException {
        final String path = BuildConfig.JREM_ACTIVATION_OTHERS_PATH;

        try {
            final Activation.Request request = new Activation.Request();
            request.uniqueId = uniqueId;

            final Activation.Response response = _httpClient
                    .post(makeUrl(path))
                    .addHeader("Content-Type", "application/json; charset=UTF-8")
                    .addQueryParameter("oP0619_2", "aaa")
                    .setBody(request)
                    .responseObject(Activation.Response.class);

            return response;
        } catch (IOException ex) {
            throw ex;
        }
    }

    private String makeUrl(String path){
        return BuildConfig.JREM_ACTIVATION_BASE_URL + path;
    }
}