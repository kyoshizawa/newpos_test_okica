package jp.mcapps.android.multi_payment_terminal.ui.emoney.edy;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;

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
import io.reactivex.rxjava3.subjects.PublishSubject;
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
import jp.mcapps.android.multi_payment_terminal.data.JremDisplayTextMap;
import jp.mcapps.android.multi_payment_terminal.data.JremLcdTextMap;
import jp.mcapps.android.multi_payment_terminal.data.MoneyType;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentEmoneyEdyBinding;
import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorCodes;
import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorMap;
import jp.mcapps.android.multi_payment_terminal.error.iCASErrorMap;
import jp.mcapps.android.multi_payment_terminal.iCAS.BusinessParameter;
import jp.mcapps.android.multi_payment_terminal.iCAS.IiCASClient;
import jp.mcapps.android.multi_payment_terminal.iCAS.data.DeviceClient;
import jp.mcapps.android.multi_payment_terminal.iCAS.iCASClient;
// import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
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

public class EMoneyEdyFragment extends BaseFragment implements EMoneyEdyEventHandlers, IiCASClient {
    public static EMoneyEdyFragment newInstance() {
        return new EMoneyEdyFragment();
    }
    private final String SCREEN_NAME = "Edy";

    public static int FORCE_CANCEL_TIMEOUT = 60*1000;  // 中止ボタンを押してから強制的に画面を抜けるまでの時間
    private final MainApplication _app = MainApplication.getInstance();
    private final iCASClient _icasClient = iCASClient.getInstance();
    private final Handler _handler = new Handler(Looper.getMainLooper());
    private EMoneyEdyViewModel _eMoneyEdyViewModel;
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
    private boolean _hasError = false;
    private boolean _backFlag = false;
    private boolean _firstContact = true;
    private int _amount = 0;
    private Integer[] _bar;
    private boolean _isIcasWorking = false;
    private String _sid = null;
    private Integer _slipId = null;
    private boolean _usePrinter = false;
    private int _businessId = -1;
    private TransLogger _transLogger;
    private Integer _currentSound;
    private Integer _value;
    private boolean _isChancel = false;
    private boolean _isNoEndError = false;   // 処理未了フラグ（OnError時に未了が発生していたか判別）
    private boolean _isForceBalance = false;  // 強制残高照会実行フラグ
    private PublishSubject<Boolean> _forceBalanceResultNg = PublishSubject.create();
    //ADD-S BMT S.Oyama 2025/02/07 フタバ双方向向け改修
    private boolean _is820ResetAbort = false;  // 820側でリセット応答を750へ送付してきた場合（スキャン中の空車等）
    //ADD-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final FragmentEmoneyEdyBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_emoney_edy, container, false);

        final FragmentActivity activity = getActivity();
        if (activity != null) {
            _sharedViewModel =
                    new ViewModelProvider(activity).get(SharedViewModel.class);

            binding.setSharedViewModel(_sharedViewModel);


        }

        _eMoneyEdyViewModel = new ViewModelProvider(this).get(EMoneyEdyViewModel.class);

        binding.setViewModel(_eMoneyEdyViewModel);

        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.setHandlers(this);

        _lcd1Controller = new LcdController(_eMoneyEdyViewModel.getLcd1());
        _lcd2Controller = new LcdController(_eMoneyEdyViewModel.getLcd2());
        _lcd3Controller = new LcdController(_eMoneyEdyViewModel.getLcd3());

        HashMap<Integer, Integer> soundMap = new HashMap<Integer, Integer>();

