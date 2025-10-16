package jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data;

import com.google.gson.annotations.Expose;

public class OpeningInfoNanaco extends MoneyCommon {
    @Expose
    public Boolean cashChargeFlg;

    @Expose
    public String receiptOutputFlg;

    @Expose
    public Integer chargeUnit;
}
