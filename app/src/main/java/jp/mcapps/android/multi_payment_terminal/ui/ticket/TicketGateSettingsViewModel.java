package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import android.content.Intent;
import android.content.res.Resources;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.amplifyframework.core.model.temporal.Temporal;

import java.io.IOException;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsData;
import jp.mcapps.android.multi_payment_terminal.model.ticket.TicketRepository;
import jp.mcapps.android.multi_payment_terminal.service.PeriodicGateCheckService;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApi;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketGateEmbark;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketRoute;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketTrip;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketGateEmbark;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketRoute;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketTrip;
import timber.log.Timber;

public class TicketGateSettingsViewModel extends ViewModel {
    public TicketGateSettingsViewModel() {
        super();
    }

    private final int DEFAULT_GATE_CHECK_START_TIME_POS = 2;
    private final int DEFAULT_GATE_CHECK_INTERVAL_POS = 1;
    private final Integer TRIP_TIME_DISPLAY_NUM = 3;   // 時刻表示件数

    private final TicketGateSettingsDao _ticketGateSettingsDao = DBManager.getTicketGateSettingsDao();

    private TicketGateSettingsData _ticketGateSettingData = null;

    private TicketGateSettingsData _ticketGateSettingDataBackup = null;

    private final TerminalDao _terminalDao = LocalDatabase.getInstance().terminalDao();

    private TerminalData _terminalData = null;

    private String _serviceInstanceId = null;

    private String _datetime = null;

    private final TicketSalesApi _ticketSalesApiClient = TicketSalesApiImpl.getInstance();

    private List<TicketGateLocation> _ticketGateLocationList = new ArrayList<>();

    private List<TicketGateRoute> _ticketGateRouteList = new ArrayList<>();

    private List<TicketGateRoute> _ticketGateRouteListBackup = new ArrayList<>();

    private List<Pair<String, String>> _tripTimeList = new ArrayList<>();
    private List<Pair<String, String>> _startEndNameList = new ArrayList<>();

    private String[] _startTimes;

    private String[] _intervalTimes;

    private boolean _bInit = false;

    private boolean _bInitRoute = false;

    private TicketGateSettingsData getTicketGateSettingsData() {
        return _ticketGateSettingsDao.getTicketGateSettingsLatest();
    }

