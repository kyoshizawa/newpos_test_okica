package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type;

import com.google.gson.annotations.Expose;

public class DynamicTicketItem {

    @Expose
    public String dynamic_ticket_id;

    @Expose
    public String expiration_at;

    @Expose
    public String ticket_name;

    @Expose
    public NameI18n[] ticket_il8n;

    @Expose
    public String stop_name;

    @Expose
    public NameI18n[] stop_il8n;

    @Expose
    public String reservation_date;

    @Expose
    public String departure_time;

    @Expose
    public String reservation_no;

    @Expose
    public TicketPeople[] peoples;

    @Expose
    public String qr_code_item;
}
