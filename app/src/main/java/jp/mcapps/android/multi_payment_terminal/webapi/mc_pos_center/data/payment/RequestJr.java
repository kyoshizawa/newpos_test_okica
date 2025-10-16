package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class RequestJr {
    @Expose
    public Base base;   //売上基本データ

    @Expose
    public DataJr dataJr;   //交通系固有データ

    public RequestJr(UriData data) {
        base = new Base(data);
        dataJr = new DataJr(data);
    }
}
