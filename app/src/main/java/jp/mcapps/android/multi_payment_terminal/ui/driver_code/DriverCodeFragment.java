package jp.mcapps.android.multi_payment_terminal.ui.driver_code;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.CurrentRadio;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.driver.DriverDao;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingDao;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentDriverCodeBinding;
import jp.mcapps.android.multi_payment_terminal.error.McPosCenterErrorMap;
//import jp.mcapps.android.multi_payment_terminal.httpserver.events.EventBroker;
import jp.mcapps.android.multi_payment_terminal.service.GetRadioService;
import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorEventHandlers;
import timber.log.Timber;

// このFragmentは戻るボタンの表示/非表示が条件で変わるのでBaseFragmentを継承しない
public class DriverCodeFragment extends Fragment implements DriverCodeEventHandlers {
    public static DriverCodeFragment newInstance() {
        return new DriverCodeFragment();
    }
    private MainApplication _app = MainApplication.getInstance();
    private DriverCodeViewModel _driverCodeViewModel;
    private Handler _handler = new Handler(Looper.getMainLooper());
    private SharedViewModel _sharedViewModel;
    private final String SCREEN_NAME = "係員番号入力";
    private final List<Disposable> _disposables = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentDriverCodeBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_driver_code, container, false);

        _driverCodeViewModel =
                new ViewModelProvider(this, MainApplication.getViewModelFactory()).get(DriverCodeViewModel.class);

        binding.setViewModel(_driverCodeViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.setHandlers(this);

        _driverCodeViewModel.fetchDrivers();
        _driverCodeViewModel.getDriverCode().observe(getViewLifecycleOwner(), binding.textDriverCode::setText);
        _driverCodeViewModel.getDriverName().observe(getViewLifecycleOwner(), binding.textDriverName::setText);

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        final boolean hasBackStack = Navigation.findNavController(
                requireActivity(), R.id.fragment_main_nav_host).getPreviousBackStackEntry() != null;

        _sharedViewModel.setBackVisibleFlag(hasBackStack);

        _sharedViewModel.allowDriverSignIn(true);

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

        // MC認証結果確認
        if (CreditSettlement.getInstance()._mcCenterCommManager.getTerOpePort() > 0 || AppPreference.isDemoMode()) {
            ArrayList<Long> slipIds = AppPreference.getPrepaidSlipId();
            if (slipIds != null && slipIds.size() > 0) {
                // メインメニューへ遷移
                requireActivity().runOnUiThread(() -> {
                    NavigationWrapper.navigate(view, R.id.action_navigation_driver_code_to_navigation_redirect_to_menu);
                });
            }

            /* MC認証に成功した場合、またはデモモードの場合（MC認証結果確認は必要ない）*/
            // 端末内部保持データの係員番号入力画面の表示確認
            if (AppPreference.isDriverCodeInput()) {
                if (AppPreference.getTabletLinkInfo() != null) {
//
//                    _driverCodeViewModel.getTabletSignedInDriver()
//                            .subscribeOn(Schedulers.io())
//                            .doOnSubscribe(_disposables::add)
//                            .doOnSubscribe(d -> {
//                                _sharedViewModel.setLoading(true);
//                            })
//                            .doFinally(() -> {
//                                _sharedViewModel.setLoading(false);
//                            })
//                            .observeOn(AndroidSchedulers.mainThread())
//                            .subscribe((d, e) -> {
//                                if (e != null) {
//                                    Timber.e(e);
////                                    EventBroker.signIn
////                                            .subscribeOn(Schedulers.newThread())
////                                            .doOnSubscribe(_disposables::add)
////                                            .observeOn(AndroidSchedulers.mainThread())
////                                            .subscribe(signIn -> {
////                                                _sharedViewModel.setLoading(true);
////
////                                                _driverCodeViewModel.setDriverCode(signIn.driverCode);
////                                                _driverCodeViewModel.setDriverName(signIn.driverName);
////                                                _driverCodeViewModel.isTabletLinkSuccess(true);
////                                                onEnter(this.getView());
////
////                                                _sharedViewModel.setLoading(false);
////                                            });
//                                    return;
//                                }
//
//                                _driverCodeViewModel.setDriverCode(d.code);
//                                _driverCodeViewModel.setDriverName(d.name);
//                                _driverCodeViewModel.isTabletLinkSuccess(true);
//                                onEnter(this.getView());
//                            });
//                }
//                // 表示設定の場合、HOME画面へ遷移させない（係員番号入力画面を表示）
//            }else{
//                // 非表示設定の場合、HOME画面へ遷移させる（係員番号入力画面を非表示）
//                requireActivity().runOnUiThread(() -> {
//                    NavigationWrapper.navigate(view, R.id.action_navigation_driver_code_to_navigation_redirect_to_menu);
//                });
//            }
//        }else{
//            // MC認証に失敗した場合、エラー設定
//            AppPreference.setMcCarId(_app.getResources().getInteger(R.integer.setting_default_mc_driverid));
//            if (CurrentRadio.getImageLevel() == GetRadioService.AIRPLANE_MODE) {
//                // 機内モード状態の場合、機内モードのエラー設定
//                _app.setErrorCode(_app.getString(R.string.error_type_airplane_mode));
//            } else {
//                _app.setErrorCode(_app.getString(R.string.error_type_payment_system_2094));
//            }
//            // HOME画面へ遷移させる（係員番号入力画面を非表示）
//            requireActivity().runOnUiThread(() -> {
//                NavigationWrapper.navigate(view, R.id.action_navigation_driver_code_to_navigation_redirect_to_menu);
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        _sharedViewModel.allowDriverSignIn(false);

        for (Disposable d : _disposables) {
            if (!d.isDisposed()) {
                d.dispose();
            }
        }
        _disposables.clear();
    }

    @Override
    public void onInputNumber(String number) {
        CommonClickEvent.RecordClickOperation(number, false);
        _driverCodeViewModel.inputNumber(number);
    }

    @Override
    public void onCorrection(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, false);
        _driverCodeViewModel.correct();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onEnter(View view) {
        CommonClickEvent.RecordInputOperation(view, _driverCodeViewModel.getDriverCode().getValue(), true);
        if (_driverCodeViewModel.getDriverCode().getValue().equals("")) {
            CommonErrorDialog dialog = new CommonErrorDialog();
            dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
                @Override
                public void onPositiveClick(String errorCode) {
                    CommonClickEvent.RecordClickOperation("はい", "係員番号未入力", true);
                    if (AppPreference.isDriverIdHistory() && _driverCodeViewModel.isHistoryExists()) {
                        final DriverDao.Driver driver = _driverCodeViewModel.getLatestDriver();
                        AppPreference.setDriverCode(driver.code);
                        AppPreference.setDriverName(driver.name);

                        view.post(() -> {
                            NavigationWrapper.navigate(view, R.id.action_navigation_driver_code_to_navigation_redirect_to_menu);
                        });
                    }
                }

                @Override
                public void onNegativeClick(String errorCode) {
                    CommonClickEvent.RecordClickOperation("いいえ", "係員番号未入力", true);
                }

                @Override
                public void onNeutralClick(String errorCode) {

                }

                @Override
                public void onDismissClick(String errorCode) {

                }
            });

            final String errorCode = AppPreference.isDriverIdHistory() && _driverCodeViewModel.isHistoryExists()
                    ? "2011"
                    : "2010";

            dialog.ShowErrorMessage(requireActivity(), errorCode);

            return;
        }

        new Thread(() -> {
            if (!AppPreference.isDemoMode()) {
                _sharedViewModel.setLoading(true);
                final String errorCode = _driverCodeViewModel.enter();
                _sharedViewModel.setLoading(false);

                if (errorCode != null) {
                    _handler.post(() -> {
                        final CommonErrorDialog dialog = new CommonErrorDialog();
                        if(errorCode.equals(getString(R.string.error_type_comm_reception))
                        || errorCode.equals(getString(R.string.error_type_payment_system_2012))) {
                            dialog.ShowErrorMessage(requireActivity(), errorCode);
                        } else {
                            dialog.ShowErrorMessage(requireActivity(), McPosCenterErrorMap.get(errorCode));
                        }
                    });

                    return;
                }
            }

            final ErrorStackingDao errorStackingDao = DBManager.getErrorStackingDao();
            final ErrorStackingData errorStackingData = errorStackingDao.getErrorStackingData(
                    _app.getString(R.string.error_type_payment_system_2015));

            if (errorStackingData != null) {
                errorStackingDao.deleteErrorStackingData(errorStackingData.id);
            }

            view.post(() -> {
                NavigationWrapper.navigate(view, R.id.action_navigation_driver_code_to_navigation_redirect_to_menu);
            });
        }).start();
    }

    @Override
    public void onPrevClick(View view) {
        _driverCodeViewModel.prevDriver();
    }

    @Override
    public void onNextClick(View view) {
        _driverCodeViewModel.nextDriver();
    }

}