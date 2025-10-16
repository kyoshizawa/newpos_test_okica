package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketClassData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentTicketClassBinding;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentTicketSearchBinding;
import jp.mcapps.android.multi_payment_terminal.ui.pos.ProductCategorySelectModel;
import jp.mcapps.android.multi_payment_terminal.ui.pos.ProductSelectAdapter;
import timber.log.Timber;


public class TicketClassFragment extends BaseFragment implements TicketClassHandlers{

    private final String SCREEN_NAME = "チケット分類選択画面";
    private final String SCREEN_BAR_TITLE = "チケット分類";
    private TicketClassViewModel _ticketClassViewModel;
    private SharedViewModel _sharedViewModel;
    private List<TicketClassData> _ticketClassList;
    private TicketClassAdapter _adapter;

    public static TicketClassFragment newInstance() {
        return new TicketClassFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentTicketClassBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_ticket_class, container, false);

        _ticketClassViewModel = new ViewModelProvider(this).get(TicketClassViewModel.class);
        _ticketClassList = new ArrayList<>();
        _adapter = new TicketClassAdapter();
        _ticketClassViewModel.initFetchData()
                .subscribe(
                        result -> {
                            Timber.d("on init fetch data");
                            _ticketClassList = result;
                            _adapter.submitList(_ticketClassList);
                        },
                        error -> {
                            Timber.e(error, "error on init fetch data");
                        }
                );

        RecyclerView.LayoutManager rLayoutManager = new LinearLayoutManager(getContext());
        binding.ticketClassList.setLayoutManager(rLayoutManager);
        binding.ticketClassList.setAdapter(_adapter);

        _adapter.setOnItemClickListener(new TicketClassAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(TicketClassData item) {
                CommonClickEvent.RecordClickOperation(item.ticket_class_name, true);
                Timber.d("Ticket Class item clicked: %d : %s", item.ticket_class_id, item.ticket_class_name);
                // 選択した場合の処理
                AppPreference.setSelectedTicketClassData(item);
                // のりば・おりばクリア
                AppPreference.setSelectedTicketEmbarkData(null);
                AppPreference.setSelectedTicketDisembarkData(null);
                NavigationWrapper.popBackStack(getParentFragment().getView());
            }
        });

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
//        _sharedViewModel.setBarTitle(SCREEN_BAR_TITLE);
        _sharedViewModel.setShowBarTitleTicketClass(true);

        binding.setViewModel(_ticketClassViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setHandlers(this);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        _sharedViewModel.setBarTitle("");
        _sharedViewModel.setShowBarTitleTicketClass(false);
    }
}
