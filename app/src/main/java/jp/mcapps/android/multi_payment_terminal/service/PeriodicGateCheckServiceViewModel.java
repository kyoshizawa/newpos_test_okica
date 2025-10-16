package jp.mcapps.android.multi_payment_terminal.service;

import android.app.Service;
import android.content.Intent;
import android.util.Pair;

import androidx.lifecycle.ViewModel;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsData;
import jp.mcapps.android.multi_payment_terminal.model.ticket.TicketRepository;
import jp.mcapps.android.multi_payment_terminal.ui.ticket.TicketGateRoute;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApi;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketRoute;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketRoute;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketTrip;
import timber.log.Timber;

public class PeriodicGateCheckServiceViewModel extends ViewModel {

    public PeriodicGateCheckServiceViewModel() {super();}

    private final TicketGateSettingsDao _ticketGateSettingsDao = DBManager.getTicketGateSettingsDao();

    private final TerminalDao _terminalDao = LocalDatabase.getInstance().terminalDao();

    private TicketGateSettingsData _ticketGateSettingData = null;

    private List<TicketGateRoute> _ticketGateRouteList = null;

    private String _nextTrip_start_gate_check_time = "";

    private TerminalData _terminalData = null;

    private ScheduledExecutorService _scheduledExecutor;

    private final TicketSalesApi _ticketSalesApiClient = TicketSalesApiImpl.getInstance();

    private String _manual_next_trip_id = "";

    TicketGateSettingsData getTicketGateSettingsData() {
        return _ticketGateSettingsDao.getTicketGateSettingsLatest();
    }

    public void setTicketGateRoutes(List<TicketGateRoute> data) { _ticketGateRouteList = data; }

