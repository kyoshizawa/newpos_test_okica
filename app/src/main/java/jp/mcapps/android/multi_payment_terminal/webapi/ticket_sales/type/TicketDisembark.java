package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type;

import com.google.gson.annotations.Expose;

public class TicketDisembark {

    @Expose
    public String destination_stop_id;

    @Expose
    public String destination_stop_name;

    @Expose
    public TicketEmbarkNameI18n[] name_i18n;
}
