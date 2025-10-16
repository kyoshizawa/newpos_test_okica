package jp.mcapps.android.multi_payment_terminal.ui.pos;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.database.pos.IncludedTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReducedTaxTypes;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentPosCartManualInputBinding;
import jp.mcapps.android.multi_payment_terminal.ui.menu.MenuViewModel;

public class CartManualInputFragment extends PosBaseFragment {
    public static CartManualInputFragment newInstance(){
        return new CartManualInputFragment();
    }
    private MenuViewModel _menuViewModel;
    private final String SCREEN_NAME = "明細追加画面";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final FragmentPosCartManualInputBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_pos_cart_manual_input, container, false);


        // viewModel from viewModelProvider
        CartManualInputViewModel _cartManualInputViewModel = new ViewModelProvider(requireActivity()).get(CartManualInputViewModel.class);
        PosViewModel posViewModel = new ViewModelProvider(requireActivity()).get(PosViewModel.class);
        SharedViewModel sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        // data binding
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setPosViewModel(posViewModel);
        binding.setPosHandlers(new PosEventHandlersImpl(this));
        binding.setCartManualInputViewModel(_cartManualInputViewModel);
        binding.setCartManualInputHandlers(new CartManualInputHandlersImpl());
        binding.setSharedViewModel(sharedViewModel);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        final Bundle args = getArguments();
        if(args != null){
            String productTaxTypeKey = args.getString("productTaxType");
            String reducedTaxTypeKey = args.getString("reducedTaxType");
            String includedTaxTypeKey= args.getString("includedTaxType");
            if(productTaxTypeKey != null){
                _cartManualInputViewModel.setProductTaxType(ProductTaxTypes.fromKey(productTaxTypeKey));
            }
            if(reducedTaxTypeKey != null){
                _cartManualInputViewModel.setReducedTaxType(ReducedTaxTypes.fromKey(reducedTaxTypeKey));
            }
            if(includedTaxTypeKey != null){
                _cartManualInputViewModel.setIncludedTaxType(IncludedTaxTypes.fromKey(includedTaxTypeKey));
            }
        }
        _cartManualInputViewModel.setUnitPrice(-1);

        return binding.getRoot();
    }
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
