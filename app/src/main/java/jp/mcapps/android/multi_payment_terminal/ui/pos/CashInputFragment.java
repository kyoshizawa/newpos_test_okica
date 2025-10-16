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

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentCashInputBinding;

public class CashInputFragment extends BaseFragment {
    private MainApplication _app = MainApplication.getInstance();
    public static CashInputFragment newInstance(){
        return new CashInputFragment();
    }

    private boolean _isFixedAmountPostalOrder = false;

    private final String SCREEN_NAME = "現金支払金額入力";
    private final String SCREEN_NAME_POSTAL_ORDER = "為替類支払金額入力";
    private CashInputViewModel _cashInputViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentCashInputBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_cash_input, container, false);

        // viewModel from viewModelProvider
        _cashInputViewModel = new ViewModelProvider(requireActivity()).get(CashInputViewModel.class);
        _cashInputViewModel.clearDeposit();

        final Bundle args = getArguments();
        if (args != null) {
            _isFixedAmountPostalOrder = args.getBoolean("isFixedAmountPostalOrder", false);
        } else {
            _isFixedAmountPostalOrder = false;
        }

        _cashInputViewModel.setIsFixedAmountPostalOrder(_isFixedAmountPostalOrder);

        // data binding
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setViewModel(_cashInputViewModel);
        binding.setHandlers(new CashInputHandlersImpl());

        if(_isFixedAmountPostalOrder){
            ScreenData.getInstance().setScreenName(SCREEN_NAME_POSTAL_ORDER);
        } else {
            ScreenData.getInstance().setScreenName(SCREEN_NAME);
        }

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        if (AppPreference.isPosTransaction()) {
            _cashInputViewModel.fetch();
        }

        if (AppPreference.isTicketTransaction()) {
            _cashInputViewModel.setTotalPrice(Amount.getTotalAmount());
        }
        super.onResume();
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
