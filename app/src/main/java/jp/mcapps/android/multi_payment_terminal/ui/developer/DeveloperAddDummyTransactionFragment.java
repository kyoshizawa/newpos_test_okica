package jp.mcapps.android.multi_payment_terminal.ui.developer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentDeveloperAddDummyTransactionBinding;

public class DeveloperAddDummyTransactionFragment extends BaseFragment {

    private final String SCREEN_NAME = "ダミー決済追加";

    public static DeveloperAddDummyTransactionFragment newInstance() {
        return new DeveloperAddDummyTransactionFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        FragmentDeveloperAddDummyTransactionBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_developer_add_dummy_transaction, container, false);
        binding.setHandlers(new DeveloperEventHandlersImpl());

        DeveloperAddDummyTransactionViewModel viewModel = new ViewModelProvider(this).get(DeveloperAddDummyTransactionViewModel.class);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setViewModel(viewModel);

        SharedViewModel sharedViewModel = new ViewModelProvider(getActivity()).get(SharedViewModel.class);
        binding.setSharedViewModel(sharedViewModel);

        /*
        //nanacoでの決済は取消を選択できないように設定
        _viewModel.getPaymentType().observe(getViewLifecycleOwner(), value -> {
            if(value == 5) {
                binding.spinnerDummyTransType.setEnabled(false);
                binding.spinnerDummyTransType.setSelection(0);
            } else {
                binding.spinnerDummyTransType.setEnabled(true);
            }
        });
        */
        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }
}
