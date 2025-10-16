package jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data;

import com.google.gson.annotations.Expose;

public class OpeningInfoQuicpay extends MoneyCommon {
    @Expose
    public Boolean manualFlg;

    @Expose
    public Boolean partRtFlg;

    @Expose
    public String receiptOutputFlg;
}
