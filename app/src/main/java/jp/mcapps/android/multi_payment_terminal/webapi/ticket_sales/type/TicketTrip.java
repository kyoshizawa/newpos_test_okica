package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type;

import com.google.gson.annotations.Expose;

public class TicketTrip {

    @Expose
    public String table_name;

    @Expose
    public String id;

    @Expose
    public String feed_id;

    @Expose
    public String route_id;

    @Expose
    public String service_id;

    @Expose
    public String trip_id;

    @Expose
    public String trip_headsign;

    @Expose
    public String start_stop_id;

    @Expose
    public String start_arrival_time;

    @Expose
    public String start_departure_time;

    @Expose
    public String start_stop_sequence;

    @Expose
    public String start_stop_headsign;

    @Expose
    public String start_stop_code;

    @Expose
    public String start_stop_name;

    @Expose
    public String start_stop_lat;

    @Expose
    public String start_stop_lon;

    @Expose
    public String start_zone_id;

    @Expose
    public String end_stop_id;

    @Expose
    public String end_arrival_time;

    @Expose
    public String end_departure_time;

    @Expose
    public String end_stop_sequence;

    @Expose
    public String end_stop_headsign;

    @Expose
    public String end_stop_code;

    @Expose
    public String end_stop_name;

    @Expose
    public String end_stop_lat;

    @Expose
    public String end_stop_lon;

    @Expose
    public String end_zone_id;
}
