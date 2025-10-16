package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type;

import com.google.gson.annotations.Expose;

public class TicketTripByDateTime {

    @Expose
    public String trip_id;

    @Expose
    public String ticket_id;

    @Expose
    public String origin_stop_id;

    @Expose
    public String origin_stop_name;

    @Expose
    public String departure_time;

    @Expose
    public String destination_stop_id;

    @Expose
    public String destination_stop_name;

    @Expose
    public String arrival_time;

    @Expose
    public TicketTripPageInfo page_info;

    @Expose
    public Boolean has_prev_trip;

    @Expose
    public Boolean has_next_trip;

    @Expose
    public Boolean is_suspension;
}
