package jp.mcapps.android.multi_payment_terminal.ui.emoney.suica;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import org.json.JSONException;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.CommonClickEvent;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.JremLcdTextMap;
import jp.mcapps.android.multi_payment_terminal.data.MoneyType;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentEmoneySuicaBinding;
import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorCodes;
import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorMap;
import jp.mcapps.android.multi_payment_terminal.error.iCASErrorMap;
import jp.mcapps.android.multi_payment_terminal.iCAS.BusinessParameter;
import jp.mcapps.android.multi_payment_terminal.iCAS.IiCASClient;
import jp.mcapps.android.multi_payment_terminal.iCAS.data.DeviceClient;
import jp.mcapps.android.multi_payment_terminal.iCAS.iCASClient;
import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
import jp.mcapps.android.multi_payment_terminal.model.OptionalTicketTransFacade;
import jp.mcapps.android.multi_payment_terminal.model.OptionalTransFacade;
import jp.mcapps.android.multi_payment_terminal.model.TransLogger;
import jp.mcapps.android.multi_payment_terminal.thread.felica.FelicaManager;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.ui.Converters;
import jp.mcapps.android.multi_payment_terminal.ui.PostPaymentProcess;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputSeparationPayFDViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.emoney.ActionBarController;
import jp.mcapps.android.multi_payment_terminal.ui.emoney.LcdController;
import jp.mcapps.android.multi_payment_terminal.ui.emoney.SoundController;
import timber.log.Timber;

public class EMoneySuicaFragment extends BaseFragment implements EMoneySuicaEventHandlers, IiCASClient {
    public static EMoneySuicaFragment newInstance() {
        return new EMoneySuicaFragment();
    }
    private final String SCREEN_NAME = "交通系";

    public static int FORCE_CANCEL_TIMEOUT = 60*1000;  // 中止ボタンを押してから強制的に画面を抜けるまでの時間
    private final iCASClient _icasClient = iCASClient.getInstance();
    private final Handler _handler = new Handler(Looper.getMainLooper());
    private FragmentEmoneySuicaBinding _binding;
    private EMoneySuicaViewModel _eMoneySuicaViewModel;
    private SharedViewModel _sharedViewModel;
    private ActionBarController _actionBarController;
    private LcdController _lcd1Controller;
    private LcdController _lcd2Controller;
    private LcdController _lcd3Controller;
    private SoundController _soundController;
    private int _balance = 0;
    private String _currentStatus = "";
    private boolean _backFlag = false;
    private boolean _firstContact = true;
    private Integer[] _bar;
    private boolean _isIcasWorking = false;
    private String _sid = null;
    private Integer _slipId = null;
    private boolean _usePrinter = false;
    private boolean _isUnfinished = false;
    private int _businessId = -1;
    private String _IDi;
    private TransLogger _transLogger;
    private Integer _currentSound = null;
    private Integer _value;
    private boolean _isChancel = false;
    private boolean _isNoEndError = false;   // 処理未了フラグ（OnError時に未了が発生していたか判別）
    private boolean _isFinished = false;  // 交通系はかざし不良でステータスが行ったり来たりするので別フラグで管理
    //ADD-S BMT S.Oyama 2025/02/07 フタバ双方向向け改修
    private boolean _is820ResetAbort = false;  // 820側でリセット応答を750へ送付してきた場合（スキャン中の空車等）
    //ADD-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        _binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_emoney_suica, container, false);

        final FragmentActivity activity = getActivity();
        if (activity != null) {
            _sharedViewModel =
                    new ViewModelProvider(activity).get(SharedViewModel.class);

            _binding.setSharedViewModel(_sharedViewModel);
        }

        _eMoneySuicaViewModel = new ViewModelProvider(this).get(EMoneySuicaViewModel.class);

        _binding.setViewModel(_eMoneySuicaViewModel);

        _binding.setLifecycleOwner(getViewLifecycleOwner());

        _binding.setHandlers(this);

        _lcd1Controller = new LcdController(_eMoneySuicaViewModel.getLcd1());
        _lcd2Controller = new LcdController(_eMoneySuicaViewModel.getLcd2());
        _lcd3Controller = new LcdController(_eMoneySuicaViewModel.getLcd3());

        HashMap<Integer, Integer> soundMap = new HashMap<Integer, Integer>();

        soundMap.put(0x00, R.raw.emoney_touch_default);  // タッチ
        soundMap.put(0x01, R.raw.emoney_touch_only);     // タッチ（残高照会用）
        soundMap.put(0x03, R.raw.stopsettlement);        // 回復不能エラー
        soundMap.put(0x04, R.raw.completeover1000);      // 正常終了
        soundMap.put(0x05, R.raw.completeunder1000);     // 正常終了(残額1000円以下)
        soundMap.put(0x06, R.raw.unfinished);            // 異常終了
        soundMap.put(0x07, R.raw.completereadvalue);     // 正常終了
        soundMap.put(0x63, null);  // 鳴動停止

        _soundController = new SoundController(soundMap);

