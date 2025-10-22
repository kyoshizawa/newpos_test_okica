package jp.mcapps.android.multi_payment_terminal.ui.menu;
//
//import static jp.mcapps.android.multi_payment_terminal.model.IFBoxManager.exitManualModeDisposable;
//import static jp.mcapps.android.multi_payment_terminal.model.IFBoxManager.printEndManualDisposable;
import static jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl.FORCE_DEACT_END;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.databinding.BindingAdapter;
import androidx.databinding.BindingConversion;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.gson.Gson;

import org.json.JSONException;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.data.EmoneyOpeningInfo;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.MoneyType;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorData;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentMenuBinding;
import jp.mcapps.android.multi_payment_terminal.error.ErrorManage;
import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorCodes;
import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorMap;
//import jp.mcapps.android.multi_payment_terminal.iCAS.BusinessParameter;
//import jp.mcapps.android.multi_payment_terminal.iCAS.IiCASClient;
//import jp.mcapps.android.multi_payment_terminal.iCAS.data.DeviceClient;
//import jp.mcapps.android.multi_payment_terminal.iCAS.iCASClient;
import jp.mcapps.android.multi_payment_terminal.model.McTerminal;
import jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl;
import jp.mcapps.android.multi_payment_terminal.model.OptionalTransFacade;
import jp.mcapps.android.multi_payment_terminal.model.TransLogger;
import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement;
import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlementAdapter;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterProc;
import jp.mcapps.android.multi_payment_terminal.ui.Converters;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.SuccessDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorEventHandlers;
//import jp.mcapps.android.multi_payment_terminal.ui.pos.PosEventHandlers;
//import jp.mcapps.android.multi_payment_terminal.ui.pos.PosEventHandlersImpl;
//import jp.mcapps.android.multi_payment_terminal.ui.pos.PosViewModel;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Version;
import timber.log.Timber;

public class MenuFragment extends Fragment /*implements IiCASClient*/ {
    private MainApplication _app = MainApplication.getInstance();
//    private iCASClient _icasClient = null;
    private int _recoveryRetryCount = 0;
    private SharedViewModel _sharedViewModel;
//    private PosViewModel _appBar;
    private final Handler _handler = new Handler(Looper.getMainLooper());
    private Gson _gson = new Gson();
    private TransLogger _transLogger;
    private MenuViewModel _menuViewModel;
    private SlipData _slipData;

    private final PublishSubject<Boolean> _exitManualMode = PublishSubject.create();

    private NavController.OnDestinationChangedListener destListener = null;

    public static MenuFragment newInstance() {
        return new MenuFragment();
    }


    // サインの案内用
    private CreditSettlementAdapter creditListener =  new CreditSettlementAdapter() {};
    //POSメニュー用
//    private PosEventHandlers _posEventHandlers;
//    private PosViewModel _posViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        _menuViewModel = new ViewModelProvider(this, MainApplication.getViewModelFactory()).get(MenuViewModel.class);
        _menuViewModel.setDriverInfoVisibility(AppPreference.isDriverCodeInput());
        _menuViewModel.setDriverName(AppPreference.getDriverName());
        _menuViewModel.setCarIdVisibility(AppPreference.isCarIdInput());

        _menuViewModel.getBodyType().observe(getViewLifecycleOwner(), value -> {
            final MenuHeadActions headAction = _menuViewModel.getHeadAction();
            if (headAction == MenuHeadActions.NONE) return;

            if (headAction == MenuHeadActions.HIDE) {
                requireActivity().runOnUiThread(() -> {
                    NavigationWrapper.navigate(getActivity(), R.id.fragment_menu_head_nav_host,
                            R.id.action_navigation_menu_head_to_navigation_menu_head_empty);
                });
            } else if (headAction == MenuHeadActions.SHOW) {
                requireActivity().runOnUiThread(() -> {
                    NavigationWrapper.navigate(getActivity(), R.id.fragment_menu_head_nav_host,
                            R.id.action_navigation_menu_head_empty_to_navigation_menu_head);
                });
            }
        });

