package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type;

import com.google.gson.annotations.Expose;

public class TicketGateEmbark {

    @Expose
    public String stop_name;

    @Expose
    public String[] stop_ids;
}
