package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type;

import com.google.gson.annotations.Expose;

public class TripReserve {

    @Expose
    public String trip_id;          // 便ID

    @Expose
    public String sprcial_trip_id;  // 臨時便ID ※センター側スペルミスと思われる

    @Expose
    public ReserveSlots[] reserve_slots;  // 予約枠
}