        final FragmentMenuBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_menu, container, false);


        if(binding.fragmentMenuBodyNavHost == null) {
            Log.d("-----------", "binding.fragmentMenuBodyNavHost == null");
        }else{
            Log.d("-----------", "binding.fragmentMenuBodyNavHost != null");
        }


        Log.d("---------",String.valueOf(binding.fragmentMenuBodyNavHost.getTag()));


        List<Fragment> a= getChildFragmentManager().getFragments();
        NavHostFragment navHostBody = (NavHostFragment)getChildFragmentManager().findFragmentById(R.id.fragment_menu_body_nav_host);
        assert navHostBody != null;

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
//        _posEventHandlers = new PosEventHandlersImpl(this , navHostBody);
//        _posViewModel = new ViewModelProvider(requireActivity()).get(PosViewModel.class);
//        _posViewModel.setNavigateUpVisible(true);

        // POS用アクションバーの色を変更する
        destListener = (controller, destination, arguments) -> {
            final String resourceEntryName = getResources().getResourceEntryName(destination.getId());
            Timber.d("body navigation onDestinationChanged: %s", resourceEntryName);

//            Toolbar toolbar = binding.appPosBar.toolbarPos;
//            if (destination.getId() == R.id.navigation_menu_balance) {
//                // 残高照会
//                toolbar.setBackground(ContextCompat.getDrawable(requireContext(), R.color.menu_balance));
//            } else if (destination.getId() == R.id.navigation_menu_charge) {
//                // チャージ
//                toolbar.setBackground(ContextCompat.getDrawable(requireContext(), R.color.menu_charge));
//            } else {
//                // その他
//                toolbar.setBackground(ContextCompat.getDrawable(requireContext(), R.color.primary));
//            }
        };
        navHostBody.getNavController().addOnDestinationChangedListener(destListener);

        applyCashMenu();

