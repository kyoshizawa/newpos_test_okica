package jp.mcapps.android.multi_payment_terminal.webapi.validation_check;

import android.os.Build;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.data.OptionService;
import jp.mcapps.android.multi_payment_terminal.util.DeviceUtils;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.McHttpClient;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_activation.data.Download;
import jp.mcapps.android.multi_payment_terminal.webapi.validation_check.data.*;
import timber.log.Timber;

public class ValidationCheckApiImpl implements ValidationCheckApi {
    private final McHttpClient _httpClient = McHttpClient.getInstance();
    private final Map<String, String> _header = new HashMap<>();

    public ValidationCheckApiImpl() {
        OptionService service = MainApplication.getInstance().getOptionService();
        if (service == null) {
            // 開局できていないのにインスタンスを作ろうとしたらエラーにする
            // このクラスを使用する画面はUI上で選択できないようにする
            throw new IllegalStateException("option service is null");
        }

        final String model = Build.MODEL;
        final String serial = DeviceUtils.getSerial();
        final String nonce = UUID.randomUUID().toString();

        _header.put("X-Client-Model", model);
        _header.put("X-Client-SN", serial);
        _header.put("X-Client-Nonce", nonce);

        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            final String sign = String.format(
                    "X-Client-Model=%s&X-Client-SN=%s&X-Client-Nonce=%s&Key=%s",
                    model, serial, nonce, service.getServiceKey());
            _header.put("X-Client-Sign", McUtils.bytesToHexString(md5.digest(sign.getBytes(StandardCharsets.UTF_8))));
        } catch (Exception ignore) { }  // MD5のインスタンスを呼び出すのに例外は絶対に出ないので無視する
    }

    public CardValidation.Response cardValidation(CardValidation.Request request) throws IOException, HttpStatusException {
        final String path = "/AddonCC/v1/Analyze/CardValidation";

        final CardValidation.Response response = _httpClient
                .post(makeUrl(path))
                .addHeader(_header)
                .setBody(request)
                .responseObject(CardValidation.Response.class);

        return response;
    }

    @Override
    public ProcessResult.Response processResult(ProcessResult.Request request) throws IOException {
        final String path = "/AddonCC/v1/Common/ProcessResult";

        final ProcessResult.Response response = _httpClient
                .post(makeUrl(path))
                .addHeader(_header)
                .setBody(request)
                .responseObject(ProcessResult.Response.class);

        return response;
    }

    private String makeUrl(String path){
        final OptionService service = MainApplication.getInstance().getOptionService();
        if (service == null || service.getDomain() == null || service.getDomain().equals("")) {
            throw new IllegalStateException("invalid domain");
        }

        return String.format("https://%s%s", service.getDomain(), path);
    }
}