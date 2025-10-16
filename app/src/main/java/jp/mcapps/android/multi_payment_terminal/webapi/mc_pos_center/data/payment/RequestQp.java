package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class RequestQp {
    @Expose
    public Base base;   //売上基本データ

    @Expose
    public DataQp dataQp;   //QuicPay固有データ

    public RequestQp(UriData data) {
        base = new Base(data);
        dataQp = new DataQp(data);
    }
}
