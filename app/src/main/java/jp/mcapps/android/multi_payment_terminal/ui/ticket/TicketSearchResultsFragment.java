package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentTicketSearchResultsBinding;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.TicketTripPageInfo;
import timber.log.Timber;

public class TicketSearchResultsFragment extends BaseFragment implements TicketSearchResultsHandlers{

    private final String SCREEN_NAME = "チケット検索結果画面";
    private TicketSearchResultsViewModel _ticketSearchResultsViewModel;
    private TicketSearchResults _searchResults;
    private List<TicketCategoryData> _ticketCategoryList;
    private TicketSearchResultsAdapter _adapter;
    private Resources _resources = MainApplication.getInstance().getResources();
    private final long _delayMillis = 1000;
    private long _pushedMillis = 0;

    public static TicketSearchResultsFragment newInstance() {
        return new TicketSearchResultsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        final FragmentTicketSearchResultsBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_ticket_search_results, container, false);

        _ticketSearchResultsViewModel = new ViewModelProvider(this).get(TicketSearchResultsViewModel.class);

        binding.setViewModel(_ticketSearchResultsViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setHandlers(this);

        // チケット検索結果表示
        ticketSearchResultsView();
        _adapter = new TicketSearchResultsAdapter();
        _adapter.submitList(_ticketCategoryList);
        RecyclerView.LayoutManager rLayoutManager = new LinearLayoutManager(getContext());
        binding.listTicketCategoryInfo.setLayoutManager(rLayoutManager);
        binding.listTicketCategoryInfo.setAdapter(_adapter);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }

    @Override
    public void navigateToMenuHome(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        if (_searchResults.totalAmount == null || _searchResults.totalAmount < 0) {
            Timber.e("検索結果の合計金額:%s", _searchResults.totalAmount);
            return;
        }

        final Bundle args = new Bundle();
        args.putBoolean("cashMenu", true);

        Amount.isPosAmount(false);
        Amount.setTotalChangeAmount(0);
        Amount.setFlatRateAmount(_searchResults.totalAmount);
        // HOME画面(各マネー選択、現金支払のメニュー)に遷移する
        NavigationWrapper.navigate(view, R.id.action_ticketSearchResultsFragment_to_navigation_menu, args);
    }

