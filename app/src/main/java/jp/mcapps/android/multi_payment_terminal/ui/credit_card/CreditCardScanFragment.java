package jp.mcapps.android.multi_payment_terminal.ui.credit_card;

import android.app.Activity;
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
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.fragment.NavHostFragment;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.ActionBarColors;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.MoneyType;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.data.TransactionResults;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentCreditCardScanBinding;
import jp.mcapps.android.multi_payment_terminal.model.Disposer;
// import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.model.McTerminal;
// import jp.mcapps.android.multi_payment_terminal.model.OptionalTicketTransFacade;
import jp.mcapps.android.multi_payment_terminal.model.OptionalTransFacade;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import jp.mcapps.android.multi_payment_terminal.model.TransLogger;
import jp.mcapps.android.multi_payment_terminal.thread.credit.CardManager;
import jp.mcapps.android.multi_payment_terminal.thread.emv.Constants.*;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputSeparationPayFDViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.common_head.CommonHeadViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.dialog.RadioDialogFragment;
import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.ui.pin.PinInputEventHandlers;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Version;
import timber.log.Timber;

import static jp.mcapps.android.multi_payment_terminal.AppPreference.isDemoMode;
import static jp.mcapps.android.multi_payment_terminal.data.BusinessType.PAYMENT;
import static jp.mcapps.android.multi_payment_terminal.data.BusinessType.REFUND;
import static jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement.k_MSIC_KBN_CONTACTLESS_IC;
import static jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement.k_MSIC_KBN_IC;

import java.util.concurrent.atomic.AtomicReference;

