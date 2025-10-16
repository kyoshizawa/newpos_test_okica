package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data;

import com.google.gson.annotations.Expose;

import org.jetbrains.annotations.Nullable;

import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.PurchasedTicketDetails;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketPeople;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TripReserve;

public class TicketUpdateDynamicTicketStatus {

    public static class Location {
        @Expose
        @Nullable
        public String sampled_at;           // 位置情報取得日時

        @Expose
        @Nullable
        public double latitude;             // 緯度

        @Expose
        @Nullable
        public double longitude;            // 経度
    }
    public static class Request {

        @Expose
        public String reservation_date;     // 運行日（予約日） ※必須

        @Expose
        public String trip_id;              // 便ID ※便IDと臨時便IDはどちらか一方が必須 ホーバーでは便IDのみ使用

        @Expose
        @Nullable
        public String special_trip_id;      // 臨時便ID ※未使用

        @Expose
        @Nullable
        public String tap_method;           // タッチ種別(one:１タップ, two:２タップ)

        @Expose
        @Nullable
        public String event_type;           // イベント種別(entrance:入場, exit:出場) ホーバーではentrance固定

        @Expose
        public String terminal_tid;         // 端末識別番号 ※必須

        @Expose
        public String station_no;          // 車番 ※必須

        @Expose
        @Nullable
        public String param_id;            // パラメータID ※ホーバーでは設定不要

        @Expose
        @Nullable
        public String telegram_id;         // 交通利用イベント向け パラメータ メッセージID

        @Expose
        @Nullable
        public String telegram_datetime;   // メッセージ日時

        @Expose
        @Nullable
        public String production;           // production:本番環境, test:テスト環境

        @Expose
        @Nullable
        public Location event_location;     // 位置情報

        @Expose
        @Nullable
        public String transit_type;         // bus:バス, hover:ホーバー ※hover固定

        @Expose
        @Nullable
        public String stop_id;              // QRを読み取った停留所ID

        @Expose
        @Nullable
        public String route_id;             // QRを読み取った経路ID

        @Expose
        @Nullable
        public String account_media;        // credit_card:クレジットカード, ticket:事前購入者 ※ホーバーではticket固定
    }

    public static class DynamicTicketStatusResponse {
        @Expose
        @Nullable
        public int total_num;               // 合計人数

        @Expose
        @Nullable
        public TicketPeople[] peoples;      // 乗客人数
    }

    public static class Response {

        @Expose
        @Nullable
        public DynamicTicketStatusResponse data;

        @Expose
        @Nullable
        public Status error;
    }
}
