package jp.mcapps.android.multi_payment_terminal.ui.discount;
//ADD-S BMT S.Oyama 2024/09/06 フタバ双方向向け改修

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
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentDiscountFutabadBinding;

public class DiscountFutabaDFragment extends BaseFragment {
    private DiscountFutabaDViewModel _discountFutabaDViewModel;

    public static DiscountFutabaDFragment newInstance() {
        return new DiscountFutabaDFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final FragmentDiscountFutabadBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_discount_futabad, container, false);

        _discountFutabaDViewModel = new ViewModelProvider(this).get(DiscountFutabaDViewModel.class);

        binding.setViewModel(_discountFutabaDViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        _discountFutabaDViewModel.start();

        binding.setHandlers(new DiscountFutabaDEventHandlersImpl(this));

        return binding.getRoot();
    }

    @Override
    public void onStop() {
        super.onStop();
        _discountFutabaDViewModel.stop();
    }
}
//ADD-E BMT S.Oyama 2024/09/06 フタバ双方向向け改修
