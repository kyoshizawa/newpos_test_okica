package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data;

import com.google.gson.annotations.Expose;

import org.jetbrains.annotations.Nullable;

import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketReservationStatusByTrip;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketTripByDateTime;

public class ListTicketReservationStatusByTrip {

    public static class Response {

        @Expose
        @Nullable
        public TicketReservationStatusByTrip item;

        @Expose
        @Nullable
        public Status error;
    }
}
