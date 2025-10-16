package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.gtfs.data.ListGTFSFeeds;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.AuthTest;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.DynamicTicket;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.GetTenant;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketClass;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketDisembark;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketEmbark;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketGateEmbark;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketReservationStatusByTrip;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketRoute;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketSale;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketTrip;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketTripByDateTime;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.Status;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.TicketPurchasedCancel;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.TicketPurchasedConfirm;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.TicketPurchasedHistory;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.TicketSale;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.TicketUpdateDynamicTicketStatus;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.Tenant;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketTrip;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketTripPageInfo;
import jp.mcapps.android.multi_payment_terminal.webapi.utils.TokenAuthenticator;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

public class TicketSalesApiImpl implements TicketSalesApi {

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final TicketSalesApiImpl instance = new TicketSalesApiImpl();

    public static TicketSalesApi getInstance() {
        return instance;
    }

    private final OkHttpClient _client; // singletonにしたいやつはこれ
    private final Gson _gson = new GsonBuilder().disableHtmlEscaping().create();

    private TicketSalesApiImpl() {

        final TicketSalesAuthenticationRepository repository = new TicketSalesAuthenticationRepository();
        final TokenAuthenticator auth = new TokenAuthenticator(repository);

        _client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor((chain) -> {
                    Request req = chain.request().newBuilder()
                            .addHeader("Accept", "application/json")
                            .build();
                    return chain.proceed(req);
                })
                .addInterceptor(auth) // リクエスト時にAuthorizationヘッダをつけます
                .authenticator(auth) // レスポンスエラー(401)時にトークンを更新を行います
                .build();
    }

