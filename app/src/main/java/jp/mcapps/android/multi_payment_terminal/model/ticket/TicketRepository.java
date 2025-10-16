package jp.mcapps.android.multi_payment_terminal.model.ticket;

import android.util.Log;
import android.util.Pair;

import androidx.core.util.Consumer;

import com.google.common.base.Strings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.pos.ServiceFunctionDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.ServiceFunctionData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TaxRates;
import jp.mcapps.android.multi_payment_terminal.database.pos.TenantDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TenantData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketClassDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketClassData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketClassNameLangDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketClassNameLangData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketEmbarkDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketEmbarkData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketEmbarkNameLangDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketEmbarkNameLangData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateEmbarkDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateEmbarkData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGtfsDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGtfsData;
import jp.mcapps.android.multi_payment_terminal.error.DomainErrors;
import jp.mcapps.android.multi_payment_terminal.error.McPosCenterErrorMap;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.gtfs.data.ListGTFSFeeds;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.McPosCenterApi;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.McPosCenterApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.TerminalInfo;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.AuthTest;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketClass;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketDisembark;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketEmbark;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketGateEmbark;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.ListTicketRoute;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.Tenant;

import jp.mcapps.android.multi_payment_terminal.database.ticket.GenerationIDs;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApi;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketClass;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketClassNameI18n;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketDisembark;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketEmbark;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketEmbarkNameI18n;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketGateEmbark;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketRoute;
import timber.log.Timber;

public class TicketRepository {

    public TicketRepository() {
        _terminalDao = LocalDatabase.getInstance().terminalDao();
        _tenantDao = LocalDatabase.getInstance().tenantDao();
        _serviceFunctionDao = LocalDatabase.getInstance().serviceFunctionDao();
        _ticketClassDao = LocalDatabase.getInstance().ticketClassDao();
        _ticketClassNameLangDao = LocalDatabase.getInstance().ticketClassNameLangDao();
        _ticketEmbarkDao = LocalDatabase.getInstance().ticketEmbarkDao();
        _ticketEmbarkNameLangDao = LocalDatabase.getInstance().ticketEmbarkNameLangDao();
        _ticketGateEmbarkDao = LocalDatabase.getInstance().ticketGateEmbarkDao();
        _ticketGateSettingsDao = LocalDatabase.getInstance().ticketGateSettingsDao();
        _ticketGtfsDao = LocalDatabase.getInstance().ticketGtfsDao();

    }

    // network
    private final McPosCenterApi _apiClient = new McPosCenterApiImpl();
    private final TicketSalesApi _ticketSalesApiClient = TicketSalesApiImpl.getInstance();

    // dao
    private final TerminalDao _terminalDao;
    private final TenantDao _tenantDao;
    private final ServiceFunctionDao _serviceFunctionDao;
    private final TicketClassDao _ticketClassDao;
    private final TicketClassNameLangDao _ticketClassNameLangDao;
    private final TicketEmbarkDao _ticketEmbarkDao;
    private final TicketEmbarkNameLangDao _ticketEmbarkNameLangDao;
    private final TicketGateEmbarkDao _ticketGateEmbarkDao;
    private final TicketGateSettingsDao _ticketGateSettingsDao;
    private final TicketGtfsDao _ticketGtfsDao;

    private final int DEFAULT_FETCH_SIZE = 1000;

    private final String TICKET_EMBARK = "embark";
    private final String TICKET_DISEMBARK = "disembark";

