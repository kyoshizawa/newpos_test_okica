package jp.mcapps.android.multi_payment_terminal.ui.qr;

import static jp.mcapps.android.multi_payment_terminal.data.BusinessType.REFUND;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.fragment.NavHostFragment;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.CustomScannerActivity;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.ActionBarColors;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.QRLayouts;
import jp.mcapps.android.multi_payment_terminal.data.TransactionResults;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentQrBinding;
import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputSeparationPayFDViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.common_head.CommonHeadViewModel;
import timber.log.Timber;

public class QRPaymentFragment extends BaseFragment implements QREventHandlers {
    private final String SCREEN_NAME = "QR読み取り";
    private static final int MESSAGE_DISPLAY_TIME_MS = 5000;

    private MainApplication _app = MainApplication.getInstance();
    private QRViewModel _qrViewModel;
    private SharedViewModel _sharedViewModel;
    private CommonHeadViewModel _commonHeadViewModel;
    private Handler _handler = new Handler(Looper.getMainLooper());
    private boolean _isNoEndError = false;   // 処理未了フラグ
    //ADD-S BMT S.Oyama 2025/02/07 フタバ双方向向け改修
    private boolean _is820ResetAbort = false;  // 820側でリセット応答を750へ送付してきた場合（スキャン中の空車等）
    //ADD-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修

    public static QRPaymentFragment newInstance() {
        return new QRPaymentFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        final FragmentQrBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_qr, container, false);

        _sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        _qrViewModel = new ViewModelProvider(this).get(QRViewModel.class);
        _qrViewModel.makeSound(R.raw.qr_start);

        binding.setViewModel(_qrViewModel);
        binding.setSharedViewModel(_sharedViewModel);
        binding.setHandlers(this);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        _commonHeadViewModel = new ViewModelProvider(this).get(CommonHeadViewModel.class);
        binding.setCommonHeadViewModel(_commonHeadViewModel);
        getLifecycle().addObserver(_commonHeadViewModel);

        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setOrientationLocked(true);
        integrator.setPrompt("");
//        integrator.setTimeout(30000); //中止ボタン押下と判別出来ないため(エラー表示が出来ない)タイムアウト設定なし
        integrator.setCaptureActivity(CustomScannerActivity.class)
                .addExtra(QRLayouts.KEY, QRLayouts.PAYMENT)
                .setBeepEnabled(false)
                .initiateScan();