    @Override
    public void ticketSearchPrevNext(String tripType) {
        if (tripType.equals("next")) {
            CommonClickEvent.RecordClickOperation("次の便に変更", true);
        } else {
            CommonClickEvent.RecordClickOperation("前の便に変更", true);
        }

        long timeMillis = System.currentTimeMillis();

        // 連打防止
        if (timeMillis - _pushedMillis < _delayMillis) return;
        _pushedMillis = timeMillis;
        Timber.d("push time millis:%s",_pushedMillis);

        _ticketSearchResultsViewModel.setProcessing(true);
        Integer searchAdultNumber = _searchResults.searchAdultNumber;
        Integer searchChildNumber = _searchResults.searchChildNumber;
        Integer searchBabyNumber = _searchResults.searchBabyNumber;
        Integer searchAdultDisabilityNumber = _searchResults.searchAdultDisabilityNumber;
        Integer searchChildDisabilityNumber = _searchResults.searchChildDisabilityNumber;
        Integer searchCaregiverNumber = _searchResults.searchCaregiverNumber;
        TicketTripPageInfo ticketTripPageInfo = _searchResults.ticketTripPageInfo;

        // 検索情報セット
        TicketSearchInfo searchInfo = new TicketSearchInfo();
        Boolean result = searchInfo.setInfo(searchAdultNumber,
                searchChildNumber,
                searchBabyNumber,
                searchAdultDisabilityNumber,
                searchChildDisabilityNumber,
                searchCaregiverNumber,
                ticketTripPageInfo,
                tripType);

        TicketSearchProcess ticketSearchProcess = new TicketSearchProcess();
        _searchResults = new TicketSearchResults();
        _searchResults.searchAdultNumber = searchAdultNumber;
        _searchResults.searchChildNumber = searchChildNumber;
        _searchResults.searchBabyNumber = searchBabyNumber;
        _searchResults.searchAdultDisabilityNumber = searchAdultDisabilityNumber;
        _searchResults.searchChildDisabilityNumber = searchChildDisabilityNumber;
        _searchResults.searchCaregiverNumber = searchCaregiverNumber;

        String trip = "前便";
        if (tripType.equals("next")) trip = "次便";
        Timber.i("%s検索開始: 日時=[%s/%s] チケット分類名=[%s] のりば名=[%s] おりば名=[%s] 大人=%s 小人=%s 障がい者(大人)=%s 障がい者(小人)=%s 介助者=%s",
                trip, searchInfo.date, searchInfo.departureTime,
                AppPreference.getSelectedTicketClassData().ticket_class_name, AppPreference.getSelectedTicketEmbarkData().stop_name, AppPreference.getSelectedTicketDisembarkData().stop_name,
                searchInfo.adultNumber, searchInfo.childNumber, searchInfo.adultDisabilityNumber, searchInfo.childDisabilityNumber, searchInfo.caregiverNumber);

        if (result) {
            // 別スレッド：検索結果取得
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    _searchResults = ticketSearchProcess.Execute(searchInfo, _searchResults);
                }
            });
            thread.start();

            try {
                thread.join();
                saveTicketSearchResults();
                ticketSearchResultsView();
            } catch (Exception e) {
                Timber.e(e);
                _searchResults.searchResult = false;
                Resources resources = MainApplication.getInstance().getResources();
                _searchResults.errorCode = 8019;
                _searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8019);
                _searchResults.errorMessageInformation = resources.getString(R.string.error_detail_ticket_8019);
                saveTicketSearchResults();
                ticketSearchResultsView();
            }
        } else {
            Timber.e("検索条件設定異常");
            _searchResults.searchResult = false;
            Resources resources = MainApplication.getInstance().getResources();
            _searchResults.errorCode = 8010;
            _searchResults.nextTrip = false;
            _searchResults.prevTrip = false;
            _searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8010);
            _searchResults.errorMessageInformation = resources.getString(R.string.error_detail_ticket_8010);
            saveTicketSearchResults();
            ticketSearchResultsView();
        }
    }

    private void saveTicketSearchResults() {
        AppPreference.setTicketSearchResults(_searchResults);
        _ticketSearchResultsViewModel.setProcessing(false);
    }

    private void ticketSearchResultsView() {

        _searchResults = AppPreference.getTicketSearchResults();
        if (_searchResults == null) {
            Timber.e("AppPreference.getTicketSearchResults() = null");
            Resources resources = MainApplication.getInstance().getResources();
            _ticketSearchResultsViewModel.setErrorCode(resources.getString(R.string.error_type_ticket_8019));
            _ticketSearchResultsViewModel.setErrorMessage(resources.getString(R.string.error_message_ticket_8019));
            _ticketSearchResultsViewModel.setErrorMessageInformation(resources.getString(R.string.error_detail_ticket_8019));
            _ticketSearchResultsViewModel.setSearchResult(false);
            _ticketSearchResultsViewModel.setPrevTrip(false);
            _ticketSearchResultsViewModel.setNextTrip(false);
            return;
        }

        /* 「次の便に変更」ボタン表示制御 */
        boolean nextTrip = false;
        if (isNextTrip(_searchResults.departureTime)) {
            _ticketSearchResultsViewModel.setNextTrip(_searchResults.nextTrip);
            nextTrip = _searchResults.nextTrip;
        } else {
            _ticketSearchResultsViewModel.setNextTrip(false);
        }

        if (_searchResults.searchResult) {
            /* 検索結果：成功 */
            _ticketSearchResultsViewModel.setSearchResult(true);
            _ticketSearchResultsViewModel.setPrevTrip(_searchResults.prevTrip);
            _ticketSearchResultsViewModel.setTicketName(_searchResults.ticketName);
            _ticketSearchResultsViewModel.setTicketTripInfo(_searchResults.embarkStopName, _searchResults.departureTime);
            _ticketSearchResultsViewModel.setRemainingSeats(_searchResults.remainingSeats);
            _ticketSearchResultsViewModel.setTotalAmount(_searchResults.totalAmount);
            Timber.i("検索結果成功: 前便=[%s] 次便=[%s] チケット名=[%s] のりば名=[%s] 出発時刻=[%s] 残数=[%s] 合計金額=[%s]",
                    _searchResults.prevTrip, nextTrip, _searchResults.ticketName, _searchResults.embarkStopName, _searchResults.departureTime.substring(0,5), _searchResults.remainingSeats, _searchResults.totalAmount);

            _ticketCategoryList = new ArrayList<>();
            TicketCategoryData ticketCategoryData;
            int categoryCount = _searchResults.categoryData.size();
            for (int i = 0; i < categoryCount; i++) {
                ticketCategoryData = new TicketCategoryData();

                ticketCategoryData.TicketInfo = getTicketInfo(_searchResults.categoryData.get(i).categoryType,
                        _searchResults.categoryData.get(i).ticketsNumber,
                        _searchResults.categoryData.get(i).quantity);

                ticketCategoryData.Amount = getAmount(_searchResults.categoryData.get(i).amount);
                Timber.i("カテゴリリスト%d: [%s %s]",i ,ticketCategoryData.TicketInfo, ticketCategoryData.Amount);
                _ticketCategoryList.add(ticketCategoryData);
            }
        } else {
            /* 検索結果：失敗 */
            _ticketSearchResultsViewModel.setSearchResult(false);
            _ticketSearchResultsViewModel.setPrevTrip(_searchResults.prevTrip);
            _ticketSearchResultsViewModel.setErrorCode(_searchResults.errorCode.toString());
            _ticketSearchResultsViewModel.setErrorMessage(_searchResults.errorMessage);
            _ticketSearchResultsViewModel.setErrorMessageInformation(_searchResults.errorMessageInformation);
            Timber.i("検索結果失敗: 前便=[%s] 次便=[%s] エラーコード=[%s] エラーメッセージ=[%s] 補足メッセージ=[%s]",
                    _searchResults.prevTrip, nextTrip, _searchResults.errorCode, _searchResults.errorMessage, _searchResults.errorMessageInformation);
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

    @SuppressLint("DefaultLocale")
    private String getAmount(Integer amount){
        String Amount = "";

        // 金額
        if (amount != null)
        {
            if (amount >= 0) {
                Amount = String.format("￥%,d", amount);
            } else {
                Timber.e("金額 = %s", amount);
            }
        } else {
            Timber.e("金額 = null");
        }

        return Amount;
    }

    private boolean isNextTrip(String departureTime){
        boolean result = true;

        if (null == _searchResults.departureTime || _searchResults.departureTime.equals("")) return false;

        Date nowDate = new Date();
        SimpleDateFormat sdf1
                = new SimpleDateFormat("HH:mm:ss");
        String formatNowDate = sdf1.format(nowDate);

        Integer now = Integer.parseInt(formatNowDate.replace(":",""));
        Integer next = Integer.parseInt(departureTime.replace(":",""));

        // 現在時刻以降の便に関しては、「次の便に変更」ボタンを非表示
        if (next > now) result = false;
        Timber.d("next:%d > now:%d = isNextTrip:%s",next ,now ,result);

        return result;
    }
}