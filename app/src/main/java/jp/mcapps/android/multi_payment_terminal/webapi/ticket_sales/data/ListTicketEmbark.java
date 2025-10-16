package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data;

import com.google.gson.annotations.Expose;

import org.jetbrains.annotations.Nullable;

import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketClass;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketEmbark;

public class ListTicketEmbark {

    public static class Response {

        @Expose
        @Nullable
        public TicketEmbark[] items = new TicketEmbark[0];

        @Expose
        @Nullable
        public int total_count;

        @Expose
        @Nullable
        public int offset;

        @Expose
        @Nullable
        public Status error;
    }
}
