package jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data;

import com.google.gson.annotations.Expose;

public class MoneyCommon {
    @Expose
    public String moneyname;

    @Expose
    public Boolean mresult;

    @Expose
    public Integer t1;

    @Expose
    public String termIdentId;

    @Expose
    public Integer code;

    @Expose
    public String url;
}
