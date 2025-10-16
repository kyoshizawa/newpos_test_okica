package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data;

import com.google.gson.annotations.Expose;

import org.jetbrains.annotations.Nullable;

import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketEmbark;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketRoute;

public class ListTicketRoute {

    public static class Response {

        @Expose
        @Nullable
        public TicketRoute[] items = new TicketRoute[0];

        @Expose
        @Nullable
        public Status error;
    }
}