    public void fetch(Service service) {
        Observable.fromCallable(() -> {
                    _terminalData = _terminalDao.getTerminal();
                    return getTicketGateSettingsData();
                })
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            Timber.d("on fetch data");
                            _ticketGateSettingData = result;
                            _scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

                            _scheduledExecutor.scheduleWithFixedDelay(() -> {
                                Timber.d("Periodic Gate Check Service Timeout");
                                try {
                                    updateTrip();
                                    Intent broadcastIntent = new Intent();
                                    broadcastIntent.setAction("TICKET_GATE_CHECK");
                                    broadcastIntent.putExtra("settingData", _ticketGateSettingData);
                                    broadcastIntent.putExtra("nextTripGateCheckStartTime", _nextTrip_start_gate_check_time);
                                    LocalBroadcastManager.getInstance(service).sendBroadcast(broadcastIntent);
                                } catch (IOException | HttpStatusException | TicketSalesStatusException | ParseException e) {
                                    // 定期チェック時のエラーはログに残すのみでユーザーには通知しない 8097, 8098
                                    Timber.e("定期チェックエラー : %s", e.getMessage());
                                }

                            }, 0, _ticketGateSettingData.auto_gate_check_interval, TimeUnit.MINUTES); //チェック間隔 改札設定より取得
                        },
                        error -> {
                            // 定期チェック時のエラーはログに残すのみでユーザーには通知しない
                            Timber.e("定期チェックエラー : %s", error.getMessage());
                        }
                );
    }

    public List<TicketGateRoute> getRoutes(String[] stopIds) throws IOException, HttpStatusException, TicketSalesStatusException {
        String service_instance_id = _terminalData.service_instance_abt;
       List<TicketGateRoute> ticketGateRouteList = new ArrayList<>();

        for (String item: stopIds) {
            ListTicketRoute.Response route = _ticketSalesApiClient.listTicketGateRouteEmbark(service_instance_id, item);
            if (route != null && route.items != null && route.items.length > 0) {
                // routeリストに登録
                for (TicketRoute route_item : route.items) {
                    TicketGateRoute gateRoute = new TicketGateRoute(route_item.route_id, route_item.route_name);
                    ticketGateRouteList.add(gateRoute);
                }
            }
        }
        return ticketGateRouteList;
    }

    public void updateTrip() throws IOException, HttpStatusException, TicketSalesStatusException, ParseException {
        String service_instance_id = _terminalData.service_instance_abt;
        String feed_id = AppPreference.getGTFSCurrentFeedId();
        Date now = new Date();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(cal.getTimeZone());
        String datetime = sdf.format(cal.getTime());
        TicketTrip nextTrip = null;

        SimpleDateFormat sdfYyyymmdd = new SimpleDateFormat("yyyy/MM/dd");
        SimpleDateFormat sdfYyyymmddhhMMss = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String nowDate = sdfYyyymmdd.format(now);
        Date nowTrip_start_departure_time = null;
        Date nextTrip_start_departure_time = null;

        if (null == feed_id || feed_id.equals("")) {
            // GTFS feed id 異常時、チケット販売マスタ更新実行
            Timber.e("updateTrip->feed_id:%s",feed_id);
            try {
                // チケット販売マスタ更新
                TicketRepository repo = new TicketRepository();
                repo.refreshTicketSales();
                feed_id = AppPreference.getGTFSCurrentFeedId();
            } catch (Exception e) {
                Timber.e(e, "updateTrip->repo.refreshTicketSales()");
                return;
            }
        }

        if (!_ticketGateSettingData.start_departure_time.equals("")) {
            nowTrip_start_departure_time = sdfYyyymmddhhMMss.parse(nowDate + " " + _ticketGateSettingData.start_departure_time);
        }

        if (_ticketGateSettingData.auto_gate_check) {
            for (TicketGateRoute item : _ticketGateRouteList) {
                TicketTrip trip = _ticketSalesApiClient.ticketGateTripLatest(service_instance_id, feed_id, item.routeId, datetime);
                if (trip == null) continue;
                if (nextTrip == null) {
                    nextTrip = trip;
                    nextTrip_start_departure_time = sdfYyyymmddhhMMss.parse(nowDate + " " + nextTrip.start_departure_time);
                } else {
                    Date start_departure_time = sdfYyyymmddhhMMss.parse(nowDate + " " + trip.start_departure_time);
                    // 比較してもっとも早い出発時刻のものを検札対象候補とする
                    if (start_departure_time.compareTo(nextTrip_start_departure_time) < 0) {
                        nextTrip = trip;
                        nextTrip_start_departure_time = start_departure_time;
                    }
                }
            }
        } else {
            nextTrip = new TicketTrip();
            if (!_ticketGateSettingData.trip_id.equals("")) {
                _manual_next_trip_id = _ticketGateSettingData.trip_id;
                _ticketGateSettingData.trip_id = "";    // 手動更新の場合いったん検札対象なしにする
            }
            nextTrip.trip_id = _manual_next_trip_id;
            nextTrip.start_stop_name = _ticketGateSettingData.start_stop_name;
            nextTrip.start_departure_time = _ticketGateSettingData.start_departure_time;
            nextTrip.end_stop_name = _ticketGateSettingData.end_stop_name;
            nextTrip.end_arrival_time = _ticketGateSettingData.end_arrival_time;
            nextTrip_start_departure_time = sdfYyyymmddhhMMss.parse(nowDate + " " + nextTrip.start_departure_time);
        }

        if (nextTrip != null
        && ((nowTrip_start_departure_time == null)
        || (_ticketGateSettingData.trip_id == "")   // 現在検札対象なし
        || (_ticketGateSettingData.trip_id != nextTrip.trip_id && nowTrip_start_departure_time.compareTo(now) < 0 ))) { // 現在便と直近の便が異なり かつ 現在便の出発時刻を過ぎている(自動検札のため便の遅れは考慮しない）
            cal = Calendar.getInstance();
            cal.setTime(nextTrip_start_departure_time);
            cal.add(Calendar.MINUTE, _ticketGateSettingData.gate_check_start_time * -1); // 検札開始設定分現在時刻から引く
            Date nextTrip_start_gate_check_time = cal.getTime();
            SimpleDateFormat sdfhhmm = new SimpleDateFormat("HH:mm");
            _nextTrip_start_gate_check_time = sdfhhmm.format(nextTrip_start_gate_check_time);

            if (now.compareTo(nextTrip_start_gate_check_time) >= 0) { // 次の便の検札開始時刻を過ぎている
                // 改札設定を更新（次の便を検札対象に）
                _ticketGateSettingData.trip_id = nextTrip.trip_id;
            } else {
                // 改札設定を更新（検札対象なし）
                _ticketGateSettingData.trip_id = "";    // 検札対象なしにするためIDは空欄に
            }
            _ticketGateSettingData.start_stop_name = nextTrip.start_stop_name;
            _ticketGateSettingData.start_departure_time = nextTrip.start_departure_time;
            _ticketGateSettingData.end_stop_name = nextTrip.end_stop_name;
            _ticketGateSettingData.end_arrival_time = nextTrip.end_arrival_time;
            _ticketGateSettingData.created_at = now;
            _ticketGateSettingsDao.insertTicketGateSettingsData(_ticketGateSettingData);
        }
    }

    public void shutdown() {
        _scheduledExecutor.shutdown();
    }
}
