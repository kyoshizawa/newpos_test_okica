package jp.mcapps.android.multi_payment_terminal;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.common.base.Strings;
import com.pos.device.sys.SystemManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import io.grpc.Status;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.data.FirmWareInfo;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.OptionService;
import jp.mcapps.android.multi_payment_terminal.data.sam.Constants;
import jp.mcapps.android.multi_payment_terminal.devices.SamRW;
import jp.mcapps.android.multi_payment_terminal.error.DomainErrors;
import jp.mcapps.android.multi_payment_terminal.error.GmoErrorMap;
import jp.mcapps.android.multi_payment_terminal.error.McPosCenterErrorCodes;
import jp.mcapps.android.multi_payment_terminal.error.McPosCenterErrorMap;
import jp.mcapps.android.multi_payment_terminal.logger.EventLogger;
//import jp.mcapps.android.multi_payment_terminal.model.IFBoxManager;
//import jp.mcapps.android.multi_payment_terminal.model.JremActivator;
//import jp.mcapps.android.multi_payment_terminal.model.JremOpener;
import jp.mcapps.android.multi_payment_terminal.model.McAuthenticator;
import jp.mcapps.android.multi_payment_terminal.model.McCredit;
import jp.mcapps.android.multi_payment_terminal.model.McTerminal;
import jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl;
import jp.mcapps.android.multi_payment_terminal.model.QRSettlement;
import jp.mcapps.android.multi_payment_terminal.model.Updater;
import jp.mcapps.android.multi_payment_terminal.model.Validator;
import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvCLProcess;
import jp.mcapps.android.multi_payment_terminal.model.pos.ProductRepository;
// import jp.mcapps.android.multi_payment_terminal.model.ticket.TicketRepository;
import jp.mcapps.android.multi_payment_terminal.service.PosTransactionService;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.McOkicaCenterApi;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.McOkicaCenterApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.GetAccessToken;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.PaypfStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesStatusException;
import timber.log.Timber;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class StartViewModel extends ViewModel
{
    private final MainApplication _app = MainApplication.getInstance();
    private final ArrayList<String> _errors = new ArrayList<String>();
    private final Handler _handler = new Handler(Looper.getMainLooper());
    private final OkicaMasterControl _okicaMasterCtrl = new OkicaMasterControl();
//    private final IFBoxManager _ifBoxManager;
    private final Updater _updater;
    private FirmWareInfo _ifBoxFirmWareInfo = null;
    private boolean isInitialized = false;
    private final EventLogger _eventLogger;

    public StartViewModel(EventLogger eventLogger, Updater updater) {
        //_ifBoxManager = ifBoxManager;
        _eventLogger = eventLogger;
        _updater = updater;
    }

    private MutableLiveData<Boolean> _isUpdateChecking = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isUpdateChecking() {
        return _isUpdateChecking;
    }
    public void isUpdateChecking(boolean b) {
        _handler.post(() -> {
            _isUpdateChecking.setValue(b);
        });
    }

    private MutableLiveData<Boolean> _showUpdateConfirm = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> showUpdateConfirm() {
        return _showUpdateConfirm;
    }
    public void showUpdateConfirm(boolean b) {
        _handler.post(() -> {
            _showUpdateConfirm.setValue(b);
        });
    }

    private MutableLiveData<Boolean> _isInstalling = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isInstalling() {
        return _isInstalling;
    }
    public void isInstalling(boolean b) {
        _handler.post(() -> {
            _isInstalling.setValue(b);
        });
    }

    private MutableLiveData<Boolean> _isDownloading = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isDownloading() {
        return _isDownloading;
    }
    public void isDownloading(boolean b) {
        _handler.post(() -> {
            _isDownloading.setValue(b);
        });
    }

    private MutableLiveData<Integer> _progress = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getProgress() {
        return _progress;
    }
    public void isEnd(boolean b) {
        _handler.post(() -> {
            _isEnd.setValue(b);
        });
    }

    private MutableLiveData<Boolean> _isBooting = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isBooting() {
        return _isBooting;
    }
    public void isBooting(boolean b) {
        _handler.post(() -> {
            _isBooting.setValue(b);
        });
    }

    private MutableLiveData<Boolean> _isEnd = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isEnd() {
        return _isEnd;
    }

    private MutableLiveData<Updater.Products> _updateType = new MutableLiveData<>(Updater.Products.None);
    public MutableLiveData<Updater.Products> getUpdateType() {
        return _updateType;
    }
    public void setUpdateType(Updater.Products type) {
        _handler.post(() -> {
            _updateType.setValue(type);
        });
    }

    private MutableLiveData<String> _downloadFileNo = new MutableLiveData<>("-");
    public MutableLiveData<String> getDownloadFileNo() {
        return _downloadFileNo;
    }
    public void setDownloadFileNo(String n) {
        _handler.post(() -> {
            _downloadFileNo.setValue(n);
        });
    }

    private MutableLiveData<String> _downloadTotalNum = new MutableLiveData<>("-");
    public MutableLiveData<String> getDownloadTotalNum() {
        return _downloadTotalNum;
    }
    public void setDownloadTotalNum(String n) {
        _handler.post(() -> {
            _downloadTotalNum.setValue(n);
        });
    }

    private MutableLiveData<Boolean> _needReboot = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> needReboot() {
        return _needReboot;
    }
    public void needReboot(Boolean b) {
        _handler.post(() -> {
            _needReboot.setValue(b);
        });
    }

    public String[] getErrors() {
        // 重複は削除して返す
        final HashSet set = new HashSet(_errors);
        final String[] arr = new String[set.size()];
        set.toArray(arr);

        return arr;
    }

    public void initialize() {
        if (isInitialized) return;
        else isInitialized = true;
        Timber.i("更新チェック実行");

        // 他アプリの情報取得
        _updater.getPrepaidAppVersion();

        try {
            updateCheck();
        } catch (Exception e) {
            // Todo エラーコードをセットする
            authAndOpening();
            Timber.e(e);
        }
    }

    public void authAndOpening() {
        setScreenTimeout(true); //更新キャンセルや失敗後は必ずここを通るためここで有効
        new Thread(() -> {
            isBooting(true);
            boolean isMcAuth = false;
            try {
                isMcAuth = mcAuthentication();
                if (isMcAuth) {
                    getTerminalInfo();

                    //ScreenLock設定 SDKManagerの初期化が完了している場合のみ
                    if (MainApplication.getInstance().isSDKInit()) {
                        try {
                            if(AppPreference.isScreenlockEnabled()) {
                                SystemManager.setPinLockScreenPassword(AppPreference.getScreenlockPassword());
                                SystemManager.setLockTimeOut(AppPreference.getScreenLockSec());
                            } else {
                                SystemManager.cancelLockScreen();
                            }
                            SystemManager.setScreenTimeOut(AppPreference.getTimeoutScreen());
                        } catch (Exception e) {
                            Timber.e(e);
                            _errors.add(_app.getString(R.string.error_type_payment_system_2054));
                        }
                    }

                    if (AppPreference.isMoneyCredit()) {
                        getCreditCAKey();
                        if (AppPreference.isMoneyContactless()) {
                            getRiskParameter();
                            EmvCLProcess.emvInit();
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
            } catch (Exception e) {
                _errors.add(McPosCenterErrorMap.INTERNAL_ERROR_CODE);
                Timber.e(e);
            }

            try {
                jremOpening();
            } catch (Exception e) {
                // Todo エラーコードをセットする
                Timber.e(e);
            }

            if(isMcAuth) {
                echo();
                postTerminalInfo();
                postPayment();
            }

            try {
                loginGmo();
            } catch (Exception e) {
                _errors.add(GmoErrorMap.INTERNAL_ERROR_CODE);
                Timber.e(e);
            }

            try {
                // OKICA起動処理
                if (AppPreference.isMoneyOkica()) {
                    if (!Strings.isNullOrEmpty(AppPreference.getOkicaAccessToken())) {
                        if (_okicaMasterCtrl.okicaOpening(_errors)) {
                            _app.isInitFeliCaSAM(SamRW.open(Constants.MC_NORMAL_KEY, SamRW.States.Normal) == SamRW.OpenResult.SUCCESS);
                        }
                    }
                    else if (AppPreference.getOkicaAuthCode() != null) {
                        Timber.i("OKICA端末設置状態未確認 アクセストークン取得実行");

                        final McOkicaCenterApi api = new McOkicaCenterApiImpl();

                        final GetAccessToken.Response resp = api.getAccessToken(
                                jp.mcapps.android.multi_payment_terminal.data.okica.Constants.TERMINAL_INSTALL_ID,
                                AppPreference.getOkicaAuthCode());
                        if (resp.result) {
                            Timber.i("OKICA端末設置済み アクセストークン取得成功");

                            AppPreference.setOkicaAccessToken(resp.accessToken);
                            if(_okicaMasterCtrl.okicaOpening(_errors)) {
                                _app.isInitFeliCaSAM(SamRW.open(Constants.MC_NORMAL_KEY, SamRW.States.Normal) == SamRW.OpenResult.SUCCESS);
                            }
                        } else {
                            Timber.e("OKICA端末未設置 アクセストークン取得失敗 reason: %s", resp.errorCode);

                            // サーバーや通信ではなく端末側に問題がある場合
                            final boolean ng = resp.errorCode.equals(Status.NOT_FOUND.getCode().toString())
                                    || resp.errorCode.equals(Status.PERMISSION_DENIED.getCode().toString());

                            if (ng) {
                                AppPreference.clearOkica();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Timber.e(e);
            }

            try {
                if (AppPreference.isServicePos()) {
                    // 起動時のマスタ情報更新
                    ProductRepository repo = new ProductRepository();
                    repo.refreshProducts();
                }
            } catch (DomainErrors.Exception e) {
                Timber.e(e, "(paypf) Error on refreshing products on boot");
                if (e.getError() == DomainErrors.POS_SERVICE_INSTANCE_IS_NOT_ASSIGNED) {
                    _errors.add(Integer.toString(e.getError().code));
                } else {
                    // その他のエラー
                    _errors.add(String.format("%s@@@%s@@@", DomainErrors.POS_SERVICE_UNKNOWN_ERROR.code, e.getError().code));
                }
            } catch (PaypfStatusException e) {
                Timber.e(e, "(paypf) Error on refreshing products on boot");
                _errors.add(String.format("%s@@@%s@@@", DomainErrors.POS_SERVICE_RESPONSE_ERROR.code, e.getCode()));
            } catch (Exception e) {
                Timber.e(e, "(paypf) Error on refreshing products on boot");
                _errors.add(Integer.toString(DomainErrors.POS_SERVICE_NETWORK_ERROR.code));
            }

            if (AppPreference.isServicePos()) {
                // 取引データ送信サービスを起動する
                PosTransactionService.startService();
            }

            try {
                if (AppPreference.isServiceTicket()) {
                    // 起動時のチケット販売マスタ更新
//                    TicketRepository repo = new TicketRepository();
//                    repo.refreshTicketSales();
                }
//            } catch (DomainErrors.Exception e) {
//                Timber.e(e, "Error on refreshing TicketSales on boot");
//                if (e.getError() == DomainErrors.TICKET_SERVICE_INSTANCE_IS_NOT_ASSIGNED) {
//                    _errors.add(Integer.toString(e.getError().code));
//                } else {
//                    // その他のエラー
//                    _errors.add(String.format("%s@@@%s@@@", DomainErrors.TICKET_SALES_SERVICE_UNKNOWN_ERROR.code, e.getError().code));
//                }
//            } catch (TicketSalesStatusException e) {
//                Timber.e(e, "Error on refreshing TicketSales on boot");
//                _errors.add(String.format("%s@@@%s@@@", DomainErrors.TICKET_SALES_SERVICE_RESPONSE_ERROR.code, e.getCode()));
            } catch (Exception e) {
                Timber.e(e, "Error on refreshing TicketSales on boot");
                _errors.add(Integer.toString(DomainErrors.TICKET_SALES_SERVICE_NETWORK_ERROR.code));
            }

            isBooting(false);
            isEnd(true);

        }).start();
    }

    private void updateCheck() {
        // SDK初期化を待機する (シリアルがとれないため
        if (!MainApplication.getInstance().isSDKInit()) {
            Timber.d("SDK 未初期化、100ms 後に再試行");
            _handler.postDelayed(this::updateCheck, 100);
            return;
        }

        _updater.getLatestVersion(false, true)
                .observeOn(Schedulers.io())
                .doOnSubscribe(d -> {
                    isUpdateChecking(true);
                })
                .subscribe((needReboot, error) -> {
                    if (error != null) {
                        isUpdateChecking(false);
                        authAndOpening();
                        return;
                    }

                    needReboot(needReboot);
                    isUpdateChecking(false);

                    if (_updater.isForcedUpdate()) {
                        // 強制アップデート
                        update();
                    } else {
                        showUpdateConfirm(true);
                    }
                });
    }

    private boolean mcAuthentication() {
        final McAuthenticator authenticator = new McAuthenticator();

        final String errorCode = authenticator.authenticate();

        if (errorCode != null) {
            if (!errorCode.equals(McPosCenterErrorMap.INTERNAL_ERROR_CODE)) {
                _errors.add(errorCode); //2094エラーが表示され認証失敗したと分かるため、端末内部エラーはスタックさせない
            }
            return false;
        }

        return true;
    }

    private void getTerminalInfo() {
        final McTerminal terminal = new McTerminal();
        final String errorCode = terminal.getTerminalInfo();
        if (errorCode != null) {
            _errors.add(errorCode);
        }
        /*インボイス対応*/
        if (AppPreference.judgeInvoiceStack() && !AppPreference.isServicePos()){
            _errors.add(MainApplication.getInstance().getString(R.string.error_type_invoice_info_error));
        }
    };

    private void jremOpening() {
        boolean useOtherMoney = AppPreference.isMoneyWaon()
                | AppPreference.isMoneyNanaco()
                | AppPreference.isMoneyEdy()
                | AppPreference.isMoneyId()
                | AppPreference.isMoneyQuicpay();

        if (AppPreference.isMoneySuica() || useOtherMoney) {
            final File certFile = new File (
                    MainApplication.getInstance().getFilesDir(), BuildConfig.JREM_CLIENT_CERTIFICATE);

            if (!certFile.exists()) {
                final String errCode = jremActivate();
                if (errCode != null) {
                    _errors.add(errCode);
                    return;
                }
            }
        }
//
//        final JremOpener opener = new JremOpener();
//
//        if (AppPreference.isMoneySuica()) {
//            Timber.i("交通系開局実行");
//            final String errCode = opener.openingSuica();
//
//            if (errCode != null) {
//                Timber.e("交通系開局失敗(エラーコード:%s)", errCode);
//                _errors.add(errCode);
//            } else {
//                Timber.i("交通系開局成功");
//            }
//        }
//
//        if (useOtherMoney) {
//            Timber.i("他マネー開局実行");
//            _errors.addAll(opener.openingEmoney());
//        }

//        //ログ送信
//        _eventLogger.submit();
    }

    private String jremActivate() {
        if (AppPreference.getJremActivateId().equals("null") || AppPreference.getJremPassword() == null) {
            // Todo エラー追加
            return "";
        }

        final File certFile = new File(
                MainApplication.getInstance().getFilesDir(), BuildConfig.JREM_CLIENT_CERTIFICATE);

        if (!certFile.exists()) {
//            final JremActivator activator = new JremActivator();
//            final String errCode = activator.install();
//            if (errCode != null) {
//                return errCode;
//            }
        }

        return null;
    }

    private void echo() {
        //疎通確認
        final McTerminal terminal = new McTerminal();
        final String errorCode = terminal.echo();
        if (errorCode != null) {
            _errors.add(errorCode);
        }
    }

    private void postTerminalInfo() {
        //端末稼働情報連携
        final McTerminal terminal = new McTerminal();
        final String errorCode = terminal.postTerminalInfo(0);
        if (errorCode != null) {
            _errors.add(errorCode);
        }
    }

    private void postPayment() {
        //売上情報送信
        final McTerminal terminal = new McTerminal();
        final String errorCode = terminal.postPayment();
        // 起動時の売上送信エラーは致命的なエラーのみ表示する
        if (errorCode != null
        && (errorCode.equals(McPosCenterErrorCodes.E0098)
        ||  errorCode.equals(McPosCenterErrorCodes.E0901))) {
            _errors.add(McPosCenterErrorMap.get(errorCode));
        }

        if (AppPreference.isServiceTicket()) {
            final String ec = terminal.postTicketCancel();
            if (ec != null) Timber.e("post ticket cancel error %s", ec);
        }
    }

    private String getCreditCAKey() {
        //クレジットCA公開鍵DL
        String errCode = new McCredit().getCAKey();
        if (errCode != null) {
            return errCode;
        }
        return null;
    }

    private String getRiskParameter() {
        //リスク管理パラメータ
        String errCode = new McCredit().getRiskParameterContactless();
        if (errCode != null) {
            return errCode;
        }
        return null;
    }

    private void loginGmo() {
        if (!AppPreference.isMoneyQr()) return;

        QRSettlement qrSettlement  = new QRSettlement();

        addErrorOrNothing(qrSettlement.login());
    }

    public void update() {
        setScreenTimeout(false); //更新確認がSDKManagerの初期化前だったためここで無効
        showUpdateConfirm(false);
        isDownloading(true);

        _updater.download().subscribeOn(Schedulers.io()).subscribe(downloadProgress -> {
            setDownloadFileNo(Integer.toString(downloadProgress.fileNo));
            setDownloadTotalNum(Integer.toString(downloadProgress.totalFileNum));

            _progress.setValue(downloadProgress.progress);
        }, error -> {
            Timber.e(error);
            isDownloading(false);
            authAndOpening();
        }, () -> {
            isDownloading(false);
            install();
        });
    }

    public void install() {
        _updater.install(1, TimeUnit.MINUTES)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(d -> {
                    isInstalling(true);
                })
                .doFinally(() -> {
                    isInstalling(false);
                    if (!needReboot().getValue()) {
                        authAndOpening();
                    }
                })
                .subscribe(() -> {
                }, Timber::e);
    }

    public void cancel() {
        showUpdateConfirm(false);
        authAndOpening();
    }

    public void appStart() {
        //アプリ起動ログ
        _eventLogger.appStart();
        new Thread(_eventLogger::submit).start();
    }

    private void addErrorOrNothing(String errCode) {
        if (errCode != null) {
            _errors.add(errCode);
        }
    }

    //画面OFFの有効、無効を変更
    private void setScreenTimeout(boolean isValid) {
        //有効時 -> センター設定値、無効時 -> 1日
        final int SCREEN_TIMEOUT_INVALID_SECOND = 60 * 60 * 24;
        int timeout = isValid ? AppPreference.getTimeoutScreen() : SCREEN_TIMEOUT_INVALID_SECOND;
        try {
            if(MainApplication.getInstance().isSDKInit()) {
                //別Activityに遷移するため、WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON は使わない
                SystemManager.setScreenTimeOut(timeout);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }
}