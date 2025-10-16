package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketPeople;

public class TicketSale {

    public static class Request {
        @Expose
        public Boolean round_trip;

        @Expose
        public TicketPeople[] peoples;

        @Expose
        public Boolean need_single_ticket;
    }
}