        _transLogger = new TransLogger();

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return _binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        _actionBarController = new ActionBarController(this, _sharedViewModel);
        _actionBarController.setStatus(
                ActionBarController.ControlCodes.OFF,
                ActionBarController.ColorCodes.NONE,
                0);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //ADD-S BMT S.Oyama 2025/02/07 フタバ双方向向け改修
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {
            _is820ResetAbort = false;
            IFBoxManager.meterDataV4Disposable_ScanEmoneyQR = PrinterManager.getInstance().getIFBoxManager().
                    getMeterDataV4().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(meter -> {
                Timber.i("[FUTABA-D]EMoneySuicaFragment:750<-820 meter_data event cmd:%d", meter.meter_sub_cmd);

                if (meter.meter_sub_cmd == 2)           //820よりリセットを送られてきた場合
                {
                    Timber.i("[FUTABA-D]EMoneySuicaFragment:!!820 Recv Reset event");
                    _is820ResetAbort = true;
                    view.post(() -> {
                        onCancelClick(view);
                    });
                }
            });
        }
        //ADD-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修

        _sharedViewModel.setLoading(true);

        _icasClient.SetRWUIEventListener(this);
        BusinessParameter businessParameter = new BusinessParameter();
        BusinessParameter.Suica suica = new BusinessParameter.Suica();
//            businessParameter.businessId = String.valueOf(_iCASClient.BUSINESS_ID_SUICA_PAY);
//        businessParameter.sid = _iCASClient.MakeSid();
        businessParameter.money = suica;
//        suica.value = "1";
        suica.idi = null;
        suica.together = null;
        suica.oldSid = null;
        suica.uiGuideline = "ON";
        suica.inProgressUI = "ON";

        final BusinessType type = MainApplication.getInstance().getBusinessType();
        if (type == BusinessType.PAYMENT) {
            businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_SUICA_PAY);
            suica.value = Integer.toString(Amount.getFixedAmount());
        }
        else if (type == BusinessType.BALANCE) {
            businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_SUICA_REMAIN);
            suica.value = null;
        }
        else if (type == BusinessType.REFUND) {
            final Bundle args = getArguments();

            _slipId = args.getInt("slipId");

            businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_SUICA_REFUND);
            suica.value = null;

            if(AppPreference.isDemoMode()) {
                // デモモードでは取消金額を知る必要があるのでvalueとして渡す
                final SlipData[] slipData = {null};
                Thread thread = new Thread(() -> {
                    slipData[0] = DBManager.getSlipDao().getOneById(_slipId);
                });
                thread.start();

                try {
                    thread.join();

                    suica.value = String.valueOf(slipData[0].transAmount);
                } catch (InterruptedException e) {
                    Timber.e(e);
                    back();
                }
            }
        }
        else if (type == BusinessType.RECOVERY_PAYMENT) {
            final Bundle args = getArguments();

            _slipId = args.getInt("slipId");

            final SlipData[] slipData = {null};
            Thread thread = new Thread(() -> {
                slipData[0] = DBManager.getSlipDao().getOneById(_slipId);
            });
            thread.start();

            try {
                thread.join();

                businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_SUICA_PAY);
                _sid = slipData[0].transId;

                if (slipData[0].transCashTogetherAmount != null) {
                    suica.value = String.valueOf(slipData[0].transCashTogetherAmount);
                    suica.together = "ON";
                } else {
                    suica.value = String.valueOf(slipData[0].transAmount);
                    suica.together = "OFF";
                }
            } catch (InterruptedException e) {
                Timber.e(e);
                back();
            }
        }
        else if (type == BusinessType.RECOVERY_REFUND) {
            final Bundle args = getArguments();

            _slipId = args.getInt("slipId");

            final SlipData[] slipData = {null};
            Thread thread = new Thread(() -> {
                slipData[0] = DBManager.getSlipDao().getOneById(_slipId);
            });
            thread.start();

            try {
                thread.join();

                businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_SUICA_REFUND);
                suica.value = null;
                _sid = slipData[0].transId;

            } catch (InterruptedException e) {
                Timber.e(e);
                back();
            }
        }
        else {
            throw new IllegalStateException("取引種別がない");
        }
