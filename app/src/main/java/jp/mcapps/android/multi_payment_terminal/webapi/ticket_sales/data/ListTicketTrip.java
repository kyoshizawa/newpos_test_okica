package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data;

import com.google.gson.annotations.Expose;

import org.jetbrains.annotations.Nullable;

import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketRoute;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketTrip;

public class ListTicketTrip {

    public static class Response {

        @Expose
        @Nullable
        public TicketTrip[] items = new TicketTrip[0];

        @Expose
        @Nullable
        public String total_count;

        @Expose
        @Nullable
        public String offset;

        @Expose
        @Nullable
        public Status error;
    }
}
