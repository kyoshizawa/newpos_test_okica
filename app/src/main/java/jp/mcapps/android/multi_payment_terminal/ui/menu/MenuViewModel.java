package jp.mcapps.android.multi_payment_terminal.ui.menu;


import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.databinding.ObservableBoolean;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModel;

import com.google.common.base.Strings;
import com.pos.device.emv.EMVHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.CurrentRadio;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.OptionService;
import jp.mcapps.android.multi_payment_terminal.data.sam.Constants;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipDao;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.devices.SamRW;
import jp.mcapps.android.multi_payment_terminal.error.McPosCenterErrorCodes;
import jp.mcapps.android.multi_payment_terminal.error.McPosCenterErrorMap;
import jp.mcapps.android.multi_payment_terminal.logger.EventLogger;
//import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
//import jp.mcapps.android.multi_payment_terminal.model.JremActivator;
//import jp.mcapps.android.multi_payment_terminal.model.JremOpener;
import jp.mcapps.android.multi_payment_terminal.model.McAuthenticator;
//import jp.mcapps.android.multi_payment_terminal.model.McCredit;
import jp.mcapps.android.multi_payment_terminal.model.McTerminal;
import jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl;
import jp.mcapps.android.multi_payment_terminal.model.QRSettlement;
import jp.mcapps.android.multi_payment_terminal.model.Validator;
import jp.mcapps.android.multi_payment_terminal.service.GetRadioService;
//import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterProc;
import jp.mcapps.android.multi_payment_terminal.ui.discount.DiscountJobFutabaDViewModel;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Meter;
//import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvCLProcess;
import timber.log.Timber;

public class MenuViewModel extends ViewModel implements LifecycleObserver {
    //private final IFBoxManager _ifBoxManager;
    private final EventLogger _eventLogger;

    public MenuViewModel(EventLogger eventLogger) {
        //_ifBoxManager = ifBoxManager;
        _eventLogger = eventLogger;
    }

    final private MutableLiveData<MenuTypes> _headType = new MutableLiveData<MenuTypes>(MenuTypes.NONE);

    public MutableLiveData<MenuTypes> getHeadType() {
        return _headType;
    }

    public void setHeadType(MenuTypes menuType) {
        _headType.setValue(menuType);
    }

    final private MutableLiveData<MenuTypes> _bodyType = new MutableLiveData<MenuTypes>(MenuTypes.NONE);

    public MutableLiveData<MenuTypes> getBodyType() {
        return _bodyType;
    }

    public void setBodyType(MenuTypes menuType) {
        _bodyType.setValue(menuType);
    }

    private boolean _driverInfoVisibility = true;

    public boolean getDriverInfoVisibility() {
        return _driverInfoVisibility;
    }

    public void setDriverInfoVisibility(boolean b) {
        _driverInfoVisibility = b;
    }

    private String _driverName = "";

    public String getDriverName() {
        return _driverName;
    }

    public void setDriverName(String driverName) {
        _driverName = driverName;
    }

    private ObservableBoolean _isBalanceMenu = new ObservableBoolean(false);
    public ObservableBoolean isBalanceMenu() { return _isBalanceMenu; }
    public void setBalanceMenu(boolean b) { _isBalanceMenu.set(b); }

    private ObservableBoolean _isChargeMenu = new ObservableBoolean(false);
    public ObservableBoolean isChargeMenu() { return _isChargeMenu; }
    public void setChargeMenu(boolean b) { _isChargeMenu.set(b); }

    private boolean _carIdVisibility = true;
    public boolean getCarIdVisibility() {
        return _carIdVisibility;
    }
    public void setCarIdVisibility(boolean b) {
        _carIdVisibility = b;
    }

    private MutableLiveData<Integer> _totalAmount = new MutableLiveData<>(Amount.getTotalAmount());
    public MutableLiveData<Integer> getTotalAmount()  {
        return _totalAmount;
    }

    private final MutableLiveData<Integer> _cashAmount = new MutableLiveData<>(Amount.getCashAmount());
    public MutableLiveData<Integer> getCashAmount() {
        return _cashAmount;
    }

    private final MutableLiveData<Integer> _ticketAmount = new MutableLiveData<>(Amount.getTicketAmount());
    public MutableLiveData<Integer> getTicketAmount() {
        return _ticketAmount;
    }

    private final MutableLiveData<Integer> _PaidAmount = new MutableLiveData<>(Amount.getPaidAmount());
    public MutableLiveData<Integer> getPaidAmount() {
        return _PaidAmount;
    }

