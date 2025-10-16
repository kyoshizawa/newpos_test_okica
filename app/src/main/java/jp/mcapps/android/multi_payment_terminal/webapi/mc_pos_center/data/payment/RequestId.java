package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.payment;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;

public class RequestId {
    @Expose
    public Base base;   //売上基本データ

    @Expose
    public DataId dataId;   //iD固有データ

    public RequestId(UriData data) {
        base = new Base(data);
        dataId = new DataId(data);
    }
}
