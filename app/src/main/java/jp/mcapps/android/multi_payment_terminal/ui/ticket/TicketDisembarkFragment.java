package jp.mcapps.android.multi_payment_terminal.ui.ticket;

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

import java.util.ArrayList;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketEmbarkData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentTicketDisembarkBinding;
import timber.log.Timber;

public class TicketDisembarkFragment extends BaseFragment implements TicketDisembarkHandlers{

    private final String SCREEN_NAME = "おりば選択画面";
    private TicketDisembarkViewModel _ticketDisembarkViewModel;
    private List<TicketEmbarkData> _ticketEmbarkList;
    private SharedViewModel _sharedViewModel;
    private TicketEmbarkAdapter _adapter;
    private List<String> _routes;

    public static TicketDisembarkFragment newInstance() {
        return new TicketDisembarkFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        final FragmentTicketDisembarkBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_ticket_disembark, container, false);

        _ticketDisembarkViewModel = new ViewModelProvider(this).get(TicketDisembarkViewModel.class);
        _ticketEmbarkList = new ArrayList<>();

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        _sharedViewModel.setShowBarTitleTicketDisembark(true);

        _adapter = new TicketEmbarkAdapter();
        if(AppPreference.getSelectedTicketClassData() == null || AppPreference.getSelectedTicketEmbarkData() == null) {
            NavigationWrapper.popBackStack(getParentFragment().getView());
            return binding.getRoot();
        }

        _ticketDisembarkViewModel.getRoutes(AppPreference.getSelectedTicketClassData().ticket_class_id, AppPreference.getSelectedTicketEmbarkData().stop_id)
                .subscribe(
                result -> {
                    Timber.d("on init fetch data");
                    _routes = result;
                    if (_routes != null) {
                        _ticketDisembarkViewModel.initFetchData(AppPreference.getSelectedTicketClassData().ticket_class_id, _routes)
                                .subscribe(
                                        ret -> {
                                            Timber.d("on init fetch data");
                                            _ticketEmbarkList = ret;
                                            _adapter.submitList(_ticketEmbarkList);
                                        },
                                        error -> {
                                            Timber.e(error, "error on init fetch data");
                                        }
                                );
                    } else {
                        NavigationWrapper.popBackStack(getParentFragment().getView());
                    }
                },
                error -> {
                    Timber.e(error);
                    NavigationWrapper.popBackStack(getParentFragment().getView());
                }
        );
        RecyclerView.LayoutManager rLayoutManager = new LinearLayoutManager(getContext());
        binding.ticketDisembarkList.setLayoutManager(rLayoutManager);
        binding.ticketDisembarkList.setAdapter(_adapter);

        _adapter.setOnItemClickListener(new TicketEmbarkAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(TicketEmbarkData item) {
                CommonClickEvent.RecordClickOperation(item.stop_name, true);
                Timber.d("Ticket Embark item clicked: %d : %s : %s", item.ticket_class_id, item.stop_id, item.stop_name);
                // 選択した場合の処理
                AppPreference.setSelectedTicketDisembarkData(item);
                NavigationWrapper.popBackStack(getParentFragment().getView());
            }
        });

        binding.setViewModel(_ticketDisembarkViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setHandlers(this);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        _sharedViewModel.setBarTitle("");
        _sharedViewModel.setShowBarTitleTicketDisembark(false);
    }
}
