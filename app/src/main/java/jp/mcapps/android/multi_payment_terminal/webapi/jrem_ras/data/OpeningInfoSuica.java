package jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data;

import com.google.gson.annotations.Expose;

public class OpeningInfoSuica {
    @Expose
    public Boolean result;

    @Expose
    public Integer t1;

    @Expose
    public String sprwid;

    @Expose
    public Integer code;
}
