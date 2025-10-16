package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales;

import java.io.IOException;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.PaypfActivationApi;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.PaypfActivationApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.data.RefreshToken;
import jp.mcapps.android.multi_payment_terminal.webapi.utils.AuthenticationRepository;

public class TicketSalesAuthenticationRepository implements AuthenticationRepository {

    private final PaypfActivationApi _api = new PaypfActivationApiImpl();

    @Override
    public String getToken() {

        // アクセストークンを返す
        return AppPreference.get_serviceTicketAccessToken();
    }

    @Override
    public String refreshToken() throws IOException, HttpStatusException {
        final RefreshToken.Response resp;

        // アクセストークンを更新する
        final String refreshToken = AppPreference.get_serviceTicketRefreshToken();
        final boolean useInTest = AppPreference.isDemoMode();
        resp = _api.refreshToken(refreshToken, useInTest);

        // 更新したアクセストークンを保存する
        AppPreference.set_serviceTicket(resp.access_token, resp.refresh_token);

        return resp.access_token;
    }
}
