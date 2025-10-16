package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import java.util.ArrayList;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketTripPageInfo;

public class TicketSearchResults {
    public Boolean searchResult;                   // 検索結果
    public Boolean prevTrip;                       // 前の便
    public Boolean nextTrip;                       // 次の便
    public TicketTripPageInfo ticketTripPageInfo;  // ページ情報

    public String ticketName;                      // チケット名称
    public String embarkStopName;                  // のりば名
    public String departureTime;                   // 出発時刻
    public String errorMessage;                    // エラーメッセージ
    public String errorMessageInformation;         // エラーメッセージ(補足)

    public Integer errorCode;                      // エラーコード
    public Integer adultNumber;                    // 大人(人数)
    public Integer childNumber;                    // 小人(人数)
    public Integer babyNumber;                     // 乳幼児(人数)
    public Integer adultDisabilityNumber;          // 障がい者 大人(人数)
    public Integer childDisabilityNumber;          // 障がい者 小人(人数)
    public Integer caregiverNumber;                // 介助者(人数)
    public Integer remainingSeats;                 // 残席
    public Integer totalAmount;                    // 合計

    /* 検索時の人数情報 */
    public Integer searchAdultNumber;              // 大人(人数)
    public Integer searchChildNumber;              // 小人(人数)
    public Integer searchBabyNumber;               // 乳幼児(人数)
    public Integer searchAdultDisabilityNumber;    // 障がい者 大人(人数)
    public Integer searchChildDisabilityNumber;    // 障がい者 小人(人数)
    public Integer searchCaregiverNumber;          // 介助者(人数)

    /* チケット購入履歴作成用 */
    public String ticketId;                         // チケットID
    public String tripId;                           // 便ID
    public String transportReservationSlotId;       // 予約枠ID

    public List<CategoryData> categoryData = new ArrayList<>(); // カテゴリデータ
    public static class CategoryData {
        public String categoryType;                // カテゴリ名（API名称）
        public String ticketSettingID;             // 回数券ID
        public Integer ticketsNumber;              // 回数券（ｎ枚セット券）
        public Integer quantity;                   // 数量
        public Integer amount;                     // 金額
    }
}