    private final MutableLiveData<Boolean> _useCharge = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> useCharge() {
        if (AppPreference.isDemoMode()) {
            _useCharge.setValue(true);
        } else if (AppPreference.isMoneyOkica() == false) {
            _useCharge.setValue(false);
        }

        return _useCharge;
    }

//    public Completable checkMeterCharge() {
////        return _ifBoxManager.fetchMeter()
////                .timeout(5, TimeUnit.SECONDS)
////                .subscribeOn(Schedulers.io())
////                .observeOn(AndroidSchedulers.mainThread());
//    }

//    public PublishSubject<Meter.ResponseStatus> getMeterStatNotice() {
//        //return _ifBoxManager.getMeterStatNotice();
//    }

//    public PublishSubject<Boolean> getExitManualMode() {
//        return _ifBoxManager.getExitManualMode();
//    }
//
//    public PublishSubject<Boolean> getPrintEndManual() {
//        return _ifBoxManager.getPrintEndManual();
//    }

    private Disposable _meterDisposable;

    //ADD-S BMT S.Oyama 2024/09/18 フタバ双方向向け改修
//    public PublishSubject<Meter.ResponseFutabaD> getMeterDataV4() {
//        //return _ifBoxManager.getMeterDataV4();
//    }
//
//    public MutableLiveData<Boolean> getIsKUUSHA()
//    {
//        //String tmpStatus =_ifBoxManager.getMeterStatus();
//        //return new MutableLiveData<>(tmpStatus.equals("KUUSYA"));
//        return true;
//    }

    public String getAggregateTitle() {
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D)) {
            return MainApplication.getInstance().getString(R.string.btn_history_daily_total_print);
        } else {
            return MainApplication.getInstance().getString(R.string.btn_history_daily_total);
        }
    }
    //ADD-E BMT S.Oyama 2024/09/18 フタバ双方向向け改修


    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        // OKICA以外でチャージできるようになることはなさそうなのでとりあえずOKICAだけ見る
        final boolean useCharge = AppPreference.getOkicaTerminalInfo() != null && AppPreference.getOkicaTerminalInfo().isCharge;
        _useCharge.setValue(useCharge);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        _totalAmount.setValue(Amount.getTotalAmount());
        _cashAmount.setValue(Amount.getCashAmount());
        _ticketAmount.setValue(Amount.getTicketAmount());
        _PaidAmount.setValue(Amount.getPaidAmount());