    // チケット販売マスタを更新する
    public boolean refreshTicketSales() throws IOException, DomainErrors.Exception, TicketSalesStatusException {
        boolean result = false;
        String gtfsVersion = null;
        LocalDatabase db = LocalDatabase.getInstance();
        Timber.i("チケットデータ更新開始");

        // 残骸が残っていると嫌なので、ダウンロード済みレコードをクリアする
        db.runInTransaction(() -> doCleanup(GenerationIDs.DOWNLOADING.value));

        // 端末情報を取得する
        final TerminalData terminal = fetchTerminal();
        saveTerminal(terminal);

        // ABTサービスインスタンスIDが必要
        final String serviceInstanceID = terminal.service_instance_abt;
        if (Strings.isNullOrEmpty(serviceInstanceID)) {
            DomainErrors.POS_SERVICE_INSTANCE_IS_NOT_ASSIGNED.raise("サービスインスタンスIDが空白です. 端末をPOSサービスに割り当ててください.");
        }

        // GTFSの最新のversion取得
        gtfsVersion = getGTFSFeeds(serviceInstanceID);
        if (gtfsVersion == null) {
            AppPreference.isTicketDataInit(true);
            Timber.i("チケットデータ更新結果：更新不要");
            return result;    // 更新不要
        }

        // 改札設定を最新１件のみにする
        refreshTicketGateSettingsData();

//            if (!AppPreference.isServicePos()) { //POS機能有効時はそちらで更新するため不要
//                // POSサービス機能を取得する (PCI環境)
//                final ServiceFunctionData serviceFunction = fetchServiceFunction();
//                saveServiceFunction(serviceFunction);

//                // 店舗情報を取得する
//                final TenantData tenant = fetchTenant(serviceInstanceID, serviceFunction.customer_code);
//                saveTenant(tenant);
//                AppPreference.posTenantSave(tenant);
//            }

        // チケット分類一覧を取得する
        List<TicketClassData> list = fetchTicketClasses(serviceInstanceID, this::saveTicketClasses);

        // のりば・おりば一覧を取得する（チケット発売用）
        fetchTicketEmbark(serviceInstanceID, list, this::saveTicketEmbark);

        // 画面描画時にセンターに取得にいくためコメントアウト
        //        // のりば・おりば一覧を取得する（改札用）
        //        fetchTicketGateEmbark(serviceInstanceID, this::saveTicketGateEmbark);

        // ダウンロードしたコンテンツを有効化する
        db.runInTransaction(() -> {
            // 現在アクティブなレコードを全て削除
            doCleanup(GenerationIDs.CURRENTLY_ACTIVE.value);
            // ここでダウンロードしたレコードをアクティブにする
            doActivate();
        });

        // GTFSの最新のversion更新
        updateGTFSFeeds(gtfsVersion);

        AppPreference.isTicketDataInit(true);
        Timber.i("チケットデータ更新結果：更新成功");
        return true;
    }

    // 端末情報を取得する
    TerminalData fetchTerminal() throws IOException {
        // call api
        final AuthTest.Response resp = _ticketSalesApiClient.authTest();

        // 変換
        TerminalData item = new TerminalData();
        item.terminal_id = resp.sub;
        item.terminal_no = resp.terminal_no;
        item.customer_id = resp.customer_id;
        item.service_instance_abt = resp.service_instance_abt;
        item.service_instance_pos = resp.service_instance_pos;
        item.created_at = new Date();

        return item;
    }

    // 端末情報を保存する
    void saveTerminal(TerminalData item) {
        item.generation_id = GenerationIDs.DOWNLOADING.value;
        _terminalDao.insertTerminals(item);
    }

