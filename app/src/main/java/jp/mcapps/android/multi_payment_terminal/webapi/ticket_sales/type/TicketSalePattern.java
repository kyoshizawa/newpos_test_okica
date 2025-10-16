package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type;

import com.google.gson.annotations.Expose;

public class TicketSalePattern {

    @Expose
    public String ticket_id;

    @Expose
    public String ticket_name;

    @Expose
    public NameI18n[] ticket_name_i18n;

    @Expose
    public SalePattern[] sale_patterns;
}
