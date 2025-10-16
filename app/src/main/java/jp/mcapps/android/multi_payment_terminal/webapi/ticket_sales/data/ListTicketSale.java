package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data;

import com.google.gson.annotations.Expose;

import org.jetbrains.annotations.Nullable;

import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketReservationStatusByTrip;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketSalePattern;

public class ListTicketSale {

    public static class Response {

        @Expose
        @Nullable
        public TicketSalePattern[] ticket_sale_patterns = new TicketSalePattern[0];

        @Expose
        @Nullable
        public Status error;
    }
}