    // 店舗情報を取得する
    TenantData fetchTenant(String serviceInstanceID, String customerCode) throws IOException, TicketSalesStatusException {
        // call api
        final Tenant resp = _ticketSalesApiClient.getTenantByCustomerCode(serviceInstanceID, customerCode);

        // 変換
        TenantData item = new TenantData();
        item.service_instance_id = serviceInstanceID;
        item.tenant_id = resp.id;
        item.tenant_code = resp.tenant_code;
        item.merchant_id = resp.merchant_id;
        item.customer_code = resp.customer_code;
        item.name = resp.name;
        item.name_kana = resp.name_kana;
        item.zipcode = resp.zipcode;
        item.pref_cd = resp.pref_cd;
        item.city = resp.city;
        item.address_line1 = resp.address_line1;
        item.address_line2 = resp.address_line2;
        item.address_line3 = resp.address_line3;
        item.kana_city = resp.kana_city;
        item.address_kana_line1 = resp.address_kana_line1;
        item.address_kana_line2 = resp.address_kana_line2;
        item.address_kana_line3 = resp.address_kana_line3;
        item.phone_number = resp.phone_number;
        item.fax = resp.fax;
        item.houjin_bangou = resp.houjin_bangou;
        item.alphabet_name = resp.alphabet_name;
        if(resp.parentInfo != null) {
            // 親情報がある場合はセット
            item.parent_name = resp.parentInfo.name;
        }
        item.created_at = new Date();

        return item;
    }

    // 店舗情報を保存する
    void saveTenant(TenantData item) {
        item.generation_id = GenerationIDs.DOWNLOADING.value;
        _tenantDao.insertTenants(item);
    }

    List<TicketClassData> fetchTicketClasses(String serviceInstanceID, Consumer<TicketClassData[]> thunk) throws IOException, HttpStatusException, TicketSalesStatusException {
        boolean continued = true;
        List<TicketClassData> list = new ArrayList<>();
        for (int offset = 0; continued; offset += DEFAULT_FETCH_SIZE) {
            final Pair<TicketClassData[], Boolean> resp = fetchTicketClasses(serviceInstanceID, offset);
            thunk.accept(resp.first);
            Collections.addAll(list, resp.first);
            continued = resp.second;
        }
        return list;
    }

    // チケット分類を取得する
    Pair<TicketClassData[], Boolean> fetchTicketClasses(String serviceInstanceID, int offset) throws IOException, HttpStatusException, TicketSalesStatusException {
        // call api
        final ListTicketClass.Response data = _ticketSalesApiClient.listTicketClass(serviceInstanceID, DEFAULT_FETCH_SIZE, offset);

        // 変換
        final List<TicketClassData> list = new ArrayList<>();
        final List<TicketClassNameLangData> nameLangList = new ArrayList<>();
        for (TicketClass it: data.items) {
            TicketClassData item = new TicketClassData();

            item.ticket_class_id = Long.parseLong(it.id);
            item.ticket_class_name = it.name;
            item.service_instance_id = serviceInstanceID;
            item.reserve_type = it.reserve_type;
            item.enable_route_judge = it.enable_route_judge;
            item.enable_trip_judge = it.enable_trip_judge;
            item.enable_stop_judge = it.enable_stop_judge;
            item.ticket_class_created_at = it.created_at;
            item.ticket_class_created_auth_server_id = Long.parseLong(it.created_user.auth_server_id);
            item.ticket_class_created_user_id = it.created_user.user_id;
            item.ticket_class_created_user_name = it.created_user.user_name;
            item.ticket_class_updated_at = it.updated_at;
            item.ticket_class_updated_auth_server_id = Long.parseLong(it.updated_user.auth_server_id);
            item.ticket_class_updated_user_id = it.updated_user.user_id;
            item.ticket_class_updated_user_name = it.updated_user.user_name;
            item.created_at = new Date();

            list.add(item);

            for (TicketClassNameI18n i18n: it.name_i18n) {
                TicketClassNameLangData i18nItem = new TicketClassNameLangData();

                i18nItem.ticket_class_id = item.ticket_class_id;
                i18nItem.lang = i18n.lang;
                i18nItem.name = i18n.name;
                i18nItem.created_at = new Date();

                nameLangList.add(i18nItem);
            }
            if(nameLangList.size() > 0) {
                saveTicketClassNameLang(nameLangList.toArray(new TicketClassNameLangData[0]));
            }
        }

        TicketClassData[] items = list.toArray(new TicketClassData[0]);
        boolean hasNext = (offset + items.length) < data.total_count;

        return new Pair<>(items, hasNext);
    }

