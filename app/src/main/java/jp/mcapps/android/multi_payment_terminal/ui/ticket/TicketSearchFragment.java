package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentTicketSearchBinding;
import timber.log.Timber;

public class TicketSearchFragment extends BaseFragment implements TicketSearchHandlers{

    private final String SCREEN_NAME = "チケット検索条件画面";
    private TicketSearchViewModel _ticketSearchViewModel;
    private TicketSearchResults _searchResults;
    public static TicketSearchFragment newInstance() {
        return new TicketSearchFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentTicketSearchBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_ticket_search, container, false);

        _ticketSearchViewModel = new ViewModelProvider(this).get(TicketSearchViewModel.class);

        _ticketSearchViewModel.isInitResult(AppPreference.isTicketDataInit());
        binding.setViewModel(_ticketSearchViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setHandlers(this);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToTicketClass(View view, String textName) {
        CommonClickEvent.RecordClickOperation("チケット分類", true);
        String info = String.format("チケット分類「%s」", textName);
        Timber.i("%s", info);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        // チケット分類選択画面に遷移する
        NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, R.id.action_navigation_menu_to_fragment_ticket_class);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToTicketEmbark(View view) {
        CommonClickEvent.RecordClickOperation("のりば", true);
        String info = String.format("のりば「%s」", ((TextView) view).getText());
        Timber.i("%s", info);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        // のりば選択画面に遷移する
        NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, R.id.action_navigation_menu_to_fragment_ticket_embark);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void navigateToTicketDisembark(View view) {
        CommonClickEvent.RecordClickOperation("おりば", true);
        String info = String.format("おりば「%s」", ((TextView) view).getText());
        Timber.i("%s", info);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        // おりば選択画面に遷移する
        NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, R.id.action_navigation_menu_to_fragment_ticket_disembark);
    }

    @Override
    public void minusAdult(View view) {
        Integer now = _ticketSearchViewModel.getCountAdult().getValue();
        String info = "大人「－」";
        CommonClickEvent.RecordClickOperation(info, true);
        if (null != now) {
            info += String.format("(%s->%s)", now, now-1);
        } else {
            info += String.format("(%s)", now);
        }
        Timber.i("%s", info);
        _ticketSearchViewModel.minusCountAdult();
        _ticketSearchViewModel.searchEnabledUpdate();
    }

    @Override
    public void plusAdult(View view) {
        Integer now = _ticketSearchViewModel.getCountAdult().getValue();
        String info = "大人「＋」";
        CommonClickEvent.RecordClickOperation(info, true);
        if (null != now) {
            info += String.format("(%s->%s)", now, now+1);
        } else {
            info += String.format("(%s)", now);
        }
        Timber.i("%s", info);
        _ticketSearchViewModel.plusCountAdult();
        _ticketSearchViewModel.searchEnabledUpdate();
    }

    @Override
    public void minusChild(View view) {
        Integer now = _ticketSearchViewModel.getCountChild().getValue();
        String info = "小人「－」";
        CommonClickEvent.RecordClickOperation(info, true);
        if (null != now) {
            info += String.format("(%s->%s)", now, now-1);
        } else {
            info += String.format("(%s)", now);
        }
        Timber.i("%s", info);
        _ticketSearchViewModel.minusCountChild();
        _ticketSearchViewModel.searchEnabledUpdate();
    }

    @Override
    public void plusChild(View view) {
        Integer now = _ticketSearchViewModel.getCountChild().getValue();
        String info = "小人「＋」";
        CommonClickEvent.RecordClickOperation(info, true);
        if (null != now) {
            info += String.format("(%s->%s)", now, now+1);
        } else {
            info += String.format("(%s)", now);
        }
        Timber.i("%s", info);
        _ticketSearchViewModel.plusCountChild();
        _ticketSearchViewModel.searchEnabledUpdate();
    }

    @Override
    public void minusBaby(View view) {
        Integer now = _ticketSearchViewModel.getCountBaby().getValue();
        String info = "乳幼児「－」";
        CommonClickEvent.RecordClickOperation(info, true);
        if (null != now) {
            info += String.format("(%s->%s)", now, now-1);
        } else {
            info += String.format("(%s)", now);
        }
        Timber.i("%s", info);
        _ticketSearchViewModel.minusCountBaby();
        _ticketSearchViewModel.searchEnabledUpdate();
    }

    @Override
    public void plusBaby(View view) {
        Integer now = _ticketSearchViewModel.getCountBaby().getValue();
        String info = "乳幼児「＋」";
        CommonClickEvent.RecordClickOperation(info, true);
        if (null != now) {
            info += String.format("(%s->%s)", now, now+1);
        } else {
            info += String.format("(%s)", now);
        }
        Timber.i("%s", info);
        _ticketSearchViewModel.plusCountBaby();
        _ticketSearchViewModel.searchEnabledUpdate();
    }

    @Override
    public void minusAdultDisability(View view) {
        Integer now = _ticketSearchViewModel.getCountAdultDisability().getValue();
        String info = "大人(障)「－」";
        CommonClickEvent.RecordClickOperation(info, true);
        if (null != now) {
            info += String.format("(%s->%s)", now, now-1);
        } else {
            info += String.format("(%s)", now);
        }
        Timber.i("%s", info);
        _ticketSearchViewModel.minusCountAdultDisability();
        _ticketSearchViewModel.searchEnabledUpdate();
    }

    @Override
    public void plusAdultDisability(View view) {
        Integer now = _ticketSearchViewModel.getCountAdultDisability().getValue();
        String info = "大人(障)「＋」";
        CommonClickEvent.RecordClickOperation(info, true);
        if (null != now) {
            info += String.format("(%s->%s)", now, now+1);
        } else {
            info += String.format("(%s)", now);
        }
        Timber.i("%s", info);
        _ticketSearchViewModel.plusCountAdultDisability();
        _ticketSearchViewModel.searchEnabledUpdate();
    }

