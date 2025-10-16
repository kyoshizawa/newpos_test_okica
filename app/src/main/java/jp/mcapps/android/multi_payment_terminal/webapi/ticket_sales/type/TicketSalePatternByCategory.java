package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type;

import com.google.gson.annotations.Expose;

public class TicketSalePatternByCategory {

    @Expose
    public String category_type;

    @Expose
    public TicketSale[] sale_tickets;

    @Expose
    public TicketSaleSettings[] sale_ticket_settings;

    @Expose
    public String sub_total_amount;
}
