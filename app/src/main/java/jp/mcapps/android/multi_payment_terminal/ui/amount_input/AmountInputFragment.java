package jp.mcapps.android.multi_payment_terminal.ui.amount_input;

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
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentAmountInputBinding;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorEventHandlers;

public class AmountInputFragment extends BaseFragment implements AmountInputEventHandlers {
    private final String SCREEN_NAME = "料金入力";
    public static AmountInputFragment newInstance() {
        return new AmountInputFragment();
    }
    private AmountInputViewModel _amountInputViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentAmountInputBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_amount_input, container, false);

        _amountInputViewModel =
                new ViewModelProvider(this, MainApplication.getViewModelFactory()).get(AmountInputViewModel.class);

        binding.setViewModel(_amountInputViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.setHandlers(this);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);
        _amountInputViewModel.fetchMeterCharge();

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

    @Override
    public void onInputNumber(View view, String number) {
        CommonClickEvent.RecordButtonClickOperation(view, false);
        _amountInputViewModel.inputNumber(number);
    }

    @Override
    public void onCorrection(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, false);
        _amountInputViewModel.correct();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onEnter(View view) {
        if (_amountInputViewModel.getChangeAmount().getValue() != null &&  _amountInputViewModel.getChangeAmount().getValue() != 0) {
            CommonErrorDialog dialog = new CommonErrorDialog();
            dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
                @Override
                public void onPositiveClick(String errorCode) {
                    CommonClickEvent.RecordClickOperation("はい", "金額警告", true);
                    fixAmount(view);
                }

                @Override
                public void onNegativeClick(String errorCode) {
                    CommonClickEvent.RecordClickOperation("いいえ", "金額警告", true);
                }

                @Override
                public void onNeutralClick(String errorCode) {

                }

                @Override
                public void onDismissClick(String errorCode) {

                }
            });
            dialog.ShowErrorMessage(getContext(), "2002");
        } else {
            fixAmount(view);
        }
    }

    private void fixAmount(View view) {
        Integer amount = _amountInputViewModel.getFlatRateAmount().getValue() > 0
                ? _amountInputViewModel.getFlatRateAmount().getValue()
                : _amountInputViewModel.getMeterCharge().getValue();
        amount += _amountInputViewModel.getTotalChangeAmount().getValue();
        CommonClickEvent.RecordInputOperation(view, String.valueOf(amount), true);
        _amountInputViewModel.enter();

        view.post(() -> {
            NavigationWrapper.popBackStack(view);
        });
    }

    @Override
    public void onIncrease(View view) {
        CommonClickEvent.RecordInputOperation(view, String.valueOf(_amountInputViewModel.getChangeAmount().getValue()), true);
        _amountInputViewModel.setInputMode(AmountInputViewModel.InputModes.INCREASE);
//        _amountInputViewModel.increase();
    }

    @Override
    public void onDecrease(View view) {
        CommonClickEvent.RecordInputOperation(view, String.valueOf(_amountInputViewModel.getChangeAmount().getValue()), true);
        _amountInputViewModel.setInputMode(AmountInputViewModel.InputModes.DECREASE);
//        _amountInputViewModel.decrease();
    }

    @Override
    public void onFlatRate(View view) {
        CommonClickEvent.RecordInputOperation(view, String.valueOf(_amountInputViewModel.getChangeAmount().getValue()), true);
        _amountInputViewModel.setInputMode(AmountInputViewModel.InputModes.FLAT_RATE);
//        _amountInputViewModel.flatRate();
    }

    @Override
    public void onCancel(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _amountInputViewModel.cancel();
    }

    @Override
    public void onReset(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _amountInputViewModel.reset();
    }

    @Override
    public void onChangeBack(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, false);
        _amountInputViewModel.changeBack();
    }

    @Override
    public void onApply(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, false);
        _amountInputViewModel.apply();
    }
}