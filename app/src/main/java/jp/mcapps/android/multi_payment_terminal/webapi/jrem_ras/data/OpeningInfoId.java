package jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data;

import com.google.gson.annotations.Expose;

public class OpeningInfoId extends MoneyCommon {
    @Expose
    public Boolean manualFlg;

    @Expose
    public Boolean goodsCodeFlg;

    @Expose
    public Boolean taxOtherFlg;

    @Expose
    public String[] payment;

    @Expose
    public Boolean refBusFlg;

    @Expose
    public String receiptOutputFlg;
}
