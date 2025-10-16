package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class RequestUp {
    @Expose
    public Base base;   //売上基本データ

    @Expose
    public DataUp dataUp;   //銀聯固有データ

    public RequestUp(UriData data) {
        base = new Base(data);
        dataUp = new DataUp(data);
    }
}
