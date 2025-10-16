package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data;

import com.google.gson.annotations.Expose;

import org.jetbrains.annotations.Nullable;

import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.PurchasedTicketDetails;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TripReserve;

public class TicketPurchasedHistory {

    public static class Request {

        @Expose
        @Nullable
        public String idp_account_id;       // 購入した会員ID             空欄

        @Expose
        @Nullable
        public String idp_account_number;   // 購入した会員番号            空欄

        @Expose
        @Nullable
        public String idp_account_name;     // 購入した会員名              空欄

        @Expose
        @Nullable
        public String purchased_time;       // 購入した時間               HH:mm:ss

        @Expose
        @Nullable
        public String sales_method;         // 販売方法                  in_person(対面)固定

        @Expose
        @Nullable
        public String terminal_tid;         // 端末識別番号               terminal_idをセット

        @Expose
        @Nullable
        public String terminal_transaction_no;  // 端末通番              決裁時の伝票番号をセット

        @Expose
        @Nullable
        public String card_category;        // 支払カードカテゴリ

        @Expose
        @Nullable
        public String card_brand_code;      // 支払カードブランドコード

        @Expose
        @Nullable
        public String card_brand_name;      // 支払カードブランド名

        @Expose
        @Nullable
        public String purchased_amount;     // 購入金額

        @Expose
        @Nullable
        public String reservation_date;     // 予約日または現在の日付      YYYY/MM/DD

        @Expose
        @Nullable
        public PurchasedTicketDetails[] details;  // 購入したチケットの明細

        @Expose
        @Nullable
        public TripReserve trip_reserve;  // 購入したチケットの明細
    }

    public static class PurchasedHistoryResponseData {
        @Expose
        @Nullable
        public String purchased_ticket_deal_id;     // 作成したチケット購入のID

        @Expose
        @Nullable
        public String trip_reservation_id;          // 作成した便予約ID
    }

    public static class Response {

        @Expose
        @Nullable
        public TicketPurchasedHistory.PurchasedHistoryResponseData data;

        @Expose
        @Nullable
        public Status error;
    }
}
