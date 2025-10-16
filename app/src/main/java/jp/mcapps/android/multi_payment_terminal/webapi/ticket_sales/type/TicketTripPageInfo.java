package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type;

import com.google.gson.annotations.Expose;

public class TicketTripPageInfo {

    @Expose
    public TicketTripInfo trip_info;

    @Expose
    public String date;

    @Expose
    public String departure_time;

    @Expose
    public String arrival_time;

}
