package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type;

import com.google.gson.annotations.Expose;

public class ReserveSlots {

    @Expose
    public String transport_reservation_slot_id;    // 予約枠ID

    @Expose
    public String passenger_category_enum;          // 乗客カテゴリ
}
