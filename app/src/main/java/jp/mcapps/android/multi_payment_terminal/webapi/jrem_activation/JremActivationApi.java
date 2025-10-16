package jp.mcapps.android.multi_payment_terminal.webapi.jrem_activation;

import java.io.IOException;

import jp.mcapps.android.multi_payment_terminal.webapi.jrem_activation.data.Activation;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_activation.data.Authentication;

public interface JremActivationApi {
    Authentication.Response authenticate(String activateId, String activatePassword) throws IOException;
    byte[] download(String activateId, String activatePassword) throws IOException;
    Activation.Response activate(String uniqueId) throws IOException;
}
