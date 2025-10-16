package jp.mcapps.android.multi_payment_terminal.ui.car_id;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentCarIdBinding;
import jp.mcapps.android.multi_payment_terminal.error.McPosCenterErrorMap;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorEventHandlers;
import jp.mcapps.android.multi_payment_terminal.ui.installation_and_removal.InstallationAndRemovalFragment;

public class CarIdFragment extends BaseFragment implements CarIdEventHandlers {
    private static final String SCREEN_NAME = "号機番号入力";

    public static CarIdFragment newInstance() {
        return new CarIdFragment();
    }
    private CarIdViewModel _carIdViewModel;
    private SharedViewModel _sharedViewModel;
    private Handler _handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentCarIdBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_car_id, container, false);

        _carIdViewModel = new ViewModelProvider(this).get(CarIdViewModel.class);

        binding.setViewModel(_carIdViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.setHandlers(this);

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

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

    @Override
    public void onInputNumber(String number) {
        CommonClickEvent.RecordClickOperation(number, false);
        _carIdViewModel.inputNumber(number);
    }

    @Override
    public void onCorrection(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, false);
        _carIdViewModel.correct();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onEnter(View view) {
        CommonClickEvent.RecordInputOperation(view, _carIdViewModel.getCarId().getValue(), true);
        if (_carIdViewModel.getCarId().getValue().equals("")) {
            CommonErrorDialog dialog = new CommonErrorDialog();
            dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
                @Override
                public void onPositiveClick(String errorCode) {
                    CommonClickEvent.RecordClickOperation("はい", "号機番号未入力", true);
                    if (AppPreference.getMcCarId() != R.integer.setting_default_mc_carid) {

                        view.post(() -> {
                            NavigationWrapper.popBackStack(view);
                        });
                    }
                }

                @Override
                public void onNegativeClick(String errorCode) {
                    CommonClickEvent.RecordClickOperation("いいえ", "号機番号未入力", true);
                }

                @Override
                public void onNeutralClick(String errorCode) {

                }

                @Override
                public void onDismissClick(String errorCode) {

                }
            });

            final String errorCode = AppPreference.getMcCarId() != R.integer.setting_default_mc_carid
                    ? "2021"
                    : "2020";


            dialog.ShowErrorMessage(requireActivity(), errorCode);

            return;
        }

        new Thread(() -> {
            _sharedViewModel.setLoading(true);
            final String errorCode = _carIdViewModel.enter();
            _sharedViewModel.setLoading(false);

            if (errorCode != null) {
                _handler.post(() -> {
                    final CommonErrorDialog dialog = new CommonErrorDialog();
                    if(errorCode.equals(getString(R.string.error_type_comm_reception))) {
                        dialog.ShowErrorMessage(requireActivity(), errorCode);
                    } else {
                        dialog.ShowErrorMessage(requireActivity(), McPosCenterErrorMap.get(errorCode));
                    }
                });
                return;
            }

            final Bundle args = getArguments();

            final Bundle params = new Bundle();
            params.putBoolean("showSuccessDialog", true);
            final String destination = args.getString("destination");

            if (destination != null) {
                if (destination.equals(InstallationAndRemovalFragment.class.getName())) {
                    view.post(() -> {
                        NavigationWrapper.navigate(
                                view, R.id.action_navigation_car_id_to_navigation_installation_and_removal, params);
                    });
                    return;
                }
            }

            view.post(() -> {
                NavigationWrapper.popBackStack(view);
            });
        }).start();
    }
}