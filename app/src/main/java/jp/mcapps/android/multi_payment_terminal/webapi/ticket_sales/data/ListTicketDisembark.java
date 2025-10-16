package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data;

import com.google.gson.annotations.Expose;

import org.jetbrains.annotations.Nullable;

import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketDisembark;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketEmbark;

public class ListTicketDisembark {

    public static class Response {

        @Expose
        @Nullable
        public TicketDisembark[] items = new TicketDisembark[0];

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