/*
        // 交通系取消
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_SUICA_REFUND);
        suica.value = null;
*/
/*
        // 交通系 チャージ
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_SUICA_CHARGE);
        businessParameter.money = suica;
        suica.value = "8000";
        suica.idi = null;
        suica.together = null;
        suica.oldSid = null;
        suica.uiGuideline = "ON";
        suica.inProgressUI = "ON";
*/
/*
        // 交通系 業務処理状態応答
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_SUICA_STATUS_REPLY);
        businessParameter.money = suica;
        suica.value = null;
        suica.idi = null;
        suica.together = null;
        suica.oldSid = "510131804";
        suica.uiGuideline = null;
        suica.inProgressUI = null;
*/
/*
        // iD 支払
        BusinessParameter.iD iD = new BusinessParameter.iD();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_ID_PAY);
        businessParameter.money = iD;
        iD.value = "1";
        // オンラインPIN入力確認はこちら
//        iD.value = "35000";
        iD.payment = "sin";
        iD.training = "OFF";
        iD.uiGuideline = "ON";
        iD.inProgressUI = "ON";
*/
/*
        // iD 取消
        BusinessParameter.iD iD = new BusinessParameter.iD();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_ID_REFUND);
        businessParameter.money = iD;
        iD.value = "1";
        iD.slipNo = "5";
        iD.oldTermIdentId = "9999901200069";
        iD.payment = "sin";
        iD.training = "OFF";
        iD.uiGuideline = "ON";
        iD.inProgressUI = "ON";
*/
/*
        // iD 照会
        BusinessParameter.iD iD = new BusinessParameter.iD();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_ID_REMAIN);
        businessParameter.money = iD;
        iD.training = "OFF";
        iD.uiGuideline = "ON";
        iD.inProgressUI = "ON";
*/
/*
        // iD 業務処理状態応答
        BusinessParameter.iD iD = new BusinessParameter.iD();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_ID_STATUS_REPLY);
        businessParameter.money = iD;
        iD.oldSid = "510133400";
        iD.training = "OFF";
*/
/*
        // WAON 残高照会
        BusinessParameter.Waon waon = new BusinessParameter.Waon();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_WAON_REMAIN);
        businessParameter.money = waon;
        waon.value = null;
        waon.training = "OFF";
        waon.uiGuideline = "ON";
        waon.inProgressUI = "ON";
*/
/*
        // WAON 支払
        BusinessParameter.Waon waon = new BusinessParameter.Waon();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_WAON_PAY);
        businessParameter.money = waon;
        waon.value = "50000";
        waon.together = "OFF";
        waon.totalValue = "50000";
        waon.pointValue = "50000";
        waon.training = "OFF";
        waon.uiGuideline = "ON";
        waon.inProgressUI = "ON";
*/
/*
        // WAON 取消
        BusinessParameter.Waon waon = new BusinessParameter.Waon();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_WAON_REFUND);
        businessParameter.money = waon;
        waon.value = "17927";
        waon.together = null;
        waon.totalValue = null;
        waon.pointValue = null;
        waon.slipNo = "16";
        waon.oldTermIdentId = "9999901200069";
        waon.training = "OFF";
        waon.uiGuideline = "ON";
        waon.inProgressUI = "ON";
*/
/*
        // WAON 業務処理状態応答
        BusinessParameter.Waon waon = new BusinessParameter.Waon();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_WAON_STATUS_REPLY);
        businessParameter.money = waon;
        waon.value = null;
        waon.oldSid = "510135603";
        waon.training = "OFF";
*/
/*
        BusinessParameter.Waon waon = new BusinessParameter.Waon();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_WAON_CHARGE);
        businessParameter.money = waon;
        waon.value = "1000";
        waon.training = "OFF";
        waon.uiGuideline = "ON";
        waon.inProgressUI = "ON";
*/
/*
        // nanaco 残高確認
        BusinessParameter.nanaco nanaco = new BusinessParameter.nanaco();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_NANACO_REMAIN);
        businessParameter.money = nanaco;
        nanaco.oldSid = null;
        nanaco.value = null;
        nanaco.training = "OFF";
        nanaco.cancelStopFlg = "OFF";
        nanaco.uiGuideline = "ON";
        nanaco.inProgressUI = "ON";
*/
/*
        // nanaco 現金チャージ
        BusinessParameter.nanaco nanaco = new BusinessParameter.nanaco();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_NANACO_CHARGE);
        businessParameter.money = nanaco;
        nanaco.oldSid = null;
        nanaco.value = "2";
        nanaco.training = "OFF";
        nanaco.cancelStopFlg = "OFF";
        nanaco.uiGuideline = "ON";
        nanaco.inProgressUI = "ON";
 */
/*
        // nanaco 支払
        BusinessParameter.nanaco nanaco = new BusinessParameter.nanaco();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_NANACO_PAY);
        businessParameter.money = nanaco;
        nanaco.oldSid = null;
        nanaco.value = "1";
        nanaco.totalAmountDischargeFlg = "OFF";
        nanaco.otherCardUseFlg = "OFF";
        nanaco.training = "OFF";
        nanaco.cancelStopFlg = "OFF";
        nanaco.uiGuideline = "ON";
        nanaco.inProgressUI = "ON";
*/
/*
        // nanaco 前回取引確認（未確認）
        BusinessParameter.nanaco nanaco = new BusinessParameter.nanaco();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_NANACO_PREV_TRAN);
        businessParameter.money = nanaco;
        nanaco.oldSid = "510140700";
        nanaco.value = null;
        nanaco.training = "OFF";
*/
/*
        // nanaco 日計
        BusinessParameter.nanaco nanaco = new BusinessParameter.nanaco();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_NANACO_JOURNAL);
        businessParameter.money = nanaco;
//        nanaco.daTermFrom = "";
        nanaco.training = "OFF";
*/
/*
        // Edy 初回通信業務
        BusinessParameter.Edy edy = new BusinessParameter.Edy();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_EDY_FIRST_COMM);
        businessParameter.money = edy;
        edy.value = null;
*/
/*
        // Edy 入金認証（入金認証後、自動で入金を行う）
        BusinessParameter.Edy edy = new BusinessParameter.Edy();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_EDY_CHARGE_AUTH);
        businessParameter.money = edy;
        edy.value = null;
 */
