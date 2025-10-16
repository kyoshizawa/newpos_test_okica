package jp.mcapps.android.multi_payment_terminal.webapi.gtfs.data;

import com.google.gson.annotations.Expose;

import org.jetbrains.annotations.Nullable;

import jp.mcapps.android.multi_payment_terminal.webapi.gtfs.type.GTFSFeeds;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.Status;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketEmbark;

public class ListGTFSFeeds {

    public static class Response {

        @Expose
        @Nullable
        public GTFSFeeds[] items = new GTFSFeeds[0];

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