//        _meterDisposable = _ifBoxManager.getMeterInfo().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(meter -> {
//            _totalAmount.setValue(Amount.getTotalAmount());
//            _cashAmount.setValue(Amount.getCashAmount());
//            _ticketAmount.setValue(Amount.getTicketAmount());
//            _PaidAmount.setValue(Amount.getPaidAmount());
//        });
    }

    //ADD-S BMT S.Oyama 2024/09/18 フタバ双方向向け改修
    public void onResumeExt() {
        _totalAmount.setValue(Amount.getTotalAmount());
        _cashAmount.setValue(Amount.getCashAmount());
        _ticketAmount.setValue(Amount.getTicketAmount());
        _PaidAmount.setValue(Amount.getPaidAmount());
    }
    //ADD-E BMT S.Oyama 2024/09/18 フタバ双方向向け改修

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        if (_meterDisposable != null) {
            _meterDisposable.dispose();
            _meterDisposable = null;
        }
    }

    public MenuHeadActions getHeadAction() {
        MenuTypes bodyType = _bodyType.getValue();
        MenuTypes headType = _headType.getValue();

        Log.d("MenuTypes--------" , String.valueOf(bodyType));
        if(bodyType == MenuTypes.CASH){
            return MenuHeadActions.HIDE;
        }else if (bodyType == MenuTypes.HISTORY || bodyType == MenuTypes.BUSINESS) {
            if (headType == MenuTypes.HEAD) return MenuHeadActions.HIDE;
            else return MenuHeadActions.NONE;
        } else if (bodyType == MenuTypes.HOME) {
            if (headType == MenuTypes.HEAD_EMPTY) return MenuHeadActions.SHOW;
            else return MenuHeadActions.NONE;
        }

        return MenuHeadActions.NONE;
    }

    private MutableLiveData<Drawable> _warningImage = new MutableLiveData<Drawable>(null);
    public MutableLiveData<Drawable> getWarningImage() {
        return _warningImage;
    }
    public void setWarningImage() {
        _warningImage.setValue(MainApplication.getInstance().getDrawable(R.drawable.ic_warning));
    }
    public void setErrorImage() {
        _warningImage.setValue(MainApplication.getInstance().getDrawable(R.drawable.ic_error));
    }
    public void resetWarningImage() {
        _warningImage.setValue(null);
    }

    // 手動開局
    public String[] manualOpening() {
        final ArrayList<String> errors = new ArrayList<>();
        final McAuthenticator authenticator = new McAuthenticator();
        final QRSettlement qr = new QRSettlement();
        final McTerminal terminal = new McTerminal();
//        final McCredit credit = new McCredit();

        // デモモードの場合、手動開局処理は行わない
        if(AppPreference.isDemoMode()){
            final HashSet set = new HashSet(errors);
            final String[] arr = new String[set.size()];
            set.toArray(arr);

            return arr;
        }

        //　通信状態確認
        if (CurrentRadio.getImageLevel() == 0) {
            // 電波強度が弱の場合は、以降の処理は行わずに即時終了、通信エラー表示（1001）
            errors.add(MainApplication.getInstance().getString(R.string.error_type_comm_reception));

            final HashSet set = new HashSet(errors);
            final String[] arr = new String[set.size()];
            set.toArray(arr);

            return arr;
        } else if (CurrentRadio.getImageLevel() == GetRadioService.AIRPLANE_MODE) {
            // 機内モード状態の場合は、以降の処理は行わずに即時終了、通信エラー表示（1003）
            errors.add(MainApplication.getInstance().getString(R.string.error_type_airplane_mode));

            final HashSet set = new HashSet(errors);
            final String[] arr = new String[set.size()];
            set.toArray(arr);

            return arr;
        }

        String errCode = authenticator.authenticate();
        String authErr = errCode;

        if (errCode != null) {
//            if (CreditSettlement.getInstance()._mcCenterCommManager.getTerOpePort() <= 0) {
//                // MC認証チェック
//                errors.add(MainApplication.getInstance().getString(R.string.error_type_payment_system_2094));
//            } else {
//                errors.add(errCode);
//            }
        } else {
            //センター設定情報取得
            errCode = terminal.getTerminalInfo();
            if (errCode != null) {
                errors.add(errCode);
            }
            /*インボイス対応*/
            if (AppPreference.judgeInvoiceStack() && !AppPreference.isServicePos()){
                errors.add(MainApplication.getInstance().getString(R.string.error_type_invoice_info_error));
            }

            if (AppPreference.isMoneyCredit()) {
                if (AppPreference.isMoneyContactless()) {
                    /*
                     * 非接触有効の場合は手動開局時に毎回CA公開鍵を取得する
                     * 無効の場合は既存のまま処理をする
                     */
//                    errCode = credit.getCAKey();
//
//                    if (errCode != null) {
//                        Timber.e("手動開局 CA公開鍵取得エラー");
//                        errors.add(errCode);
//                    } else {
//                        errCode = credit.getRiskParameterContactless();
//                        if (errCode != null) {
//                            Timber.e("手動開局 リスク管理パラメータ取得エラー");
//                            errors.add(errCode);
//                        } else {
//                            if (!EmvCLProcess.emvInit()) {
//                                Timber.e("手動開局 非接触EMVの初期化に失敗");
//                                errors.add(McPosCenterErrorMap.INTERNAL_ERROR_CODE);
//                            }
//                        }
//                    }
                } else {
                    //クレジットCA公開鍵DL DLしていない場合のみ
//                    if (EMVHandler.getInstance().getCAPublicKeyNum() <= 0) {
//                        errCode = new McCredit().getCAKey();
//                        // クレジット決済前にもCA公開鍵DLを行うため、ここでのエラー保存は行わない
//                        errCode = credit.getCAKey();
//                    }
                }
            }

            final OptionService optionService = MainApplication.getInstance().getOptionService();
            if (optionService != null && optionService.isAvailable()) {
                for (OptionService.Func func : optionService.getFuncs()) {
                    if (func.getFuncID() == OptionService.FuncIds.MAGNETIC_CARD_VALIDATION) {
                        new Validator().sendResultSync();
                    }
                }
            }
        }

        final boolean useOtherMoney = AppPreference.isMoneyWaon()
                | AppPreference.isMoneyNanaco()
                | AppPreference.isMoneyEdy()
                | AppPreference.isMoneyId()
                | AppPreference.isMoneyQuicpay();

        if (AppPreference.isMoneySuica() || useOtherMoney) {
            errCode = null;

            final File certFile = new File (
                    MainApplication.getInstance().getFilesDir(), BuildConfig.JREM_CLIENT_CERTIFICATE);

            if (!certFile.exists()) {
//                errCode = new JremActivator().install();
            }

            if (errCode == null) {
//                final JremOpener opener = new JremOpener();
//                if (AppPreference.isMoneySuica()) {
//                    errCode = opener.openingSuica();
//
//                    if (errCode != null) {
//                        errors.add(errCode);
//                    }
//                }
//
//                if (useOtherMoney) {
//                    errors.addAll(opener.openingEmoney());
//                }
            }
        }

        if (authErr == null) {
            //疎通確認
            errCode = terminal.echo();
            if (errCode != null) {
                errors.add(errCode);
            }

            //端末稼働情報連携
            errCode = terminal.postTerminalInfo(0);
            if (errCode != null) {
                errors.add(errCode);
            }

            //売上情報送信
            errCode = terminal.postPayment();
            // 起動時の売上送信エラーは致命的なエラーのみ表示する
            if (errCode != null
            && (errCode.equals(McPosCenterErrorCodes.E0098)
            ||  errCode.equals(McPosCenterErrorCodes.E0901))) {
                errors.add(McPosCenterErrorMap.get(errCode));
            }
            // チケット購入の取消はエラー表示はしなくてよい
            if (AppPreference.isServiceTicket()) {
                errCode = terminal.postTicketCancel();
            }
        }

        if (AppPreference.isMoneyOkica() && !Strings.isNullOrEmpty(AppPreference.getOkicaAccessToken())) {
            final OkicaMasterControl okicaMasterCtrl = new OkicaMasterControl();
            if (okicaMasterCtrl.okicaOpening(errors)) {
                MainApplication.getInstance().isInitFeliCaSAM(
                        SamRW.open(Constants.MC_NORMAL_KEY, SamRW.States.Normal) == SamRW.OpenResult.SUCCESS);
            }
        }

        if (AppPreference.isMoneyQr()) {
            errCode = qr.login();

            if (errCode != null) {
                errors.add(errCode);
            }
        }

        final HashSet set = new HashSet(errors);
        final String[] arr = new String[set.size()];
        set.toArray(arr);

        return arr;
    }

    private final MutableLiveData<Integer> _radioImageResource = new MutableLiveData<>();
    public MutableLiveData<Integer> getRadioImageResource() {
        return _radioImageResource;
    }
    public void setRadioImageResource(int radioImageResource) {
        _radioImageResource.setValue(radioImageResource);
    }

    public EventLogger getEventLogger() {
        return _eventLogger;
    }

    public boolean isMeterDuplex() {
//CHG-S BMT S.Oyama 2024/09/10 フタバ双方向向け改修
        //return IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) || IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D) ;
        boolean fl = IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) ||
                IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D ) ||
                IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D);

        return fl;
