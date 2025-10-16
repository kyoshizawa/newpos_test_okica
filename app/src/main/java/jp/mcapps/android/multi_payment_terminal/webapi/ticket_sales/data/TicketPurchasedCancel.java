package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data;

import com.google.gson.annotations.Expose;

import org.jetbrains.annotations.Nullable;

import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.PurchasedTicketDetails;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TripReserve;

public class TicketPurchasedCancel {

    public static class Response {

        @Expose
        @Nullable
        public Object data;

        @Expose
        @Nullable
        public Status error;
    }
}
