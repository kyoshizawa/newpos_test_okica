package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class RequestWn {
    @Expose
    public Base base;   //売上基本データ

    @Expose
    public DataWn dataWn;   //WAON固有データ

    public RequestWn(UriData data) {
        base = new Base(data);
        dataWn = new DataWn(data);
    }
}