    // チケット分類を保存する
    void saveTicketClasses(TicketClassData[] items) {
        for (TicketClassData it: items) {
            it.generation_id = GenerationIDs.DOWNLOADING.value;
        }
        _ticketClassDao.insertTicketClassesData(Arrays.asList(items));
    }

    // チケット分類名称言語情報を保存する
    void saveTicketClassNameLang(TicketClassNameLangData[] items) {
        for (TicketClassNameLangData it: items) {
            it.generation_id = GenerationIDs.DOWNLOADING.value;
        }
        _ticketClassNameLangDao.insertTicketClassesNameLangData(Arrays.asList(items));
    }

    void fetchTicketEmbark(String serviceInstanceID, List<TicketClassData> list, Consumer<TicketEmbarkData[]> thunk) throws IOException, HttpStatusException, TicketSalesStatusException {
        boolean continued = true;
        for (TicketClassData it: list) {
            continued = true;
            // のりば取得
            for (int offset = 0; continued; offset += DEFAULT_FETCH_SIZE) {
                final Pair<TicketEmbarkData[], Boolean> resp = fetchTicketEmbark(serviceInstanceID, it.ticket_class_id, offset);
                thunk.accept(resp.first);
                continued = resp.second;
            }
            continued = true;
            // おりば取得
            for (int offset = 0; continued; offset += DEFAULT_FETCH_SIZE) {
                final Pair<TicketEmbarkData[], Boolean> resp = fetchTicketDisembark(serviceInstanceID, it.ticket_class_id, offset);
                thunk.accept(resp.first);
                continued = resp.second;
            }
        }
    }

    // のりば（チケット発売用）を取得する
    Pair<TicketEmbarkData[], Boolean> fetchTicketEmbark(String serviceInstanceID, long ticketClassId, int offset) throws IOException, HttpStatusException, TicketSalesStatusException {
        final ListTicketEmbark.Response data;

        // get ticket embark or disembark
        // call api
        data = _ticketSalesApiClient.listTicketEmbark(serviceInstanceID, ticketClassId, DEFAULT_FETCH_SIZE, offset);

        // 変換
        final List<TicketEmbarkData> list = new ArrayList<>();
        final List<TicketEmbarkNameLangData> nameLangList = new ArrayList<>();
        int cnt = 0;
        TicketEmbarkData item = null;
        for (TicketEmbark it: data.items) {
            ListTicketRoute.Response route_data = null;
            int count = 0;

            while(true) {
                try {
                    route_data = _ticketSalesApiClient.listTicketRouteEmbark(serviceInstanceID, it.origin_stop_id);
                    break;
                } catch (Exception e) {
                    count++;
                    Timber.d("Retry GetTicketRouteEmbark：%d", count);
                    if (count > 3) break;
                }
            }

            if (route_data != null) {
                for (TicketRoute route : route_data.items) {
                    item = new TicketEmbarkData();
                    item.ticket_class_id = ticketClassId;
                    item.route_id = route.route_id;
                    item.route_name = route.route_name;
                    item.stop_id = it.origin_stop_id;
                    item.stop_name = it.origin_stop_name;
                    item.stop_type = TICKET_EMBARK;
                    item.created_at = new Date();

                    list.add(item);
                }

                for (TicketEmbarkNameI18n i18n : it.name_i18n) {
                    TicketEmbarkNameLangData i18nItem = new TicketEmbarkNameLangData();

                    i18nItem.stop_id = item.stop_id;
                    i18nItem.lang = i18n.lang;
                    i18nItem.name = i18n.name;
                    i18nItem.created_at = new Date();

                    nameLangList.add(i18nItem);
                }
                if (nameLangList.size() > 0) {
                    saveTicketEmbarkNameLang(nameLangList.toArray(new TicketEmbarkNameLangData[0]));
                }
            }
        }

        TicketEmbarkData[] items = list.toArray(new TicketEmbarkData[0]);
        boolean hasNext = (offset + items.length) < data.total_count;

        return new Pair<>(items, hasNext);
    }

