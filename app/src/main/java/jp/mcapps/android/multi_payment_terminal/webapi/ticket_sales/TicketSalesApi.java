package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.gtfs.data.ListGTFSFeeds;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.AuthTest;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.DynamicTicket;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketClass;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketDisembark;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketEmbark;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketGateEmbark;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketReservationStatusByTrip;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketRoute;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketSale;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketTrip;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketTripByDateTime;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.TicketPurchasedCancel;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.TicketPurchasedConfirm;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.TicketPurchasedHistory;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.TicketSale;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.TicketUpdateDynamicTicketStatus;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.Tenant;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketTrip;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketTripPageInfo;


public interface TicketSalesApi {

    /**
     * アクセストークンを検証する
     *
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    AuthTest.Response authTest() throws IOException, HttpStatusException;

    /**
     * 取引先コードから店舗情報を取得します
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param customerCode      取引先コード
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    Tenant getTenantByCustomerCode(@NotNull String serviceInstanceID, @NotNull String customerCode) throws IOException, HttpStatusException, TicketSalesStatusException;

    /**
     * チケット分類一覧を取得する
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param limit             最大取得件数
     * @param offset            取得開始位置
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    ListTicketClass.Response listTicketClass(@NotNull String serviceInstanceID, int limit, int offset) throws IOException, HttpStatusException, TicketSalesStatusException;

    /**
     * のりば一覧を取得する（チケット発売用）
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param ticketClassId     チケットの分類ID
     * @param limit             最大取得件数
     * @param offset            取得開始位置
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    ListTicketEmbark.Response listTicketEmbark(@NotNull String serviceInstanceID, long ticketClassId, int limit, int offset) throws IOException, HttpStatusException, TicketSalesStatusException;

    /**
     * のりば停留所に対応する経路情報を取得する
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param origin_stopId     停留所ID
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    ListTicketRoute.Response listTicketRouteEmbark(@NotNull String serviceInstanceID, String origin_stopId) throws IOException, HttpStatusException, TicketSalesStatusException;

    /**
     * おりば一覧を取得する（チケット発売用）
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param ticketClassId     チケットの分類ID
     * @param limit             最大取得件数
     * @param offset            取得開始位置
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    ListTicketDisembark.Response listTicketDisembark(@NotNull String serviceInstanceID, long ticketClassId, int limit, int offset) throws IOException, HttpStatusException, TicketSalesStatusException;

    /**
     * おりば停留所に対応する経路情報を取得する
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param destination_stopId 停留所ID
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    ListTicketRoute.Response listTicketRouteDisembark(@NotNull String serviceInstanceID, String destination_stopId) throws IOException, HttpStatusException, TicketSalesStatusException;

    /**
     * のりば一覧を取得する（改札用）
     *
     * @param serviceInstanceID サービスインスタンスID
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    ListTicketGateEmbark.Response listTicketGateEmbark(@NotNull String serviceInstanceID) throws IOException, HttpStatusException, TicketSalesStatusException;

    /**
     * おりば一覧を取得する（改札用）
     *
     * @param serviceInstanceID サービスインスタンスID
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    ListTicketGateEmbark.Response listTicketGateDisembark(@NotNull String serviceInstanceID) throws IOException, HttpStatusException, TicketSalesStatusException;

    /**
     * GTFSの最新Feed情報取得
     *
     * @param serviceInstanceID サービスインスタンスID
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    ListGTFSFeeds.Response listGTFSLatestFeedInfo(@NotNull String serviceInstanceID) throws IOException, HttpStatusException, TicketSalesStatusException;

    /**
     * 指定されたのりばに対する経路の一覧を取得する（改札用）
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param stopID            停留所ID
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    ListTicketRoute.Response listTicketGateRouteEmbark(@NotNull String serviceInstanceID, @NotNull String stopID) throws IOException, HttpStatusException, TicketSalesStatusException;

    /**
     * 指定されたおりばに対する経路の一覧を取得する（改札用）
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param stopID            停留所ID
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    ListTicketRoute.Response listTicketGateRouteDisembark(@NotNull String serviceInstanceID, @NotNull String stopID) throws IOException, HttpStatusException, TicketSalesStatusException;

    /**
     * 指定された経路に対する便の一覧を取得する（改札用）
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param feedID            FeedID
     * @param routeID           経路ID
     * @param datetime          起点となる日時
     * @param limit             取得件数
     * @param offset            オフセット
     * @param offsetToLatest    直近の便を先頭に一覧を返却する
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    ListTicketTrip.Response listTicketGateTrip(@NotNull String serviceInstanceID, @NotNull String feedID, @NotNull String routeID, @NotNull String datetime, @NotNull String limit, @NotNull String offset, @NotNull String offsetToLatest) throws IOException, HttpStatusException, TicketSalesStatusException;

    /**
     * 指定された日時から運行便を取得する（チケット発売用）
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param ticketClassID     チケット分類ID
     * @param date              対象日(YYYYMMDD)　※基本的には当日を指定
     * @param departureTime     出発時刻(hhmmss)　※基本的には現在時刻を指定。 ただし、前便・次便検索を検索する場合は起点となる時刻（検索結果の出発時刻）を設定
     * @param embarkStopID      のりばID
     * @param disEmbarkStopID   おりばID
     * @param PageInfo          ページ情報
     * @param prevNextTripType  前便 or 次便 or 指定なし（null）　※前便を検索する場合はprevを、次便を検索する場合はnextを指定
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    ListTicketTripByDateTime.Response listTicketTripByDateTime(@NotNull String serviceInstanceID, @NotNull String ticketClassID, @NotNull String date,
                                                               @NotNull String departureTime, @NotNull String embarkStopID, @NotNull String disEmbarkStopID,
                                                               TicketTripPageInfo PageInfo, String prevNextTripType) throws IOException, HttpStatusException, TicketSalesStatusException;

    /**
     * 便IDから予約枠の情報を取得する（チケット発売用）
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param reservationDate   予約日(YYYYMMDD) ※検索対象日を指定
     * @param tripID            便ID(運行便取得で取得したtrip_info.trip_idを設定）
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    ListTicketReservationStatusByTrip.Response listTicketReservationStatusByTrip(@NotNull String serviceInstanceID, @NotNull String reservationDate, @NotNull String tripID) throws IOException, HttpStatusException, TicketSalesStatusException;

    /**
     * チケットIDからチケット販売パターンの候補を返す。（チケット発売用）
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param ticketID          チケットID
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    ListTicketSale.Response listTicketConfirmSaleByTicket(@NotNull String serviceInstanceID, @NotNull String ticketID, @NotNull TicketSale.Request request) throws IOException, HttpStatusException, TicketSalesStatusException;

    /**
     * 指定した日時から直近の便を返す。（改札用）
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param feedID            feedID
     * @param routeID           routeID
     * @param datetime          便を取得する際の基点日時（現在日時）
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    TicketTrip ticketGateTripLatest(@NotNull String serviceInstanceID, @NotNull String feedID, @NotNull String routeID, @NotNull String datetime) throws IOException, HttpStatusException, TicketSalesStatusException;

    /**
     * 動的チケットを生成する（便予約）
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param request           リクエスト                    【購入履歴送信】よりデータをセット
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    DynamicTicket.Response CreateDynamicTicket(@NotNull String serviceInstanceID, @NotNull DynamicTicket.Request request) throws IOException, HttpStatusException, TicketSalesStatusException;

    /**
     * チケット購入履歴作成
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param ticketID          購入したチケットID
     * @param request           リクエスト                    決裁時の情報よりデータをセット
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    TicketPurchasedHistory.Response TicketPurchasedHistory(@NotNull String serviceInstanceID, @NotNull String ticketID, @NotNull TicketPurchasedHistory.Request request) throws IOException, HttpStatusException, TicketSalesStatusException;

    /**
     * チケット購入の取消（可能かどうか確認する）
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param ticketID          購入したチケットID
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    TicketPurchasedConfirm.Response TicketPurchasedConfirm(@NotNull String serviceInstanceID, @NotNull String ticketID) throws IOException, HttpStatusException, TicketSalesStatusException;

    /**
     * チケット購入の取消
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param ticketID          購入したチケットID
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    TicketPurchasedCancel.Response TicketPurchasedCancel(@NotNull String serviceInstanceID, @NotNull String ticketID) throws IOException, HttpStatusException, TicketSalesStatusException;

    /**
     * 便予約の動的チケット判定およびステータス更新
     *
     * @param serviceInstanceID サービスインスタンスID
     * @param qrCodeItem        QRコード読込データ
     * @param request           リクエスト
     * @return
     * @throws IOException
     * @throws HttpStatusException
     */
    TicketUpdateDynamicTicketStatus.Response TicketUpdateDynamicTicketStatus(@NotNull String serviceInstanceID, @NotNull String qrCodeItem, @NotNull TicketUpdateDynamicTicketStatus.Request request) throws IOException, HttpStatusException, TicketSalesStatusException;
}
