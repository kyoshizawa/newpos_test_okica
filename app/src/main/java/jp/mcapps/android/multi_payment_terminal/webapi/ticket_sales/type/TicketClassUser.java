package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type;

import com.google.gson.annotations.Expose;

public class TicketClassUser {
    @Expose
    public String auth_server_id;
    @Expose
    public String user_id;
    @Expose
    public String user_name;
}
