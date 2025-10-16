package jp.mcapps.android.multi_payment_terminal.ui.ticket;


import android.content.res.Resources;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.error.DomainErrors;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApi;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketReservationStatusByTrip;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketSale;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketTripByDateTime;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.TicketSale;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketPeople;
import timber.log.Timber;

public class TicketSearchProcess {
    private final TicketSalesApi _ticketSalesApiClient = TicketSalesApiImpl.getInstance();

    private TicketSale.Request setRequestBody(TicketSearchInfo searchInfo) {
        TicketSale.Request request = new TicketSale.Request();
        // 往復指定
        request.round_trip = false;
        // すべて片道券購入※true指定すると人数パターンの計算（最安値判定）、回数券利用をしない
        request.need_single_ticket = false;

        // 乳幼児は予約APIのみに適用されるカテゴリのため、チケット検索APIのリクエストには含めない ※仕様が変更になる可能性あり
        int categoryCount = 0;
        if (searchInfo.adultNumber > 0) categoryCount += 1;
        if (searchInfo.childNumber > 0) categoryCount += 1;
        if (searchInfo.adultDisabilityNumber > 0) categoryCount += 1;
        if (searchInfo.childDisabilityNumber > 0) categoryCount += 1;
        if (searchInfo.caregiverNumber > 0) categoryCount += 1;

        request.peoples = new TicketPeople[categoryCount];

        if (searchInfo.adultNumber > 0) {
            categoryCount -= 1;
            request.peoples[categoryCount] = new TicketPeople();
            request.peoples[categoryCount].category_type = "unknown";
            request.peoples[categoryCount].num = searchInfo.adultNumber;
        }

        if (searchInfo.childNumber > 0) {
            categoryCount -= 1;
            request.peoples[categoryCount] = new TicketPeople();
            request.peoples[categoryCount].category_type = "child";
            request.peoples[categoryCount].num = searchInfo.childNumber;
        }

        if (searchInfo.adultDisabilityNumber > 0) {
            categoryCount -= 1;
            request.peoples[categoryCount] = new TicketPeople();
            request.peoples[categoryCount].category_type = "disabled";
            request.peoples[categoryCount].num = searchInfo.adultDisabilityNumber;
        }

        if (searchInfo.childDisabilityNumber > 0) {
            categoryCount -= 1;
            request.peoples[categoryCount] = new TicketPeople();
            request.peoples[categoryCount].category_type = "child_disabled";
            request.peoples[categoryCount].num = searchInfo.childDisabilityNumber;
        }

        if (searchInfo.caregiverNumber > 0) {
            categoryCount -= 1;
            request.peoples[categoryCount] = new TicketPeople();
            request.peoples[categoryCount].category_type = "carer";
            request.peoples[categoryCount].num = searchInfo.caregiverNumber;
        }

        return request;
    }

