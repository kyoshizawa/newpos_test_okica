package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type;

import com.google.gson.annotations.Expose;

import org.jetbrains.annotations.Nullable;

public class TicketClass {

    @Expose
    public String id;

    @Expose
    public String service_instance_id;

    @Expose
    public String name;

    @Expose
    public TicketClassNameI18n[] name_i18n;

    @Expose
    public String reserve_type;

    @Expose
    public Boolean enable_route_judge;

    @Expose
    public Boolean enable_trip_judge;

    @Expose
    public Boolean enable_stop_judge;

    @Expose
    public String created_at;

    @Expose
    public TicketClassUser created_user;

    @Expose
    public String updated_at;

    @Expose
    public TicketClassUser updated_user;
}
