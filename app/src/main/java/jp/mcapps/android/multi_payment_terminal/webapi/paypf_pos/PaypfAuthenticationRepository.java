package jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos;

import java.io.IOException;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.PaypfActivationApi;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.PaypfActivationApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.data.RefreshToken;
import jp.mcapps.android.multi_payment_terminal.webapi.utils.AuthenticationRepository;

public class PaypfAuthenticationRepository implements AuthenticationRepository {

    private final PaypfActivationApi _api = new PaypfActivationApiImpl();

    @Override
    public String getToken() {

        // アクセストークンを返す
        return AppPreference.get_servicePosAccessToken();
    }

    @Override
    public String refreshToken() throws IOException, HttpStatusException {
        final RefreshToken.Response resp;

        // アクセストークンを更新する
        final String refreshToken = AppPreference.get_servicePosRefreshToken();
        final boolean useInTest = AppPreference.isDemoMode();
        resp = _api.refreshToken(refreshToken, useInTest);

        // 更新したアクセストークンを保存する
        AppPreference.set_servicePos(resp.access_token, resp.refresh_token);

        return resp.access_token;
    }
}
