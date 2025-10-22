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
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.data.OptionService;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentHistoryValidationCheckBinding;

public class HistoryValidationCheckFragment extends BaseFragment {
    private final MainApplication _app = MainApplication.getInstance();
    private final String SCREEN_NAME = validationCheckName();

    public static HistoryTransactionFragment newInstance() {
        return new HistoryTransactionFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final FragmentHistoryValidationCheckBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_history_validation_check, container, false);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        binding.setLifecycleOwner(getViewLifecycleOwner());
        HistoryValidationCheckAdapter adapter = new HistoryValidationCheckAdapter();
        RecyclerView.LayoutManager rLayoutManager = new LinearLayoutManager(getContext());
        binding.listHistoryValidationCheck.setLayoutManager(rLayoutManager);
        // binding.listHistoryValidationCheck.setAdapter(adapter);

        HistoryValidationCheckViewModel viewModel = new ViewModelProvider(this).get(HistoryValidationCheckViewModel.class);
        binding.setViewModel(viewModel);
        // viewModel.getHistory().observe(getViewLifecycleOwner(), adapter::submitList);
        viewModel.setTitle(SCREEN_NAME);
        viewModel.init();

        return binding.getRoot();
    }

    // 〇〇履歴 の形で返す
    private String validationCheckName() {
        final OptionService service = _app.getOptionService();

        int index = service != null ? service.indexOfFunc(OptionService.FuncIds.MAGNETIC_CARD_VALIDATION) : -1;

        // Fragment生成前の為MainApplicationからgetString
        return index >= 0
                ? service.getFunc(index).getDisplayName() + _app.getString(R.string.btn_home_history)
                : _app.getString(R.string.btn_other_validation) + _app.getString(R.string.btn_home_history);
    }
}
