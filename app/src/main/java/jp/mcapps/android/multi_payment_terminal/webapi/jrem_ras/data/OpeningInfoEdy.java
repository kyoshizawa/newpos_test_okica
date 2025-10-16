package jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data;

import com.google.gson.annotations.Expose;

public class OpeningInfoEdy extends MoneyCommon {
    @Expose
    public String receiptOutputFlg;

    @Expose
    public Boolean initCommunicationFlg;

    @Expose
    public Boolean chargeAuthFlg;

    @Expose
    public Integer useType;
}
