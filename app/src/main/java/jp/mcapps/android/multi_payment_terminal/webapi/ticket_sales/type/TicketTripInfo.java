package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type;

import com.google.gson.annotations.Expose;

public class TicketTripInfo {

    @Expose
    public String trip_id;

    @Expose
    public Boolean is_special;

}