    @Override
    public void minusChildDisability(View view) {
        Integer now = _ticketSearchViewModel.getCountChildDisability().getValue();
        String info = "小人(障)「－」";
        CommonClickEvent.RecordClickOperation(info, true);
        if (null != now) {
            info += String.format("(%s->%s)", now, now-1);
        } else {
            info += String.format("(%s)", now);
        }
        Timber.i("%s", info);
        _ticketSearchViewModel.minusCountChildDisability();
        _ticketSearchViewModel.searchEnabledUpdate();
    }

    @Override
    public void plusChildDisability(View view) {
        Integer now = _ticketSearchViewModel.getCountChildDisability().getValue();
        String info = "小人(障)「＋」";
        CommonClickEvent.RecordClickOperation(info, true);
        if (null != now) {
            info += String.format("(%s->%s)", now, now+1);
        } else {
            info += String.format("(%s)", now);
        }
        Timber.i("%s", info);
        _ticketSearchViewModel.plusCountChildDisability();
        _ticketSearchViewModel.searchEnabledUpdate();
    }

    @Override
    public void minusCaregiver(View view) {
        Integer now = _ticketSearchViewModel.getCountCaregiver().getValue();
        String info = "介助者「－」";
        CommonClickEvent.RecordClickOperation(info, true);
        if (null != now) {
            info += String.format("(%s->%s)", now, now-1);
        } else {
            info += String.format("(%s)", now);
        }
        Timber.i("%s", info);
        _ticketSearchViewModel.minusCountCaregiver();
        _ticketSearchViewModel.searchEnabledUpdate();
    }

    @Override
    public void plusCaregiver(View view) {
        Integer now = _ticketSearchViewModel.getCountCaregiver().getValue();
        String info = "介助者「＋」";
        CommonClickEvent.RecordClickOperation(info, true);
        if (null != now) {
            info += String.format("(%s->%s)", now, now+1);
        } else {
            info += String.format("(%s)", now);
        }
        Timber.i("%s", info);
        _ticketSearchViewModel.plusCountCaregiver();
        _ticketSearchViewModel.searchEnabledUpdate();
    }

    @Override
    public void ticketSearch(View view) {
        String info = String.format("%s", ((TextView) view).getText());
        CommonClickEvent.RecordClickOperation(info, true);
        Activity activity = (Activity) view.getContext();
        if (activity == null) return;

        _ticketSearchViewModel.setProcessing(true);
        // 検索情報セット
        TicketSearchInfo searchInfo = new TicketSearchInfo();
        Boolean result = searchInfo.setInfo(_ticketSearchViewModel.getCountAdult().getValue(),
                _ticketSearchViewModel.getCountChild().getValue(),
                _ticketSearchViewModel.getCountBaby().getValue(),
                _ticketSearchViewModel.getCountAdultDisability().getValue(),
                _ticketSearchViewModel.getCountChildDisability().getValue(),
                _ticketSearchViewModel.getCountCaregiver().getValue(),
                null,
                null);

        TicketSearchProcess ticketSearchProcess = new TicketSearchProcess();
        _searchResults = new TicketSearchResults();
        _searchResults.searchAdultNumber = searchInfo.adultNumber;
        _searchResults.searchChildNumber = searchInfo.childNumber;
        _searchResults.searchBabyNumber = searchInfo.babyNumber;
        _searchResults.searchAdultDisabilityNumber = searchInfo.adultDisabilityNumber;
        _searchResults.searchChildDisabilityNumber = searchInfo.childDisabilityNumber;
        _searchResults.searchCaregiverNumber = searchInfo.caregiverNumber;

        Timber.i("検索開始: 日時=[%s/%s] チケット分類名=[%s] のりば名=[%s] おりば名=[%s] 大人=%s 小人=%s 障がい者(大人)=%s 障がい者(小人)=%s 介助者=%s",
                searchInfo.date, searchInfo.departureTime,
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
                navigateToTicketSearchResults(activity);
            } catch (Exception e) {
                final Resources resources = MainApplication.getInstance().getResources();
                Timber.e(e);
                _searchResults.searchResult = false;
                _searchResults.nextTrip = false;
                _searchResults.prevTrip = false;
                _searchResults.errorCode = 8019;
                _searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8019);
                _searchResults.errorMessageInformation = resources.getString(R.string.error_detail_ticket_8019);
                saveTicketSearchResults();
                navigateToTicketSearchResults(activity);
            }
        } else {
            final Resources resources = MainApplication.getInstance().getResources();
            Timber.e("検索条件設定異常");
            _searchResults.searchResult = false;
            _searchResults.nextTrip = false;
            _searchResults.prevTrip = false;
            _searchResults.errorCode = 8010;
            _searchResults.errorMessage = resources.getString(R.string.error_message_ticket_8010);
            _searchResults.errorMessageInformation = resources.getString(R.string.error_detail_ticket_8010);
            saveTicketSearchResults();
            navigateToTicketSearchResults(activity);
        }
    }

    private void saveTicketSearchResults() {
        AppPreference.setTicketSearchResults(_searchResults);
        _ticketSearchViewModel.setProcessing(false);
    }

    private void navigateToTicketSearchResults(Activity activity) {
        // チケット検索結果画面に遷移する
        NavigationWrapper.navigate(activity, R.id.fragment_main_nav_host, R.id.action_navigation_menu_to_fragment_ticket_search_results);
    }
}
