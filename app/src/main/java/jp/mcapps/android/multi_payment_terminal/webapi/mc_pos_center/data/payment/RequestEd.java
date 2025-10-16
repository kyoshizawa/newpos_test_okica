package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class RequestEd {
    @Expose
    public Base base;   //売上基本データ

    @Expose
    public DataEd dataEd;   //Edy固有データ

    public RequestEd(UriData data) {
        base = new Base(data);
        dataEd = new DataEd(data);
    }
}
