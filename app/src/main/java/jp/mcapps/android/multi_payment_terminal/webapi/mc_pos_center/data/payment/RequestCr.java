package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class RequestCr {
    @Expose
    public Base base;   //売上基本データ

    @Expose
    public DataCr dataCr;   //クレジット固有データ

    public RequestCr(UriData data) {
        base = new Base(data);
        dataCr = new DataCr(data);
    }
}
