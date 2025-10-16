package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import javax.annotation.Nullable;

import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentPosSelectInputBinding;

public class PosSelectInputFragment extends BaseFragment {
    private final String SCREEN_NAME = "商品変更";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final FragmentPosSelectInputBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_pos_select_input, container, false);

        // viewModel from viewModelProvider
        PosViewModel _posViewModel = new ViewModelProvider(requireActivity()).get(PosViewModel.class);
        SharedViewModel _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        PosSelectInputViewModel _posSelectInputViewModel = new ViewModelProvider(requireActivity()).get(PosSelectInputViewModel.class);

        _posViewModel.setHomeVisible(false);
        _posViewModel.setQRScanVisible(false);
        _posViewModel.setSearchVisible(false);
        _posViewModel.setCartConfirmVisible(false);
        _posViewModel.setNavigateUpVisible(true);

        final Bundle args = getArguments();
        assert args != null;
        _posSelectInputViewModel.setProductId(args.getInt("product_id"));
        _posSelectInputViewModel.setCount(args.getInt("count"));
        _posSelectInputViewModel.setPrice(args.getInt("price"));
        _posSelectInputViewModel.setIsCustomPrice(args.getBoolean("is_custom_price"));
        _posSelectInputViewModel.setIsCountEditable(args.getBoolean("is_count_editable", true));
        _posSelectInputViewModel.setIsPriceEditable(args.getBoolean("is_price_editable", true));
        _posSelectInputViewModel.setProductName(args.getString("product_name"));
        _posSelectInputViewModel.setInputFragment(this);

        // data binding
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setSharedViewModel(_sharedViewModel);
        binding.setPosHandlers(new PosEventHandlersImpl(this));
        binding.setPosSelectInputHandlers(new PosSelectInputHandlersImpl());
        binding.setPosViewModel(_posViewModel);
        binding.setPosSelectViewModel(_posSelectInputViewModel);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
