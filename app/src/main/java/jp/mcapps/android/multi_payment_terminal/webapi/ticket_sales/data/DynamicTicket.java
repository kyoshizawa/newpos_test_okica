package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data;

import com.google.gson.annotations.Expose;

import org.jetbrains.annotations.Nullable;

import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.DynamicTicketItem;

public class DynamicTicket {

    public static class Request {

        @Expose
        @Nullable
        public String trip_reservation_id;  // 便予約ID                     【購入履歴送信】のレスポンスにあるtrip_reservation_idをセット

        @Expose
        @Nullable
        public String sale_method;          // in_person(対面), net(ネット） 【購入履歴送信】のボディにあるsale_methodをセット

        @Expose
        @Nullable
        public String idp_account_id;       // 購入した会員ID                【購入履歴送信】のボディにあるidp_account_idをセット

        @Expose
        @Nullable
        public String terminal_tid;         // 端末識別番号                  【購入履歴送信】のボディにあるterminal_tidをセット
    }

    public static class DynamicTicketData {
        @Expose
        @Nullable
        public DynamicTicketItem item;
    }

    public static class Response {

        @Expose
        @Nullable
        public DynamicTicketData data;

        @Expose
        @Nullable
        public Status error;
    }
}
