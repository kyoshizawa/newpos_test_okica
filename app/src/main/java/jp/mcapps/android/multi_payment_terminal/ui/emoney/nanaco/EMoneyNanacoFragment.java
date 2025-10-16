package jp.mcapps.android.multi_payment_terminal.ui.emoney.nanaco;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

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
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentEmoneyNanacoBinding;
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

public class EMoneyNanacoFragment extends BaseFragment implements EMoneyNanacoEventHandlers, IiCASClient {
    public static EMoneyNanacoFragment newInstance() {
        return new EMoneyNanacoFragment();
    }
    private final String SCREEN_NAME = "nanaco";

    public static int FORCE_CANCEL_TIMEOUT = 60*1000;  // 中止ボタンを押してから強制的に画面を抜けるまでの時間
    private final MainApplication _app = MainApplication.getInstance();
    private final iCASClient _icasClient = iCASClient.getInstance();
    private final Handler _handler = new Handler(Looper.getMainLooper());
    private EMoneyNanacoViewModel _eMoneyNanacoViewModel;
    private SharedViewModel _sharedViewModel;
    private ActionBarController _actionBarController;
    private LcdController _lcd1Controller;
    private LcdController _lcd2Controller;
    private LcdController _lcd3Controller;
    private SoundController _soundController;
    private int _t = 0;
    private Observer<Boolean> _cancelObserver = null;
    private int _balance = 0;
    private String _currentStatus = "";
    private boolean _backFlag = false;
    private boolean _firstContact = true;
    private int _amount = 0;
    private Integer[] _bar;
    private boolean _isIcasWorking = false;
    private String _sid = null;
    private Integer _slipId = null;
    private boolean _usePrinter = false;
    private int _businessId = -1;
    private Integer _value;
    private TransLogger _transLogger;
    private Integer _currentSound = null;
    private boolean _isChancel = false;
    private boolean _isNoEndError = false;   // 処理未了フラグ（OnError時に未了が発生していたか判別）
    //ADD-S BMT S.Oyama 2025/02/07 フタバ双方向向け改修
    private boolean _is820ResetAbort = false;  // 820側でリセット応答を750へ送付してきた場合（スキャン中の空車等）
    //ADD-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        MainApplication.getInstance().setErrorCode(null);

        final FragmentEmoneyNanacoBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_emoney_nanaco, container, false);

        final FragmentActivity activity = getActivity();
        if (activity != null) {
            _sharedViewModel =
                    new ViewModelProvider(activity).get(SharedViewModel.class);

            binding.setSharedViewModel(_sharedViewModel);
        }

        _eMoneyNanacoViewModel = new ViewModelProvider(this).get(EMoneyNanacoViewModel.class);

        binding.setViewModel(_eMoneyNanacoViewModel);

        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.setHandlers(this);

        _lcd1Controller = new LcdController(_eMoneyNanacoViewModel.getLcd1());
        _lcd2Controller = new LcdController(_eMoneyNanacoViewModel.getLcd2());
        _lcd3Controller = new LcdController(_eMoneyNanacoViewModel.getLcd3());

        HashMap<Integer, Integer> soundMap = new HashMap<Integer, Integer>();

        soundMap.put(0x00, R.raw.emoney_touch_default);  // かざし待ち
        soundMap.put(0x01, R.raw.emoney_touch_only);     // タッチ（残高照会用）
        soundMap.put(0x03, R.raw.stopsettlement);        // 警告音
        soundMap.put(0x04, R.raw.complete_nanaco);       // 決済音（"nanaco”）
        soundMap.put(0x06, R.raw.unfinished);            // エラー発生（注意音）
        soundMap.put(0x63, null);  // 鳴動停止

        _soundController = new SoundController(soundMap);

        _transLogger = new TransLogger();

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
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
                        Timber.i("[FUTABA-D]EMoneyNanacoFragment:750<-820 meter_data event cmd:%d", meter.meter_sub_cmd);

                        if (meter.meter_sub_cmd == 2)           //820よりリセットを送られてきた場合
                        {
                            Timber.i("[FUTABA-D]EMoneyNanacoFragment:!!820 Recv Reset event");
                            _is820ResetAbort = true;
                            view.post(() -> {
                                onCancelClick(view);
                            });
                        }
                    });
        }
        //ADD-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修

        _sharedViewModel.setLoading(true);


        BusinessParameter businessParameter = new BusinessParameter();
        BusinessParameter.nanaco nanaco = new BusinessParameter.nanaco();
        businessParameter.money = nanaco;

        final BusinessType type = MainApplication.getInstance().getBusinessType();

        if (type == BusinessType.PAYMENT) {
            // nanaco 支払
            businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_NANACO_PAY);
            nanaco.oldSid = null;
            nanaco.value = String.valueOf(Amount.getFixedAmount());
            nanaco.totalAmountDischargeFlg = "OFF";
            nanaco.otherCardUseFlg = "OFF";
            nanaco.training = "OFF";
            nanaco.cancelStopFlg = "OFF";
            nanaco.uiGuideline = "ON";
            nanaco.inProgressUI = "ON";
        }
        else if (type == BusinessType.BALANCE) {
            // nanaco 残高確認
            businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_NANACO_REMAIN);
            nanaco.oldSid = null;
            nanaco.value = null;
            nanaco.training = "OFF";
            nanaco.cancelStopFlg = "OFF";
            nanaco.uiGuideline = "ON";
            nanaco.inProgressUI = "ON";
        }
        // その他
        else {
            throw new IllegalStateException("取引種別がない");
        }

        _businessId = Integer.parseInt(businessParameter.businessId);

        try {
            _icasClient.SetRWUIEventListener(this);
            _icasClient.OnStart(businessParameter);
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
                        IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_NANACO);      //820へ決済中止を通知
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
        _eMoneyNanacoViewModel.setShowWithCashAndCancelBtn(false);

        _transLogger.setCashTogetherAmount(_value);

        BusinessParameter businessParameter = new BusinessParameter();
        BusinessParameter.nanaco nanaco  = new BusinessParameter.nanaco();
        businessParameter.money = nanaco;
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_NANACO_PAY);

        // 全額引去は10円丸めをこちらで自由にできないので丸めた金額で新規引き去りを行う
        nanaco.oldSid = null;
        nanaco.value = String.valueOf(_value);
        nanaco.totalAmountDischargeFlg = "OFF";
        nanaco.otherCardUseFlg = "OFF";
        nanaco.training = "OFF";
        nanaco.cancelStopFlg = "OFF";
        nanaco.uiGuideline = "ON";
        nanaco.inProgressUI = "ON";

        _firstContact = true;
        _sharedViewModel.setLoading(true);
        _eMoneyNanacoViewModel.setPhase(EMoneyNanacoViewModel.Phases.WAITING);

        _app.setCashValue(Amount.getFixedAmount() - _value);

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
        Timber.d("OnUIUpdate");

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

                if (_eMoneyNanacoViewModel.isNotEnough().get()) {
                    Timber.d("タイマー 現金併用・中止ボタン表示");
                    _eMoneyNanacoViewModel.setShowWithCashAndCancelBtn(true);
                } else if (_currentStatus.equals("3")) {
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
                    _eMoneyNanacoViewModel.setPhase(EMoneyNanacoViewModel.Phases.WAITING);
                    _soundController.setStatus(rwParam.sound[1], true);
                    _actionBarController.setShowIndicatorArrow(true);
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
            _transLogger.setAntennaLevel();   //アンテナレベルを取得
            _isIcasWorking = true;
            if (_firstContact) {
                final BusinessType type = _app.getBusinessType();

                if (type == BusinessType.BALANCE) {
                    _currentSound = 0x01;
                } else {
                    _transLogger.setAntennaLevel();   //アンテナレベルを取得
                    _currentSound = 0x00;
                }

                _soundController.setStatus(_currentSound, false);
                _firstContact = false;
                _sharedViewModel.setLoading(false);
            }

            _eMoneyNanacoViewModel.setPhase(EMoneyNanacoViewModel.Phases.WAITING);
            _actionBarController.setShowIndicatorArrow(true);
        } else if (status.status.equals("2")) {
            _actionBarController.setShowIndicatorArrow(false);
            _eMoneyNanacoViewModel.setPhase(EMoneyNanacoViewModel.Phases.CONNECTING);
            AppPreference.setConfirmNormalEmoney(false);
            AppPreference.setConfirmObstacleBusinessid(_businessId);
            AppPreference.setConfirmObstacleSid(_icasClient.GetSid());
            AppPreference.setConfirmAmount();
        } else if (status.status.equals("3")) {
            _actionBarController.setShowIndicatorArrow(false);
            _isIcasWorking = false;
            _eMoneyNanacoViewModel.setCancelable(true);

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
        Timber.d("OnStatusChanged");
    }
    @Override
    public void OnOperation(DeviceClient.Operation operation) {
        Timber.d("OnOperation");
    }
    @Override
    public void OnCancelDisable(boolean bDisable) {
        Timber.d("OnCancelDisable: %s", bDisable);
        _eMoneyNanacoViewModel.setCancelable(!bDisable);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void OnResultSuica(DeviceClient.Result resultSuica) {
        Timber.d("OnResultSuica");
    }
    @Override
    public void OnResultID(DeviceClient.ResultID resultID) {
        Timber.d("OnResultID");
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void OnResultWAON(DeviceClient.ResultWAON resultWAON) {
        Timber.d("OnResultWAON");
    }
    @Override
    public void OnResultEdy(DeviceClient.ResultEdy resultEdy) {
        Timber.d("OnResultEdy");
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void OnResultQUICPay(DeviceClient.ResultQUICPay resultQuicPay) {
        Timber.d("OnResultQUICPay");
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void OnResultnanaco(DeviceClient.Resultnanaco resultnanaco) {
        Timber.d("OnResultnanaco");
        //Timber.e(new Gson().toJson(resultnanaco));

        if (MainApplication.getInstance().getBusinessType() == BusinessType.REFUND && _slipId != null) {
            _transLogger.setRefundParam(_slipId);
        }

        if(resultnanaco .result.equals("true")) {
            //正常終了
            MainApplication.getInstance().setErrorCode(null);

            if (MainApplication.getInstance().getBusinessType() != BusinessType.BALANCE) {
                //残高照会以外の正常終了

                if (resultnanaco.logfullFlg.equals("true")) {
                    //ログフル
                    MainApplication.getInstance().setErrorCode(getString(R.string.error_type_jrem_ras_4088));
                } else if (resultnanaco.nearfullFlg.equals("true")) {
                    //ニアフル
                    MainApplication.getInstance().setErrorCode(getString(R.string.error_type_nanaco_near_full_warning));
                }

                _transLogger.setTransResult(TransMap.RESULT_SUCCESS, TransMap.DETAIL_NORMAL);   //取引結果
                _transLogger.nanaco(resultnanaco);
                _usePrinter = true;
                if (MainApplication.getInstance().getBusinessType() == BusinessType.PAYMENT) {
                    // クリア前に現金分割分を現金併用金額に加算
                    _app.setCashValue(_app.getCashValue() + Amount.getCashAmount());
                    Amount.reset();
                }
            }

            _eMoneyNanacoViewModel.setPhase(EMoneyNanacoViewModel.Phases.COMPLETED);
        } else {
            Timber.e("resultnanaco code: %s", resultnanaco.code);

//            _value = AppPreference.isInput1yenEnabled()
            _value = AppPreference.isWishcash1yenEnabled()
                    ? _balance
                    : (_balance/10)*10;

            MainApplication.getInstance().setCashValue(0);

            // 残高不足
            if (Integer.parseInt(resultnanaco.code) == JremRasErrorCodes.E88 && _value > 0) {
                // 残高不足は現金併用ができるのでエラーとして扱わない
                _eMoneyNanacoViewModel.setPhase(EMoneyNanacoViewModel.Phases.NOT_ENOUGH);

                _actionBarController.cleanup();
                _lcd1Controller.cleanup();
                _lcd2Controller.cleanup();
                _lcd3Controller.cleanup();
            } else if(Integer.parseInt(resultnanaco.code) == JremRasErrorCodes.E353) {
                _isNoEndError = true;
                _eMoneyNanacoViewModel.setPhase(EMoneyNanacoViewModel.Phases.ERROR);

                if(resultnanaco.saleHistories != null && resultnanaco.saleHistories[0].memberMaskNanacoNum != null){
                    //処理未了
                    _transLogger.setTransResult(TransMap.RESULT_UNFINISHED, TransMap.DETAIL_UNFINISHED);   //取引結果
                }else{
                    //通信障害※カード番号Null
                    _transLogger.setTransResult(TransMap.RESULT_UNFINISHED, TransMap.DETAIL_COMMUNICATION_FAILURE);   //取引結果
                }

                _transLogger.nanaco(resultnanaco);
                //取引伝票印刷
                _usePrinter = true;
            } else if(resultnanaco.code.equals(String.valueOf(JremRasErrorCodes.E370))) {
                // キャンセル時は処理なし
            } else {
                _eMoneyNanacoViewModel.setPhase(EMoneyNanacoViewModel.Phases.ERROR);
            }

            // (POS有効時) E95,E10095,E370の時にエラーコードが設定されていない状態でアラートダイアログの設定を行うため 先にセットする
            if(AppPreference.isServicePos()){
                if(!resultnanaco.code.equals(String.valueOf(JremRasErrorCodes.E370))) {
                    String errorCode = JremRasErrorMap.get(Integer.parseInt(resultnanaco.code)); //MCエラーコードに変換
                    _app.setErrorCode(errorCode);
                }
            }

            //ADD-S BMT S.Oyama 2025/03/25 フタバ双方向向け改修
            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {              //フタバD時
                switch (Integer.parseInt(resultnanaco.code)) {
                    case JremRasErrorCodes.E353:            //処理未了
                        //x59（中断）は飛ばさない
                        break;
                    default:
                        boolean notEnough = false;
                        boolean balanceIsZero = false;
                        if (MainApplication.getInstance().getBusinessType() != BusinessType.BALANCE) {
                            notEnough = Integer.parseInt(resultnanaco.code) == JremRasErrorCodes.E88 ? true : false;
                            balanceIsZero = _value <= 0 ? true : false;
                            if (notEnough == true && balanceIsZero == false) { // 残高不足（残高はあるが決済金額未満）
                                //x59（中断）は飛ばさない
                            } else {
                                //ADD-S BMT S.Oyama 2025/02/07 フタバ双方向向け改修
                                if (_is820ResetAbort == false) {    //820リセット中止要求フラグがfalseの場合
                                    PrinterManager printerManager = PrinterManager.getInstance();
                                    printerManager.send820_FunctionCodeErrorResult(this.getView(), IFBoxManager.SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_EMONEY, false,
                                            IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_NANACO);      //820へ決済中止を通知
                                } else                                //820リセット中止要求フラグがtrueの場合(キャンセルボタンイベントに乗っかって処理させたので，飛び先をホームにするため_isChancelをfalseにする)
                                {
                                    _isChancel = false;
                                }
                                //ADD-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修
                            }
                        }

                        if (notEnough == false) { // 残高不足ではない
                            back();
                        }
                        break;
                }
            }else {                                                                      //フタバDでないとき
                switch (Integer.parseInt(resultnanaco.code)) {
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
                if (!resultnanaco.code.equals(String.valueOf(JremRasErrorCodes.E370))) {
                    String errorCode = JremRasErrorMap.get(Integer.parseInt(resultnanaco.code)); //MCエラーコードに変換
                    MainApplication.getInstance().setErrorCode(errorCode);
                }
            }
        }
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

        if (lErrorType == 1) {
            Timber.d("キャンセル完了");
        } else {
            if(_isNoEndError) {
                String errorCode = JremRasErrorMap.get(JremRasErrorCodes.E353);
                MainApplication.getInstance().setErrorCode(errorCode);
            } else {
                Timber.e("エラー発生 lErrorType: %s, errorMessage: %s", lErrorType, errorMessage);
                // RASのエラーがセットされていたらそっちを優先する
                if (MainApplication.getInstance().getErrorCode() == null) {
                    MainApplication.getInstance().setErrorCode(iCASErrorMap.get((int) lErrorType));
                }
                if (MainApplication.getInstance().getBusinessType() != BusinessType.BALANCE) {
                    PrinterManager printerManager = PrinterManager.getInstance();
                    printerManager.send820_FunctionCodeErrorResult(this.getView(), IFBoxManager.SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_EMONEY, false,
                            IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_NANACO);      //820へ決済中止を通知
                }
            }
        }

        back();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void OnRecovery(Object result) {
        Timber.d("OnRecovery");
        OnResultnanaco((DeviceClient.Resultnanaco) result);
        back();
    }

    private void back() {
        if (!_backFlag) {
            _backFlag = true;
            _sharedViewModel.setLoading(false);

            final Bundle params = new Bundle();

            if (_usePrinter) {
                if (MainApplication.getInstance().getBusinessType() != BusinessType.BALANCE) {
                    _transLogger.setProcTime(_icasClient.GetProcTimeMillseconds(), _icasClient.GetPinTimeMillseconds());
                }

                _slipId = _transLogger.insert();
                params.putInt("slipId", _slipId);
                if (AppPreference.isPosTransaction()) {
                    // 通常の取引レコード以外の取引情報を作成する
                    OptionalTransFacade optionalTransFacade = new OptionalTransFacade(MoneyType.NANACO);
                    optionalTransFacade = _transLogger.setDataForFacade(optionalTransFacade);
                    optionalTransFacade.CreateReceiptData(_slipId); // 取引明細書、取消票、領収書のデータを作成
                    optionalTransFacade.CreateByUriData(); // DBにセット
                }

                if (AppPreference.isTicketTransaction()) {
                    // チケット販売時の取引情報を作成する
                    OptionalTicketTransFacade optionalTicketTransFacade = new OptionalTicketTransFacade(MoneyType.NANACO);
                    optionalTicketTransFacade = _transLogger.setTicketDataForFacade(optionalTicketTransFacade);
                    optionalTicketTransFacade.CreateReceiptData(_slipId); // 取引明細書、取消票、領収書のデータを作成
                }
            }

            if (AppPreference.isServicePos() || AppPreference.isServiceTicket()) {
                navigateToMenuOrPopBack(params);
            } else if (!_isChancel || _usePrinter) {
                NavigationWrapper.navigate(this, R.id.action_navigation_emoney_nanaco_to_navigation_menu, params);
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
        NavigationWrapper.navigate(this, R.id.action_navigation_emoney_nanaco_to_navigation_menu);
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
            NavigationWrapper.navigate(this, R.id.action_navigation_emoney_nanaco_to_navigation_menu);
        } else if (AppPreference.isTicketTransaction() && _isNoEndError) {
            // チケット購入の決済または取消時の処理未了発生した場合、トップメニュー画面に遷移
            NavigationWrapper.navigate(this, R.id.action_navigation_emoney_nanaco_to_navigation_menu);
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

            _amount = Integer.valueOf(price);

            price = Converters.integerToNumberFormat(_amount);

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