    @Override
    public AuthTest.Response authTest() throws IOException, HttpStatusException {

        String url = makeUrl("/auth/test");

        // request
        Request req = new Request.Builder()
                .url(url)
                .get()
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s", req.method(), req.url().toString());
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            return handleResponse(resp, AuthTest.Response.class);
        }
    }

    @Override
    public Tenant getTenantByCustomerCode(@NotNull String serviceInstanceID, @NotNull String customerCode) throws IOException, HttpStatusException, TicketSalesStatusException {
        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");
        customerCode = URLEncoder.encode(customerCode, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/tenant-by-customer-code/%s", serviceInstanceID, customerCode));

        // query
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();

        // request
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s", req.method(), req.url().toString());
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            GetTenant.Response r = handleResponse(resp, GetTenant.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r.data;
        }
    }

    @Override
    public ListTicketClass.Response listTicketClass(@NotNull String serviceInstanceID, int limit, int offset) throws IOException, HttpStatusException, TicketSalesStatusException {
        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/ticket-classes", serviceInstanceID));

        // query
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        urlBuilder.addQueryParameter("limit", Integer.toString(limit));
        urlBuilder.addQueryParameter("offset", Integer.toString(offset));

        // request
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s", req.method(), req.url().toString());
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            ListTicketClass.Response r = handleResponse(resp, ListTicketClass.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r;
        }
    }

    @Override
    public ListTicketEmbark.Response listTicketEmbark(@NotNull String serviceInstanceID, long ticketClassId, int limit, int offset) throws IOException, HttpStatusException, TicketSalesStatusException {
        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/ticket-classes/%s/origin-stops", serviceInstanceID, ticketClassId));

        // query
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        urlBuilder.addQueryParameter("limit", Integer.toString(limit));
        urlBuilder.addQueryParameter("offset", Integer.toString(offset));

        // request
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s", req.method(), req.url().toString());
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            ListTicketEmbark.Response r = handleResponse(resp, ListTicketEmbark.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r;
        }
    }

    @Override
    public ListTicketDisembark.Response listTicketDisembark(@NotNull String serviceInstanceID, long ticketClassId, int limit, int offset) throws IOException, HttpStatusException, TicketSalesStatusException {
        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/ticket-classes/%s/destination-stops", serviceInstanceID, ticketClassId));

        // query
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        urlBuilder.addQueryParameter("limit", Integer.toString(limit));
        urlBuilder.addQueryParameter("offset", Integer.toString(offset));

        // request
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s", req.method(), req.url().toString());
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            ListTicketDisembark.Response r = handleResponse(resp, ListTicketDisembark.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r;
        }
    }

    @Override
    public ListTicketRoute.Response listTicketRouteEmbark(@NotNull String serviceInstanceID, String origin_stopId) throws IOException, HttpStatusException, TicketSalesStatusException {
        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");
        origin_stopId = URLEncoder.encode(origin_stopId, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/ticket-routes-containing-origin-stops/%s", serviceInstanceID, origin_stopId));

        // query
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();

        // request
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s", req.method(), req.url().toString());
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            ListTicketRoute.Response r = handleResponse(resp, ListTicketRoute.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r;
        }
    }

    @Override
    public ListTicketRoute.Response listTicketRouteDisembark(@NotNull String serviceInstanceID, String destination_stopId) throws IOException, HttpStatusException, TicketSalesStatusException {
        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");
        destination_stopId = URLEncoder.encode(destination_stopId, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/ticket-routes-containing-destination-stops/%s", serviceInstanceID, destination_stopId));

        // query
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();

        // request
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s", req.method(), req.url().toString());
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            ListTicketRoute.Response r = handleResponse(resp, ListTicketRoute.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r;
        }
    }

    @Override
    public ListTicketGateEmbark.Response listTicketGateEmbark(@NotNull String serviceInstanceID) throws IOException, HttpStatusException, TicketSalesStatusException {
        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/ticket-classes-all/origin-stops", serviceInstanceID));

        // query
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();

        // request
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s", req.method(), req.url().toString());
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            ListTicketGateEmbark.Response r = handleResponse(resp, ListTicketGateEmbark.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r;
        }
    }

    @Override
    public ListTicketGateEmbark.Response listTicketGateDisembark(@NotNull String serviceInstanceID) throws IOException, HttpStatusException, TicketSalesStatusException {
        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/ticket-classes-all/destination-stops", serviceInstanceID));

        // query
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();

        // request
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s", req.method(), req.url().toString());
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            ListTicketGateEmbark.Response r = handleResponse(resp, ListTicketGateEmbark.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r;
        }
    }

    @Override
    public ListGTFSFeeds.Response listGTFSLatestFeedInfo(@NotNull String serviceInstanceID) throws IOException, HttpStatusException, TicketSalesStatusException {
        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/gtfs-feeds", serviceInstanceID));

        // query
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        urlBuilder.addQueryParameter("limit", "1");
        urlBuilder.addQueryParameter("latest", "true");
        urlBuilder.addQueryParameter("env", "production");

        // request
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s", req.method(), req.url().toString());
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            ListGTFSFeeds.Response r = handleResponse(resp, ListGTFSFeeds.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r;
        }
    }

    @Override
    public ListTicketRoute.Response listTicketGateRouteEmbark(@NotNull String serviceInstanceID, @NotNull String stopID) throws IOException, HttpStatusException, TicketSalesStatusException {
        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");
        stopID = URLEncoder.encode(stopID, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/ticket-routes-containing-origin-stops/%s", serviceInstanceID, stopID));

        // query
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();

        // request
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s", req.method(), req.url().toString());
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            ListTicketRoute.Response r = handleResponse(resp, ListTicketRoute.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r;
        }
    }

    @Override
    public ListTicketRoute.Response listTicketGateRouteDisembark(@NotNull String serviceInstanceID, @NotNull String stopID) throws IOException, HttpStatusException, TicketSalesStatusException {
        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");
        stopID = URLEncoder.encode(stopID, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/ticket-routes-containing-destination-stops/%s", serviceInstanceID, stopID));

        // query
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();

        // request
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s", req.method(), req.url().toString());
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            ListTicketRoute.Response r = handleResponse(resp, ListTicketRoute.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r;
        }
    }

    @Override
    public ListTicketTrip.Response listTicketGateTrip(@NotNull String serviceInstanceID, @NotNull String feedID, @NotNull String routeID,
                                                      @NotNull String datetime, @NotNull String limit, @NotNull String offset, @NotNull String offsetToLatest) throws IOException, HttpStatusException, TicketSalesStatusException {
        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");
        feedID = URLEncoder.encode(feedID, "UTF-8");
        routeID = URLEncoder.encode(routeID, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/ticket-route-trips/%s/%s", serviceInstanceID, feedID, routeID));

        // query
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        urlBuilder.addQueryParameter("service_timepoint", datetime);
        urlBuilder.addQueryParameter("limit", limit);
        urlBuilder.addQueryParameter("offset", offset);
        urlBuilder.addQueryParameter("offset_to_latest", offsetToLatest);

        // request
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s", req.method(), req.url().toString());
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            ListTicketTrip.Response r = handleResponse(resp, ListTicketTrip.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r;
        }
    }

    @Override
    public ListTicketTripByDateTime.Response listTicketTripByDateTime(@NotNull String serviceInstanceID, @NotNull String ticketClassID, @NotNull String date,
                                                                      @NotNull String departureTime, @NotNull String embarkStopID, @NotNull String disEmbarkStopID,
                                                                      TicketTripPageInfo PageInfo, String prevNextTripType) throws IOException, HttpStatusException, TicketSalesStatusException {

        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");
        ticketClassID = URLEncoder.encode(ticketClassID, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/ticket-classes/%s/trip-by-date-time/%s/%s", serviceInstanceID, ticketClassID, date, departureTime));

        // query
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        urlBuilder.addQueryParameter("origin_stop_id", embarkStopID);
        urlBuilder.addQueryParameter("destination_stop_id", disEmbarkStopID);
        if (null != PageInfo) {
            urlBuilder.addQueryParameter("page_info.trip_info.trip_id", PageInfo.trip_info.trip_id);
            urlBuilder.addQueryParameter("page_info.trip_info.is_special", String.valueOf(PageInfo.trip_info.is_special));
            urlBuilder.addQueryParameter("page_info.date", PageInfo.date);
            urlBuilder.addQueryParameter("page_info.departure_time", PageInfo.departure_time);
            urlBuilder.addQueryParameter("page_info.arrival_time", PageInfo.arrival_time);
        }
        if (prevNextTripType == "prev" || prevNextTripType == "next") {
            urlBuilder.addQueryParameter("prev_next_trip_type", prevNextTripType);
        }

        // request body
        String bodyJson = _gson.toJson("");
        RequestBody body = RequestBody.create(bodyJson, JSON);

        // request
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .post(body)
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s", req.method(), req.url().toString());
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            ListTicketTripByDateTime.Response r = handleResponse(resp, ListTicketTripByDateTime.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r;
        }
    }

    @Override
    public ListTicketReservationStatusByTrip.Response listTicketReservationStatusByTrip(@NotNull String serviceInstanceID, @NotNull String reservationDate, @NotNull String tripID) throws IOException, HttpStatusException, TicketSalesStatusException {
        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/reservation-slot-status-by-trip/%s", serviceInstanceID, reservationDate));

        // query
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        urlBuilder.addQueryParameter("trip_info.trip_id", tripID);

        // request body
        String bodyJson = _gson.toJson("");
        RequestBody body = RequestBody.create(bodyJson, JSON);

        // request
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .post(body)
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s", req.method(), req.url().toString());
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            ListTicketReservationStatusByTrip.Response r = handleResponse(resp, ListTicketReservationStatusByTrip.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r;
        }
    }

    @Override
    public ListTicketSale.Response listTicketConfirmSaleByTicket(@NotNull String serviceInstanceID, @NotNull String ticketID, @NotNull TicketSale.Request request) throws IOException, HttpStatusException, TicketSalesStatusException {
        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");
        ticketID = URLEncoder.encode(ticketID, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/confirm-sale-by-ticket/%s", serviceInstanceID, ticketID));

        // request body
        String bodyJson = _gson.toJson(request);
        RequestBody body = RequestBody.create(bodyJson, JSON);

        // request
        Request req = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s %s", req.method(), req.url().toString(), bodyJson);
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            ListTicketSale.Response r = handleResponse(resp, ListTicketSale.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r;
        }
    }

    private <T> T handleResponse(final Response resp, Class<T> clazz) throws IOException {
        assert resp.body() != null;
        String s = resp.body().string();
        String name = clazz.getName();
//        if(name.contains("ListTicketClass"))
//        {
//            s = "{\"items\":[{\"id\":\"24\"}]}";
//        }
        Request req = resp.request();
        if (BuildConfig.DEBUG) {
            Timber.d("Response: %s %s (StatusCode: %s) %s", req.method(), req.url().toString(), resp.code(), s);
        }
        return _gson.fromJson(s, clazz);
    }

    private void handleErrorStatus(final Response resp, final Status st) throws TicketSalesStatusException {
        if (st != null) {
            Request req = resp.request();
            Timber.e("Domain error on api request: %s %s (code: %s message: '%s')", req.method(), req.url().toString(), st.code, st.message);
            throw new TicketSalesStatusException(st.code, st.message);
        }
    }

    private void handleHttpStatus(final Response resp) throws HttpStatusException, IOException {
        if (!resp.isSuccessful()) {
            Request req = resp.request();
            String s = resp.body() != null ? resp.body().string() : null;

            Timber.e("StatusCode error on api request: %s %s (StatusCode: %s) %s", req.method(), req.url().toString(), resp.code(), s);
            throw new HttpStatusException(resp.code(), s);
        }
    }

    private String makeUrl(@NotNull String path) {
        return BuildConfig.POS_ACTIVATE_BASE_URL + path;
    }

    @Override
    public TicketTrip ticketGateTripLatest(@NotNull String serviceInstanceID, @NotNull String feedID, @NotNull String routeID, @NotNull String datetime) throws IOException, HttpStatusException, TicketSalesStatusException {
        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");
        feedID = URLEncoder.encode(feedID, "UTF-8");
        routeID = URLEncoder.encode(routeID, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/ticket-route-trips/%s/%s", serviceInstanceID, feedID, routeID));

        // query
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        urlBuilder.addQueryParameter("service_timepoint", datetime);
        urlBuilder.addQueryParameter("limit", "1");
        urlBuilder.addQueryParameter("offset_to_latest", "true");

        // request
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s", req.method(), req.url().toString());
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            ListTicketTrip.Response r = handleResponse(resp, ListTicketTrip.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            if (r != null && r.items.length > 0) {
                return r.items[0];
            }

            return null;
        }
    }

    @Override
    public DynamicTicket.Response CreateDynamicTicket(@NotNull String serviceInstanceID, @NotNull DynamicTicket.Request request) throws IOException, HttpStatusException, TicketSalesStatusException {
        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/create-dynamic-ticket-from-trip-reservation", serviceInstanceID));

        // request body
        String bodyJson = _gson.toJson(request);
        RequestBody body = RequestBody.create(bodyJson, JSON);

        // request
        Request req = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s %s", req.method(), req.url().toString(), bodyJson);
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            DynamicTicket.Response r = handleResponse(resp, DynamicTicket.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r;
        }
    }

    @Override
    public TicketPurchasedHistory.Response TicketPurchasedHistory(@NotNull String serviceInstanceID, @NotNull String ticketID, @NotNull TicketPurchasedHistory.Request request) throws IOException, HttpStatusException, TicketSalesStatusException {
        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");
        ticketID = URLEncoder.encode(ticketID, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/purchased-history/%s", serviceInstanceID, ticketID));

        // request body
        String bodyJson = _gson.toJson(request);
        RequestBody body = RequestBody.create(bodyJson, JSON);

        // request
        Request req = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s %s", req.method(), req.url().toString(), bodyJson);
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            TicketPurchasedHistory.Response r = handleResponse(resp, TicketPurchasedHistory.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r;
        }
    }

    @Override
    public TicketPurchasedConfirm.Response TicketPurchasedConfirm(@NotNull String serviceInstanceID, @NotNull String ticketID) throws IOException, HttpStatusException, TicketSalesStatusException {
        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");
        ticketID = URLEncoder.encode(ticketID, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/confirm-purchased-ticket/%s", serviceInstanceID, ticketID));

        // query
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();

        // request
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s", req.method(), req.url().toString());
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            TicketPurchasedConfirm.Response r = handleResponse(resp, TicketPurchasedConfirm.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r;
        }
    }

    @Override
    public TicketPurchasedCancel.Response TicketPurchasedCancel(@NotNull String serviceInstanceID, @NotNull String ticketID) throws IOException, HttpStatusException, TicketSalesStatusException {
        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");
        ticketID = URLEncoder.encode(ticketID, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/cancel-purchased-ticket/%s", serviceInstanceID, ticketID));

        // query
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();

        // request
        Request req = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s", req.method(), req.url().toString());
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            TicketPurchasedCancel.Response r = handleResponse(resp, TicketPurchasedCancel.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r;
        }
    }

    @Override
    public TicketUpdateDynamicTicketStatus.Response TicketUpdateDynamicTicketStatus(@NotNull String serviceInstanceID, @NotNull String qrCodeItem, TicketUpdateDynamicTicketStatus.@NotNull Request request) throws IOException, HttpStatusException, TicketSalesStatusException {
        serviceInstanceID = URLEncoder.encode(serviceInstanceID, "UTF-8");
        qrCodeItem = URLEncoder.encode(qrCodeItem, "UTF-8");

        String url = makeUrl(String.format("/instances/%s/update-dynamic-ticket-status-from-trip-reservation/%s", serviceInstanceID, qrCodeItem));

        // request body
        String bodyJson = _gson.toJson(request);
        RequestBody body = RequestBody.create(bodyJson, JSON);

        // request
        Request req = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        if (BuildConfig.DEBUG) {
            Timber.d("Request: %s %s %s", req.method(), req.url().toString(), bodyJson);
        }

        // response
        try (final Response resp = _client.newCall(req).execute()) {
            // error handling
            handleHttpStatus(resp);

            // parse body
            TicketUpdateDynamicTicketStatus.Response r = handleResponse(resp, TicketUpdateDynamicTicketStatus.Response.class);

            // error handling
            handleErrorStatus(resp, r.error);

            return r;
        }
    }
}
