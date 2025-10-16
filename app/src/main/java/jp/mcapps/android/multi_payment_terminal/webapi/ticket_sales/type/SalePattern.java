package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type;

import com.google.gson.annotations.Expose;

public class SalePattern {

    @Expose
    public String simple_total_amount;

    @Expose
    public String total_amount;

    @Expose
    public TicketPeople[] peoples;

    @Expose
    public TicketSalePatternByCategory[] sale_patterns;
}
