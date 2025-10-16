package jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras;

import java.io.IOException;

import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningEmoney;
import jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data.OpeningSuica;

public interface JremRasApi {
    OpeningSuica.Response openingSuica() throws IOException, HttpStatusException;
    OpeningEmoney.Response openingEmoney() throws IOException, HttpStatusException;
}