    // おりば（チケット発売用）を取得する
    Pair<TicketEmbarkData[], Boolean> fetchTicketDisembark(String serviceInstanceID, long ticketClassId, int offset) throws IOException, HttpStatusException, TicketSalesStatusException {
        final ListTicketDisembark.Response data;
        // get ticket embark or disembark
        // call api
        data = _ticketSalesApiClient.listTicketDisembark(serviceInstanceID, ticketClassId, DEFAULT_FETCH_SIZE, offset);

        // 変換
        final List<TicketEmbarkData> list = new ArrayList<>();
        final List<TicketEmbarkNameLangData> nameLangList = new ArrayList<>();
        TicketEmbarkData item = null;
        for (TicketDisembark it: data.items) {
            ListTicketRoute.Response route_data = null;
            int count = 0;

            while(true) {
                try {
                    route_data = _ticketSalesApiClient.listTicketRouteDisembark(serviceInstanceID, it.destination_stop_id);
                    break;
                } catch (Exception e) {
                    count++;
                    Timber.d("Retry GetTicketRouteDisembark：%d", count);
                    if (count > 3) break;
                }
            }

            if (route_data != null) {
                for (TicketRoute route : route_data.items) {
                    item = new TicketEmbarkData();
                    item.ticket_class_id = ticketClassId;
                    item.route_id = route.route_id;
                    item.route_name = route.route_name;
                    item.stop_id = it.destination_stop_id;
                    item.stop_name = it.destination_stop_name;
                    item.stop_type = TICKET_DISEMBARK;
                    item.created_at = new Date();

                    list.add(item);
                }

                for (TicketEmbarkNameI18n i18n : it.name_i18n) {
                    TicketEmbarkNameLangData i18nItem = new TicketEmbarkNameLangData();

                    i18nItem.stop_id = item.stop_id;
                    i18nItem.lang = i18n.lang;
                    i18nItem.name = i18n.name;
                    i18nItem.created_at = new Date();

                    nameLangList.add(i18nItem);
                }
                if (nameLangList.size() > 0) {
                    saveTicketEmbarkNameLang(nameLangList.toArray(new TicketEmbarkNameLangData[0]));
                }
            }
        }

        TicketEmbarkData[] items = list.toArray(new TicketEmbarkData[0]);
        boolean hasNext = (offset + items.length) < data.total_count;

        return new Pair<>(items, hasNext);
    }

    // のりば・おりば（チケット発売用）を保存する
    void saveTicketEmbark(TicketEmbarkData[] items) {
        for (TicketEmbarkData it: items) {
            it.generation_id = GenerationIDs.DOWNLOADING.value;
        }
        _ticketEmbarkDao.insertTicketEmbarkData(Arrays.asList(items));
    }

    // のりば・おりば（チケット発売用）名称言語情報を保存する
    void saveTicketEmbarkNameLang(TicketEmbarkNameLangData[] items) {
        for (TicketEmbarkNameLangData it: items) {
            it.generation_id = GenerationIDs.DOWNLOADING.value;
        }
        _ticketEmbarkNameLangDao.insertTicketEmbarkNameLangData(Arrays.asList(items));
    }

    void fetchTicketGateEmbark(String serviceInstanceID, Consumer<TicketGateEmbarkData[]> thunk) throws IOException, HttpStatusException, TicketSalesStatusException {
        // のりば取得
        final TicketGateEmbarkData[] resp = fetchTicketGateEmbark(serviceInstanceID);
        thunk.accept(resp);
    }

