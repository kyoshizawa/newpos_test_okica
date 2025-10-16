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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentPosCartInputBinding;

// このFragmentは戻るボタンの表示/非表示が条件で変わるのでBaseFragmentを継承しない
public class InputCartFragment extends Fragment{
    public static InputCartFragment newInstance() {
        return new InputCartFragment();
    }
    private final String SCREEN_NAME = "数値入力";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentPosCartInputBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_pos_cart_input, container, false);

        final Bundle args = getArguments();

        assert args != null;
        final String inputCartType = args.getString("input_cart_type");
        final Integer productId = args.getInt("product_id");

        // viewModel from viewModelProvider
        PosViewModel _posViewModel = new ViewModelProvider(requireActivity()).get(PosViewModel.class);
        SharedViewModel _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        InputCartViewModel _inputCartViewModel = new ViewModelProvider(this).get(InputCartViewModel.class);

        if(inputCartType.equals(InputCartTypes.COUNT.toString())) {
            _inputCartViewModel.setInputCartType(InputCartTypes.COUNT);
            // 入力画面の表示初期値
            final int count = args.getInt("count");
            _inputCartViewModel.setOriginalNumber(Integer.toString(count));
        } else if(inputCartType.equals(InputCartTypes.PRICE.toString())) {
            _inputCartViewModel.setInputCartType(InputCartTypes.PRICE);
            // 入力画面の表示初期値
            final int price = args.getInt("price");
            _inputCartViewModel.setOriginalNumber(Integer.toString(price));
        } else {
            _inputCartViewModel.setInputCartType(InputCartTypes.UNKNOWN);
            _inputCartViewModel.setOriginalNumber("0");
        }
        _inputCartViewModel.setInputFragment(this);
        _inputCartViewModel.setId(productId);

        _posViewModel.setHomeVisible(false);
        _posViewModel.setQRScanVisible(false);
        _posViewModel.setSearchVisible(false);
        _posViewModel.setCartConfirmVisible(false);
        _posViewModel.setNavigateUpVisible(true);

        // data binding
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setInputCartViewModel(_inputCartViewModel);
        binding.setSharedViewModel(_sharedViewModel);
        binding.setPosHandlers(new PosEventHandlersImpl(this));
        binding.setPosViewModel(_posViewModel);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
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
