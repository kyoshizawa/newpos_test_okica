package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type;

import com.google.gson.annotations.Expose;

public class TicketSaleSettings {

    @Expose
    public String ticket_setting_id;

    @Expose
    public Integer count;

    @Expose
    public Integer fare;
}
