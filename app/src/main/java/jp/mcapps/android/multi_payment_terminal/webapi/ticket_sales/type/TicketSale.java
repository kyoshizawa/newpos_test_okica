package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type;

import com.google.gson.annotations.Expose;

public class TicketSale {

    @Expose
    public String ticket_id;

    @Expose
    public Integer fare;
}
