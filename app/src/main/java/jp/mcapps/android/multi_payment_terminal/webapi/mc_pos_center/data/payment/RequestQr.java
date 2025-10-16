package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class RequestQr {
    @Expose
    public Base base;               //売上基本データ

    @Expose
    public DataQr dataQr;           //QR決済固有データ

    public RequestQr(UriData data) {
        base = new Base(data);
        dataQr = new DataQr(data);
    }
}