/*
        // Edy 支払
        BusinessParameter.Edy edy = new BusinessParameter.Edy();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_EDY_PAY);
        businessParameter.money = edy;
        edy.value = "1";
        edy.retryFlg = "OFF";
        edy.totalAmountDischargeFlg = "OFF";
        edy.otherCardUseFlg = "OFF";
        edy.training = "OFF";
        edy.uiGuideline = "ON";
        edy.inProgressUI = "ON";
*/
/*
        // Edy 残額照会
        BusinessParameter.Edy edy = new BusinessParameter.Edy();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_EDY_REMAIN);
        businessParameter.money = edy;
        edy.value = null;
        edy.training = "OFF";
        edy.uiGuideline = "ON";
        edy.inProgressUI = "ON";
*/
/*
        // Edy 業務処理状態応答
        BusinessParameter.Edy edy = new BusinessParameter.Edy();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_EDY_STATUS_REPLY);
        businessParameter.money = edy;
        edy.value = null;
        edy.training = "OFF";
        edy.oldSid = "510152000";
*/
/*
        // Edy 日計
        BusinessParameter.Edy edy = new BusinessParameter.Edy();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_EDY_JOURNAL);
        businessParameter.money = edy;
        edy.value = null;
        edy.daTermFrom = "210607112805";
        edy.training = "OFF";
 */
