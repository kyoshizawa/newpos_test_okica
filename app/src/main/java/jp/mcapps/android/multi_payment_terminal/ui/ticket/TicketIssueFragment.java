package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.pos.CardBrand;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentTicketIssueBinding;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentTicketSearchResultsBinding;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApi;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.DynamicTicket;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.TicketPurchasedHistory;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.DynamicTicketItem;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.PurchasedTicketDetails;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.ReserveSlots;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketPeople;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TripReserve;
import jp.mcapps.android.multi_payment_terminal.ui.history.HistoryEventHandlers;
import jp.mcapps.android.multi_payment_terminal.ui.history.HistoryEventHandlersImpl;
import jp.mcapps.android.multi_payment_terminal.ui.pos.CartConfirmHandlers;
import jp.mcapps.android.multi_payment_terminal.ui.pos.CartConfirmHandlersImpl;
import timber.log.Timber;

public class TicketIssueFragment extends BaseFragment implements TicketIssueHandlers {

    private final String SCREEN_NAME = "QR券発行画面";
    private TicketIssueViewModel _ticketIssueViewModel;
    private TicketSearchResults _searchResults;
    private List<TicketCategoryData> _ticketCategoryList;
    private SharedViewModel _sharedViewModel;
    private final Resources _resources = MainApplication.getInstance().getResources();
    private final TerminalDao _terminalDao = LocalDatabase.getInstance().terminalDao();
    private final TicketSalesApi _ticketSalesApiClient = TicketSalesApiImpl.getInstance();
    private TerminalData _terminalData = null;
    private String _serviceInstanceId = null;
    private int _slipId = 0;
    private SlipData _slipData = null;
    private TicketPurchasedHistory.Request _ticketPurchasedHistoryRequest = null;
    private TicketPurchasedHistory.Response _ticketPurchasedHistoryResponse = null;
    private DynamicTicket.Request _dynamicTicketRequest = null;
    private DynamicTicket.Response _dynamicTicketResponse = null;
    private boolean _isTicketPurchasedHistory = false;
    private boolean _isQrTicket = false;
    private final long _delayMillis = 1000;
    private long _pushedMillis = 0;

    public static TicketIssueFragment newInstance() {
        return new TicketIssueFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        final FragmentTicketIssueBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_ticket_issue, container, false);

        _ticketIssueViewModel = new ViewModelProvider(this).get(TicketIssueViewModel.class);
        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        _sharedViewModel.setShowBarTitleTicketIssue(true);
        binding.setViewModel(_ticketIssueViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setHandlers(this);

        // 決済情報表示
        ticketIssueInitView();
        // アダプタは検索結果を流用
        TicketSearchResultsAdapter _adapter = new TicketSearchResultsAdapter();
        _adapter.submitList(_ticketCategoryList);
        RecyclerView.LayoutManager rLayoutManager = new LinearLayoutManager(getContext());
        binding.listTicketCategoryInfo.setLayoutManager(rLayoutManager);
        binding.listTicketCategoryInfo.setAdapter(_adapter);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        _ticketIssueViewModel.setProcessing(true);
        sendTicketPurchasedHistory();

        return binding.getRoot();
    }

