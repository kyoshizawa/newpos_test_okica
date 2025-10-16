package jp.mcapps.android.multi_payment_terminal.ui.amount_input;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentAmountInputLt27Binding;
import timber.log.Timber;

public class AmountInputLt27Fragment extends BaseFragment implements AmountInputLt27EventHandlers {
    private final String SCREEN_NAME = "料金入力";
    public static AmountInputLt27Fragment newInstance() {
        return new AmountInputLt27Fragment();
    }

    private AmountInputLt27ViewModel _amountInputLt27ViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentAmountInputLt27Binding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_amount_input_lt27, container, false);

        _amountInputLt27ViewModel =
                new ViewModelProvider(this, MainApplication.getViewModelFactory()).get(AmountInputLt27ViewModel.class);

        binding.setViewModel(_amountInputLt27ViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setHandlers(this);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);
        _amountInputLt27ViewModel.fetchMeterCharge();

        return binding.getRoot();
    }

    @Override
    public void onInputNumber(View view, String number) {
        CommonClickEvent.RecordButtonClickOperation(view, false);
        _amountInputLt27ViewModel.inputNumber(number);
    }

    @Override
    public void onPayment(View view) {
        CommonClickEvent.RecordInputOperation(view, String.valueOf(_amountInputLt27ViewModel.getChangeAmount().getValue()), true);

        if ( _amountInputLt27ViewModel.paymentAmount() ) {
            view.post(() -> NavigationWrapper.popBackStack(view));
        }
    }

    @Override
    public void onCash(View view) {
        CommonClickEvent.RecordInputOperation(view, String.valueOf(_amountInputLt27ViewModel.getChangeAmount().getValue()), true);

        if ( _amountInputLt27ViewModel.cashAmount() ) {
            view.post(() -> NavigationWrapper.popBackStack(view));
        }
    }

    @Override
    public void onReset(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _amountInputLt27ViewModel.reset();
        view.post(() -> NavigationWrapper.popBackStack(view));
    }

    @Override
    public void onChangeBack(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, false);
        _amountInputLt27ViewModel.changeBack();
    }
}