    private void insertTicketGateSettingsData(TicketGateSettingsData data) {
        _ticketGateSettingsDao.insertTicketGateSettingsData(data);
    }
/*
    private void updateTicketGateSettingsData(TicketGateSettingsData data) {
        Observable.fromCallable(() -> {
                    _ticketGateSettingsDao.deleteAll();
                    return true;
                })
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            _ticketGateSettingsDao.insertTicketGateSettingsData(data);
                        },
                        error -> {
                            Timber.e(error, "error delete GateSettingsData");
                        }
                );

    }
 */
    public void fetch(TicketGateSettingsLocationAdapter locationAdapter, TicketGateSettingsRouteAdapter routeAdapter, String[] startTimes, String[] intervalTimes) {
        _bInit = false;
        _bInitRoute = false;
        _startTimes = startTimes;
        _intervalTimes = intervalTimes;

        // 現在時刻を取得（UTC）
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(cal.getTimeZone());
        _datetime = sdf.format(cal.getTime());

        initTripSelect();

        String feed_id = AppPreference.getGTFSCurrentFeedId();

        if (null == feed_id || feed_id.equals("")) {
            // GTFS feed id 異常時、チケット販売マスタ更新実行
            Timber.e("fetch->feed_id:%s",feed_id);
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // チケット販売マスタ更新
                    try {
                        TicketRepository repo = new TicketRepository();
                        repo.refreshTicketSales();
                    } catch (Exception e) {
                        Timber.e(e, "fetch->repo.refreshTicketSales()");
                    }
                }
            });
            thread.start();

            try {
                thread.join();
                String feedId = AppPreference.getGTFSCurrentFeedId();
                if (null == feedId || feedId.equals("")) {
                    Timber.e("fetch->feedId:%s",feedId);
                    isInitResult(false);
                    return;
                }
            } catch (Exception e) {
                Timber.e(e);
                isInitResult(false);
                return;
            }
        }

        Observable.fromCallable(() -> {
                    _terminalData = _terminalDao.getTerminal();
                    _serviceInstanceId = _terminalData.service_instance_abt;
                    return getTicketGateSettingsData();
                })
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            Timber.d("on fetch data");
                            _ticketGateSettingData = result;
                            _ticketGateSettingDataBackup = (TicketGateSettingsData) _ticketGateSettingData.clone();
                            makeInitData(_ticketGateSettingData)
                            .subscribe(
                                    ret -> {
                                        Timber.d("insert gate settings data");
                                        locationAdapter.addAll(_ticketGateLocationList);
                                        routeAdapter.addAll(_ticketGateRouteList);
                                    },
                                    err -> {
                                        Timber.e(err);
                                        _ticketGateSettingData = null;
                                        _ticketGateSettingDataBackup = null;
                                        isInitResult(false);
                                        Resources resources = MainApplication.getInstance().getResources();

                                        if (err.getClass() == TicketSalesStatusException.class) {
                                            Timber.e(err, "makeInitData error %s %s",String.valueOf(((TicketSalesStatusException) err).getCode()), err.getMessage());
                                            setErrorCode(resources.getString(R.string.error_type_ticket_8210));
                                            setErrorMessage(resources.getString(R.string.error_message_ticket_8210));
                                            setErrorMessageInformation(String.format(resources.getString(R.string.error_detail_ticket_8210), String.valueOf(((TicketSalesStatusException) err).getCode())));
                                        } else if (err.getClass() == HttpStatusException.class) {
                                            Timber.e(err, "makeInitData error %s %s", String.valueOf(((HttpStatusException) err).getStatusCode()), err.getMessage());
                                            setErrorCode(resources.getString(R.string.error_type_ticket_8210));
                                            setErrorMessage(resources.getString(R.string.error_message_ticket_8210));
                                            setErrorMessageInformation(String.format(resources.getString(R.string.error_detail_ticket_8210), String.valueOf(((HttpStatusException) err).getStatusCode())));
                                        } else {
                                            Timber.e(err, "makeInitData error %s", err.getMessage());
                                            setErrorCode(resources.getString(R.string.error_type_ticket_8097));
                                            setErrorMessage(resources.getString(R.string.error_message_ticket_8097));
                                            setErrorMessageInformation(resources.getString(R.string.error_detail_ticket_8097));
                                        }
                                    });
                        },
                        error -> {
                            // エラーハンドリングの処理
                            Timber.e(error);
                            makeInitData(null)
                            .subscribe(
                                      result -> {
                                          Timber.d("insert gate settings data");
                                          locationAdapter.addAll(_ticketGateLocationList);
                                          routeAdapter.addAll(_ticketGateRouteList);
                                      },
                                      err -> {
                                          Timber.e(err);
                                          _ticketGateSettingData = null;
                                          _ticketGateSettingDataBackup = null;
                                          isInitResult(false);
                                          Resources resources = MainApplication.getInstance().getResources();

                                          if (err.getClass() == TicketSalesStatusException.class) {
                                              Timber.e(err, "makeInitData error %s %s",String.valueOf(((TicketSalesStatusException) err).getCode()), err.getMessage());
                                              setErrorCode(resources.getString(R.string.error_type_ticket_8210));
                                              setErrorMessage(resources.getString(R.string.error_message_ticket_8210));
                                              setErrorMessageInformation(String.format(resources.getString(R.string.error_detail_ticket_8210), String.valueOf(((TicketSalesStatusException) err).getCode())));
                                          } else if (err.getClass() == HttpStatusException.class) {
                                              Timber.e(err, "makeInitData error %s %s", String.valueOf(((HttpStatusException) err).getStatusCode()), err.getMessage());
                                              setErrorCode(resources.getString(R.string.error_type_ticket_8210));
                                              setErrorMessage(resources.getString(R.string.error_message_ticket_8210));
                                              setErrorMessageInformation(String.format(resources.getString(R.string.error_detail_ticket_8210), String.valueOf(((HttpStatusException) err).getStatusCode())));
                                          } else {
                                              Timber.e(err, "makeInitData error %s", err.getMessage());
                                              setErrorCode(resources.getString(R.string.error_type_ticket_8097));
                                              setErrorMessage(resources.getString(R.string.error_message_ticket_8097));
                                              setErrorMessageInformation(resources.getString(R.string.error_detail_ticket_8097));
                                          }
                                      });
                        });
    }

    private Observable<TicketGateSettingsData> makeInitData(TicketGateSettingsData ticketGateSettingsData) {
        return Observable.fromCallable(() -> {
                    String stopId = "", stopName = "", routeId = "", routeName = "";
                    String tripId = "", startStopName = "", startDepartureTime = "", endStopName = "", endArrivalTime = "";

                    // 改札の乗り場一覧を取得
                    ListTicketGateEmbark.Response response = _ticketSalesApiClient.listTicketGateEmbark(_serviceInstanceId);
                    if (response != null && response.items != null && response.items.length > 0) {
                        // locationリストに登録
                        for (TicketGateEmbark item : response.items) {
                            TicketGateLocation location = new TicketGateLocation(item.stop_ids, item.stop_name);
                            _ticketGateLocationList.add(location);
                        }
                    }
                    // locationをソート
                    Collator collator = Collator.getInstance(Locale.JAPAN);
                    for (int i = 0; i < _ticketGateLocationList.size() - 1; i++) {
                        for (int j = _ticketGateLocationList.size() - 1; j > i; j--) {
                            if (collator.compare(_ticketGateLocationList.get(j).stopName, _ticketGateLocationList.get(j - 1).stopName) < 0) {
                                Collections.swap(_ticketGateLocationList, j, j-1);
                            };
                        }
                    }
                    if (ticketGateSettingsData == null && _ticketGateLocationList.size() > 0) {
                        // 先頭の乗り場を初期値としてセット
                        stopId = _ticketGateLocationList.get(0).stopIds[0];
                        stopName = _ticketGateLocationList.get(0).stopName;
                    } else {
                        // のりば、経路は保存されたものをセット
                        stopId = ticketGateSettingsData.stop_id;
                        routeId = ticketGateSettingsData.route_id;
                    }

                    // 経路の一覧を取得
                    getRoute(_ticketGateLocationList.get(0).stopIds);
                    if (_ticketGateRouteList.size() > 0) {
                        if (stopId != "" && routeId == "") {
                            // 先頭の経路を初期値としてセット
                            routeId = _ticketGateRouteList.get(0).routeId;
                            routeName = _ticketGateRouteList.get(0).routeName;
                        }
                    }

                    // 便の一覧を取得（直近３件）
                    if (routeId != "") {
                        TicketTrip ticketTrip = getTrip(routeId);
                        tripId = ticketTrip.trip_id;
                        startStopName = ticketTrip.start_stop_name;
                        startDepartureTime = ticketTrip.start_departure_time;
                        endStopName = ticketTrip.end_stop_name;
                        endArrivalTime = ticketTrip.end_arrival_time;
                    }

                    if (_ticketGateSettingData == null) {
                        Timber.d("初回の改札設定");
                        TicketGateSettingsData data = new TicketGateSettingsData();
                        data.stop_id = stopId;
                        data.stop_name = stopName;
                        data.stop_type = "embark";
                        data.gate_check_start_time = Integer.parseInt(_startTimes[DEFAULT_GATE_CHECK_START_TIME_POS]);
                        data.auto_gate_check = true;
                        data.auto_gate_check_interval = Integer.parseInt(_intervalTimes[DEFAULT_GATE_CHECK_INTERVAL_POS]);
                        data.route_id = routeId;
                        data.route_name = routeName;
                        data.trip_id = tripId;
                        data.start_stop_name = startStopName;
                        data.start_departure_time = startDepartureTime;
                        data.end_stop_name = endStopName;
                        data.end_arrival_time = endArrivalTime;
                        data.created_at = new Date();
                        _ticketGateSettingData = data;
                        _ticketGateSettingDataBackup = (TicketGateSettingsData) _ticketGateSettingData.clone();
                        insertTicketGateSettingsData(data);
                    }
                    return _ticketGateSettingData;
                })
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<List<TicketGateRoute>> changeLocation(String[] stopIds, String stopName) {
        final Resources resources = MainApplication.getInstance().getResources();

        initTripSelect();

        return Observable.fromCallable(() -> {
                    if (!_bInitRoute && _ticketGateSettingData.auto_gate_check) {
                        // 初期表示時に自動更新の場合 経路選択がないためここで初期値をセット
                        _bInitRoute = true;
                        _ticketGateRouteListBackup = new ArrayList<>(_ticketGateRouteList);
                    }
                    if (_bInit) {
                        // 選択が変わったので路線情報、便情報を取得しなおし
                        TicketGateSettingsData data = getSettings();
                        _ticketGateSettingData.stop_id = stopIds[0];
                        _ticketGateSettingData.stop_name = stopName;

                        Pair<String, String> route = getRoute(stopIds);
                        _ticketGateSettingData.route_id = route.first;
                        _ticketGateSettingData.route_name = route.second;
/*
                        if (!_bInitRoute && _autoGateCheck.getValue()) {
                            // 初期表示時に自動更新の場合 経路選択がないためここで初期値をセット
                            _bInitRoute = true;
                            _ticketGateRouteListBackup = new ArrayList<>(_ticketGateRouteList);
                        }
*/
                        TicketTrip ticketTrip = getTrip(route.first);
                        _ticketGateSettingData.trip_id = ticketTrip.trip_id;
                        _ticketGateSettingData.start_stop_name = ticketTrip.start_stop_name;
                        _ticketGateSettingData.start_departure_time = ticketTrip.start_departure_time;
                        _ticketGateSettingData.end_stop_name = ticketTrip.end_stop_name;
                        _ticketGateSettingData.end_arrival_time = ticketTrip.end_arrival_time;
                    }
                    return _ticketGateRouteList;
                })
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread());
    }


    public Pair<String, String> getRoute(String[] stopIds) throws IOException, HttpStatusException, TicketSalesStatusException {
        String routeId = null;
        String routeName = null;
        _ticketGateRouteList.clear();
        for (String item: stopIds) {
            ListTicketRoute.Response route = _ticketSalesApiClient.listTicketGateRouteEmbark(_serviceInstanceId, item);
            if (route != null && route.items != null && route.items.length > 0) {
                // routeリストに登録
                for (TicketRoute route_item : route.items) {
                    if (routeId == null) {
                        routeId = route_item.route_id;
                        routeName = route_item.route_name;
                    }
                    TicketGateRoute gateRoute = new TicketGateRoute(route_item.route_id, route_item.route_name);
                    _ticketGateRouteList.add(gateRoute);
                }
            }
        }
        return new Pair<String, String>(routeId, routeName);
    }
    public Observable<Pair<String, String>> changeRoute(String routeId, String routeName) {
        initTripSelect();
        return Observable.fromCallable(() -> {
                    Pair<String, String> trip = new Pair<>("", "");
                    if (_bInit) {
                        if (_bInitRoute) {
                            _ticketGateSettingData.route_id = routeId;
                            _ticketGateSettingData.route_name = routeName;
                            // 選択が変わったので便情報を取得しなおし
                            TicketTrip ticketTrip = getTrip(routeId);
                            _ticketGateSettingData.trip_id = ticketTrip.trip_id;
                            _ticketGateSettingData.start_stop_name = ticketTrip.start_stop_name;
                            _ticketGateSettingData.start_departure_time = ticketTrip.start_departure_time;
                            _ticketGateSettingData.end_stop_name = ticketTrip.end_stop_name;
                            _ticketGateSettingData.end_arrival_time = ticketTrip.end_arrival_time;
                        } else {
                            _bInitRoute = true; // 初期化後の１回は初期値を設定しようとするので飛ばす
                            _ticketGateRouteListBackup = new ArrayList<>(_ticketGateRouteList);
                        }

                    }
                    return trip;
                })
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public TicketTrip getTrip(String routeId) throws IOException, HttpStatusException, TicketSalesStatusException {
        TicketTrip ticketTrip = new TicketTrip();
        String tripId = "", startStopName = "", startDepartureTime = "", endStopName = "", endArrivalTime = "";

        // オフセット
        Integer offset = _offSet;
        String offsetToLatest = "false";
        if (_isFirstViewTripTimeList) {
            offset = 0;
            offsetToLatest = "true";
            _offSet = 0;
            _totalCount = 0;
        }

        // 便の一覧を取得（直近３件）
        ListTicketTrip.Response trip = _ticketSalesApiClient.listTicketGateTrip(_serviceInstanceId, AppPreference.getGTFSCurrentFeedId(), routeId, _datetime, TRIP_TIME_DISPLAY_NUM.toString(), offset.toString(), offsetToLatest);

        _tripTimeList.clear();
        _startEndNameList.clear();

        if (trip != null) {
            if (trip.offset != null) _offSet = Integer.valueOf(trip.offset);
            if (trip.total_count != null) _totalCount = Integer.valueOf(trip.total_count);

            if (trip.items != null && trip.items.length > 0) {
                // 直近を初期値としてセット
                tripId = trip.items[0].trip_id;
                startStopName = trip.items[0].start_stop_name;
                startDepartureTime = trip.items[0].start_departure_time;
                endStopName = trip.items[0].end_stop_name;
                endArrivalTime = trip.items[0].end_arrival_time;

                ticketTrip.trip_id = tripId;
                ticketTrip.start_stop_name = startStopName;
                ticketTrip.start_departure_time = startDepartureTime;
                ticketTrip.end_stop_name = endStopName;
                ticketTrip.end_arrival_time = endArrivalTime;

                String tripTime = startDepartureTime.substring(0, 2) + ":" + startDepartureTime.substring(3, 5)
                        + " ～ " + endArrivalTime.substring(0, 2) + ":" + endArrivalTime.substring(3, 5);
                final Pair<String, String> data1 = new Pair<>(tripId, tripTime);
                _tripTimeList.add(data1);
                final Pair<String, String> name1 = new Pair<>(startStopName, endStopName);
                _startEndNameList.add(name1);

                // 次の候補もあればリストに登録
                if (trip.items.length > 1) {
                    tripId = trip.items[1].trip_id;
                    startStopName = trip.items[1].start_stop_name;
                    startDepartureTime = trip.items[1].start_departure_time;
                    endStopName = trip.items[1].end_stop_name;
                    endArrivalTime = trip.items[1].end_arrival_time;

                    tripTime = startDepartureTime.substring(0, 2) + ":" + startDepartureTime.substring(3, 5)
                            + " ～ " + endArrivalTime.substring(0, 2) + ":" + endArrivalTime.substring(3, 5);

                    final Pair<String, String> data2 = new Pair<>(tripId, tripTime);
                    _tripTimeList.add(data2);
                    final Pair<String, String> name2 = new Pair<>(startStopName, endStopName);
                    _startEndNameList.add(name2);

                }
                // 次の候補もあればリストに登録
                if (trip.items.length > 2) {
                    tripId = trip.items[2].trip_id;
                    startStopName = trip.items[2].start_stop_name;
                    startDepartureTime = trip.items[2].start_departure_time;
                    endStopName = trip.items[2].end_stop_name;
                    endArrivalTime = trip.items[2].end_arrival_time;

                    tripTime = startDepartureTime.substring(0, 2) + ":" + startDepartureTime.substring(3, 5)
                            + " ～ " + endArrivalTime.substring(0, 2) + ":" + endArrivalTime.substring(3, 5);

                    final Pair<String, String> data3 = new Pair<>(tripId, tripTime);
                    _tripTimeList.add(data3);
                    final Pair<String, String> name3 = new Pair<>(startStopName, endStopName);
                    _startEndNameList.add(name3);
                }
            }
        }

        return ticketTrip;
    }

    public void updateTripTime() {
        //Timber.d("updateTripTime");
        if (_tripTimeList != null && _tripTimeList.size() > 2) {
            setTripTimeOne(_tripTimeList.get(0).second);
            setTripTimeTwo(_tripTimeList.get(1).second);
            setTripTimeThree(_tripTimeList.get(2).second);
        } else if (_tripTimeList != null && _tripTimeList.size() > 1) {
            setTripTimeOne(_tripTimeList.get(0).second);
            setTripTimeTwo(_tripTimeList.get(1).second);
            setTripTimeThree("");
        } else if (_tripTimeList != null && _tripTimeList.size() > 0) {
            setTripTimeOne(_tripTimeList.get(0).second);
            setTripTimeTwo("");
            setTripTimeThree("");
        } else {
            setTripTimeOne("");
            setTripTimeTwo("");
            setTripTimeThree("");
        }
    }

    public void arrowUp() {
        goPrevPage();
        Observable.fromCallable(() -> {
                    getTrip(_ticketGateSettingData.route_id);
                    return true;
                })
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        Timber.d("success arrowUp");
                        updateTripTime();
                        updateArrowUpEnable();
                        updateArrowDownEnable();
                    },
                    error -> {
                        Timber.e(error, "failure arrowUp");
                    }
                );
    }

    public void arrowDown() {
        goNextPage();
        Observable.fromCallable(() -> {
                    getTrip(_ticketGateSettingData.route_id);
                    return true;
                })
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            Timber.d("success arrowDown");
                            updateTripTime();
                            updateArrowUpEnable();
                            updateArrowDownEnable();
                        },
                        error -> {
                            Timber.e(error, "failure arrowDown");
                        }
                );
    }

    public void updateArrowUpEnable() {
        setArrowUpEnable(false);
        if (_offSet > 0) setArrowUpEnable(true);
        //Timber.d("updateArrowUpEnable=%s _offSet=%s", isArrowUpEnable().getValue(), _offSet);
    }

    public void updateArrowDownEnable() {
        setArrowDownEnable(false);
        if ((_totalCount-_offSet) > TRIP_TIME_DISPLAY_NUM) setArrowDownEnable(true);
        //Timber.d("updateArrowDownEnable=%s _offSet=%s _totalCount=%s", isArrowDownEnable().getValue(), _offSet ,_totalCount);
    }

    public Observable<Boolean> saveGateSettings() {
        //Timber.d("saveGateSettings");
        if (_autoGateCheck.getValue()) _ticketGateSettingData.start_departure_time = "";
        return Observable.fromCallable(() -> {
                    TicketGateSettingsData data = new TicketGateSettingsData();
                    data.stop_id = _ticketGateSettingData.stop_id;
                    data.stop_name = _ticketGateSettingData.stop_name;
                    data.stop_type = "embark";
                    data.gate_check_start_time = Integer.parseInt(_startTimes[_gateStart.getValue()]);
                    data.auto_gate_check = _autoGateCheck.getValue();
                    data.auto_gate_check_interval = data.auto_gate_check ? Integer.parseInt(_intervalTimes[_gateUpdateInterval.getValue()]) : 1; // 手動更新時は1分事に検札開始時刻チェック
                    data.route_id = _ticketGateSettingData.route_id;
                    data.route_name = _ticketGateSettingData.route_name;
                    data.trip_id = _ticketGateSettingData.trip_id;
                    data.start_stop_name = _ticketGateSettingData.start_stop_name;
                    data.start_departure_time = _ticketGateSettingData.start_departure_time;
                    data.end_stop_name = _ticketGateSettingData.end_stop_name;
                    data.end_arrival_time = _ticketGateSettingData.end_arrival_time;
                    data.created_at = new Date();
                    insertTicketGateSettingsData(data);

                    return true;
                })
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread());
    }

    // 変更してかざし待ちへボタンの活性化
    private final MutableLiveData<Boolean> _isSettingModified = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isSettingModified() {
        return _isSettingModified;
    }

    // UI更新処理
    public void updateSettingModified(Spinner location, Spinner route, Spinner gateStart, Spinner gateUpdateInterval) {
        if (_ticketGateSettingData == null) return;

        boolean bFound = false;
        if (location != null) {
            for (int i = 0; i < _ticketGateLocationList.size(); i++) {
                if (_ticketGateLocationList.get(i).stopIds[0].equals(_ticketGateSettingData.stop_id)) {
                    if (!((TicketGateLocation) location.getSelectedItem()).stopIds[0].equals(_ticketGateSettingData.route_id)) {
                        location.setSelection(i);
                    }
                    bFound = true;
                    break;
                }
            }
            if (!bFound) location.setSelection(0);
        }
        if (route != null) {
            bFound = false;
            for (int i = 0; i < _ticketGateRouteList.size(); i++) {
                if (_ticketGateRouteList.get(i).routeId.equals(_ticketGateSettingData.route_id)) {
                    if (route.getSelectedItem() != null) {
                        if (!((TicketGateRoute) route.getSelectedItem()).routeId.equals(_ticketGateSettingData.route_id)) {
                            route.setSelection(i);
                        }
                    } else {
                        route.setSelection(0);
                    }
                    bFound = true;
                    break;
                }
            }
            if (!bFound) route.setSelection(0);
        }

        bFound = false;
        if (gateStart != null) {
            for (int i = 0; i < _startTimes.length; i++) {
                if (Integer.parseInt(_startTimes[i]) == _ticketGateSettingData.gate_check_start_time) {
                    if (gateStart.getSelectedItem() != null) {
                        if (Integer.parseInt((String) gateStart.getSelectedItem()) != _ticketGateSettingData.gate_check_start_time) {
                            gateStart.setSelection(i);
                        }
                    } else {
                        gateStart.setSelection(0);
                    }
                    bFound = true;
                    break;
                }
            }
            if (!bFound) gateStart.setSelection(0);
        }

        bFound = false;
        if (gateUpdateInterval != null) {
            for (int i = 0; i < _intervalTimes.length; i++) {
                if (Integer.parseInt(_intervalTimes[i]) == _ticketGateSettingData.auto_gate_check_interval) {
                    if (gateUpdateInterval.getSelectedItem() != null) {
                        if (Integer.parseInt((String) gateUpdateInterval.getSelectedItem()) != _ticketGateSettingData.auto_gate_check_interval) {
                            gateUpdateInterval.setSelection(i);
                        }
                    } else {
                        gateUpdateInterval.setSelection(0);
                    }
                    bFound = true;
                    break;
                }
            }
            if (!bFound) gateStart.setSelection(0);
        }

        setAutoGateCheck(_ticketGateSettingData.auto_gate_check);

        _bInit = true;
//        setGateStart(_ticketGateSettingData.gate_check_start_time);
//        setGateUpdateInterval(_ticketGateSettingData.auto_gate_check_interval);

        updateTripTime();
        //TripOneSelect();
        updateArrowUpEnable();
        updateArrowDownEnable();

        Boolean ret = _ticketGateSettingData.equals(_ticketGateSettingDataBackup) || (getAutoGateCheck().getValue() == false && _tripTimeList.size() == 0);
        _isSettingModified.setValue(!ret);
    }

    public List<TicketGateRoute> getRouteIds() {
        return _ticketGateRouteList;
    }

    public List<TicketGateRoute> getRouteIdsBackup() {
        return _ticketGateRouteListBackup;
    }
    // 時刻表示ページング制御
    private boolean _isFirstViewTripTimeList = true;
    private Integer _offSet = 0;
    private Integer _totalCount = 0;

    private MutableLiveData<Boolean> _isArrowUpEnable = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isArrowUpEnable() { return _isArrowUpEnable; }
    public void setArrowUpEnable(Boolean enable) { _isArrowUpEnable.setValue(enable); }
    private MutableLiveData<Boolean> _isArrowDownEnable = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isArrowDownEnable() { return _isArrowDownEnable; }
    public void setArrowDownEnable(Boolean enable) { _isArrowDownEnable.setValue(enable); }

    public void goPrevPage() {
        //Timber.d("goPrevPage() _totalCount:%s _offSet:%s",_totalCount,_offSet);
        _isFirstViewTripTimeList = false;
        if (_offSet < TRIP_TIME_DISPLAY_NUM) {
            _offSet = 0;
        } else {
            _offSet -= TRIP_TIME_DISPLAY_NUM;
        }
    }
    public void goNextPage() {
        //Timber.d("goNextPage() _totalCount:%s _offSet:%s",_totalCount,_offSet);
        _isFirstViewTripTimeList = false;
        if (_totalCount-_offSet < TRIP_TIME_DISPLAY_NUM) {
            _offSet = _totalCount-2;
        } else {
            _offSet += TRIP_TIME_DISPLAY_NUM;
        }
    }

    public void changeTripTimeList() {
        //Timber.d("changeTripTimeList");
        _isFirstViewTripTimeList = true;
    }
    public void initTripSelect() {
        //Timber.d("initTripSelect");
        _isTripOneSelected.setValue(false);
        _isTripTwoSelected.setValue(false);
        _isTripThreeSelected.setValue(false);
    }

    // 検札開始位置
    private final MutableLiveData<Integer> _gateStart = new MutableLiveData<>(DEFAULT_GATE_CHECK_START_TIME_POS);
    public MutableLiveData<Integer> getGateStart(){ return _gateStart; }
    public void setGateStart(Integer i) {
        _gateStart.setValue(i);
        if (_ticketGateSettingData != null) _ticketGateSettingData.gate_check_start_time = Integer.parseInt(_startTimes[i]);
    }

    // 便自動更新
    private final MutableLiveData<Boolean> _autoGateCheck = new MutableLiveData<>(true);
    public MutableLiveData<Boolean> getAutoGateCheck(){ return _autoGateCheck; }
    public void setAutoGateCheck(Boolean b) {
        _autoGateCheck.setValue(b);
        if (_ticketGateSettingData != null) _ticketGateSettingData.auto_gate_check = b;
    }

    // 便更新間隔
    private final MutableLiveData<Integer> _gateUpdateInterval = new MutableLiveData<>(DEFAULT_GATE_CHECK_INTERVAL_POS);
    public MutableLiveData<Integer> getGateUpdateInterval(){ return _gateUpdateInterval; }
    public void setGateUpdateInterval(Integer i) {
        _gateUpdateInterval.setValue(i);
        if (_ticketGateSettingData != null) _ticketGateSettingData.auto_gate_check_interval = Integer.parseInt(_intervalTimes[i]);
    }

    // 設定データ更新
    public void updateSettings(TicketGateSettingsData data) { _ticketGateSettingData = data; }
    public TicketGateSettingsData getSettings() { return _ticketGateSettingData; }

    // ロケーション情報取得
    public String[] getLocationStopIds(int position) { return _ticketGateLocationList.get(position).stopIds; }

    // 便表示
    private MutableLiveData<String> _tripTimeOne = new MutableLiveData<>("");
    private void setTripTimeOne(String data) { _tripTimeOne.setValue(data); }
    public MutableLiveData<String> getTripTimeOne() { return _tripTimeOne; }
    private MutableLiveData<String> _tripTimeTwo = new MutableLiveData<>("");
    private void setTripTimeTwo(String data) { _tripTimeTwo.setValue(data); }
    public MutableLiveData<String> getTripTimeTwo() { return _tripTimeTwo; }
    private MutableLiveData<String> _tripTimeThree = new MutableLiveData<>("");
    private void setTripTimeThree(String data) { _tripTimeThree.setValue(data); }
    public MutableLiveData<String> getTripTimeThree() { return _tripTimeThree; }

    // 便選択位置
    private MutableLiveData<Boolean> _isTripOneSelected = new MutableLiveData<>(true);
    public void TripOneSelect() {
        _isTripTwoSelected.setValue(false);
        _isTripThreeSelected.setValue(false);
        if (_tripTimeList.size() > 0) {
            _isTripOneSelected.setValue(true);
            _ticketGateSettingData.trip_id = _tripTimeList.get(0).first;
            _ticketGateSettingData.start_stop_name = _startEndNameList.get(0).first;
            _ticketGateSettingData.start_departure_time = _tripTimeList.get(0).second.split(" ～ ")[0] + ":00";    // 時分になっているので秒を追加
            _ticketGateSettingData.end_stop_name = _startEndNameList.get(0).second;
            _ticketGateSettingData.end_arrival_time = _tripTimeList.get(0).second.split(" ～ ")[1]+ ":00";    // 時分になっているので秒を追加
        } else {
            _isTripOneSelected.setValue(false);
        }

        if (!_ticketGateSettingData.auto_gate_check) { _isSettingModified.setValue(true); }
    }
    public MutableLiveData<Boolean> isTripOneSelected() { return _isTripOneSelected; }
    public String getTripOneTimeInfo() {
        String tripTimeInfo = "";
        if (_tripTimeList.size() > 0) tripTimeInfo = _tripTimeList.get(0).second;
        return tripTimeInfo;
    }

    private MutableLiveData<Boolean> _isTripTwoSelected = new MutableLiveData<>(false);
    public void TripTwoSelect() {
        if (_tripTimeList.size() > 1) {
            _isTripOneSelected.setValue(false);
            _isTripTwoSelected.setValue(true);
            _isTripThreeSelected.setValue(false);
            _ticketGateSettingData.trip_id = _tripTimeList.get(1).first;
            _ticketGateSettingData.start_stop_name = _startEndNameList.get(1).first;
            _ticketGateSettingData.start_departure_time = _tripTimeList.get(1).second.split(" ～ ")[0] + ":00";    // 時分になっているので秒を追加
            _ticketGateSettingData.end_stop_name = _startEndNameList.get(1).second;
            _ticketGateSettingData.end_arrival_time = _tripTimeList.get(1).second.split(" ～ ")[1] + ":00";    // 時分になっているので秒を追加;

            if (!_ticketGateSettingData.auto_gate_check) { _isSettingModified.setValue(true); }
        }
    }
    public MutableLiveData<Boolean> isTripTwoSelected() { return _isTripTwoSelected; }
    public String getTripTwoTimeInfo() {
        String tripTimeInfo = "";
        if (_tripTimeList.size() > 1) tripTimeInfo = _tripTimeList.get(1).second;
        return tripTimeInfo;
    }

    private MutableLiveData<Boolean> _isTripThreeSelected = new MutableLiveData<>(false);
    public void TripThreeSelect() {
        if (_tripTimeList.size() > 2) {
            _isTripOneSelected.setValue(false);
            _isTripTwoSelected.setValue(false);
            _isTripThreeSelected.setValue(true);
            _ticketGateSettingData.trip_id = _tripTimeList.get(2).first;
            _ticketGateSettingData.start_stop_name = _startEndNameList.get(2).first;
            _ticketGateSettingData.start_departure_time = _tripTimeList.get(2).second.split(" ～ ")[0] + ":00";    // 時分になっているので秒を追加;
            _ticketGateSettingData.end_stop_name = _startEndNameList.get(2).second;
            _ticketGateSettingData.end_arrival_time = _tripTimeList.get(2).second.split(" ～ ")[1] + ":00";    // 時分になっているので秒を追加;

            if (!_ticketGateSettingData.auto_gate_check) { _isSettingModified.setValue(true); }
        }
    }
    public MutableLiveData<Boolean> isTripThreeSelected() { return _isTripThreeSelected; }

    public String getTripThreeTimeInfo() {
        String tripTimeInfo = "";
        if (_tripTimeList.size() > 2) tripTimeInfo = _tripTimeList.get(2).second;
        return tripTimeInfo;
    }

    public void gateStartItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (_bInit) {
            String item = (String)parent.getItemAtPosition(position);
            CommonClickEvent.RecordClickOperation(item, "検札開始(分)", true);
            setGateStart(position);
            updateSettingModified(null, null, null, null);
        }
    }

    public void gateUpdateIntervalItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (_bInit) {
            String item = (String)parent.getItemAtPosition(position);
            CommonClickEvent.RecordClickOperation(item, "便更新間隔(分)", true);
            setGateUpdateInterval(position);
            updateSettingModified(null, null, null, null);
        }
    }

    public void autoGateCheckCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (_bInit) {
            String info = "OFF";
            if (b) info = "ON";
            CommonClickEvent.RecordClickOperation(info, "便自動更新", true);
            setAutoGateCheck(b);
            updateSettingModified(null, null, null, null);
        }
    }

    // 画面初期化結果
    private final MutableLiveData<Boolean> _isInitResult = new MutableLiveData<>(true);
    public MutableLiveData<Boolean> isInitResult() { return _isInitResult; }
    public void isInitResult(boolean b) {
        _isInitResult.setValue(b);

        if (!b) {
            setErrorMessage("データ取得エラー");
            setErrorMessageInformation("業務メニューのチケットデータ更新を行ってください");
        }
    }

    // エラーコード
    private final MutableLiveData<String> _errorCode = new MutableLiveData<String>("");
    public MutableLiveData<String> getErrorCode() { return _errorCode; }
    public void setErrorCode(String msg) {
        _errorCode.setValue("コード：" + msg);
    }

    // エラーメッセージ
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<String>("");
    public MutableLiveData<String> getErrorMessage() { return _errorMessage; }
    public void setErrorMessage(String msg) {
        _errorMessage.setValue(msg);
    }

    // エラーメッセージ(補足)
    private final MutableLiveData<String> _errorMessageInformation = new MutableLiveData<String>("");
    public MutableLiveData<String> getErrorMessageInformation() { return _errorMessageInformation; }
    public void setErrorMessageInformation(String msg) {
        _errorMessageInformation.setValue(msg);
    }
}