/*
        // QUICPay 支払
        BusinessParameter.QUICPay quicPay = new BusinessParameter.QUICPay();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_QUICPAY_PAY);
        businessParameter.money = quicPay;
        quicPay.value = "1";
        quicPay.training = "OFF";
        quicPay.uiGuideline = "ON";
        quicPay.inProgressUI = "ON";
*/
/*
        // QUICPay 取消
        BusinessParameter.QUICPay quicPay = new BusinessParameter.QUICPay();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_QUICPAY_REFUND);
        businessParameter.money = quicPay;
        quicPay.value = "1";
        quicPay.slipNo = "7";
        quicPay.oldTermIdentId = "9999901200069";
        quicPay.training = "OFF";
        quicPay.uiGuideline = "ON";
        quicPay.inProgressUI = "ON";
*/
/*
        // QUICPay 履歴出力業務
        BusinessParameter.QUICPay quicPay = new BusinessParameter.QUICPay();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_QUICPAY_HISTORY);
        businessParameter.money = quicPay;
        quicPay.training = "OFF";
        quicPay.uiGuideline = "ON";
        quicPay.inProgressUI = "ON";
*/
/*
        // QUICPay 業務処理状態応答
        BusinessParameter.QUICPay quicPay = new BusinessParameter.QUICPay();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_QUICPAY_STATUS_REPLY);
        businessParameter.money = quicPay;
        quicPay.value = null;
        quicPay.oldSid = "510141509";
        quicPay.training = "OFF";
*/
/*
        // QUICPay 日計
        BusinessParameter.QUICPay quicPay = new BusinessParameter.QUICPay();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_QUICPAY_JOURNAL);
        businessParameter.money = quicPay;
        quicPay.value = null;
        quicPay.daTermForm = "210510155916";
        quicPay.training = "OFF";
*/

        _businessId = Integer.parseInt(businessParameter.businessId);

        try {
            if (_sid != null) {
                _icasClient.OnStart(businessParameter, _sid);
            } else {
                _icasClient.OnStart(businessParameter);
            }
        } catch (JSONException | KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | InterruptedException e) {
            Timber.e(e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _icasClient.SetRWUIEventListener(null);

        _actionBarController.cleanup();
        _lcd1Controller.cleanup();
        _lcd2Controller.cleanup();
        _lcd3Controller.cleanup();
        _soundController.cleanup();

        if (_sharedViewModel != null) {
            // アクションバーの状態戻すため
            _sharedViewModel.setUpdatedFlag(true);
            _sharedViewModel.setScreenInversion(false);
        }

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
    public void onCancelClick(View view) {
        if (requireActivity().findViewById(R.id.btn_emoney_cancel).getVisibility() != View.VISIBLE) {
            return;
        }

        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {
            if (requireActivity().findViewById(R.id.btn_emoney_cash_together).getVisibility() == View.VISIBLE) { // 現金併用ボタンが表示されている（つまり現金併用確認画面）
                // ここに入るということは、現金併用確認画面で「中止」がタッチされた
                PrinterManager printerManager = PrinterManager.getInstance();
                printerManager.send820_FunctionCodeErrorResult(this.getView(), IFBoxManager.SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_EMONEY, false,
                        IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_SUICA);      //820へ決済中止を通知
            }
        }

        CommonClickEvent.RecordButtonClickOperation(view, true);
        Timber.d("キャンセル開始");
        _isChancel = true;
        _icasClient.OnStop(false);

        if (!_isIcasWorking) {
            back();
        } else {
            _sharedViewModel.setLoading(true);
        }
    }

    @Override
    public void onConfirmationClick(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        back();
    }

    @Override
    public void onWithCashClick(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        _eMoneySuicaViewModel.setShowWithCashAndCancelBtn(false);
        _isFinished = false;
        _transLogger.setCashTogetherAmount(_value);

        BusinessParameter businessParameter = new BusinessParameter();
        BusinessParameter.Suica suica = new BusinessParameter.Suica();
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_SUICA_IDI_PAY);
//            businessParameter.businessId = String.valueOf(_iCASClient.BUSINESS_ID_SUICA_PAY);
//        businessParameter.sid = _iCASClient.MakeSid();
//            businessParameter.value = "1";
//        businessParameter.value = Integer.toString(value);
        businessParameter.money = suica;
        suica.value = Integer.toString(_value);
        suica.idi = null;
        suica.together = "ON";
        suica.oldSid = null;
        suica.uiGuideline = "ON";
        suica.inProgressUI = "ON";
        suica.idi = _IDi;

        _firstContact = true;
        _sharedViewModel.setLoading(true);
        _eMoneySuicaViewModel.setPhase(EMoneySuicaViewModel.Phases.WAITING);

        MainApplication.getInstance().setCashValue(Amount.getFixedAmount() - _value);

        try {
            if (_sid != null) {
                _icasClient.OnStart(businessParameter, _sid);
            } else {
                _icasClient.OnStart(businessParameter);
            }
        } catch (JSONException | KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | InterruptedException e) {
            Timber.e(e);
        }
    }

    @Override
    public void onInversion(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        if (_sharedViewModel != null) {
            _sharedViewModel.inverseScreen();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void OnUIUpdate(DeviceClient.RWParam rwParam) {
        Timber.d("OnUiUpdate");

        // ステータスによってバーの値を表示する時があるので保持しておく
        _bar = rwParam.bar;

        // 一部のパラメータだけ無期限(0)の指示があったりしたので値が入ってるものを使う
        int t = rwParam.lcd1 != null && !rwParam.lcd1[2].equals("0")
                ? Integer.parseInt(rwParam.lcd1[2])
                : rwParam.lcd2 != null && !rwParam.lcd2[2].equals("0")
                ? Integer.parseInt(rwParam.lcd2[2])
                : rwParam.lcd3 != null && !rwParam.lcd3[2].equals("0")
                ? Integer.parseInt(rwParam.lcd3[2])
                : rwParam.bar != null && rwParam.bar[2] != 0
                ? rwParam.bar[2]
                : rwParam.ring != null && rwParam.ring[2] != 0
                ? rwParam.ring[2]
                : 0;

        if (t != 0) {
            Timber.d("タイマーセット: %s秒後", t);
            _handler.removeCallbacksAndMessages(null);

            _handler.postDelayed(() -> {

                if (_eMoneySuicaViewModel.isNotEnough().get()) {
                    Timber.d("タイマー 現金併用・中止ボタン表示");
                    _eMoneySuicaViewModel.setShowWithCashAndCancelBtn(true);
                } else if (_isFinished) {
                    Timber.d("タイマー 戻る");
                    back();
                } else {
                    Timber.d("タイマー処理なし");
                }
            } ,t*1000);
        } else {
            Timber.d("タイマーセットなし");
        }

        if (rwParam.ring != null) {
            _actionBarController.setStatus(rwParam.ring[0], rwParam.ring[1], rwParam.ring[2]);
        }

        if (rwParam.lcd1 != null) {
            final String message = parseLcdMessage(rwParam.lcd1);
            _lcd1Controller.setStatus(message, rwParam.lcd1[2]);
        }

        if (rwParam.lcd2 != null) {
            final String message = parseLcdMessage(rwParam.lcd2);
            _lcd2Controller.setStatus(message, rwParam.lcd2[2]);
        }

        if (rwParam.lcd3 != null) {
            final String message = parseLcdMessage(rwParam.lcd3);
            _lcd3Controller.setStatus(message, rwParam.lcd3[2]);
        }

        final Integer sound = rwParam.sound != null
                ? rwParam.sound[1]
                : null;

        if (sound != null) {
            // 未了は連続鳴動させる
            if (sound == 0x06) {
                if (_currentSound == null || _currentSound != 0x06) {
                    _isUnfinished = true;
                    _eMoneySuicaViewModel.setPhase(EMoneySuicaViewModel.Phases.UNFINISHED);
                    _soundController.setStatus(rwParam.sound[1], true);
                }
            } else {
                // 起動時の「タッチしてください」がUI指示で消されるので無視する
                if (_currentSound == null || !(sound == 0x63 && (_currentSound == 0x00 || _currentSound == 0x01))) {
                    _soundController.setStatus(rwParam.sound[1], false);
                }
            }
            _currentSound = sound;
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void OnStatusChanged(DeviceClient.Status status) {
        Timber.d("OnStatusChanged");
        if (status.status.equals("1")) {
            _isIcasWorking = true;
            if (_firstContact) {
                final BusinessType type = MainApplication.getInstance().getBusinessType();
                if (type == BusinessType.BALANCE) {
                    _currentSound = 0x01;
                } else {
                    _transLogger.setAntennaLevel();   //アンテナレベルを取得
                    _currentSound = 0x00;
                }
                _soundController.setStatus(_currentSound, false);    // 残高照会
                _firstContact = false;
                _sharedViewModel.setLoading(false);
            }

            if (_isUnfinished) {
                _eMoneySuicaViewModel.setPhase(EMoneySuicaViewModel.Phases.UNFINISHED);
            } else {
                _eMoneySuicaViewModel.setPhase(EMoneySuicaViewModel.Phases.WAITING);
            }

            _actionBarController.setShowIndicatorArrow(true);
        } else if (status.status.equals("2")) {
            _actionBarController.setShowIndicatorArrow(false);
            _eMoneySuicaViewModel.setPhase(EMoneySuicaViewModel.Phases.CONNECTING);
            AppPreference.setConfirmNormalEmoney(false);
            AppPreference.setConfirmObstacleBusinessid(_businessId);
            AppPreference.setConfirmObstacleSid(_icasClient.GetSid());
            AppPreference.setConfirmAmount();
        } else if (status.status.equals("3")) {
            _eMoneySuicaViewModel.setCancelable(true);
            _actionBarController.setShowIndicatorArrow(false);
            _isIcasWorking = false;

            if (_bar != null) {
                _actionBarController.setStatus(_bar[0], _bar[1], 0);
            }

            // 表示を固定する
            _lcd1Controller.cleanup();
            _lcd2Controller.cleanup();
            _lcd3Controller.cleanup();
        }

        _currentStatus = status.status;
    }
    @Override
    public void OnDisplay(DeviceClient.Display display) {
        Timber.d("OnDisplay");
    }
    @Override
    public void OnOperation(DeviceClient.Operation operation) {
        Timber.d("OnOperation");
    }
    @Override
    public void OnCancelDisable(boolean bDisable) {
        Timber.d("OnCancelDisable");
        _eMoneySuicaViewModel.setCancelable(false);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void OnResultSuica(DeviceClient.Result resultSuica) {
        Timber.d("OnResultSuica");
        _isFinished = true;
        _isUnfinished = false;

        if (MainApplication.getInstance().getBusinessType() == BusinessType.REFUND && _slipId != null) {
            _transLogger.setRefundParam(_slipId);
        }
        if(resultSuica.result.equals("true")) {
            //正常終了
            MainApplication.getInstance().setErrorCode(null);

            if (resultSuica.time != null) {
                //残高照会以外の正常終了

                _transLogger.setTransResult(TransMap.RESULT_SUCCESS, TransMap.DETAIL_NORMAL);   //取引結果
                _transLogger.suica(resultSuica);
                //取引伝票印刷
                _usePrinter = true;
                if (MainApplication.getInstance().getBusinessType() == BusinessType.PAYMENT) {
                    // クリア前に現金分割分を現金併用金額に加算
                    MainApplication.getInstance().setCashValue(MainApplication.getInstance().getCashValue() + Amount.getCashAmount());
                    Amount.reset();
                }
            }

            _eMoneySuicaViewModel.setPhase(EMoneySuicaViewModel.Phases.COMPLETED);
        } else {
            Timber.e("resultSuica code: %s", resultSuica.code);

//            _value = AppPreference.isInput1yenEnabled()
            _value = AppPreference.isWishcash1yenEnabled()
                    ? _balance
                    : (_balance/10)*10;

            MainApplication.getInstance().setCashValue(0);

            if (Integer.parseInt(resultSuica.code) == JremRasErrorCodes.E88 && _value > 0) {
                // 残高不足
                // 残高不足は現金併用ができるのでエラーとして扱わない
                _eMoneySuicaViewModel.setPhase(EMoneySuicaViewModel.Phases.NOT_ENOUGH);
                _IDi = resultSuica.IDi;
            } else if(Integer.parseInt(resultSuica.code) == JremRasErrorCodes.E353) {
                _isNoEndError = true;

                if(resultSuica.IDi != null){
                    //処理未了
                    _eMoneySuicaViewModel.setPhase(EMoneySuicaViewModel.Phases.ERROR);
                    _transLogger.setTransResult(TransMap.RESULT_UNFINISHED, TransMap.DETAIL_UNFINISHED);   //取引結果
                }else{
                    //通信障害※カード番号Null
                    //OnFinishedが呼ばれないためここで緯度経度を取得 処理時間は0固定
//                    _transLogger.setProcTime(0, 0);
                    _transLogger.setTransResult(TransMap.RESULT_UNFINISHED, TransMap.DETAIL_COMMUNICATION_FAILURE);   //取引結果
                }

                _transLogger.suica(resultSuica);
                //取引伝票印刷
                _usePrinter = true;
            } else if(resultSuica.code.equals(String.valueOf(JremRasErrorCodes.E370))) {
                // キャンセル時は処理なし
            } else {
                _eMoneySuicaViewModel.setPhase(EMoneySuicaViewModel.Phases.ERROR);
            }

            // (POS有効時) E95,E10095,E370の時にエラーコードが設定されていない状態でアラートダイアログの設定を行うため 先にセットする
            if(AppPreference.isServicePos()){
                if(!resultSuica.code.equals(String.valueOf(JremRasErrorCodes.E370))) {
                    String errorCode = JremRasErrorMap.get(Integer.parseInt(resultSuica.code)); //MCエラーコードに変換
                    MainApplication.getInstance().setErrorCode(errorCode);
                }
            }

            //ADD-S BMT S.Oyama 2025/03/25 フタバ双方向向け改修
            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {              //フタバD時
                switch(Integer.parseInt(resultSuica.code))
                {
                    case JremRasErrorCodes.E353:            //処理未了
                        //x59（中断）は飛ばさない
                        break;
                    default:                                //それ以外のエラー
                        boolean notEnough = false;
                        boolean balanceIsZero = false;
                        if (MainApplication.getInstance().getBusinessType() != BusinessType.BALANCE) {
                            notEnough = Integer.parseInt(resultSuica.code) == JremRasErrorCodes.E88 ? true : false;
                            balanceIsZero = _value <= 0 ? true : false;
                            if (notEnough == true && balanceIsZero == false) { // 残高不足（残高はあるが決済金額未満）
                                //x59（中断）は飛ばさない
                            } else {
                                if (_is820ResetAbort == false) {    //820リセット中止要求フラグがfalseの場合
                                    PrinterManager printerManager = PrinterManager.getInstance();
                                    printerManager.send820_FunctionCodeErrorResult(this.getView(), IFBoxManager.SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_EMONEY, false,
                                            IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_SUICA);      //820へ決済中止を通知
                                } else                                //820リセット中止要求フラグがtrueの場合(キャンセルボタンイベントに乗っかって処理させたので，飛び先をホームにするため_isChancelをfalseにする)
                                {
                                    _isChancel = false;
                                }
                            }
                        }

                        if (notEnough == false) { // 残高不足ではない
                            back();
                        }
                        break;
                }
            } else {                                                                    //フタバD以外
                switch (Integer.parseInt(resultSuica.code)) {
                    case JremRasErrorCodes.E95:
                    case JremRasErrorCodes.E10095:
                    case JremRasErrorCodes.E370:
                        back();
                        break;
                    default:
                        break;
                }
            }
            //ADD-E BMT S.Oyama 2025/03/25 フタバ双方向向け改修

            // POSではないときはそのまま
            if(!AppPreference.isServicePos()) {
                if (!resultSuica.code.equals(String.valueOf(JremRasErrorCodes.E370))) {
                    String errorCode = JremRasErrorMap.get(Integer.parseInt(resultSuica.code)); //MCエラーコードに変換
                    MainApplication.getInstance().setErrorCode(errorCode);
                }
            }
        }
    }
    @Override
    public void OnResultID(DeviceClient.ResultID resultID) {
        Timber.d("OnResultID");
    }
    @Override
    public void OnResultWAON(DeviceClient.ResultWAON resultWAON) {
        Timber.d("OnResultWAON");
    }
    @Override
    public void OnResultQUICPay(DeviceClient.ResultQUICPay resultQUICPay) {
        Timber.d("OnResultQUICPay");
    }
    @Override
    public void OnResultEdy(DeviceClient.ResultEdy resultEdy) {
        Timber.d("OnResultEdy");
    }
    @Override
    public void OnResultnanaco(DeviceClient.Resultnanaco resultnanaco) {
        Timber.d("OnResultnanaco");
        NavigationWrapper.popBackStack(this);
    }
    @Override
    public void OnJournalEdy(String daTermTo) {
        Timber.d("OnJournalEdy");
    }
    @Override
    public void OnJournalnanaco(String daTermTo) {
        Timber.d("OnJournalnanaco");
    }
    @Override
    public void OnJournalQUICPay(String daTermTo) {
        Timber.d("OnJournalQUICPay");
    }
    @Override
    public long OnTransmitRW(byte[] command, long timeout, byte[] response) {
        Timber.d("OnTransmitRW");

        FelicaManager felicaManager = new FelicaManager();
        byte[] felicaResponse;

        //FeliCaコマンド実行
        felicaResponse = felicaManager.executeCommand(command, timeout);

        System.arraycopy(felicaResponse, 0, response, 0, felicaResponse.length);
        return felicaResponse.length;
    }
    @Override
    public void OnFinished(int statusCode) {
        if (MainApplication.getInstance().getBusinessType() != BusinessType.BALANCE) {
            _transLogger.setProcTime(_icasClient.GetProcTimeMillseconds(), _icasClient.GetPinTimeMillseconds());
        }
        AppPreference.setConfirmNormalEmoney(true);
        AppPreference.setConfirmObstacleBusinessid(-1);
        AppPreference.setConfirmObstacleSid(null);
        AppPreference.removeConfirmAmount();
        AppPreference.setPoweroffTrans(false);

        Timber.d("OnFinished");
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void OnErrorOccurred(long lErrorType, final String errorMessage) {
        Timber.d("OnErrorOccurred");

        AppPreference.setConfirmNormalEmoney(true);
        AppPreference.setConfirmObstacleBusinessid(-1);
        AppPreference.setConfirmObstacleSid(null);
        AppPreference.removeConfirmAmount();
        AppPreference.setPoweroffTrans(false);

        MainApplication.getInstance().setCashValue(0);

        if (lErrorType == iCASClient.ERROR_CLIENT_CANCEL) {
            Timber.d("キャンセル完了");
        } else {
            if(_isNoEndError) {
                String errorCode = JremRasErrorMap.get(JremRasErrorCodes.E353);
                MainApplication.getInstance().setErrorCode(errorCode);
            } else {
                Timber.e("エラー発生 lErrorType: %s, errorMessage: %s", lErrorType, errorMessage);
                MainApplication.getInstance().setErrorCode(iCASErrorMap.get((int) lErrorType));
                if (MainApplication.getInstance().getBusinessType() != BusinessType.BALANCE) {
                    PrinterManager printerManager = PrinterManager.getInstance();
                    printerManager.send820_FunctionCodeErrorResult(this.getView(), IFBoxManager.SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_EMONEY, false,
                            IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_SUICA);      //820へ決済中止を通知
                }
            }
        }

        back();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void OnRecovery(Object result) {
        Timber.d("OnRecovery");
        OnResultSuica((DeviceClient.Result) result);
        back();
    }

    private void back() {
        if (!_backFlag) {
            _backFlag = true;
            _sharedViewModel.setLoading(false);

            final BusinessType type = MainApplication.getInstance().getBusinessType();

            final Bundle params = new Bundle();

            if (_usePrinter) {
                if (MainApplication.getInstance().getBusinessType() != BusinessType.BALANCE) {
                    _transLogger.setProcTime(_icasClient.GetProcTimeMillseconds(), _icasClient.GetPinTimeMillseconds());
                }

                if (AppPreference.isPosTransaction()) {
                    // 通常の取引レコード以外の取引情報を作成する（insertでカード暗号化される前にカード番号が欲しいので先に実行）
                    OptionalTransFacade optionalTransFacade = new OptionalTransFacade(MoneyType.JR);
                    optionalTransFacade = _transLogger.setDataForFacade(optionalTransFacade);
                    _slipId = _transLogger.insert();
                    params.putInt("slipId", _slipId);
                    optionalTransFacade.CreateReceiptData(_slipId); // 取引明細書、取消票、領収書のデータを作成
                    optionalTransFacade.CreateByUriData(); // DBにセット
                } else {
                    _slipId = _transLogger.insert();
                    params.putInt("slipId", _slipId);
                }

                if (AppPreference.isTicketTransaction()) {
                    // チケット販売時の取引情報を作成する
                    OptionalTicketTransFacade optionalTicketTransFacade = new OptionalTicketTransFacade(MoneyType.JR);
                    optionalTicketTransFacade = _transLogger.setTicketDataForFacade(optionalTicketTransFacade);
                    optionalTicketTransFacade.CreateReceiptData(_slipId); // 取引明細書、取消票、領収書のデータを作成
                }
            }

            if (AppPreference.isServicePos() || AppPreference.isServiceTicket()) {
                navigateToMenuOrPopBack(params);
            } else if (!_isChancel) {
                NavigationWrapper.navigate(this, R.id.action_navigation_emoney_suica_to_navigation_menu, params);
            } else {
                //ADD-S BMT S.Oyama 2024/09/20 フタバ双方向向け改修
// onResultで実施するためここは削除
//                PrinterManager printerManager = PrinterManager.getInstance();
//                printerManager.send820_FunctionCodeErrorResult(this.getView(), IFBoxManager.SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_EMONEY , false);      //820へ決済中止を通知

                //Amount.setFlatRateAmount(0);        // 分別払いからの遷移でないときは定額金額をクリア
                NavigationWrapper.popBackStack(this);
                //ADD-E BMT S.Oyama 2024/09/20 フタバ双方向向け改修
            }
        }
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
        NavigationWrapper.navigate(this, R.id.action_navigation_emoney_suica_to_navigation_menu);
    }
    //ADD-E BMT S.Oyama 2024/09/20 フタバ双方向向け改修

    private void navigateToMenuOrPopBack(@Nullable Bundle params) {

        // 伝票印刷
        Integer slipId = null;
        if (params != null && params.containsKey("slipId")) {
            slipId = params.getInt("slipId");
        }
        PostPaymentProcess.getInstance().execute(requireActivity(), slipId);

        if (_usePrinter && !_isNoEndError) {
            // 残高照会以外の成功時はメニューに遷移
            NavigationWrapper.navigate(this, R.id.action_navigation_emoney_suica_to_navigation_menu);
        } else if (AppPreference.isTicketTransaction() && _isNoEndError) {
            // チケット購入の決済または取消時の処理未了発生した場合、トップメニュー画面に遷移
            NavigationWrapper.navigate(this, R.id.action_navigation_emoney_suica_to_navigation_menu);
        } else {
            // 上記以外はポップバック
            NavigationWrapper.popBackStack(this);
        }
    }

    private String parseLcdMessage (String[] lcd) {
        String message = JremLcdTextMap.get(lcd[0]);

        if (message.contains(JremLcdTextMap.DISPLAYED_HEAD)) {
            String price = lcd[1].substring(lcd[1].length() - 7)
                    .substring(0, 6).replaceAll(" ", "");

            price = Converters.integerToNumberFormat(Integer.valueOf(price));

            ((TextView) requireView().findViewById(R.id.text_emoney_head_amount)).setText(
                    price + MainApplication.getInstance().getString(R.string.yen));

            message = message.replace(JremLcdTextMap.DISPLAYED_HEAD, price);
        } else if (message.contains(JremLcdTextMap.DISPLAYED_BALANCE)) {
            String price = lcd[1].substring(lcd[1].length() - 7)
                    .substring(0, 6).replaceAll(" ", "");

            _balance = Integer.parseInt(price);
            price = Converters.integerToNumberFormat(Integer.valueOf(price));

            message = message.replace(JremLcdTextMap.DISPLAYED_BALANCE, price);
        } else if (message.contains(JremLcdTextMap.DISPLAYED_LCD)) {
            String price = lcd[1].substring(lcd[1].length() - 7)
                    .substring(0, 6).replaceAll(" ", "");

            price = Converters.integerToNumberFormat(Integer.valueOf(price));

            message = message.replace(JremLcdTextMap.DISPLAYED_LCD, price);
        }

        return message;
    }
}