    // のりば・おりば（改札用）を取得する
    TicketGateEmbarkData[] fetchTicketGateEmbark(String serviceInstanceID) throws IOException, HttpStatusException, TicketSalesStatusException {
        final ListTicketGateEmbark.Response data;
        final ListTicketGateEmbark.Response disembark_data;

        // のりば
        // get ticket embark or disembark
        // call api
        data = _ticketSalesApiClient.listTicketGateEmbark(serviceInstanceID);

        // 変換
        final List<TicketGateEmbarkData> list = new ArrayList<>();
        for (TicketGateEmbark it: data.items) {
            for (String stop_id: it.stop_ids) {
                TicketGateEmbarkData item = new TicketGateEmbarkData();
                item.stop_name = it.stop_name;
                item.stop_id = stop_id;
                item.stop_type = TICKET_EMBARK;
                item.created_at = new Date();
                list.add(item);
            }
        }

        // おりば
        // get ticket embark or disembark
        // call api
        disembark_data = _ticketSalesApiClient.listTicketGateDisembark(serviceInstanceID);

        // 変換
        for (TicketGateEmbark it: disembark_data.items) {
            for (String stop_id: it.stop_ids) {
                TicketGateEmbarkData item = new TicketGateEmbarkData();
                item.stop_name = it.stop_name;
                item.stop_id = stop_id;
                item.stop_type = TICKET_DISEMBARK;
                item.created_at = new Date();
                list.add(item);
            }
        }

        TicketGateEmbarkData[] items = list.toArray(new TicketGateEmbarkData[0]);
        return items;
    }

    // のりば・おりば（改札用）を保存する
    void saveTicketGateEmbark(TicketGateEmbarkData[] items) {
        for (TicketGateEmbarkData it: items) {
            it.generation_id = GenerationIDs.DOWNLOADING.value;
        }
        _ticketGateEmbarkDao.insertTicketGateEmbarkData(Arrays.asList(items));
    }

    // GTFSバージョンを取得
    String getGTFSFeeds(String serviceInstanceID) throws IOException, TicketSalesStatusException {
        String gtfsVersion = null;
        ListGTFSFeeds.Response data = null;
        TicketGtfsData ticketGtfsData = null;

        try {
            // 保持しているGTFSバージョンをAppPreferenceにセット
            ticketGtfsData = _ticketGtfsDao.getLatestOne();
            if (ticketGtfsData != null) AppPreference.setGTFSCurrentFeedId(ticketGtfsData.gtfs_version);

            // GTFS 最新Feed取得
            // call api
            data = _ticketSalesApiClient.listGTFSLatestFeedInfo(serviceInstanceID);
        } catch (Exception e) {
            // GTFS取得できなかった場合はデータ取得エラーになるためここでは処理不要
            Timber.e(e);
        }

        // 最新GTFSバージョン取得
        if(data != null && data.items != null) {
            gtfsVersion = data.items[0].id;
        }
        return gtfsVersion;
    }

    // GTFSバージョンを更新
    void updateGTFSFeeds(String gtfsVersion) throws IOException, TicketSalesStatusException {

        // DBおよびAppPreferenceにセット
        TicketGtfsData ticketGtfsData = _ticketGtfsDao.getLatestOne();
        if (ticketGtfsData != null) {
            _ticketGtfsDao.deleteAll();
        } else {
            ticketGtfsData = new TicketGtfsData();
        }
        ticketGtfsData.gtfs_version = gtfsVersion;
        ticketGtfsData.created_at = new Date();

        _ticketGtfsDao.insertGtfs(ticketGtfsData);
        AppPreference.setGTFSCurrentFeedId(gtfsVersion);
    }

    // 改札設定を最新１件のみにする
    void refreshTicketGateSettingsData() {
        final TicketGateSettingsData data;

        data = _ticketGateSettingsDao.getTicketGateSettingsLatest();
        if (data == null) return;
        _ticketGateSettingsDao.deleteAll();
        _ticketGateSettingsDao.insertTicketGateSettingsData(data);
    }

