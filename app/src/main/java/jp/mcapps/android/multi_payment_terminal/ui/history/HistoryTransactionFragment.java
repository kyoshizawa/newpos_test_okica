package jp.mcapps.android.multi_payment_terminal.ui.history;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.TransHead;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentHistoryTransactionBinding;

public class HistoryTransactionFragment extends BaseFragment {
    private final String SCREEN_NAME = "取引履歴";

    private List<TransHead> _transHeadList;

    public static HistoryTransactionFragment newInstance() {
        return new HistoryTransactionFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final FragmentHistoryTransactionBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_history_transaction, container, false);

        SharedViewModel sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        sharedViewModel.setLoading(true);

        binding.setHandlers(new HistoryEventHandlersImpl(this));

        HistoryTransactionAdapter adapter = new HistoryTransactionAdapter();

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable run = () -> {
            _transHeadList = DBManager.getSlipDao().getTransHead();
            handler.post(() -> {
                adapter.submitList(_transHeadList);

                RecyclerView.LayoutManager rLayoutManager = new LinearLayoutManager(getContext());
                binding.listTransactionHistory.setLayoutManager(rLayoutManager);
                binding.listTransactionHistory.setAdapter(adapter);

                sharedViewModel.setLoading(false);
            });
        };
        new Thread(run).start();

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }
}