//        binding.setPosHandlers(_posEventHandlers);
//        binding.setPosViewModel(_posViewModel);
        binding.setSharedViewModel(_sharedViewModel);

        return binding.getRoot();
    }

    private void applyCashMenu() {
//        if (_posViewModel == null) return;
//        //表示の設定
//        final Bundle args = getArguments();
//        boolean cashMenu = false;
//
//        // QR券の発行画面から中止
//        if (_sharedViewModel.getShowBarTicketIssueCancel().get()) {
//            _sharedViewModel.setShowBarTicketIssueCancel(false);
//            _posViewModel.setNavigateUpVisible(false);
//            return;
//        }
//
//        //POSの方の決済画面かどうか
//        if(args != null){
//            Timber.v("getArguments() is not null: %s", args);
//            cashMenu = args.getBoolean("cashMenu");
//            _posViewModel.setNavigateUpVisible(true);
//        }else {
//            Timber.v("getArguments() is not null");
//            _posViewModel.setNavigateUpVisible(false);
//        }
//        _sharedViewModel.setCashMenu(cashMenu);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Timber.v("onViewCreated: %s", this.hashCode());

        _sharedViewModel.setScreenMode(SharedViewModel.ScreenMode.MAIN);

        Timber.d("businessId: %s", AppPreference.getConfirmObstacleBusinessid());
        Timber.d("sid: %s", AppPreference.getConfirmObstacleSid());
        // 初回表示のみアプリの正常終了確認をチェックする
        AppPreference.setPoweroffTrans(false);

        if (!_app.isAppFinishCheck()) {
            final boolean doReprint = !AppPreference.getConfirmNormalEmoney()
                    && AppPreference.getConfirmObstacleBusinessid() >= 0
                    && AppPreference.getConfirmObstacleSid() != null
                    && getEmoneyBrand() != null;

            if (doReprint) {
                final CommonErrorDialog dialog = new CommonErrorDialog();

                final String brand = getEmoneyBrand();
                String errorCode;

                /**/ if (brand.equals(_app.getString(R.string.money_brand_suica))) {
                    errorCode = EmoneyOpeningInfo.getSuica() != null
                            ? _app.getString(R.string.error_type_suica_power_off_error)
                            : _app.getString(R.string.error_type_abnormal_shutdown);
                }
                else if (brand.equals(_app.getString(R.string.money_brand_id))) {
                    errorCode = EmoneyOpeningInfo.getId() != null
                            ? _app.getString(R.string.error_type_id_power_off_error)
                            : _app.getString(R.string.error_type_abnormal_shutdown);
                }
                else if (brand.equals(_app.getString(R.string.money_brand_waon))) {
                    errorCode = EmoneyOpeningInfo.getWaon() != null
                            ? _app.getString(R.string.error_type_waon_power_off_error)
                            : _app.getString(R.string.error_type_abnormal_shutdown);
                }
                else if (brand.equals(_app.getString(R.string.money_brand_nanaco))) {
                    errorCode = EmoneyOpeningInfo.getNanaco() != null
                            ? _app.getString(R.string.error_type_nanaco_power_off_error)
                            : _app.getString(R.string.error_type_abnormal_shutdown);
                }
                else if (brand.equals(_app.getString(R.string.money_brand_edy))) {
                    errorCode = EmoneyOpeningInfo.getEdy() != null
                            ? _app.getString(R.string.error_type_edy_power_off_error)
                            : _app.getString(R.string.error_type_abnormal_shutdown);
                }
                else if (brand.equals(_app.getString(R.string.money_brand_qp))) {
                    errorCode = EmoneyOpeningInfo.getQuicpay() != null
                            ? _app.getString(R.string.error_type_quicpay_power_off_error)
                            : _app.getString(R.string.error_type_abnormal_shutdown);
                }
                else {
                    errorCode = _app.getString(R.string.error_type_abnormal_shutdown);
                    powerOffTransResultFinish();
                }

                if (!errorCode.equals(_app.getString(R.string.error_type_abnormal_shutdown))) {

                    dialog.setCommonErrorEventHandlers(new CommonErrorEventHandlers() {
                        @Override
                        public void onPositiveClick(String errorCode) {
                            CommonClickEvent.RecordClickOperation("はい", "電マネ復旧", true);
                            AppPreference.setPoweroffTrans(true);
                            powerOffTransResult();
                        }

                        @Override
                        public void onNegativeClick(String errorCode) {
                            Timber.e("negative");
                        }

                        @Override
                        public void onNeutralClick(String errorCode) {
                        }

                        @Override
                        public void onDismissClick(String errorCode) {
                            Timber.e("dismiss");
                        }
                    });
                }

                dialog.ShowErrorMessage(requireContext(), errorCode);
            }
            else if (!AppPreference.getConfirmNormalShutdown() && AppPreference.getPrevAppVersionCode() == BuildConfig.VERSION_CODE) {
                //異常終了検知 更新後の再起動は正しい挙動の為エラー表示しない
                final CommonErrorDialog dialog = new CommonErrorDialog();
                dialog.ShowErrorMessage(requireContext(), _app.getString(R.string.error_type_abnormal_shutdown));
            }

            // OKICA強制撤去受信
            if(OkicaMasterControl.force_deactivation_stat == FORCE_DEACT_END) {
                final CommonErrorDialog dialog = new CommonErrorDialog();
                dialog.ShowErrorMessage(requireContext(), _app.getString(R.string.error_type_okica_force_deactivation));
            }

            AppPreference.setPrevAppVersionCode();
            AppPreference.setConfirmNormalShutdown(false);
            _app.setAppFinishCheck(true);
        }

        // 現金併用時の現金受け取り額を表示
        if (_app.getCashValue() > 0) {
            //ADD-S BMT S.Oyama 2025/03/05 フタバ双方向向け改修
            String tmpCashMesStr = "";
            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {
                tmpCashMesStr = "残金があります。\n領収書を印刷します。\n残金 %s円";
            }
            else {
                tmpCashMesStr = "以下の金額を\n現金で頂いて下さい。\n%s円";
            }
            //ADD-E BMT S.Oyama 2025/03/05 フタバ双方向向け改修
            final AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                    .setMessage(String.format(tmpCashMesStr, Converters.integerToNumberFormat(_app.getCashValue())))
                    .setCancelable(false)
                    .setPositiveButton("はい", (dialog, which) -> {
                        final Bundle args = getArguments();
                        final int slipId = args.getInt("slipId");
                        if (slipId != 0) {
//                            exitManualMode(true, slipId);
                            printTrans(slipId);
                        } else {
                            // 画面遷移先の戻り値を取得して印刷する
                            NavBackStackEntry backStackEntry = NavHostFragment.findNavController(this).getCurrentBackStackEntry();
                            if (backStackEntry != null && backStackEntry.getSavedStateHandle().contains("resultSlipId")) {
                                final Integer resultSlipId = backStackEntry.getSavedStateHandle().get("resultSlipId");
                                backStackEntry.getSavedStateHandle().remove("resultSlipId");
                                if (resultSlipId != null) {
//                                    exitManualMode(true, resultSlipId);
                                    printTrans(resultSlipId);
                                }
                            }
                        }
                        // Bundleの値は保持されたままで他の画面から戻るたびに何回も印刷されるのでクリアする
                        args.putInt("slipId", 0);
                    });

            final AlertDialog alertDialog = builder.create();
            alertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

            alertDialog.show();

            TextView message = alertDialog.findViewById(android.R.id.message);
            message.setTextSize(24);

            // これをshow()の前でやるとエラーになる
            alertDialog.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            );

            alertDialog.getWindow().
                    clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

            _app.setCashValue(0);
        } else {
            final Bundle args = getArguments();
            final int slipId = args.getInt("slipId");
            if (slipId != 0) {
//                exitManualMode(true, slipId);
                printTrans(slipId);
            } else {
                // 画面遷移先の戻り値を取得して印刷する
                NavBackStackEntry backStackEntry = NavHostFragment.findNavController(this).getCurrentBackStackEntry();
                if (backStackEntry != null && backStackEntry.getSavedStateHandle().contains("resultSlipId")) {
                    final Integer resultSlipId = backStackEntry.getSavedStateHandle().get("resultSlipId");
                    backStackEntry.getSavedStateHandle().remove("resultSlipId");
                    if (resultSlipId != null) {
//                        exitManualMode(true, resultSlipId);
                        printTrans(resultSlipId);
                    }
                }
            }
            // Bundleの値は保持されたままで他の画面から戻るたびに何回も印刷されるのでクリアする
            args.putInt("slipId", 0);

            final String errorCode = _app.getErrorCode();
            if (errorCode != null) {
                if (errorCode == _app.getString(R.string.error_type_okica_common_judge_nega_check_error)) {
                    // OKICAネガヒット時の売上送信
                    new Thread(() -> {
                        if (AppPreference.isOkicaCommunicationAvailable()) {
                            String mcTerminalErrCode = new McTerminal().postOkicaPayment();
                        }
                    }).start();
                }
                final CommonErrorDialog dialog = new CommonErrorDialog();
                dialog.ShowErrorMessage(requireContext(), errorCode);

                _app.setErrorCode(null);
            }
        }

        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL)) {
//            if (printEndManualDisposable == null) {
//                printEndManualDisposable = _menuViewModel.getPrintEndManual().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(flg -> {
//
//                    AlertDialog dialog = new AlertDialog.Builder(getContext())
//                            .setTitle("手動決済モード終了")
//                            .setMessage("双方向決済モードに切り替わりました。")
//                            .setPositiveButton("確認", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialogInterface, int i) {
//                                    Version.Response ifboxVersionInfo = AppPreference.getIFBoxVersionInfo();
//                                    ifboxVersionInfo.appModel = IFBoxAppModels.FUTABA_D;
//                                    AppPreference.setIFBoxVersionInfo(ifboxVersionInfo);
//                                    _sharedViewModel.setUpdatedFlag(true);
//                                    AppPreference.setIsTemporaryManualMode(false);
//
//                                    Disposable chkDisp = _menuViewModel.checkMeterCharge()
//                                            .subscribeOn(Schedulers.io())
//                                            .observeOn(AndroidSchedulers.mainThread())
//                                            .subscribe();
//                                    disposables.add(chkDisp);
//                                }
//                            })
//                            .create();
//
//                    dialog.setCanceledOnTouchOutside(false);
//                    dialog.show();
//                });
//            }
        }
    }

    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Timber.v("onDestroyView: %s", this.hashCode());

        final CreditSettlement creditSettlement = CreditSettlement.getInstance();
        if (creditSettlement.isSameListener(creditListener)) {
            creditSettlement.setListener(null);
        }
        if (destListener != null) {
            NavHostFragment navHostBody = (NavHostFragment)getChildFragmentManager().findFragmentById(R.id.fragment_menu_body_nav_host);
            assert navHostBody != null;

            navHostBody.getNavController().removeOnDestinationChangedListener(destListener);
        }