    // POSサービス機能を取得する
    ServiceFunctionData fetchServiceFunction() throws IOException, DomainErrors.Exception, TicketSalesStatusException {

        // POST /Term/GetInfo
        final TerminalInfo.Response resp = _apiClient.getTerminalInfo();

        if (resp.result) {
            // OK
            final TerminalInfo.PosServiceFunc posServiceFunc = resp.posServiceFunc;
            if (posServiceFunc == null) {
                DomainErrors.FAILED_PRECONDITION.raise("POSサービス機能がNULL"); // POSの設定がない？？
            }

            // 受信データを詰め込む
            final ServiceFunctionData item = new ServiceFunctionData();
            item.customer_code = resp.supplierCd;
            item.is_product_category = posServiceFunc.isProductCategory;
            item.is_pos_receipt = posServiceFunc.isPosReceipt;
            item.is_manual_amount = posServiceFunc.isManualAmount;
            item.slip_title = posServiceFunc.slipTitle;
            item.tax_rounding = posServiceFunc.taxRounding;
            for (TerminalInfo.TaxRate it: posServiceFunc.taxList) {
                Log.d("TAX----" , String.valueOf(it.tax));
                if (it.taxClass == TaxRates.STANDARD_TAX_RATE.value) {
                    item.standard_tax_rate = String.valueOf(it.tax);
                    Log.d("TAXIN----" , String.valueOf(item.standard_tax_rate));
                }
                if (it.taxClass == TaxRates.REDUCED_TAX_RATE.value) {
                    item.reduced_tax_rate = String.valueOf(it.tax);
                    Log.d("TAXIN----" , String.valueOf(item.reduced_tax_rate));
                }
            }
            item.receipt_count = posServiceFunc.receiptCounts;

            return item;
        }

        // ERROR
        throw new TicketSalesStatusException(DomainErrors.INTERNAL.code, "error code: " + resp.errorCode);
    }

    // POSサービス機能を保存する
    void saveServiceFunction(ServiceFunctionData item) {
        item.generation_id = GenerationIDs.DOWNLOADING.value;
        Log.d("TAX INCLU----", String.valueOf(item.reduced_tax_rate));
        Log.d("TAX INCLU----", String.valueOf(item.standard_tax_rate));
        _serviceFunctionDao.insertServiceFunctions(item);
    }

    // 対象の世代のレコードを削除する
    void doCleanup(int generationID) {
        _terminalDao.deleteTerminalsByGenerationId(generationID);
        _tenantDao.deleteTenantsByGenerationId(generationID);
        _ticketClassDao.deleteTicketClassesByGenerationId(generationID);
        _ticketClassNameLangDao.deleteTicketClassesNameLangByGenerationId(generationID);
        _ticketEmbarkDao.deleteTicketEmbarkByGenerationId(generationID);
        _ticketEmbarkNameLangDao.deleteTicketEmbarkNameLangByGenerationId(generationID);
        _ticketGateEmbarkDao.deleteTicketGateEmbarkByGenerationId(generationID);
    }

    // ダウンロードしたレコードを有効にする
    void doActivate() {
        int srcID = GenerationIDs.DOWNLOADING.value;
        int dstID = GenerationIDs.CURRENTLY_ACTIVE.value;
        _terminalDao.swapTerminalsGenerationId(srcID, dstID);
        _tenantDao.swapTenantsGenerationId(srcID, dstID);
        _ticketClassDao.swapTicketClassesGenerationId(srcID, dstID);
        _ticketClassNameLangDao.swapTicketClassesNameLangGenerationId(srcID, dstID);
        _ticketEmbarkDao.swapTicketEmbarkGenerationId(srcID, dstID);
        _ticketEmbarkNameLangDao.swapTicketEmbarkNameLangGenerationId(srcID, dstID);
        _ticketGateEmbarkDao.swapTicketGateEmbarkGenerationId(srcID, dstID);
    }
}