        soundMap.put(0x00, R.raw.emoney_touch_default);  // かざし待ち
        soundMap.put(0x01, R.raw.emoney_touch_only);     // タッチ（残高照会用）
        soundMap.put(0x03, R.raw.stopsettlement);        // 警告音
        soundMap.put(0x04, R.raw.complete_edy);          // 決済音（"Edy”）
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
//            IFBoxManager.meterDataV4Disposable_ScanEmoneyQR = PrinterManager.getInstance().getIFBoxManager().
//                    getMeterDataV4().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(meter -> {
//                        Timber.i("[FUTABA-D]EMoneyEdyFragment:750<-820 meter_data event cmd:%d", meter.meter_sub_cmd);
//
//                        if (meter.meter_sub_cmd == 2)           //820よりリセットを送られてきた場合
//                        {
//                            Timber.i("[FUTABA-D]EMoneyEdyFragment:!!820 Recv Reset event");
//                            _is820ResetAbort = true;
//                            view.post(() -> {
//                                onCancelClick(view);
//                            });
//                        }
//                    });
        }
        //ADD-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修

        _sharedViewModel.setLoading(true);


        BusinessParameter businessParameter = new BusinessParameter();
        BusinessParameter.Edy edy = new BusinessParameter.Edy();
        businessParameter.money = edy;

        final BusinessType type = _app.getBusinessType();

        if (type == BusinessType.PAYMENT) {
            businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_EDY_PAY);
            edy.value = String.valueOf(Amount.getFixedAmount());
            edy.retryFlg = "OFF";
            edy.totalAmountDischargeFlg = "OFF";
            edy.otherCardUseFlg = "OFF";
            edy.training = "OFF";
            edy.uiGuideline = "ON";
            edy.inProgressUI = "ON";
        }
        else if (type == BusinessType.BALANCE) {
            businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_EDY_REMAIN);
            edy.value = null;
            edy.training = "OFF";
            edy.uiGuideline = "ON";
            edy.inProgressUI = "ON";
        }
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
//        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {
//            if (IFBoxManager.meterDataV4Disposable_ScanEmoneyQR != null) {
//                IFBoxManager.meterDataV4Disposable_ScanEmoneyQR.dispose();
//                IFBoxManager.meterDataV4Disposable_ScanEmoneyQR = null;
//            }
//        }
        //ADD-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onCancelClick(View view) {
        if (requireActivity().findViewById(R.id.btn_emoney_cancel).getVisibility() != View.VISIBLE) {
            return;
        }

        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {
            if (requireActivity().findViewById(R.id.btn_emoney_cash_together).getVisibility() == View.VISIBLE) { // 現金併用ボタンが表示されている（つまり現金併用確認画面）
                // ここに入るということは、現金併用確認画面で「中止」がタッチされた
//                PrinterManager printerManager = PrinterManager.getInstance();
//                printerManager.send820_FunctionCodeErrorResult(this.getView(), IFBoxManager.SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_EMONEY, false,
//                        IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_EDY);      //820へ決済中止を通知
            }
        }

        CommonClickEvent.RecordButtonClickOperation(view, true);
        Timber.d("キャンセル開始");
        _isChancel = true;
        _icasClient.OnStop(false);
        // 処理未了発生中
        if (_currentSound != null && _currentSound == 0x06) {
            _sharedViewModel.setLoading(true);  // キャンセル完了するまで強制残高照会ができないのでローディング表示
            _eMoneyEdyViewModel.setPhase(EMoneyEdyViewModel.Phases.FORCE_BALANCE);
            _actionBarController.setShowIndicatorArrow(false);
            _actionBarController.setStatus(0, 0, 0);
            _soundController.pause();
        } else if (_eMoneyEdyViewModel.isForceBalance().get()) {
            _soundController.setStatus(0x03, false);
        } else {
            if (!_isIcasWorking) {
                back();
            } else {
                _sharedViewModel.setLoading(true);
            }
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
        _eMoneyEdyViewModel.setShowWithCashAndCancelBtn(false);

        _transLogger.setCashTogetherAmount(_value);

        BusinessParameter businessParameter = new BusinessParameter();
        BusinessParameter.Edy edy  = new BusinessParameter.Edy();
        businessParameter.money = edy;
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_EDY_PAY);

        // 全額引去は10円丸めをこちらで自由にできないので丸めた金額で新規引き去りを行う
        edy.value = String.valueOf(_value);
        edy.retryFlg = "OFF";
        edy.otherCardUseFlg = "OFF";
        edy.totalAmountDischargeFlg = "OFF";
        edy.training = "OFF";
        edy.uiGuideline = "ON";
        edy.inProgressUI = "ON";

        _firstContact = true;
        _sharedViewModel.setLoading(true);
        _eMoneyEdyViewModel.setPhase(EMoneyEdyViewModel.Phases.WAITING);

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

    @Override
    public void onForceBalance(View view) {
        CommonClickEvent.RecordButtonClickOperation(view, true);
        forceBalance();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void OnUIUpdate(DeviceClient.RWParam rwParam) {
        Timber.d("OnUIUpdate");

        // OnDisplayは毎回呼ばれるわけではないので常に初期化する
        _eMoneyEdyViewModel.setDisplayMessage("");

        // displayMessageに表示を切り替えるときに画面がちらつくので少し遅らせる
        // 100回程度計測してOnUIUpdateからOnDisplayが呼ばれるまで大体1～60ミリ秒程なので100ms遅らせる
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

        removeTimer();

        if (t != 0) {
            Timber.d("タイマーセット: %s秒後", t);

            _handler.postDelayed(() -> {

                if (_eMoneyEdyViewModel.isNotEnough().get()) {
                    Timber.d("タイマー 現金併用・中止ボタン表示");
                    _eMoneyEdyViewModel.setShowWithCashAndCancelBtn(true);
                } else if (_hasError ||  _currentStatus.equals("3") && !_eMoneyEdyViewModel.isForceBalance().get()) {
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
            if (_isForceBalance && rwParam.lcd3[0].equals("E01-3-009")) {
                _forceBalanceResultNg.subscribe(b -> {
                    // 強残NGの場合はメッセージを書き換える
                    if (b) {
                        rwParam.lcd3[0] = "E01-3-109";
                    }

                    final String message = parseLcdMessage(rwParam.lcd3);
                    _lcd3Controller.setStatus(message, rwParam.lcd3[2]);
                });
            } else {
                final String message = parseLcdMessage(rwParam.lcd3);
                _lcd3Controller.setStatus(message, rwParam.lcd3[2]);
            }
        }

        final Integer sound = rwParam.sound != null
                ? rwParam.sound[1]
                : null;

        if (sound != null) {
            if (sound == 0x06) {
                if (_currentSound == null || _currentSound != 0x06) {
                    _eMoneyEdyViewModel.setPhase(EMoneyEdyViewModel.Phases.WAITING);
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

            _eMoneyEdyViewModel.setPhase(EMoneyEdyViewModel.Phases.WAITING);
            _actionBarController.setShowIndicatorArrow(true);
        } else if (status.status.equals("2")) {
            _actionBarController.setShowIndicatorArrow(false);
            _eMoneyEdyViewModel.setPhase(EMoneyEdyViewModel.Phases.CONNECTING);
            AppPreference.setConfirmNormalEmoney(false);
            AppPreference.setConfirmObstacleBusinessid(_businessId);
            AppPreference.setConfirmObstacleSid(_icasClient.GetSid());
            AppPreference.setConfirmAmount();
        } else if (status.status.equals("3")) {
            _actionBarController.setShowIndicatorArrow(false);
            _isIcasWorking = false;
            _eMoneyEdyViewModel.setCancelable(true);

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
        _eMoneyEdyViewModel.setDisplayMessage(JremDisplayTextMap.get(display.display[0]));
    }
    @Override
    public void OnOperation(DeviceClient.Operation operation) {
        Timber.d("OnOperation");
    }
    @Override
    public void OnCancelDisable(boolean bDisable) {
        Timber.d("OnCancelDisable");
        _eMoneyEdyViewModel.setCancelable(!bDisable);
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
    public void OnResultQUICPay(DeviceClient.ResultQUICPay resultQUICPay) {
        Timber.d("OnResultQUICPay");
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void OnResultEdy(DeviceClient.ResultEdy resultEdy) {
        Timber.d("OnResultEdy");
        Timber.d(new Gson().toJson(resultEdy));

        if(resultEdy.result.equals("true")) {
            //正常終了
            _app.setErrorCode(null);

            if (_app.getBusinessType() != BusinessType.BALANCE) {
                //残高照会以外の正常終了

                if (resultEdy.nearfullFlg != null && resultEdy.nearfullFlg.equals("true")) {
                    _app.setErrorCode(_app.getString(R.string.error_type_edy_near_full_warning));
                }

                _transLogger.setTransResult(TransMap.RESULT_SUCCESS, TransMap.DETAIL_NORMAL);
                _transLogger.edy(resultEdy);
                _usePrinter = true;
                if (MainApplication.getInstance().getBusinessType() == BusinessType.PAYMENT) {
                    // クリア前に現金分割分を現金併用金額に加算
                    _app.setCashValue(_app.getCashValue() + Amount.getCashAmount());
                    Amount.reset();
                }
            }

            _eMoneyEdyViewModel.setPhase(EMoneyEdyViewModel.Phases.COMPLETED);
        } else {
            Timber.e("resultEdy code: %s", resultEdy.code);

            if (_isForceBalance) {
                _forceBalanceResultNg.onNext(resultEdy.code.equals(String.valueOf(JremRasErrorCodes.E1042)));
            }

//            _value = AppPreference.isInput1yenEnabled()
            _value = AppPreference.isWishcash1yenEnabled()
                    ? _balance
                    : (_balance/10)*10;

            _app.setCashValue(0);

            // 残高不足
            if (Integer.parseInt(resultEdy.code) == JremRasErrorCodes.E88 && _value > 0) {
                // 残高不足は現金併用ができるのでエラーとして扱わない
                _eMoneyEdyViewModel.setPhase(EMoneyEdyViewModel.Phases.NOT_ENOUGH);

                _actionBarController.cleanup();
                _lcd1Controller.cleanup();
                _lcd2Controller.cleanup();
                _lcd3Controller.cleanup();
            } else if(Integer.parseInt(resultEdy.code) == JremRasErrorCodes.E353) {
                _isNoEndError = true;
                _eMoneyEdyViewModel.setPhase(EMoneyEdyViewModel.Phases.ERROR);
                _hasError = true;

                if(resultEdy.saleHistories != null && resultEdy.saleHistories[0].memberMaskMembershipNum != null){
                    //処理未了
                    _transLogger.setTransResult(TransMap.RESULT_UNFINISHED, TransMap.DETAIL_UNFINISHED);   //取引結果
                }else{
                    //通信障害※カード番号Null
                    _transLogger.setTransResult(TransMap.RESULT_UNFINISHED, TransMap.DETAIL_COMMUNICATION_FAILURE);   //取引結果
                }

                _transLogger.edy(resultEdy);
                //取引伝票印刷
                _usePrinter = true;
            } else if(Integer.parseInt(resultEdy.code) == JremRasErrorCodes.E87) {
                boolean doForceBalance = !_eMoneyEdyViewModel.isForceBalance().get()
                        && resultEdy.forcedBalanceFlg != null
                        && (resultEdy.autoRetryFlg.equals("true") || resultEdy.forcedBalanceFlg.equals("true"));

                if (doForceBalance) {
//                    forceBalance();
                    _eMoneyEdyViewModel.setPhase(EMoneyEdyViewModel.Phases.FORCE_BALANCE);
                }
                else if (_eMoneyEdyViewModel.isForceBalance().get()) {
                    // キャンセル処理が完了
                    // 強制残照確認画面のローディングを非表示にする
                    _sharedViewModel.setLoading(false);
                }
            } else if(resultEdy.code.equals(String.valueOf(JremRasErrorCodes.E370))) {
                // キャンセル時は処理なし
            } else {
                _eMoneyEdyViewModel.setPhase(EMoneyEdyViewModel.Phases.ERROR);
                _hasError = true;
            }

            // (POS有効時) E95,E10095,E370の時にエラーコードが設定されていない状態でアラートダイアログの設定を行うため 先にセットする
            if(AppPreference.isServicePos()){
                if(!resultEdy.code.equals(String.valueOf(JremRasErrorCodes.E370))) {
                    String errorCode = JremRasErrorMap.get(Integer.parseInt(resultEdy.code)); //MCエラーコードに変換
                    _app.setErrorCode(errorCode);
                }
            }

            //ADD-S BMT S.Oyama 2025/03/25 フタバ双方向向け改修
            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {              //フタバD時
                switch (Integer.parseInt(resultEdy.code)) {
                    case JremRasErrorCodes.E353:            //処理未了
                    case JremRasErrorCodes.E87:             //書き込み異常
                        //x59（中断）は飛ばさない
                        break;
                    default:                                //それ以外のエラー
                        boolean notEnough = false;
                        boolean balanceIsZero = false;
                        if (MainApplication.getInstance().getBusinessType() != BusinessType.BALANCE) {
                            notEnough = Integer.parseInt(resultEdy.code) == JremRasErrorCodes.E88 ? true : false;
                            balanceIsZero = _value <= 0 ? true : false;
                            if (notEnough == true && balanceIsZero == false) { // 残高不足（残高はあるが決済金額未満）
                                //x59（中断）は飛ばさない
                            } else {
                                if (_is820ResetAbort == false) {    //820リセット中止要求フラグがfalseの場合
//                                    PrinterManager printerManager = PrinterManager.getInstance();
//                                    printerManager.send820_FunctionCodeErrorResult(this.getView(), IFBoxManager.SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_EMONEY, false,
//                                            IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_EDY
//                                    );      //820へ決済中止を通知
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
            } else {                                                                    //フタバDでないとき
                switch (Integer.parseInt(resultEdy.code)) {
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
                if (!resultEdy.code.equals(String.valueOf(JremRasErrorCodes.E370))) {
                    String errorCode = JremRasErrorMap.get(Integer.parseInt(resultEdy.code)); //MCエラーコードに変換
                    _app.setErrorCode(errorCode);
                }
            }
        }
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
        if (_app.getBusinessType() != BusinessType.BALANCE) {
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

        _app.setCashValue(0);

        if (lErrorType == 1) {
            Timber.d("キャンセル完了");
        } else {
            if(_isNoEndError) {
                String errorCode = JremRasErrorMap.get(JremRasErrorCodes.E353);
                _app.setErrorCode(errorCode);
            } else {
                Timber.e("エラー発生 lErrorType: %s, errorMessage: %s", lErrorType, errorMessage);
                _app.setErrorCode(iCASErrorMap.get((int) lErrorType));
                if (MainApplication.getInstance().getBusinessType() != BusinessType.BALANCE) {
//                    PrinterManager printerManager = PrinterManager.getInstance();
//                    printerManager.send820_FunctionCodeErrorResult(this.getView(), IFBoxManager.SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_EMONEY, false,
//                            IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_EDY);      //820へ決済中止を通知
                }
            }
        }

        back();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void OnRecovery(Object result) {
        Timber.d("OnRecovery");
        OnResultEdy((DeviceClient.ResultEdy) result);
        back();
    }

    private void back() {
        if (_eMoneyEdyViewModel.isForceBalance().get()) {
            return;
        }
        if (!_backFlag) {
            _backFlag = true;
            _sharedViewModel.setLoading(false);

            final Bundle params = new Bundle();

            if (_usePrinter) {
                if (_app.getBusinessType() != BusinessType.BALANCE) {
                    _transLogger.setProcTime(_icasClient.GetProcTimeMillseconds(), _icasClient.GetPinTimeMillseconds());
                }

                _slipId = _transLogger.insert();
                params.putInt("slipId", _slipId);

                if (AppPreference.isPosTransaction()) {
                    // 通常の取引レコード以外の取引情報を作成する
                    OptionalTransFacade optionalTransFacade = new OptionalTransFacade(MoneyType.EDY);
                    optionalTransFacade = _transLogger.setDataForFacade(optionalTransFacade);
                    optionalTransFacade.CreateReceiptData(_slipId); // 取引明細書、取消票、領収書のデータを作成
                    optionalTransFacade.CreateByUriData(); // DBにセット
                }

                if (AppPreference.isTicketTransaction()) {
                    // チケット販売時の取引情報を作成する
                    OptionalTicketTransFacade optionalTicketTransFacade = new OptionalTicketTransFacade(MoneyType.EDY);
                    optionalTicketTransFacade = _transLogger.setTicketDataForFacade(optionalTicketTransFacade);
                    optionalTicketTransFacade.CreateReceiptData(_slipId); // 取引明細書、取消票、領収書のデータを作成
                }
            }

            if (AppPreference.isServicePos() || AppPreference.isServiceTicket()) {
                navigateToMenuOrPopBack(params);
            } else if (!_isChancel || _usePrinter) {
                NavigationWrapper.navigate(this, R.id.action_navigation_emoney_edy_to_navigation_menu, params);
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
        NavigationWrapper.navigate(this, R.id.action_navigation_emoney_edy_to_navigation_menu);
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
            NavigationWrapper.navigate(this, R.id.action_navigation_emoney_edy_to_navigation_menu);
        } else if (AppPreference.isTicketTransaction() && _isNoEndError) {
            // チケット購入の決済または取消時の処理未了発生した場合、トップメニュー画面に遷移
            NavigationWrapper.navigate(this, R.id.action_navigation_emoney_edy_to_navigation_menu);
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
                    price + _app.getString(R.string.yen));

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

    private void forceBalance() {
        _isForceBalance = true;
        removeTimer();

        BusinessParameter businessParameter = new BusinessParameter();
        BusinessParameter.Edy edy  = new BusinessParameter.Edy();
        businessParameter.money = edy;
        businessParameter.businessId = String.valueOf(iCASClient.BUSINESS_ID_EDY_FORCE_REMAIN);
        edy.value = null;
        edy.training = "OFF";
        edy.uiGuideline = "ON";
        edy.inProgressUI = "ON";

        _firstContact = true;
        _sharedViewModel.setLoading(true);
        _eMoneyEdyViewModel.setPhase(EMoneyEdyViewModel.Phases.WAITING);

        try {
            _icasClient.OnStart(businessParameter, _icasClient.GetSid());
        } catch (JSONException | KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | InterruptedException e) {
            Timber.e(e);
        }
    }

    private void removeTimer() {
        Timber.d("タイマーリセット");
        _handler.removeCallbacksAndMessages(null);
    }
}