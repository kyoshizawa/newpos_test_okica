package jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data;

import com.google.gson.annotations.Expose;

public class OpeningInfoWaon extends MoneyCommon {
    @Expose
    public Boolean cashChargeFlg;
    @Expose
    public Boolean chargeCancelFlg;
    @Expose
    public Boolean saleCancelFlg;
    @Expose
    public Boolean sentBackFlg;
    @Expose
    public Boolean pointChargeFlg;
    @Expose
    public Boolean otherSaleCancelFlg;
    @Expose
    public Boolean autoChargeFlg;
    @Expose
    public String receiptOutputFlg;
    @Expose
    public Integer pointChargeUnit;
    @Expose
    public Integer chargeUnit;
    @Expose
    public Integer pointChargeRatio;
}
