package jp.mcapps.android.multi_payment_terminal.ui.aggregate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipDao;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentAggregateBinding;

public class AggregateFragment extends BaseFragment implements AggregateEventHandlers {
    private final String SCREEN_NAME = "集計表示";
    public static AggregateFragment newInstance() {
        return new AggregateFragment();
    }

    private AggregateViewModel _aggregateCodeViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentAggregateBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_aggregate, container, false);

        _aggregateCodeViewModel =
                new ViewModelProvider(this).get(AggregateViewModel.class);
        _aggregateCodeViewModel.aggregate();

        binding.setViewModel(_aggregateCodeViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.setHandlers(this);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