    public void sendTicketPurchasedHistory() {
        _isTicketPurchasedHistory = false;
        _ticketIssueViewModel.setCancel(false);

        Observable.fromCallable(() -> {
                    _terminalData = _terminalDao.getTerminal();
                    _serviceInstanceId = _terminalData.service_instance_abt;
                    _ticketPurchasedHistoryRequest = setHistoryRequestBody();
                    return _ticketSalesApiClient.TicketPurchasedHistory(_serviceInstanceId, _searchResults.ticketId, _ticketPurchasedHistoryRequest);
                })
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            Timber.d("sendTicketPurchasedHistory result");
                            _sharedViewModel.setShowBarTicketIssueCancel(false);
                            _ticketPurchasedHistoryResponse = result;

                            _isTicketPurchasedHistory = true;
                            updateTicketData();
                            sendCreateDynamicTicket();
                        },
                        error -> {
                            Resources resources = MainApplication.getInstance().getResources();
                            _sharedViewModel.setShowBarTicketIssueCancel(true);
                            _ticketIssueViewModel.setIssueResult(false);

                            if (error.getClass() == TicketSalesStatusException.class) {
                                Timber.e(error, "sendTicketPurchasedHistory error %s %s", String.valueOf(((TicketSalesStatusException) error).getCode()), error.getMessage());
                                if (4011 == ((TicketSalesStatusException) error).getCode()) {
                                    _ticketIssueViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8141));
                                    _ticketIssueViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8141));
                                    _ticketIssueViewModel.setErrorMessageInformation(resources.getString(R.string.error_detail_ticket_8141));
                                } else if (4012 == ((TicketSalesStatusException) error).getCode()) {
                                    _ticketIssueViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8142));
                                    _ticketIssueViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8142));
                                    _ticketIssueViewModel.setErrorMessageInformation(resources.getString(R.string.error_detail_ticket_8142));
                                } else {
                                    _ticketIssueViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8140));
                                    _ticketIssueViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8140));
                                    _ticketIssueViewModel.setErrorMessageInformation(String.format(resources.getString(R.string.error_detail_ticket_8140), String.valueOf(((TicketSalesStatusException) error).getCode())));
                                }
                            } else if (error.getClass() == HttpStatusException.class) {
                                Timber.e(error, "sendTicketPurchasedHistory error %s %s", String.valueOf(((HttpStatusException) error).getStatusCode()), error.getMessage());
                                _ticketIssueViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8140));
                                _ticketIssueViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8140));
                                _ticketIssueViewModel.setErrorMessageInformation(String.format(resources.getString(R.string.error_detail_ticket_8140), String.valueOf(((HttpStatusException) error).getStatusCode())));
                            } else {
                                Timber.e(error, "sendTicketPurchasedHistory error %s", error.getMessage());
                                _ticketIssueViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8097));
                                _ticketIssueViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8097));
                                _ticketIssueViewModel.setErrorMessageInformation(resources.getString(R.string.error_detail_ticket_8097));
                            }
                            _ticketIssueViewModel.setProcessing(false);
                        }
                );
    }

    private TicketPurchasedHistory.Request setHistoryRequestBody() {

        SlipData slipData = null;
        _slipId = 0;
        Integer reserveSlotsCnt = 0;   //予約枠の数

        try {
            slipData = DBManager.getSlipDao().getLatestOne();
            _slipData = slipData;
            _slipId = slipData.id;
        } catch (Exception e) {
            Timber.e("TicketPurchasedHistory.Request setRequestBody->DBManager.getSlipDao().getLatestOne() Exception e <%s>", e.getMessage());
        }

        _searchResults = AppPreference.getTicketSearchResults();
        int categoryCount = _searchResults.categoryData.size();

        TicketPurchasedHistory.Request request = new TicketPurchasedHistory.Request();
        request.idp_account_id = null;                                   // 購入した会員ID※設定不要
        request.idp_account_name = null;                                 // 購入した会員番号※設定不要
        request.idp_account_number = null;                               // 購入した会員名※設定不要
        request.sales_method = "in_person";                              // 購入：in_person 対面（固定）

        if (slipData != null) {
            // 端末識別番号
            request.terminal_tid = slipData.termId;
            // 端末通番
            request.terminal_transaction_no = String.valueOf(slipData.termSequence);
            // 支払カードカテゴリ
            request.card_category = slipData.cardCategory;
            // 支払カードブランドコード
            request.card_brand_code = slipData.cardBrandCode;
            // 支払カードブランド名
            request.card_brand_name = slipData.cardBrandName;
            // 購入金額
            request.purchased_amount = String.valueOf(slipData.transAmount);
            // 予約日または現在の日付
            String YYYY = slipData.transDate.substring(0,4);
            String MM = slipData.transDate.substring(5,7);
            String DD = slipData.transDate.substring(8,10);
            request.reservation_date = YYYY + MM + DD;
            // 購入時間
            String hh = slipData.transDate.substring(11,13);
            String mm = slipData.transDate.substring(14,16);
            String ss = slipData.transDate.substring(17,19);
            request.purchased_time = hh + mm + ss;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000'Z'");
            try {
                int year = Integer.parseInt(YYYY);
                int month = Integer.parseInt(MM);
                int day_of_month = Integer.parseInt(DD);
                int hour_of_day = Integer.parseInt(hh);
                int minute = Integer.parseInt(mm);
                int second = Integer.parseInt(ss);

                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month-1, day_of_month, hour_of_day, minute, second);
                calendar.add(Calendar.HOUR_OF_DAY, -9);
                Date date = calendar.getTime();

                request.purchased_time = sdf.format(date);
                Timber.d("%s", request.purchased_time);
            } catch (Exception e) {
                Timber.e(e);
            }
        } else {
            // 想定外の場合、値をセットしない
        }

        /* 購入したチケットの明細 */
        request.details = new PurchasedTicketDetails[categoryCount];
        for (int i = 0; i < categoryCount; i++) {
            request.details[i] = new PurchasedTicketDetails();
            // 乗客カテゴリ
            request.details[i].passenger_category_enum = _searchResults.categoryData.get(i).categoryType;
            // 回数券ID
            request.details[i].ticket_setting_id = _searchResults.categoryData.get(i).ticketsNumber <= 1 ? null : _searchResults.categoryData.get(i).ticketSettingID;
            // 数量（枚数）
            request.details[i].count = _searchResults.categoryData.get(i).quantity;
            // 購入金額
            request.details[i].purchased_amount = _searchResults.categoryData.get(i).amount;
            reserveSlotsCnt += _searchResults.categoryData.get(i).ticketsNumber * _searchResults.categoryData.get(i).quantity;
        }

        /* 便予約 */
        request.trip_reserve = new TripReserve();
        // 便ID
        request.trip_reserve.trip_id = _searchResults.tripId;
        // 臨時便ID
        request.trip_reserve.sprcial_trip_id = null;

        /* 予約枠 */
        request.trip_reserve.reserve_slots = new ReserveSlots[reserveSlotsCnt];
        reserveSlotsCnt = 0;
        for (int i = 0; i < categoryCount; i++) {
            Integer ticketsNumber = _searchResults.categoryData.get(i).ticketsNumber;
            Integer quantity = _searchResults.categoryData.get(i).quantity;
            for (int j = 0; j < ticketsNumber*quantity; j++) {
                request.trip_reserve.reserve_slots[reserveSlotsCnt] = new ReserveSlots();
                // 予約枠ID
                request.trip_reserve.reserve_slots[reserveSlotsCnt].transport_reservation_slot_id = _searchResults.transportReservationSlotId;
                // 乗客カテゴリ
                request.trip_reserve.reserve_slots[reserveSlotsCnt].passenger_category_enum = _searchResults.categoryData.get(i).categoryType;
                reserveSlotsCnt++;
            }
        }

        return request;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void ticketIssue(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        long timeMillis = System.currentTimeMillis();

        // 連打防止
        if (timeMillis - _pushedMillis < _delayMillis) return;
        _pushedMillis = timeMillis;
        Timber.d("push time millis:%s",_pushedMillis);

        if (!_isTicketPurchasedHistory) {
            sendTicketPurchasedHistory();
        } else if (!_isQrTicket) {
            sendCreateDynamicTicket();
        } else {
            // QR券印字
            PrinterManager printerManager = PrinterManager.getInstance();
            printerManager.print_QRTicket(view);
            // HOME画面に遷移する
            NavigationWrapper.navigate(view, R.id.action_ticketIssueFragment_to_navigation_menu);
        }
    }

    public void sendCreateDynamicTicket() {
        _isQrTicket = false;

        Observable.fromCallable(() -> {
                    _terminalData = _terminalDao.getTerminal();
                    _serviceInstanceId = _terminalData.service_instance_abt;
                    _dynamicTicketRequest = setDynamicTicketRequestBody();
                    return _ticketSalesApiClient.CreateDynamicTicket(_serviceInstanceId, _dynamicTicketRequest);
                })
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            Timber.d("sendCreateDynamicTicket result");
                            _sharedViewModel.setShowBarTicketIssueCancel(false);
                            _dynamicTicketResponse = result;

                            _isQrTicket = true;
                            dynamicTicketResponseCheck();
                            cancelCheck();

                            _ticketIssueViewModel.setProcessing(false);
                        },
                        error -> {
                            Resources resources = MainApplication.getInstance().getResources();
                            _sharedViewModel.setShowBarTicketIssueCancel(false);
                            _ticketIssueViewModel.setIssueResult(false);

                            if (error.getClass() == TicketSalesStatusException.class) {
                                Timber.e(error, "sendCreateDynamicTicket error %s %s",String.valueOf(((TicketSalesStatusException) error).getCode()), error.getMessage());
                                if (4013 == ((TicketSalesStatusException) error).getCode()) {
                                    _ticketIssueViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8151));
                                    _ticketIssueViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8151));
                                    _ticketIssueViewModel.setErrorMessageInformation(resources.getString(R.string.error_detail_ticket_8151));
                                } else if (4014 == ((TicketSalesStatusException) error).getCode()) {
                                    _ticketIssueViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8152));
                                    _ticketIssueViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8152));
                                    _ticketIssueViewModel.setErrorMessageInformation(resources.getString(R.string.error_detail_ticket_8152));
                                } else if (4015 == ((TicketSalesStatusException) error).getCode()) {
                                    _ticketIssueViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8153));
                                    _ticketIssueViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8153));
                                    _ticketIssueViewModel.setErrorMessageInformation(resources.getString(R.string.error_detail_ticket_8153));
                                } else {
                                    _ticketIssueViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8150));
                                    _ticketIssueViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8150));
                                    _ticketIssueViewModel.setErrorMessageInformation(String.format(resources.getString(R.string.error_detail_ticket_8150), String.valueOf(((TicketSalesStatusException) error).getCode())));
                                }
                            } else if (error.getClass() == HttpStatusException.class) {
                                Timber.e(error, "sendCreateDynamicTicket error %s %s", String.valueOf(((HttpStatusException) error).getStatusCode()), error.getMessage());
                                _ticketIssueViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8150));
                                _ticketIssueViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8150));
                                _ticketIssueViewModel.setErrorMessageInformation(String.format(resources.getString(R.string.error_detail_ticket_8150), String.valueOf(((HttpStatusException) error).getStatusCode())));
                            } else {
                                Timber.e(error, "sendCreateDynamicTicket error %s", error.getMessage());
                                _ticketIssueViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8097));
                                _ticketIssueViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8097));
                                _ticketIssueViewModel.setErrorMessageInformation(resources.getString(R.string.error_detail_ticket_8097));
                            }
                            _ticketIssueViewModel.setProcessing(false);
                        }
                );
    }

    private DynamicTicket.Request setDynamicTicketRequestBody() {

        SlipData slipData = null;

        try {
            slipData = DBManager.getSlipDao().getLatestOne();
        } catch (Exception e) {
            Timber.e("TicketPurchasedHistory.Request setRequestBody->DBManager.getSlipDao().getLatestOne() Exception e <%s>", e);
            e.printStackTrace();
        }

        DynamicTicket.Request request = new DynamicTicket.Request();
        // 便予約ID
        if (slipData != null) request.trip_reservation_id = slipData.tripReservationId;
        // in_person 対面（固定）
        request.sale_method = "in_person";
        // 購入した会員ID※設定不要
        request.idp_account_id = null;
        // 端末識別番号
        if (slipData != null) request.terminal_tid = slipData.termId;

        return request;
    }

    private void dynamicTicketResponseCheck() {
        _isTicketPurchasedHistory = true;
        if (_dynamicTicketResponse != null && _dynamicTicketResponse.error == null && _dynamicTicketResponse.data != null && _dynamicTicketResponse.data.item != null) {
            // 応答結果：正常
            // 結果保持
            AppPreference.setDynamicTicketItem(_dynamicTicketResponse.data.item);
            _ticketIssueViewModel.setIssueResult(true);
        } else {
            // 応答結果：異常
            Timber.e("dynamicTicketResponseCheck error");
            _ticketIssueViewModel.setIssueResult(false);
            Resources resources = MainApplication.getInstance().getResources();
            _ticketIssueViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8030));
            _ticketIssueViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8030));
            _ticketIssueViewModel.setErrorMessageInformation(resources.getString(R.string.error_detail_ticket_8030));
        }
    }

    @Override
    public void ticketCancel(View view) {
        AppPreference.setCancelTargetSlipId(_slipId);
        HistoryEventHandlers handlers = new HistoryEventHandlersImpl(this);
        if (_slipData != null) {
            handlers.onCancelClick(view, _slipData.transBrand, _slipId, _slipData.transactionTerminalType, _sharedViewModel, _slipData.purchasedTicketDealId, true);
        }
//        // for test
//        _ticketIssueViewModel.setIssueResult(false);
//        _ticketIssueViewModel.setErrorMessage("チケット発行エラー");
//        _ticketIssueViewModel.setErrorMessageInformation("チケットの予約ができませんでした。");
//        // for test
    }

    private void updateTicketData() {

        if (_ticketPurchasedHistoryResponse != null && _ticketPurchasedHistoryResponse.error == null && _ticketPurchasedHistoryResponse.data != null) {
            // 応答結果：正常
            if(_slipId != 0){
                Thread updateData = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        DBManager.getSlipDao().updateTicketData(_slipId, _ticketPurchasedHistoryResponse.data.purchased_ticket_deal_id, _ticketPurchasedHistoryResponse.data.trip_reservation_id);
                        _slipData = DBManager.getSlipDao().getLatestOne();
                    }
                });
                updateData.start();
                try {
                    updateData.join();
                } catch (Exception e) {
                    Timber.e("updateTicketData error 8039");
                    _ticketIssueViewModel.setIssueResult(false);
                    Resources resources = MainApplication.getInstance().getResources();
                    _ticketIssueViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8039));
                    _ticketIssueViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8039));
                    _ticketIssueViewModel.setErrorMessageInformation(resources.getString(R.string.error_detail_ticket_8039));
                }
            }
        } else {
            // 応答結果：異常
            Timber.e("updateTicketData error 8030");
            _ticketIssueViewModel.setIssueResult(false);
            Resources resources = MainApplication.getInstance().getResources();
            _ticketIssueViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8030));
            _ticketIssueViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8030));
            _ticketIssueViewModel.setErrorMessageInformation(resources.getString(R.string.error_detail_ticket_8030));
        }
    }

    private void ticketIssueInitView() {
        // 決済結果表示用の情報として検索結果から情報を取得
        _searchResults = AppPreference.getTicketSearchResults();

        // 基本的には検索結果は成功しかないはず
        _ticketIssueViewModel.setIssueResult(true);
        _ticketIssueViewModel.setTicketName(_searchResults.ticketName);
        _ticketIssueViewModel.setTicketTripInfo(_searchResults.embarkStopName, _searchResults.departureTime);
        _ticketIssueViewModel.setRemainingSeats(_searchResults.remainingSeats);
        Timber.i("%s: チケット名=[%s] のりば名=[%s] 出発時刻=[%s] 残数=[%s]",
                SCREEN_NAME, _searchResults.ticketName, _searchResults.embarkStopName, _searchResults.departureTime.substring(0, 5), _searchResults.remainingSeats);

        _ticketCategoryList = new ArrayList<>();
        TicketCategoryData ticketCategoryData;
        int categoryCount = _searchResults.categoryData.size();
        for (int i = 0; i < categoryCount; i++) {
            ticketCategoryData = new TicketCategoryData();

            ticketCategoryData.TicketInfo = getTicketInfo(_searchResults.categoryData.get(i).categoryType,
                    _searchResults.categoryData.get(i).ticketsNumber,
                    _searchResults.categoryData.get(i).quantity);

            ticketCategoryData.Amount = "";
            Timber.i("カテゴリリスト%d: [%s %s]", i, ticketCategoryData.TicketInfo, ticketCategoryData.Amount);
            _ticketCategoryList.add(ticketCategoryData);
        }
    }

    private String getTicketInfo(String categoryType, Integer ticketsNumber, Integer quantity){
        String TicketInfo = "";

        // 乗客カテゴリ名
        if (categoryType != null) {
            if (categoryType.equals("unknown")) {
                TicketInfo = _resources.getString(R.string.text_ticket_adult);
            } else if (categoryType.equals("child")) {
                TicketInfo = _resources.getString(R.string.text_ticket_child);
            } else if (categoryType.equals("disabled")) {
                TicketInfo = _resources.getString(R.string.text_ticket_adult_disability);
            } else if (categoryType.equals("child_disabled")) {
                TicketInfo = _resources.getString(R.string.text_ticket_child_disability);
            } else if (categoryType.equals("carer")) {
                TicketInfo = _resources.getString(R.string.text_ticket_caregiver);
            } else if (categoryType.equals("baby")) {
                TicketInfo = _resources.getString(R.string.text_ticket_baby);
            } else {
                Timber.e("乗客カテゴリ名(想定外):%s", categoryType);
            }
        } else {
            Timber.e("乗客カテゴリ名 = null");
        }

        // 回数券のセット枚数
        if (ticketsNumber != null) {
            if (ticketsNumber > 1) {
                /* セット券あり */
                TicketInfo += String.format("%s%s", ticketsNumber, _resources.getString(R.string.text_tickets_number));
            }
        } else {
            Timber.e("回数券のセット枚数 = null");
        }

        // 数量
        if (quantity != null) {
            if (quantity > 0) {
                /* 数量あり */
                TicketInfo += String.format("ｘ%s", quantity);
            } else {
                Timber.e("数量 = %s", quantity);
            }
        } else {
            Timber.e("数量 = null");
        }

        return TicketInfo;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _sharedViewModel.setShowBarTitleTicketIssue(false);
    }

    private void cancelCheck() {
        if (_slipData.transBrand.equals(_resources.getString(R.string.print_brand_edy_rakute)) || _slipData.transBrand.equals(_resources.getString(R.string.money_brand_nanaco)) ) {
            // 楽天Edy、nanacoは、取消不可仕様のため、取消ボタンを非活性にする
            _ticketIssueViewModel.setCancel(false);
        } else {
            _ticketIssueViewModel.setCancel(true);
        }
    }
}