//        if (printEndManualDisposable != null) {
//            printEndManualDisposable.dispose();
//            printEndManualDisposable = null;
//        }

        disposables.clear();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.v("onCreate");
    }

    @Override
    public void onStart() {
        super.onStart();
        Timber.v("onStart");
    }

    @Override
    public void onStop() {
        super.onStop();
        Timber.v("onStop");
        _sharedViewModel.setCashMenu(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.v("onDestroy: %s", this.hashCode());
    }




    private void powerOffTransResult() {
        Timber.d("businessId: %s", AppPreference.getConfirmObstacleBusinessid());
        Timber.d("sid: %s", AppPreference.getConfirmObstacleSid());

        // 現在のモードが前回と同じ前提で設定する
        if (AppPreference.isServicePos()) {
            AppPreference.setTransactionTerminalType(AppPreference.TerminalType.Pos.ordinal());
        } else if (AppPreference.isServiceTicket()) {
            AppPreference.setTransactionTerminalType(AppPreference.TerminalType.Ticket.ordinal());
        }

        final int businessId = AppPreference.getConfirmObstacleBusinessid();
        final String sid = AppPreference.getConfirmObstacleSid();
        AppPreference.setConfirmAmount();
        Amount.fix();
//
//        final BusinessParameter businessParameter = new BusinessParameter();
//
//        switch (businessId) {
//            case iCASClient.BUSINESS_ID_SUICA_PAY:
//            case iCASClient.BUSINESS_ID_ID_PAY:
//            case iCASClient.BUSINESS_ID_WAON_PAY:
//            case iCASClient.BUSINESS_ID_NANACO_PAY:
//            case iCASClient.BUSINESS_ID_QUICPAY_PAY:
//            case iCASClient.BUSINESS_ID_EDY_PAY:
//                _app.setBusinessType(BusinessType.PAYMENT);
//                break;
//
//            case iCASClient.BUSINESS_ID_SUICA_REFUND:
//            case iCASClient.BUSINESS_ID_ID_REFUND:
//            case iCASClient.BUSINESS_ID_WAON_REFUND:
//            case iCASClient.BUSINESS_ID_QUICPAY_REFUND:
//                _app.setBusinessType(BusinessType.REFUND);
//                break;
//            default:
//                powerOffTransResultFinish();
//                return;
//        }
//
//        switch (businessId) {
//            case iCASClient.BUSINESS_ID_SUICA_PAY:
//            case iCASClient.BUSINESS_ID_SUICA_REFUND:
//                final BusinessParameter.Suica suica = new BusinessParameter.Suica();
//                businessParameter.money = suica;
//                businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_SUICA_STATUS_REPLY);
//                suica.oldSid = sid;
//                break;
//
//            case iCASClient.BUSINESS_ID_ID_PAY:
//            case iCASClient.BUSINESS_ID_ID_REFUND:
//                final BusinessParameter.iD iD = new BusinessParameter.iD();
//                businessParameter.money = iD;
//                businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_ID_STATUS_REPLY);
//                iD.oldSid = sid;
//                iD.training = "OFF";
//                break;
//
//            case iCASClient.BUSINESS_ID_WAON_PAY:
//            case iCASClient.BUSINESS_ID_WAON_REFUND:
//                final BusinessParameter.Waon waon = new BusinessParameter.Waon();
//                businessParameter.money = waon;
//                businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_WAON_STATUS_REPLY);
//                waon.oldSid = sid;
//                waon.training = "OFF";
//                break;
//
//            case iCASClient.BUSINESS_ID_EDY_PAY:
//                final BusinessParameter.Edy edy = new BusinessParameter.Edy();
//                businessParameter.money = edy;
//                businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_EDY_STATUS_REPLY);
//                edy.oldSid = sid;
//                edy.training = "OFF";
//                break;
//
//            case iCASClient.BUSINESS_ID_QUICPAY_PAY:
//            case iCASClient.BUSINESS_ID_QUICPAY_REFUND:
//                final BusinessParameter.QUICPay qp = new BusinessParameter.QUICPay();
//                businessParameter.money = qp;
//                businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_QUICPAY_STATUS_REPLY);
//                qp.oldSid = sid;
//                qp.training = "OFF";
//                break;
//
//            case iCASClient.BUSINESS_ID_NANACO_PAY:
//                final BusinessParameter.nanaco nanaco = new BusinessParameter.nanaco();
//                businessParameter.money = nanaco;
//                businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_NANACO_PREV_TRAN);
//                nanaco.oldSid = sid;
//                nanaco.training = "OFF";
//                break;
//
//            default:
//                powerOffTransResultFinish();
//                return;
//        }
//
//        try {
//            _transLogger = new TransLogger();
//            _sharedViewModel.setLoading(true);
//            _icasClient = iCASClient.getInstance();
//            _icasClient.SetRWUIEventListener(this);
//            _icasClient.OnStart(businessParameter);
//        } catch (JSONException | IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException | InterruptedException e) {
//            powerOffTransResultFinish();
//            Timber.e(e);
//        }
    }

    private void powerOffTransResultFinish() {
        AppPreference.setConfirmNormalEmoney(true);
        AppPreference.setConfirmObstacleBusinessid(-1);
        AppPreference.setConfirmObstacleSid(null);
        AppPreference.removeConfirmAmount();
        AppPreference.setPoweroffTrans(false);
        //_icasClient.SetRWUIEventListener(null);
        _sharedViewModel.setLoading(false);
    }

    private String getEmoneyBrand() {
        final int businessId = AppPreference.getConfirmObstacleBusinessid();
        switch (businessId) {
//            case iCASClient.BUSINESS_ID_SUICA_PAY:
//            case iCASClient.BUSINESS_ID_SUICA_REFUND:
//                return _app.getString(R.string.money_brand_suica);
//
//            case iCASClient.BUSINESS_ID_ID_PAY:
//            case iCASClient.BUSINESS_ID_ID_REFUND:
//                return _app.getString(R.string.money_brand_id);
//
//            case iCASClient.BUSINESS_ID_WAON_PAY:
//            case iCASClient.BUSINESS_ID_WAON_REFUND:
//                return _app.getString(R.string.money_brand_waon);
//
//            case iCASClient.BUSINESS_ID_EDY_PAY:
//                return _app.getString(R.string.money_brand_edy);
//
//            case iCASClient.BUSINESS_ID_QUICPAY_PAY:
//            case iCASClient.BUSINESS_ID_QUICPAY_REFUND:
//                return _app.getString(R.string.money_brand_qp);
//
//            case iCASClient.BUSINESS_ID_NANACO_PAY:
//                return _app.getString(R.string.money_brand_nanaco);

            default:
                powerOffTransResultFinish();
                return null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //画面オフを有効化
        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //端末ログ送信
//        new Thread(() -> _menuViewModel.getEventLogger().submit()).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void printTrans(int id) {
        //伝票印刷
        CreditSettlement.getInstance().setListener(creditListener);
        final PrinterManager printerManager = PrinterManager.getInstance();
        printerManager.print_trans(getView(), id);

        //MC認証成功している場合は売上送信と疎通確認 エラーは無視する
        if (_app.isMcAuthSuccess()) {
            new Thread(() -> {
                final McTerminal mcTerminal = new McTerminal();
                mcTerminal.postPayment();
                mcTerminal.echo();
                // OKICA売上送信
                if (AppPreference.isOkicaCommunicationAvailable()) {
                    String mcTerminalErrCode = new McTerminal().postOkicaPayment();
                }

                if (AppPreference.isServiceTicket()) {
                    mcTerminal.postTicketCancel();
                }
            }).start();
        } else {
            new Thread(() -> {
                // OKICA売上送信
                if (AppPreference.isOkicaCommunicationAvailable()) {
                    String mcTerminalErrCode = new McTerminal().postOkicaPayment();
                }
            }).start();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void insertErrorHistory(String code) {
        if (code == null) return;

        String errorCode = JremRasErrorMap.get(Integer.parseInt(code)); //MCエラーコードに変換

        // 詳細エラーコードを取り出す(未定義エラーの場合に付与)
        // <MCエラーコード>@@@<詳細エラーコード>@@@の形式
        final Pattern detailPtn = Pattern.compile("@@@.*@@@$");
        final Matcher matcher = detailPtn.matcher(errorCode);

        // 詳細コードを取り出して前後の"@@@"を削除する
        final String detailCode = matcher.find()
                ? matcher.group().replaceAll("@@@", "")
                : "";

        // 詳細コード部分はエラーコードから削除する
        errorCode = matcher.replaceAll("");

        ErrorManage errorManage = ErrorManage.getInstance();
        ErrorData errorData = errorManage.getErrorData(errorCode);

        if (errorData == null) {
            Timber.d("errorCode not found");
            return;
        }

        errorData.detail = String.format(errorData.detail, detailCode);
        errorData.date = new Date();
        new Thread(() -> DBManager.getErrorDao().insertErrorData(errorData)).start();

        //端末ログとして送信するデータを作成
        String message = String.format("MCエラーコード：%s", errorData.errorCode);
        if (errorData.detail.contains("詳細コード：")){
            message = message.concat(",").concat(errorData.detail.split("\n")[0]);
        }
        Timber.tag("エラー履歴").i(message);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void exitManualMode(boolean flg, int id) {
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL)) {
            // 手動決済モードを終了
            Timber.i("手動決済モード終了");
            Version.Response ifboxVersionInfo = AppPreference.getIFBoxVersionInfo();
            ifboxVersionInfo.appModel = IFBoxAppModels.FUTABA_D;
            AppPreference.setIFBoxVersionInfo(ifboxVersionInfo);
            _sharedViewModel.setUpdatedFlag(true);
            AppPreference.setIsTemporaryManualMode(false);
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    _slipData = DBManager.getSlipDao().getOneById(id);
                }
            });
            thread.start();

            try {
                thread.join();
                if (_slipData.transTypeCode.equals(PrinterProc.MANUALMODE_TRANS_TYPE_CODE_SALES)) {
                    SuccessDialog.show(getView().getContext(), "双方向決済モードに切り替わりました。");
                }
            } catch (Exception e) {
                //　念のため
            }
        }
    }
}