//CHG-E BMT S.Oyama 2024/09/10 フタバ双方向向け改修
    }

//ADD-S BMT S.Oyama 2024/09/10 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  金額入力立て替え払い画面表示か？
     * @note   通常の金額画面ではなく、立て替え払い画面を表示するか？（フタバD等向け）
     * @param [in] なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public boolean isAmountAdvancePayMode() {
        boolean fl = IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D);

        return fl;
    }
//ADD-E BMT S.Oyama 2024/09/10 フタバ双方向向け改修

    private final MutableLiveData<Boolean> _isPosActivated = new MutableLiveData<>();
    public LiveData<Boolean> getIsPosActivated() {
        return _isPosActivated;
    }
    public void setIsPosActivated(){
        _isPosActivated.setValue(AppPreference.isServicePos());
    }

    private final MutableLiveData<Boolean> _isCashChangerActivated = new MutableLiveData<>();
    public LiveData<Boolean> getIsCashChangerActivated() {
        return _isCashChangerActivated;
    }
    public void setIsCashChangerActivated(){
        _isCashChangerActivated.setValue(AppPreference.getIsCashChanger());
    }

    // POSデータ更新
    public String[] refreshPosProducts() {
        final ArrayList<String> errors = new ArrayList<>();
        final McAuthenticator authenticator = new McAuthenticator();
        final QRSettlement qr = new QRSettlement();
        final McTerminal terminal = new McTerminal();

        //　通信状態確認
        if (CurrentRadio.getImageLevel() == 0) {
            // 電波強度が弱の場合は、以降の処理は行わずに即時終了、通信エラー表示（1001）
            errors.add(MainApplication.getInstance().getString(R.string.error_type_comm_reception));

            final HashSet<String> set = new HashSet<>(errors);
            final String[] arr = new String[set.size()];
            set.toArray(arr);

            return arr;
        } else if (CurrentRadio.getImageLevel() == GetRadioService.AIRPLANE_MODE) {
            // 機内モード状態の場合は、以降の処理は行わずに即時終了、通信エラー表示（1003）
            errors.add(MainApplication.getInstance().getString(R.string.error_type_airplane_mode));

            final HashSet<String> set = new HashSet<>(errors);
            final String[] arr = new String[set.size()];
            set.toArray(arr);

            return arr;
        }

        final HashSet<String> set = new HashSet<>(errors);
        final String[] arr = new String[set.size()];
        set.toArray(arr);

        return arr;
    }

    private final MutableLiveData<Boolean> _isTicketActivated = new MutableLiveData<>();
    public LiveData<Boolean> getIsTicketActivated() {
        return _isTicketActivated;
    }
    public void setIsTicketActivated(){ _isTicketActivated.setValue(AppPreference.isServiceTicket()); }

    // チケット販売マスタデータ更新
    public String[] refreshTicketSales() {
        final ArrayList<String> errors = new ArrayList<>();
        final McAuthenticator authenticator = new McAuthenticator();
        final QRSettlement qr = new QRSettlement();
        final McTerminal terminal = new McTerminal();

        //　通信状態確認
        if (CurrentRadio.getImageLevel() == 0) {
            // 電波強度が弱の場合は、以降の処理は行わずに即時終了、通信エラー表示（1001）
            errors.add(MainApplication.getInstance().getString(R.string.error_type_comm_reception));

            final HashSet<String> set = new HashSet<>(errors);
            final String[] arr = new String[set.size()];
            set.toArray(arr);

            return arr;
        } else if (CurrentRadio.getImageLevel() == GetRadioService.AIRPLANE_MODE) {
            // 機内モード状態の場合は、以降の処理は行わずに即時終了、通信エラー表示（1003）
            errors.add(MainApplication.getInstance().getString(R.string.error_type_airplane_mode));

            final HashSet<String> set = new HashSet<>(errors);
            final String[] arr = new String[set.size()];
            set.toArray(arr);

            return arr;
        }

        final HashSet<String> set = new HashSet<>(errors);
        final String[] arr = new String[set.size()];
        set.toArray(arr);

        return arr;
    }