public class CreditCardScanFragment extends BaseFragment
        implements RadioDialogFragment.RadioDialogListener, CreditSettlement.CreditSettlementListener, CreditCardScanEventHandlers {

    private static final String SCREEN_NAME = "クレジットカード読み取り";
    private static final String[] _applications = new String[] {
            ApplicationTypes.CREDIT,
            ApplicationTypes.DEBIT,
    };
    private static int appNumber = 0;
    private Handler _handler = new Handler(Looper.getMainLooper());
    private SharedViewModel _sharedViewModel;
    private CommonHeadViewModel _commonHeadViewModel;
    private SoundManager _soundManager = SoundManager.getInstance();
    private float _soundVolume = AppPreference.getSoundPaymentVolume() / 10f;

    private MainApplication _app = MainApplication.getInstance();

    public static CreditCardScanFragment newInstance() {
        return new CreditCardScanFragment();
    }
    private CreditCardScanViewModel _creditCardScanViewModel;

    private CreditSettlement _creditSettlement = CreditSettlement.getInstance();

    private TransLogger _transLogger;

    private Disposer _disposer = new Disposer();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentCreditCardScanBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_credit_card_scan, container, false);

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        binding.setSharedViewModel(_sharedViewModel);

        _creditCardScanViewModel =
                new ViewModelProvider(this).get(CreditCardScanViewModel.class);

        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setViewModel(_creditCardScanViewModel);
        binding.setHandlers(this);

        _commonHeadViewModel = new ViewModelProvider(this).get(CommonHeadViewModel.class);
        binding.setCommonHeadViewModel(_commonHeadViewModel);
        getLifecycle().addObserver(_commonHeadViewModel);

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        _transLogger = new TransLogger();

        _creditCardScanViewModel.setResult(TransactionResults.None);
        _creditCardScanViewModel.getResult().observe(getViewLifecycleOwner(), result -> {
            if (result == TransactionResults.None) {
                if (AppPreference.isMoneyContactless()) {
                    _sharedViewModel.setShowMarkNfc(true);
                }
                _sharedViewModel.setActionBarColor(ActionBarColors.Normal);
            }
            else if (result == TransactionResults.SUCCESS) {
                _sharedViewModel.setShowMarkNfc(false);
                _sharedViewModel.setActionBarColor(ActionBarColors.Success);
            }
            else if (result == TransactionResults.FAILURE) {
                _sharedViewModel.setShowMarkNfc(false);
                _sharedViewModel.setActionBarColor(ActionBarColors.Error);
            }
            else if (result == TransactionResults.UNKNOWN) {
                _sharedViewModel.setShowMarkNfc(false);
                _sharedViewModel.setActionBarColor(ActionBarColors.Unknown);
            }
        });

        /* クレジットカード読み込み開始 */
        CreditSettlement creditSettlement = CreditSettlement.getInstance();
        final AtomicReference<ActivateIF> activateIF = new AtomicReference<>(ActivateIF.None);
        switch (MainApplication.getInstance().getBusinessType()) {
            case PAYMENT:
                activateIF.set(isDemoMode()
                        ? ActivateIF.ALL
                        : AppPreference.isMoneyContactless()
                        ? ActivateIF.ALL
                        : ActivateIF.MSIC
                );
                _creditCardScanViewModel.setActivateIF(activateIF.get());
                creditSettlement.startCredit(this, getActivity(), activateIF.get());
                _creditCardScanViewModel.setBusinessType("支払");
                break;
            case REFUND:
                Bundle args = getArguments();
                int slipId = args.getInt("slipId");
                _transLogger.setRefundParam(slipId);    //取消時に保存する項目を取得

                _creditCardScanViewModel.setBusinessType("取消");

                Single.create(emitter -> {
                    SlipData slipData = DBManager.getSlipDao().getOneById(slipId);
                    emitter.onSuccess(slipData);
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                        .subscribe(obj -> {
                            final SlipData slipData = (SlipData) obj;
                            _commonHeadViewModel.setAmount((slipData).transAmount);

                            activateIF.set(slipData.creditType.equals("CL")
                                            ? ActivateIF.CL
                                            : ActivateIF.MSIC);

                            _creditCardScanViewModel.setActivateIF(activateIF.get());
                            creditSettlement.startCreditCancel(this, getActivity(), slipData, activateIF.get());
                        });
                break;
            default:
                break;
        }
        creditSettlement.setListener(this);

        if (isDemoMode()) {
            creditSettlement.setDemoEventListener(_demoEventListener);
            binding.setPinInputHandlers(new PinInputEventHandlers() {
                @Override
                public void onInputNumber(View view, String number) {
                    _creditCardScanViewModel.inputNumber(number);
                    // デモモードでは、PINが1文字でも入力されたらサイン不要にする
                    creditSettlement.setSignatureFlag(CreditSettlement.k_SIGNATURE_UNNECESSARY);
                }

                @Override
                public void onCorrection(View view) {
                    _creditCardScanViewModel.correct();
                }

                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void onEnter(View view) {
                    _creditCardScanViewModel.enter();
                    _creditCardScanViewModel.isDemoPinInput(false);
                    creditSettlement.startDemoAuth();
                }

                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                    public void onCancel(View view) {
                        _creditCardScanViewModel.cancel();
                    _creditCardScanViewModel.isDemoPinInput(false);
                    OnSound(R.raw.credit_auth_ng);
                    OnError(null);
                }
            });
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        _disposer.addTo(_creditSettlement.getCLState().subscribe(state -> {
            if (state == CLState.ReadAgain || state == CLState.CardHold) {
                _sharedViewModel.setLoading(false);
            }

            _creditCardScanViewModel.setCLState(state);
        }));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _disposer.dispose();

        if (_creditSettlement.isSameListener(this)) {
            _creditSettlement.setListener(null);
        }
        _creditSettlement.setDemoEventListener(null);
//        CreditSettlement.getInstance().setListener(null);
    }

    @Override
    public void selectApplication(String[] applications) {
        requireActivity().runOnUiThread(() -> {
            appNumber = 0;
            RadioDialogFragment dialog = RadioDialogFragment.newInstance("決済選択", applications);
            dialog.show(getChildFragmentManager(), "");
        });
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        CreditSettlement.getInstance().getEmvProcInstance().setApplication(appNumber);
        dialog.dismiss();
    }

    @Override
    public void onRadioButtonClick(DialogFragment dialog, int which) {
        appNumber = which;
    }

    @Override
    public void OnProcStart() {
        Timber.d("OnProcStart");
        _transLogger.setAntennaLevel();   //アンテナレベルは処理開始時に取得
        // 非同期スレッドから呼ばれるのでハンドラーで処理する
        _handler.post(() -> {
            if (!_creditSettlement.isCL()) {
                _sharedViewModel.setLoading(true);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void OnProcEnd() {
        Timber.d("OnProcEnd");
        _creditSettlement.getCLState().onNext(CLState.None);

        // 非同期スレッドから呼ばれるのでハンドラーで処理する
        _handler.post(() -> {
            _sharedViewModel.setLoading(false);

            //売上・印刷データ保存
            CreditSettlement creditSettlement = CreditSettlement.getInstance();
            _transLogger.setTransResult(TransMap.RESULT_SUCCESS, TransMap.DETAIL_NORMAL);
            _transLogger.setProcTime(creditSettlement.GetCreditProcTime(), creditSettlement.getPinProcTime());
            _transLogger.credit(creditSettlement._creditResult);
            int slipId = _transLogger.insert();
            // POSサービス有効の時は取消成功時・取消未了時に取消不可にする
            if(AppPreference.isServicePos()) {
                _transLogger.updateCancelFlg();
            }

            if (AppPreference.isPosTransaction()) {
                // 通常の取引レコード以外の取引情報を作成する
                OptionalTransFacade optionalTransFacade = new OptionalTransFacade(MoneyType.CREDIT);
                optionalTransFacade = _transLogger.setDataForFacade(optionalTransFacade);
                optionalTransFacade.CreateReceiptData(slipId); // 取引明細書、取消票、領収書のデータを作成
                optionalTransFacade.CreateByUriData(); // DBにセット
            }

//            if (AppPreference.isTicketTransaction()) {
//                // チケット販売時の取引情報を作成する
//                OptionalTicketTransFacade optionalTicketTransFacade = new OptionalTicketTransFacade(MoneyType.CREDIT);
//                optionalTicketTransFacade = _transLogger.setTicketDataForFacade(optionalTicketTransFacade);
//                optionalTicketTransFacade.CreateReceiptData(slipId); // 取引明細書、取消票、領収書のデータを作成
//            }

            final Bundle params = new Bundle();
            params.putInt("slipId", slipId);

            final String message = creditSettlement.isIC()
                    ? String.format("クレジット　%s\nカードを抜いてください\nありがとうございました", _creditCardScanViewModel.getBusinessType())
                    : String.format("クレジット　%s\n\nありがとうございました", _creditCardScanViewModel.getBusinessType());


            _creditCardScanViewModel.setFinishedMessage(message);

            _creditCardScanViewModel.isFinished(true);
            _creditCardScanViewModel.setResult(TransactionResults.SUCCESS);

            if (MainApplication.getInstance().getBusinessType() == PAYMENT) {
                // クリア前に現金分割分を現金併用金額に加算
                MainApplication.getInstance().setCashValue(MainApplication.getInstance().getCashValue() + Amount.getCashAmount());
                Amount.reset();
            } else if (MainApplication.getInstance().getBusinessType() == REFUND) {
            }
            _handler.postDelayed(() -> {
                if (creditSettlement.isIC()) {
                    Completable
                            .create(emitter -> {
                                while (creditSettlement.getIcCardExistFlg()) {
                                    try { Thread.sleep(1000); } catch (Exception ignore) { }
                                }
                                emitter.onComplete();
                            }).subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(() -> {
                                _sharedViewModel.setActionBarColor(ActionBarColors.Normal);
                                NavigationWrapper.navigate(this, R.id.action_navigation_credit_card_scan_to_navigation_menu, params);
                            });
                } else {
                    _sharedViewModel.setActionBarColor(ActionBarColors.Normal);
                    NavigationWrapper.navigate(this, R.id.action_navigation_credit_card_scan_to_navigation_menu, params);
                }
            }, 5000);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void OnError(String errorCode) {
        Timber.d("OnError");

        _creditSettlement.getCLState().onNext(CLState.None);

        // 非同期スレッドから呼ばれるのでハンドラーで処理する
        _handler.post(() -> {
            _sharedViewModel.setLoading(false);

            MainApplication.getInstance().setErrorCode(errorCode);

            PrinterManager printerManager = PrinterManager.getInstance();
//            printerManager.send820_FunctionCodeErrorResult(getView(), IFBoxManager.SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_CREDIT ,
//                                                    false, 0);      //820へ決済中止を通知

            CreditSettlement creditSettlement = CreditSettlement.getInstance();
            if ((k_MSIC_KBN_IC == creditSettlement._creditResult.msICKbn || k_MSIC_KBN_CONTACTLESS_IC == creditSettlement._creditResult.msICKbn) &&
                PAYMENT == MainApplication.getInstance().getBusinessType() &&
                true == creditSettlement.getCreditResultValidFlg()) {
                // ICクレジット決済でエラーの場合、拒否売上・印刷データ保存（失敗時は印刷はしない）
                _transLogger.setTransResult(TransMap.RESULT_ERROR, getErrorDetail());
                _transLogger.setProcTime(creditSettlement.GetCreditProcTime(), creditSettlement.getPinProcTime());
                _transLogger.credit(creditSettlement._creditResult);
                int slipId = _transLogger.insert();

                //売上送信
                new Thread(() -> {
                    String mcTerminalErrCode = new McTerminal().postPayment();
                    if (mcTerminalErrCode != null) {
                        Timber.e("売上情報送信失敗：%s", mcTerminalErrCode);
                    }
                }).start();
            }

            final String message = creditSettlement.isIC()
                    ? String.format("クレジット　%s\nカードを抜いてください\nお取扱いできません", _creditCardScanViewModel.getBusinessType())
                    : String.format("クレジット　%s\n\nお取扱いできません", _creditCardScanViewModel.getBusinessType());

            _creditCardScanViewModel.setFinishedMessage(message);

            _creditCardScanViewModel.isFinished(true);
            _creditCardScanViewModel.setResult(TransactionResults.FAILURE);

            if (AppPreference.isTemporaryManualMode())
            {
                // フタバ双方向モードに戻す
                Version.Response ifboxVersionInfo = AppPreference.getIFBoxVersionInfo();
                ifboxVersionInfo.appModel = IFBoxAppModels.FUTABA_D;
                AppPreference.setIFBoxVersionInfo(ifboxVersionInfo);
                AppPreference.setIsTemporaryManualMode(false);
            }

            _handler.postDelayed(() -> {
                if (creditSettlement.isIC()) {
                    Completable
                            .create(emitter -> {
                                while (creditSettlement.getIcCardExistFlg()) {
                                    try { Thread.sleep(1000); } catch (Exception ignore) { }
                                }
                                emitter.onComplete();
                            }).subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(() -> {
                                _sharedViewModel.setActionBarColor(ActionBarColors.Normal);
                                // NavigationWrapper.navigate(this, R.id.action_navigation_credit_card_scan_to_navigation_menu);
                                navigateToMenuOrPopBack(null);
                            });
                } else {
                    _sharedViewModel.setActionBarColor(ActionBarColors.Normal);
                    // NavigationWrapper.navigate(this, R.id.action_navigation_credit_card_scan_to_navigation_menu);
                    navigateToMenuOrPopBack(null);
                }
            }, 5000);
        });
    }

    public float getVolumeCredit(Integer soundResource) {
        float vol = 0f;

        /* 案内音の場合は案内音の音量、それ以外は決済音の音量に */
        if (soundResource == R.raw.credit_signature
                || soundResource == R.raw.credit_start
                || soundResource == R.raw.credit_input_pin
                || soundResource == R.raw.credit_read_error
                || soundResource == R.raw.remove_card) {
            vol =  AppPreference.getSoundGuidanceVolume() / 10f;
        } else {
            vol =  AppPreference.getSoundPaymentVolume() / 10f;
        }
        return vol;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void OnSound(int id) {
        Timber.d("OnSound");
        _soundManager.load(MainApplication.getInstance(), id, 1);
        _soundVolume = getVolumeCredit(id);
        _soundManager.setOnLoadCompleteListener((soundPool, soundId, status) -> {
                soundPool.play(soundId, _soundVolume, _soundVolume, 1, 0, 1);
        });
    }

    @Override
    public void onCancelClick(View view) {
        Timber.d("onCancelClick");
        CardManager.getInstance(_creditSettlement.getActivateIF().getMode()).releaseAll();
        _sharedViewModel.setShowMarkNfc(false);
        _sharedViewModel.setActionBarColor(ActionBarColors.Normal);

        //ADD-S BMT S.Oyama 2024/09/19 フタバ双方向向け改修
        PrinterManager printerManager = PrinterManager.getInstance();
//        printerManager.send820_FunctionCodeErrorResult(view, IFBoxManager.SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_CREDIT , false, 0);      //820へ決済中止を通知

        //Amount.setFlatRateAmount(0);        // 分別払いからの遷移でないときは定額金額をクリア
        NavigationWrapper.popBackStack(this);
        //ADD-E BMT S.Oyama 2024/09/19 フタバ双方向向け改修

        if (AppPreference.isTemporaryManualMode() == true)
        {
            // フタバ双方向モードに戻す
            Version.Response ifboxVersionInfo = AppPreference.getIFBoxVersionInfo();
            ifboxVersionInfo.appModel = IFBoxAppModels.FUTABA_D;
            AppPreference.setIFBoxVersionInfo(ifboxVersionInfo);
            AppPreference.setIsTemporaryManualMode(false);
        }

        CommonClickEvent.RecordButtonClickOperation(view, true);
    }

    //ADD-S BMT S.Oyama 2024/09/20 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  画面を戻す処理（前画面が分別モード時向け）
     * @note   画面を戻す処理　前画面が分別モード時向け
     * @param [in] なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    private void backSeparation() {
        NavigationWrapper.navigate(this, R.id.action_navigation_credit_card_scan_to_navigation_menu);
    }
    //ADD-E BMT S.Oyama 2024/09/20 フタバ双方向向け改修

    @Override
    public void timeoutWaitCard(String errorCode) {
        Timber.d("timeoutWaitCard");

        // 非同期スレッドから呼ばれるのでハンドラーで処理する
        _handler.post(() -> {
            MainApplication.getInstance().setErrorCode(errorCode);

            PrinterManager printerManager = PrinterManager.getInstance();
//            printerManager.send820_FunctionCodeErrorResult(getView(), IFBoxManager.SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_CREDIT , false,0);      //820へ決済中止を通知

            _sharedViewModel.setShowMarkNfc(false);

            if (AppPreference.isTemporaryManualMode() == true)
            {
                // フタバ双方向モードに戻す
                Version.Response ifboxVersionInfo = AppPreference.getIFBoxVersionInfo();
                ifboxVersionInfo.appModel = IFBoxAppModels.FUTABA_D;
                AppPreference.setIFBoxVersionInfo(ifboxVersionInfo);
                AppPreference.setIsTemporaryManualMode(false);
            }

            _sharedViewModel.setActionBarColor(ActionBarColors.Normal);
            // NavigationWrapper.navigate(this, R.id.action_navigation_credit_card_scan_to_navigation_menu);
            navigateToMenuOrPopBack(null);
        });
    }

    @Override
    public void cancelPin() {
        Timber.d("cancelPin");

        // 非同期スレッドから呼ばれるのでハンドラーで処理する
        _handler.post(() -> {
            CardManager.getInstance(_creditSettlement.getActivateIF().getMode()).releaseAll();

            PrinterManager printerManager = PrinterManager.getInstance();
//            printerManager.send820_FunctionCodeErrorResult(getView(), IFBoxManager.SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_CREDIT , false, 0);      //820へ決済中止を通知

            _sharedViewModel.setLoading(false);
            _sharedViewModel.setShowMarkNfc(false);
            _sharedViewModel.setActionBarColor(ActionBarColors.Normal);
            // NavigationWrapper.navigate(this, R.id.action_navigation_credit_card_scan_to_navigation_menu);
            navigateToMenuOrPopBack(null);
        });
    }

    private int getErrorDetail() {
        int ret = 0;
        CreditSettlement creditSettlement = CreditSettlement.getInstance();

        switch(creditSettlement.getOnlineAuthErrorReason()) {
            case WAITRES_TOUT:  // オーソリ応答待ちタイムアウト
                ret = TransMap.DETAIL_COMMUNICATION_FAILURE;
                break;
            case RES_NG:        // オーソリNG
                ret = TransMap.DETAIL_AUTH_RESULT_NG;
                break;
            case RES_VERI_NG:   // オーソリ応答検証NG
                ret = TransMap.DETAIL_AUTH_VERIFICATION_RESULT_NG;
                break;
            default:
                Timber.e("エラー要因異常");
                ret = TransMap.DETAIL_AUTH_RESULT_NG;
                break;
        }
        return ret;
    }

    private CreditSettlement.DemoEventListener _demoEventListener = new CreditSettlement.DemoEventListener() {
        @Override
        public void onPinInputRequired() {
            _creditCardScanViewModel.isDemoPinInput(true);
        }
    };

    private void navigateToMenuOrPopBack(@Nullable Bundle params) {
        if (AppPreference.isServicePos() || AppPreference.isServiceTicket()) {
            // POSまたはチケットの時は、直前の画面に戻る
            if (params != null) {
                NavBackStackEntry backStackEntry = NavHostFragment.findNavController(this).getPreviousBackStackEntry();
                if (backStackEntry != null && params.containsKey("slipId")) {
                    backStackEntry.getSavedStateHandle().set("resultSlipId", params.getInt("slipId"));
                }
            }
            // 直前の画面に戻ってもメイン画面に遷移するまでアラートダイアログが出ないため、ここで表示する
            final String errorCode = _app.getErrorCode();
            if(errorCode != null) { // 暗証番号入力をキャンセルした場合、エラーダイアログは表示されない
                final CommonErrorDialog dialog = new CommonErrorDialog();
                dialog.ShowErrorMessage(requireContext(), errorCode);
                _app.setErrorCode(null);
            }

            NavigationWrapper.popBackStack(this);
        } else {
            NavigationWrapper.navigate(this, R.id.action_navigation_credit_card_scan_to_navigation_menu, params);
        }
    }
}