package jp.mcapps.android.multi_payment_terminal.ui.history;

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

import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentHistoryAggregateBinding;

public class HistoryAggregateFragment extends BaseFragment {
    private final String SCREEN_NAME = "集計履歴";

    public static HistoryAggregateFragment newInstance() {
        return new HistoryAggregateFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final FragmentHistoryAggregateBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_history_aggregate, container, false);

        HistoryAggregateViewModel viewModel = new ViewModelProvider(this).get(HistoryAggregateViewModel.class);
        viewModel.getAggregateHistory();

        binding.setLifecycleOwner(getViewLifecycleOwner());

        HistoryAggregateAdapter adapter = new HistoryAggregateAdapter();
        viewModel.getAggregateDataList().observe(getViewLifecycleOwner(), adapter::submitList);

        RecyclerView.LayoutManager rLayoutManager = new LinearLayoutManager(getContext());
        binding.listAggregateHistory.setLayoutManager(rLayoutManager);
        binding.listAggregateHistory.setAdapter(adapter);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }
}