//ADD-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  分別チケットのボタンのEnabled制御（フタバD等向け）
     * @note   分別チケットのボタンのEnabled制御（フタバD等向け）
     * @param [in] なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public boolean isSeparateTicketButtonEnabled() {
        //ADD-S BMT S.Oyama 2025/01/07 フタバ双方向向け改修
        //boolean fl = Amount.getTicketAmount() == 0;
        boolean fl = false;
        //ADD-E BMT S.Oyama 2025/01/07 フタバ双方向向け改修
        return fl;
    }

    /******************************************************************************/
    /*!
     * @brief  分別クレジットのボタンのEnabled制御（フタバD等向け）
     * @note   分別クレジットのボタンのEnabled制御（フタバD等向け）
     * @param [in] なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public boolean isSeparateCreditButtonEnabled() {
        return true;
    }

    /******************************************************************************/
    /*!
     * @brief  分別電子マネーのボタンのEnabled制御（フタバD等向け）
     * @note   分別電子マネーのボタンのEnabled制御（フタバD等向け）
     * @param [in] なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public boolean isSeparateEMoneyButtonEnabled() {
        return true;
    }

    /******************************************************************************/
    /*!
     * @brief  分別QRのボタンのEnabled制御（フタバD等向け）
     * @note   分別QRのボタンのEnabled制御（フタバD等向け）
     * @param [in] なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public boolean isSeparateQRButtonEnabled() {
        return true;
    }

    /******************************************************************************/
    /*!
     * @brief  分別プリペイドのボタンのEnabled制御（フタバD等向け）
     * @note   分別プリペイドのボタンのEnabled制御（フタバD等向け）
     * @param [in] なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public boolean isSeparatePrepaidButtonEnabled() {
        return true;
    }

    /******************************************************************************/
    /*!
     * @brief  820通信：フタバDモードか返す（フタバD等向け）
     * @note   820通信：フタバDモードか返す（フタバD等向け）
     * @param なし
     * @retval なし
     * @return　フタバD時はtrue
     * @private
     */
    /******************************************************************************/
    public boolean getisFutabaDMode() {
        return IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D);
    }
//ADD-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修


    public boolean getisDuplexMode() {
        return IFBoxAppModels.isDuplex();
    }

    public boolean getisManualMode() {
        return IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL);
    }

    //ADD-S BMT S.Oyama 2024/11/27 フタバ双方向向け改修
    public static Disposable meterDataV4ErrorDisposable_ADR = null;
    public static Disposable meterDataV4InfoDisposable_ADR = null;
    //ADD-E BMT S.Oyama 2024/11/27 フタバ双方向向け改修

}
