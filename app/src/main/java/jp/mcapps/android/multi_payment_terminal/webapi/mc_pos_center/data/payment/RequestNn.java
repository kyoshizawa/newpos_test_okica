package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class RequestNn {
    @Expose
    public Base base;   //売上基本データ

    @Expose
    public DataNn dataNn;   //nanaco固有データ

    public RequestNn(UriData data) {
        base = new Base(data);
        dataNn = new DataNn(data);
    }
}