    public TicketSearchResults Execute(TicketSearchInfo searchInfo, TicketSearchResults searchResults) {
        final Resources resources = MainApplication.getInstance().getResources();

        // 指定された日時から運行便を取得する（チケット発売用）
        try {
            final ListTicketTripByDateTime.Response response = _ticketSalesApiClient.listTicketTripByDateTime(
                    searchInfo.serviceInstanceID,
                    searchInfo.ticketClassID,
                    searchInfo.date,
                    searchInfo.departureTime,
                    searchInfo.embarkStopID,
                    searchInfo.disEmbarkStopID,
                    searchInfo.page_info,
                    searchInfo.prevNextTripType);

            // 直近の便IDを取得
            searchInfo.tripID = response.item.trip_id;
            searchResults.tripId = response.item.trip_id;
            // チケットIDを取得
            searchInfo.ticketID = response.item.ticket_id;
            searchResults.ticketId = response.item.ticket_id;
            // のりば名
            searchResults.embarkStopName = response.item.origin_stop_name;
            // 出発時刻
            searchResults.departureTime = response.item.departure_time;
            // ページ情報
            searchResults.ticketTripPageInfo = response.item.page_info;
            // 前便有無フラグを取得
            searchResults.prevTrip = response.item.has_prev_trip;
            // 次便有無フラグを取得
            searchResults.nextTrip = response.item.has_next_trip;
        } catch (TicketSalesStatusException e) {
            Timber.e(e);
            searchResults.searchResult = false;
            searchResults.nextTrip = false;
            searchResults.prevTrip = false;
            if (404 == e.getCode()) {
                searchResults.errorCode = Integer.valueOf(resources.getString(R.string.error_type_ticket_8111));
                searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8111);
                searchResults.errorMessageInformation = resources.getString(R.string.error_detail_ticket_8111);
            } else {
                searchResults.errorCode = Integer.valueOf(resources.getString(R.string.error_type_ticket_8110));
                searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8110);
                searchResults.errorMessageInformation = String.format(resources.getString(R.string.error_detail_ticket_8110), String.valueOf(e.getCode()));
            }
            return searchResults;
        } catch (HttpStatusException e) {
            Timber.e(e);
            searchResults.searchResult = false;
            searchResults.nextTrip = false;
            searchResults.prevTrip = false;
            if (404 == e.getStatusCode()) {
                searchResults.errorCode = Integer.valueOf(resources.getString(R.string.error_type_ticket_8111));
                searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8111);
                searchResults.errorMessageInformation = resources.getString(R.string.error_detail_ticket_8111);
            } else {
                searchResults.errorCode = Integer.valueOf(resources.getString(R.string.error_type_ticket_8110));
                searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8110);
                searchResults.errorMessageInformation = String.format(resources.getString(R.string.error_detail_ticket_8110), String.valueOf(e.getStatusCode()));
            }
            return searchResults;
        } catch (Exception e) {
            Timber.e(e);
            searchResults.searchResult = false;
            searchResults.errorCode = Integer.valueOf(resources.getString(R.string.error_type_ticket_8097));
            searchResults.nextTrip = false;
            searchResults.prevTrip = false;
            searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8097);
            searchResults.errorMessageInformation = resources.getString(R.string.error_detail_ticket_8097);
            return searchResults;
        }

        // チケットIDからチケット販売パターンの候補を返す。（チケット発売用）
        try {
            final ListTicketSale.Response response = _ticketSalesApiClient.listTicketConfirmSaleByTicket(
                    searchInfo.serviceInstanceID,
                    searchInfo.ticketID,
                    setRequestBody(searchInfo));

            if (response.ticket_sale_patterns.length > 0){
                // チケット名称を取得
                searchResults.ticketName = response.ticket_sale_patterns[0].ticket_name;
                //　カテゴリデータを取得
                getCategoryData(searchInfo, response, searchResults);
            } else {
                // カテゴリデータを取得（例外）
                Timber.e("チケット毎販売パターン = %d", response.ticket_sale_patterns.length);
                searchResults.searchResult = false;
                searchResults.errorCode = 8011;
                searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8011);
                searchResults.errorMessageInformation = resources.getString(R.string.error_detail_ticket_8011);
                return searchResults;
            }

        } catch (TicketSalesStatusException e) {
            Timber.e(e);
            searchResults.searchResult = false;
            if (404 == e.getCode()) {
                searchResults.errorCode = Integer.valueOf(resources.getString(R.string.error_type_ticket_8121));
                searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8121);
                searchResults.errorMessageInformation = resources.getString(R.string.error_detail_ticket_8121);
            } else {
                searchResults.errorCode = Integer.valueOf(resources.getString(R.string.error_type_ticket_8120));
                searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8120);
                searchResults.errorMessageInformation = String.format(resources.getString(R.string.error_detail_ticket_8120), String.valueOf(e.getCode()));
            }
            return searchResults;
        } catch (HttpStatusException e) {
            Timber.e(e);
            searchResults.searchResult = false;
            if (404 == e.getStatusCode()) {
                searchResults.errorCode = Integer.valueOf(resources.getString(R.string.error_type_ticket_8121));
                searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8121);
                searchResults.errorMessageInformation = resources.getString(R.string.error_detail_ticket_8121);
            } else {
                searchResults.errorCode = Integer.valueOf(resources.getString(R.string.error_type_ticket_8120));
                searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8120);
                searchResults.errorMessageInformation = String.format(resources.getString(R.string.error_detail_ticket_8120), String.valueOf(e.getStatusCode()));
            }
            return searchResults;
        } catch (Exception e) {
            Timber.e(e);
            searchResults.searchResult = false;
            searchResults.errorCode = Integer.valueOf(resources.getString(R.string.error_type_ticket_8097));
            searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8097);
            searchResults.errorMessageInformation = resources.getString(R.string.error_detail_ticket_8097);
            return searchResults;
        }

        // 便IDから予約枠の情報を取得する（チケット発売用）
        try {
            final ListTicketReservationStatusByTrip.Response response = _ticketSalesApiClient.listTicketReservationStatusByTrip(
                    searchInfo.serviceInstanceID,
                    searchInfo.date,
                    searchInfo.tripID);

            if (response.item.transport_reservation_slots.length > 0) {
                // 予約枠IDを取得
                searchResults.transportReservationSlotId = response.item.transport_reservation_slots[0].transport_reservation_slot_id;
                // 便IDの残数を取得
                searchResults.remainingSeats = response.item.transport_reservation_slots[0].remaining_amount;
                if (searchResults.remainingSeats < searchInfo.totalPeoples) {
                    // 残数が総人数より少ないため、残数不足エラー
                    searchResults.searchResult = false;
                    searchResults.errorCode = 8018;
                    searchResults.errorMessage = "残数が不足しています";
                    searchResults.errorMessageInformation = String.format("%s %s %s発の残数は%sです", searchResults.ticketName, searchResults.embarkStopName, searchResults.departureTime.substring(0, 5), searchResults.remainingSeats);
                } else {
                    searchResults.searchResult = true;
                }
            } else {
                // 予約枠を取得（例外）
                Timber.e("予約枠 = %d", response.item.transport_reservation_slots.length);
                searchResults.searchResult = false;
                searchResults.errorCode = 8012;
                searchResults.errorMessage = String.format("%s %s発\n", searchResults.embarkStopName, searchResults.departureTime.substring(0, 5));
                searchResults.errorMessage += resources.getString(R.string.error_message_ticket_8012);
                searchResults.errorMessageInformation = resources.getString(R.string.error_detail_ticket_8012);
                return searchResults;
            }

        } catch (TicketSalesStatusException e) {
            Timber.e(e);
            searchResults.searchResult = false;
            searchResults.errorCode = Integer.valueOf(resources.getString(R.string.error_type_ticket_8130));
            searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8130);
            searchResults.errorMessageInformation = String.format(resources.getString(R.string.error_detail_ticket_8130), String.valueOf(e.getCode()));
            return searchResults;
        } catch (HttpStatusException e) {
            Timber.e(e);
            searchResults.searchResult = false;
            searchResults.errorCode = Integer.valueOf(resources.getString(R.string.error_type_ticket_8130));
            searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8130);
            searchResults.errorMessageInformation = String.format(resources.getString(R.string.error_detail_ticket_8130), String.valueOf(e.getStatusCode()));
            return searchResults;
        } catch (Exception e) {
            Timber.e(e);
            searchResults.searchResult = false;
            searchResults.errorCode = Integer.valueOf(resources.getString(R.string.error_type_ticket_8097));
            searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8097);
            searchResults.errorMessageInformation = resources.getString(R.string.error_detail_ticket_8097);
            return searchResults;
        }
        return searchResults;
    }

    private void getCategoryData(TicketSearchInfo searchInfo, ListTicketSale.Response response, TicketSearchResults searchResults) throws Exception {
        final Resources resources = MainApplication.getInstance().getResources();

        // 販売パターン
        if (response.ticket_sale_patterns[0].sale_patterns.length > 0) {

            // 合計
            searchResults.totalAmount = Integer.valueOf(response.ticket_sale_patterns[0].sale_patterns[0].total_amount);

            // カテゴリ人数
            int peoples = response.ticket_sale_patterns[0].sale_patterns[0].peoples.length;
            if (peoples > 0) {

                for (int i = 0; i < peoples; i++) {

                    String categoryType = response.ticket_sale_patterns[0].sale_patterns[0].peoples[i].category_type;

                    if (categoryType.equals("unknown")) {
                        searchResults.adultNumber = response.ticket_sale_patterns[0].sale_patterns[0].peoples[i].num;
                    } else if (categoryType.equals("child")) {
                        searchResults.childNumber = response.ticket_sale_patterns[0].sale_patterns[0].peoples[i].num;
                    } else if (categoryType.equals("disabled")) {
                        searchResults.adultDisabilityNumber = response.ticket_sale_patterns[0].sale_patterns[0].peoples[i].num;
                    } else if (categoryType.equals("child_disabled")) {
                        searchResults.childDisabilityNumber = response.ticket_sale_patterns[0].sale_patterns[0].peoples[i].num;
                    } else if (categoryType.equals("carer")) {
                        searchResults.caregiverNumber = response.ticket_sale_patterns[0].sale_patterns[0].peoples[i].num;
                    } else if (categoryType.equals("baby")) {
                        searchResults.babyNumber = response.ticket_sale_patterns[0].sale_patterns[0].peoples[i].num;
                    } else {
                        Timber.e("categoryType err:%s", categoryType);
                    }
                }
            } else {
                // カテゴリ人数を取得（例外）
                Timber.e("カテゴリ人数 = %d", peoples);
                searchResults.searchResult = false;
                searchResults.errorCode = 8013;
                searchResults.nextTrip = false;
                searchResults.prevTrip = false;
                searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8013);
                searchResults.errorMessageInformation = resources.getString(R.string.error_detail_ticket_8013);
                throw new Exception();
            }

            int categoryCount = 0;
            TicketSearchResults.CategoryData categoryData;
            String backTicketSettingID;
            String backTicketID;
            String backCategoryType;

            //　乗客カテゴリ別販売パターン
            int salePatterns = response.ticket_sale_patterns[0].sale_patterns[0].sale_patterns.length;
            if (salePatterns > 0) {
                for (int i = 0; i < salePatterns; i++) {
                    // 初期化
                    backTicketSettingID = "";
                    backTicketID = "";
                    backCategoryType = "";

                    int saleTicketSettings = response.ticket_sale_patterns[0].sale_patterns[0].sale_patterns[i].sale_ticket_settings.length;
                    if (saleTicketSettings > 0) {
                        for (int j = 0; j < saleTicketSettings; j++) {
                            categoryData = new TicketSearchResults.CategoryData();
                            // カテゴリ名
                            categoryData.categoryType = response.ticket_sale_patterns[0].sale_patterns[0].sale_patterns[i].category_type;
                            if (!backCategoryType.equals(categoryData.categoryType)) {
                                backCategoryType = categoryData.categoryType;
                                backTicketSettingID = "";
                            }

                            if (backTicketSettingID.equals(response.ticket_sale_patterns[0].sale_patterns[0].sale_patterns[i].sale_ticket_settings[j].ticket_setting_id)) {
                                /* ticketSettingIDが同じであれば、同じ枚数券として判定 */
                                // 数量
                                searchResults.categoryData.get(categoryCount-1).quantity += 1;
                                // 金額
                                searchResults.categoryData.get(categoryCount-1).amount += response.ticket_sale_patterns[0].sale_patterns[0].sale_patterns[i].sale_ticket_settings[j].fare;
                            } else {
                                // 回数券ID
                                backTicketSettingID = response.ticket_sale_patterns[0].sale_patterns[0].sale_patterns[i].sale_ticket_settings[j].ticket_setting_id;
                                categoryData.ticketSettingID = backTicketSettingID;
                                // 回数券（ｎ枚セット券）
                                categoryData.ticketsNumber = response.ticket_sale_patterns[0].sale_patterns[0].sale_patterns[i].sale_ticket_settings[j].count;
                                // 数量
                                categoryData.quantity = 1;
                                // 金額
                                categoryData.amount = response.ticket_sale_patterns[0].sale_patterns[0].sale_patterns[i].sale_ticket_settings[j].fare;
                                // カテゴリを新規追加
                                searchResults.categoryData.add(categoryCount,categoryData);
                                categoryCount += 1;
                            }
                        }
                    }

                    int saleTickets = response.ticket_sale_patterns[0].sale_patterns[0].sale_patterns[i].sale_tickets.length;
                    if (saleTickets > 0) {
                        for (int j = 0; j < saleTickets; j++) {
                            categoryData = new TicketSearchResults.CategoryData();
                            // カテゴリ名
                            categoryData.categoryType = response.ticket_sale_patterns[0].sale_patterns[0].sale_patterns[i].category_type;
                            if (!backCategoryType.equals(categoryData.categoryType)) {
                                backCategoryType = categoryData.categoryType;
                                backTicketID = "";
                            }

                            if (backTicketID.equals(response.ticket_sale_patterns[0].sale_patterns[0].sale_patterns[i].sale_tickets[j].ticket_id)) {
                                /* ticketIDが同じであれば、同じ金額として判定 */
                                // 数量
                                searchResults.categoryData.get(categoryCount-1).quantity += 1;
                                // 金額
                                searchResults.categoryData.get(categoryCount-1).amount += response.ticket_sale_patterns[0].sale_patterns[0].sale_patterns[i].sale_tickets[j].fare;
                            } else {
                                backTicketID = response.ticket_sale_patterns[0].sale_patterns[0].sale_patterns[i].sale_tickets[j].ticket_id;
                                // 通常券
                                categoryData.ticketsNumber = 1;
                                // 数量
                                categoryData.quantity = 1;
                                // 金額
                                categoryData.amount = response.ticket_sale_patterns[0].sale_patterns[0].sale_patterns[i].sale_tickets[j].fare;
                                // カテゴリを新規追加
                                searchResults.categoryData.add(categoryCount,categoryData);
                                categoryCount += 1;
                            }
                        }
                    }
                }

                if (categoryCount > 0) {
                    // カテゴリデータを取得（成功）
                    searchResults.searchResult = true;
                } else {
                    // カテゴリデータを取得（例外）
                    Timber.e("乗客カテゴリ別販売パターンのカテゴリ = %d", categoryCount);
                    searchResults.searchResult = false;
                    searchResults.errorCode = 8014;
                    searchResults.nextTrip = false;
                    searchResults.prevTrip = false;
                    searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8014);
                    searchResults.errorMessageInformation = resources.getString(R.string.error_detail_ticket_8014);
                    throw new Exception();
                }

                // 乳幼児はリクエストに含めないため、ここでレスポンスにセットする　※仕様が変更になる可能性あり
                searchResults.babyNumber = searchInfo.babyNumber;
                if (searchInfo.babyNumber > 0) {
                    categoryData = new TicketSearchResults.CategoryData();
                    categoryData.categoryType = "baby";
                    categoryData.ticketsNumber = 1;
                    categoryData.quantity = searchInfo.babyNumber;
                    categoryData.amount = 0;
                    searchResults.categoryData.add(categoryCount,categoryData);
                }
            } else {
                // カテゴリデータを取得（例外）
                Timber.e("乗客カテゴリ別販売パターン = %d", salePatterns);
                searchResults.searchResult = false;
                searchResults.errorCode = 8015;
                searchResults.nextTrip = false;
                searchResults.prevTrip = false;
                searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8015);
                searchResults.errorMessageInformation = resources.getString(R.string.error_detail_ticket_8015);
                throw new Exception();
            }
        } else {
            // カテゴリデータを取得（例外）
            Timber.e("販売パターン = %d", response.ticket_sale_patterns[0].sale_patterns.length);
            searchResults.searchResult = false;
            searchResults.errorCode = 8016;
            searchResults.nextTrip = false;
            searchResults.prevTrip = false;
            searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8016);
            searchResults.errorMessageInformation = resources.getString(R.string.error_detail_ticket_8016);
            throw new Exception();
        }
    }
}