        _qrViewModel.setResult(TransactionResults.None);
        _qrViewModel.getResult().observe(getViewLifecycleOwner(), result -> {
            if (result == TransactionResults.None) {
                _sharedViewModel.setActionBarColor(ActionBarColors.Normal);
            }
            else if (result == TransactionResults.SUCCESS) {
                _sharedViewModel.setActionBarColor(ActionBarColors.Success);
            }
            else if (result == TransactionResults.FAILURE) {
                _sharedViewModel.setActionBarColor(ActionBarColors.Error);
            }
            else if (result == TransactionResults.UNKNOWN) {
                _sharedViewModel.setActionBarColor(ActionBarColors.Unknown);
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //ADD-S BMT S.Oyama 2025/02/07 フタバ双方向向け改修
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {
            _is820ResetAbort = false;
            IFBoxManager.meterDataV4Disposable_ScanEmoneyQR = PrinterManager.getInstance().getIFBoxManager().
                    getMeterDataV4().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(meter -> {
                        Timber.i("[FUTABA-D]QRPaymentFragment:750<-820 meter_data event cmd:%d", meter.meter_sub_cmd);
                        if (meter.meter_sub_cmd == 2)           //820よりリセットを送られてきた場合
                        {
                            Timber.i("[FUTABA-D]QRPaymentFragment:!!820 Recv Reset event");
                            _is820ResetAbort = true;

                            Intent intent = new Intent("CLOSE_QR_SCANNER");
                            LocalBroadcastManager.getInstance(view.getContext()).sendBroadcast(intent);
                        }
                    });
        }
        //ADD-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //ADD-S BMT S.Oyama 2025/02/07 フタバ双方向向け改修
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {
            if (IFBoxManager.meterDataV4Disposable_ScanEmoneyQR != null) {
                IFBoxManager.meterDataV4Disposable_ScanEmoneyQR.dispose();
                IFBoxManager.meterDataV4Disposable_ScanEmoneyQR = null;
            }
        }
        //ADD-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final IntentResult result = IntentIntegrator.parseActivityResult(resultCode, data);

        String qrcode = null;
        if(data != null) {
            if (result.getContents() != null) {
                // 背面カメラで読み取った値を設定
                qrcode = result.getContents();
            } else if (data.getStringExtra("barcodeString") != null) {
                // 外付けバーコードリーダーで読み取った値を設定
                qrcode = data.getStringExtra("barcodeString");
            }
        }
        final String contents = qrcode;

        if (contents != null) {
            _qrViewModel.getFinishedMessage().observe(getViewLifecycleOwner(), msg -> {
                if (msg != null && !msg.equals("")) {
                    _handler.removeCallbacksAndMessages(null);

                    final Bundle params = new Bundle();
                    params.putInt("slipId", _qrViewModel.getSlipId());

                    final TransactionResults transactionResults = _qrViewModel.getResult().getValue();
                    if (transactionResults != TransactionResults.UNKNOWN) {
                        _handler.postDelayed(() -> {
                            _sharedViewModel.setActionBarColor(ActionBarColors.Normal);
                            if (transactionResults == TransactionResults.SUCCESS) {
                                NavigationWrapper.navigate(this, R.id.action_navigation_qr_payment_to_navigation_menu, params);
                            } else {
                                navigateToMenuOrPopBack(params);
                            }
                        }, MESSAGE_DISPLAY_TIME_MS);
                    }
                }
            });
            new Thread(() -> {
                _qrViewModel.payment(contents);
            }).start();
        } else {
            // カメラ画面で戻るボタンが押されたとき メニュー画面に戻す
            requireActivity().runOnUiThread(() -> {
                _sharedViewModel.setActionBarColor(ActionBarColors.Normal);
                //ADD-S BMT S.Oyama 2024/09/20 フタバ双方向向け改修
                //ADD-S BMT S.Oyama 2025/02/07 フタバ双方向向け改修
                if (_is820ResetAbort == false) {
                    PrinterManager printerManager = PrinterManager.getInstance();
                    printerManager.send820_FunctionCodeErrorResult(this.getView(), IFBoxManager.SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_QR, false, 0);      //820へ決済中止を通知
                }
                //ADD-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修
                //Amount.setFlatRateAmount(0);        // 分別払いからの遷移でないときは定額金額をクリア
                NavigationWrapper.popBackStack(this);
                //ADD-E BMT S.Oyama 2024/09/20 フタバ双方向向け改修
            });
        }
    }

    @Override
    public void onCheckResultClick(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _qrViewModel.setResult(TransactionResults.None);
        new Thread(() -> {
            _qrViewModel.checkPaymentResult();
        }).start();
    }

    @Override
    public void onCancelClick(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);

        //CHG-S BMT S.Oyama 2024/09/03 フタバ双方向向け改修
        //if(IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) || IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) {
        if(IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) || IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D) || IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D)) {
        //CHG-S BMT S.Oyama 2024/09/03 フタバ双方向向け改修
            _app.setErrorCode(_app.getString(R.string.error_type_qr_3327));
        }else{
            _app.setErrorCode(_app.getString(R.string.error_type_qr_3320));
        }

        _isNoEndError = true;
        // 処理未了の売上データ生成と印刷
        _qrViewModel.createUnfinishedTransLogger();
        final Bundle params = new Bundle();
        params.putInt("slipId", _qrViewModel.getSlipId());

        _sharedViewModel.setActionBarColor(ActionBarColors.Normal);
        // NavigationWrapper.navigate(this, R.id.action_navigation_qr_payment_to_navigation_menu, params);
        navigateToMenuOrPopBack(params);
    }

    private void navigateToMenuOrPopBack(@Nullable Bundle params) {
        if (AppPreference.isServicePos() || AppPreference.isServiceTicket()) {

            if (AppPreference.isTicketTransaction() && _isNoEndError) {
                // チケット購入の決済または取消時の処理未了発生した場合、トップメニュー画面に遷移
                NavigationWrapper.navigate(this, R.id.action_navigation_qr_payment_to_navigation_menu, params);
                return;
            }

            // POSまたはチケットの時は、直前の画面に戻る
            if (params != null) {
                NavBackStackEntry backStackEntry = NavHostFragment.findNavController(this).getPreviousBackStackEntry();
                if (backStackEntry != null && params.containsKey("slipId")) {
                    backStackEntry.getSavedStateHandle().set("resultSlipId", params.getInt("slipId"));
                }
            }
            NavigationWrapper.popBackStack(this);
        } else {
            //ADD-S BMT S.Oyama 2024/09/20 フタバ双方向向け改修
            PrinterManager printerManager = PrinterManager.getInstance();
            printerManager.send820_FunctionCodeErrorResult(this.getView(), IFBoxManager.SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_QR , false,0);      //820へ決済中止を通知
            //Amount.setFlatRateAmount(0);        // 分別払いからの遷移でないときは定額金額をクリア
            NavigationWrapper.navigate(this, R.id.action_navigation_qr_payment_to_navigation_menu, params);
            //ADD-E BMT S.Oyama 2024/09/20 フタバ双方向向け改修
        }
    }

    //ADD-S BMT S.Oyama 2024/09/20 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  画面を戻す処理（分別向け）
     * @note   画面を戻す処理（分別向け
     * @param [in] なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    private void backSeparation() {
        NavigationWrapper.navigate(this, R.id.action_navigation_qr_payment_to_navigation_menu);
    }
    //ADD-E BMT S.Oyama 2024/09/20 フタバ双方向向け改修

}