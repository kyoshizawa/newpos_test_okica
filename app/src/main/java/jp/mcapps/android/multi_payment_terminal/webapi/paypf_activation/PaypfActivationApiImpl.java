package jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation;

import java.io.IOException;

import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.McHttpClient;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.data.GetToken;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.data.Activate;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.data.RefreshToken;

public class PaypfActivationApiImpl implements PaypfActivationApi {
    private final McHttpClient _httpClient = McHttpClient.getInstance();

    @Override
    public Activate.Response activate(String modelCode, String serialNo, String unitId, Boolean usePayment, String tid, String supplierCd) throws IOException, HttpStatusException {
        final String path = "/auth/activate";

        final Activate.Request request = new Activate.Request();
        request.model_code = modelCode;
        request.serial_no = serialNo;
        request.unit_id = unitId;
        request.use_payment = usePayment;
        request.tid = tid;
        request.customer_code = supplierCd; // 取引先コード

        Activate.Response reqActivate = _httpClient
                .post(makeUrl(path))
                .setBody(request)
                .responseObject(Activate.Response.class);
        return reqActivate;
    }

    @Override
    public GetToken.Response getToken(String modelCode, String serialNo, Boolean useInTest) throws IOException, HttpStatusException {
        final String path = "/auth/token";

        final GetToken.Request request = new GetToken.Request();
        request.model_code = modelCode;
        request.serial_no = serialNo;
        request.use_in_test = useInTest;

        GetToken.Response getToken = _httpClient
                .post(makeUrl(path))
                .setBody(request)
                .responseObject(GetToken.Response.class);
        return getToken;
    }

    @Override
    public RefreshToken.Response refreshToken(String refreshToken, Boolean useInTest) throws IOException, HttpStatusException {
        final String path = "/auth/refresh";

        final RefreshToken.Request request = new RefreshToken.Request();
        request.refresh_token = refreshToken;
        request.use_in_test = useInTest;

        final RefreshToken.Response response = _httpClient
                .post(makeUrl(path))
                .setBody(request)
                .responseObject(RefreshToken.Response.class);
        return response;
    }

    private String makeUrl(String path) {
        return BuildConfig.POS_ACTIVATE_BASE_URL + path;
    }
}
