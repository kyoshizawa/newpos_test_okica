package jp.mcapps.android.multi_payment_terminal.model;

import com.google.gson.Gson;
import com.google.gson.internal.Primitives;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.error.JremActivationErrorMap;
import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorMap;
import jp.mcapps.android.multi_payment_terminal.error.McPosCenterErrorCodes;
import jp.mcapps.android.multi_payment_terminal.error.McPosCenterErrorMap;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_activation.JremActivationApi;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_activation.JremActivationApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_activation.data.Activation;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_activation.data.Authentication;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_activation.data.Download;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.McPosCenterApi;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.McPosCenterApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.Echo;
import timber.log.Timber;

public class JremActivator {
    public static final String UNDEFINED_ERROR_CODE = JremActivationErrorMap.EMONEY_ACTIVATE_ERROR_CODE;
    public JremActivationApi _apiClient = new JremActivationApiImpl();

    public String install() {
        String errCode = authenticate();

        if (errCode != null) {
            return errCode;
        }

        errCode = download();

        if (errCode != null) {
            return errCode;
        }

        final File certFile = new File(MainApplication.getInstance().getFilesDir(), BuildConfig.JREM_CLIENT_CERTIFICATE);

        if (!certFile.exists()) {
            return UNDEFINED_ERROR_CODE;
        }

        try {
            final InputStream inputStream = new FileInputStream(certFile);
            KeyStore p12 = KeyStore.getInstance("PKCS12");
            p12.load(inputStream, AppPreference.getJremPassword().toCharArray());

            inputStream.close();

            Enumeration<String> e = p12.aliases();

            String cn = null;

            while (e.hasMoreElements()) {
                String alias = e.nextElement();
                X509Certificate c = (X509Certificate) p12.getCertificate(alias);
                Principal subject = c.getSubjectDN();
                String subjectArray[] = subject.toString().split(",");
                for (String s : subjectArray) {
                    String[] str = s.trim().split("=");
                    if (str[0].equals("CN")) {
                        cn = str[1];
                        break;
                    }
                }
            }

            if (cn == null) {
                certFile.delete();
                return UNDEFINED_ERROR_CODE;
            }

            errCode = activate(cn);

            if (errCode != null) {
                certFile.delete();
                return UNDEFINED_ERROR_CODE;
            }

            AppPreference.setJremUniqueId(cn);
            return null;
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            if (certFile.exists()) {
                certFile.delete();
            }
            Timber.e(e);
            return UNDEFINED_ERROR_CODE;
        }
    }

    public void removeCertFile() {
        final File certFile = new File(MainApplication.getInstance().getFilesDir(), BuildConfig.JREM_CLIENT_CERTIFICATE);
        if (certFile.exists()) {
            certFile.delete();
        }
    }

    private String authenticate() {
        Authentication.Response response;
        try {
            response = _apiClient.authenticate(
                    AppPreference.getJremActivateId(), AppPreference.getJremPassword());

        } catch (IOException e) {
            Timber.e(e);
            return UNDEFINED_ERROR_CODE;
        }

        if (response.errorObject != null) {
            return JremActivationErrorMap.get(response.errorObject.errorCode);
        }

        return null;
    }

    private String download() {
        byte[] responseBytes;

        try {
            responseBytes = _apiClient.download(
                    AppPreference.getJremActivateId(), AppPreference.getJremPassword());

            if (responseBytes == null) {
                return UNDEFINED_ERROR_CODE;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return UNDEFINED_ERROR_CODE;
        }

        try {
            // JSONのデシリアライズに成功した場合はエラー
            final Gson gson = new Gson();
            final Object object = gson.fromJson(new String(responseBytes), (Type) Download.Response.class);
            final Download.Response responseObject = Primitives.wrap(Download.Response.class).cast(object);
            final Download.ErrorObject errObj = responseObject.errorObject;

            if (errObj != null) {
                return JremActivationErrorMap.get(errObj.errorCode);
            }
        } catch (Exception ignore) {
            // 例外が出る方が正常なので何もしない
        }

        try {
            // 証明書を保存する
            final File tempFile = File.createTempFile("temp", "tmp");
            final ByteArrayInputStream input =
                    new ByteArrayInputStream(responseBytes);

            final OutputStream output = new FileOutputStream(tempFile);

            final byte[] data = new byte[1024];
            int readByte = 0;

            while ((readByte = input.read(data)) != -1) {
                output.write(data, 0, readByte);
            }

            output.flush();
            output.close();

            final File certFile = new File(MainApplication.getInstance().getFilesDir(), BuildConfig.JREM_CLIENT_CERTIFICATE);
            tempFile.renameTo(certFile);

            return null;
        } catch (IOException ex) {
            return UNDEFINED_ERROR_CODE;
        }
    }

    private String activate(String uniqueId) {
        Activation.Response response;
        try {
            response = _apiClient.activate(uniqueId);
        } catch (IOException e) {
            Timber.e(e);
            return UNDEFINED_ERROR_CODE;
        }

        if (response.errorObject != null) {
            return JremActivationErrorMap.get(response.errorObject.errorCode);
        }

        return null;
    }

    public String uninstall() {
        final File dir = MainApplication.getInstance().getFilesDir();
        final McPosCenterApi mcPosCenterApi = new McPosCenterApiImpl();
        String strResult = null;

        try {
            AppPreference.execMcEcho(); //echo実行フラグON
            AppPreference.setDetachJR(true); //JREM端末の設置解除指示をON
            final Echo.Response echoResponse = mcPosCenterApi.echo(AppPreference.isDetachJR(), AppPreference.isDetachQR());

            if (!echoResponse.result) {
                Timber.e("紐付け解除失敗(エラーコード:%s)", echoResponse.errorCode);
                return McPosCenterErrorMap.get(echoResponse.errorCode);
            } else {
                Timber.i("紐付け解除成功 利用許可状態:%s", echoResponse.useable);
                AppPreference.setIsAvailable(echoResponse.useable); //利用許可状態をRAMに保持
            }
        } catch (IOException | IllegalStateException | HttpStatusException e) {
            Timber.e(e);
            // 紐付け解除時のエラー表示は行わない
            strResult = "Exception";
        }

        final File certification = new File(dir, BuildConfig.JREM_CLIENT_CERTIFICATE);

        if (certification.exists()) {
            certification.delete();
        }

        AppPreference.clearJremActivateId();
        AppPreference.clearJremPassword();
        AppPreference.setNanacoDaTermFrom(null);
        AppPreference.setQuicpayDaTermFrom(null);
        Timber.i("端末保持情報クリア(JremActivateId,JremPassword,NanacoDaTermFrom,QuicpayDaTermFrom)");
        return strResult;
    }
}
