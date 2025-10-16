package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type;

import com.google.gson.annotations.Expose;

public class PurchasedTicketDetails {

    @Expose
    public String passenger_category_enum;  // 乗客カテゴリ

    @Expose
    public String ticket_setting_id;        // 回数券ID

    @Expose
    public int count;                       // 数量

    @Expose
    public int purchased_amount;            // 購入金額
}
