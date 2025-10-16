package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data;

import com.google.gson.annotations.Expose;

import org.jetbrains.annotations.Nullable;

public class TicketPurchasedConfirm {

    public static class Response {

        @Expose
        @Nullable
        public Object data;

        @Expose
        @Nullable
        public Status error;
    }
}
