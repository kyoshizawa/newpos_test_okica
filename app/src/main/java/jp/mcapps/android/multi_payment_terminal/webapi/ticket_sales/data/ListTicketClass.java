package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data;

import com.google.gson.annotations.Expose;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.Status;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketClass;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketClass;

public class ListTicketClass {

    public static class Response {

        @Expose
        @Nullable
        public TicketClass[] items = new TicketClass[0];

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
