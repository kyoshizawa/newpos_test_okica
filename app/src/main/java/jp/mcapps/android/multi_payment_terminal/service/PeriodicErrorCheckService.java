package jp.mcapps.android.multi_payment_terminal.service;

import static jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl.FORCE_DEACT_END;
import static jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl.FORCE_DEACT_EXIST_AGGREGATE;
import static jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl.FORCE_DEACT_EXIST_UNSENT;
import static jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl.FORCE_DEACT_NONE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.common.base.Strings;
import com.pos.device.printer.Printer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.disposables.Disposable;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.EmoneyOpeningInfo;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.okica.OkicaNegaFile;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorData;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingDao;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingData;
import jp.mcapps.android.multi_payment_terminal.error.ErrorManage;
import jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl;
import timber.log.Timber;

public class PeriodicErrorCheckService extends LifecycleService implements ViewModelStoreOwner {
    private PeriodicErrorCheckServiceViewModel _viewModel;
    private final ViewModelStore _viewModelStore = new ViewModelStore();

    private ScheduledExecutorService _scheduledExecutor;
    private Disposable _meterDisposable;
    private boolean _isBatteryLow = false;

    private final IntentFilter BATTERY_INTENT_FILTER = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    private static final int ELAPSED_TIME_ERROR = 1000 * 60 * 60 * 24; //開局時間エラーの時間 24時間
    private static final int ELAPSED_TIME_WANING = 1000 * 60 * 60 * 23; //開局時間警告の時間 23時間

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate() {
        super.onCreate();
        _viewModel = new ViewModelProvider(this, MainApplication.getViewModelFactory()).get(PeriodicErrorCheckServiceViewModel.class);

        _scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
        final String openingEMoneyError = getString(R.string.error_type_opening_emoney_error);

        // バッテリー残量の初回チェック　LOW状態で起動するとインテントが受け取れないのでここで確認
        _isBatteryLow = registerReceiver(null, BATTERY_INTENT_FILTER)
                .getIntExtra(BatteryManager.EXTRA_LEVEL, -1) <= 15;

        _scheduledExecutor.scheduleWithFixedDelay(() -> {
            final List<String> errorList = new ArrayList<>();
            final Set<String> removeList = new HashSet<>(); //重複の可能性があるためセットに格納

            if(!AppPreference.isDemoMode()){
                /* 通常モードの場合 */
                Date currentDatetime = new Date();

                //Suica開局時間チェック
                if (AppPreference.isMoneySuica()) {
                    String stringOpeningSuica = AppPreference.getDatetimeOpeningSuica();
                    try {
                        Date datetimeOpeningSuica = dateFormat.parse(stringOpeningSuica);
                        if (datetimeOpeningSuica != null) {

                            String warningCode = getString(R.string.error_type_opening_suica_warning);
                            String errorCode = getString(R.string.error_type_opening_suica_error);
                            String closedCode = getString(R.string.error_type_suica_closed_error);
                            if(null == EmoneyOpeningInfo.getSuica()) {
                                // マネーは有効なのに開局できていない
                                errorList.add(closedCode);
                            }else{
                                long elapsedTime = currentDatetime.getTime() - datetimeOpeningSuica.getTime();

                                if (elapsedTime >= ELAPSED_TIME_ERROR) {
                                    errorList.add(errorCode);
                                } else if (elapsedTime >= ELAPSED_TIME_WANING) {
                                    errorList.add(warningCode);
                                } else {
                                    removeList.addAll(Arrays.asList(warningCode, errorCode, closedCode, openingEMoneyError));
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                //iD開局時間チェック
                if(AppPreference.isMoneyId()) {
                    String stringOpeningId = AppPreference.getDatetimeOpeningId();
                    try {
                        Date datetimeOpeningId = dateFormat.parse(stringOpeningId);
                        if (datetimeOpeningId != null) {
                            String warningCode = getString(R.string.error_type_opening_id_warning);
                            String errorCode = getString(R.string.error_type_opening_id_error);
                            String closedCode = getString(R.string.error_type_id_closed_error);
                            if(null == EmoneyOpeningInfo.getId()) {
                                // マネーは有効なのに開局できていない
                                errorList.add(closedCode);
                            }else {
                                long elapsedTime = currentDatetime.getTime() - datetimeOpeningId.getTime();

                                if (elapsedTime >= ELAPSED_TIME_ERROR) {
                                    errorList.add(errorCode);
                                } else if (elapsedTime >= ELAPSED_TIME_WANING) {
                                    errorList.add(warningCode);
                                } else {
                                    removeList.addAll(Arrays.asList(warningCode, errorCode, closedCode, openingEMoneyError));
                                }
                            }
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                //WAON開局時間チェック
                if(AppPreference.isMoneyWaon()) {
                    String stringOpeningWaon = AppPreference.getDatetimeOpeningWaon();
                    try {
                        Date datetimeOpeningWaon = dateFormat.parse(stringOpeningWaon);
                        if (datetimeOpeningWaon != null) {
                            String warningCode = getString(R.string.error_type_opening_waon_warning);
                            String errorCode = getString(R.string.error_type_opening_waon_error);
                            String closedCode = getString(R.string.error_type_waon_closed_error);
                            if(null == EmoneyOpeningInfo.getWaon()) {
                                // マネーは有効なのに開局できていない
                                errorList.add(closedCode);
                            }else {
                                long elapsedTime = currentDatetime.getTime() - datetimeOpeningWaon.getTime();

                                if (elapsedTime >= ELAPSED_TIME_ERROR) {
                                    errorList.add(errorCode);
                                } else if (elapsedTime >= ELAPSED_TIME_WANING) {
                                    errorList.add(warningCode);
                                } else {
                                    removeList.addAll(Arrays.asList(warningCode, errorCode, closedCode, openingEMoneyError));
                                }
                            }
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                //nanaco開局時間チェック
                if(AppPreference.isMoneyNanaco()) {
                    String stringOpeningNanaco = AppPreference.getDatetimeOpeningNanaco();
                    try {
                        Date datetimeOpeningNanaco = dateFormat.parse(stringOpeningNanaco);
                        if (datetimeOpeningNanaco != null) {
                            String warningCode = getString(R.string.error_type_opening_nanaco_warning);
                            String errorCode = getString(R.string.error_type_opening_nanaco_error);
                            String closedCode = getString(R.string.error_type_nanaco_closed_error);
                            if(null == EmoneyOpeningInfo.getNanaco()) {
                                // マネーは有効なのに開局できていない
                                errorList.add(closedCode);
                            }else {
                                long elapsedTime = currentDatetime.getTime() - datetimeOpeningNanaco.getTime();

                                if (elapsedTime >= ELAPSED_TIME_ERROR) {
                                    errorList.add(errorCode);
                                } else if (elapsedTime >= ELAPSED_TIME_WANING) {
                                    errorList.add(warningCode);
                                } else {
                                    removeList.addAll(Arrays.asList(warningCode, errorCode, closedCode, openingEMoneyError));
                                }
                            }
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                //Edy開局時間チェック
                if(AppPreference.isMoneyEdy()) {
                    String stringOpeningEdy = AppPreference.getDatetimeOpeningEdy();
                    try {
                        Date datetimeOpeningEdy = dateFormat.parse(stringOpeningEdy);
                        if (datetimeOpeningEdy != null) {
                            String warningCode = getString(R.string.error_type_opening_edy_warning);
                            String errorCode = getString(R.string.error_type_opening_edy_error);
                            String closedCode = getString(R.string.error_type_edy_closed_error);
                            if(null == EmoneyOpeningInfo.getEdy()) {
                                // マネーは有効なのに開局できていない
                                errorList.add(closedCode);
                            }else {
                                long elapsedTime = currentDatetime.getTime() - datetimeOpeningEdy.getTime();

                                if (elapsedTime >= ELAPSED_TIME_ERROR) {
                                    errorList.add(errorCode);
                                } else if (elapsedTime >= ELAPSED_TIME_WANING) {
                                    errorList.add(warningCode);
                                } else {
                                    removeList.addAll(Arrays.asList(warningCode, errorCode, closedCode, openingEMoneyError));
                                }
                            }
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                //QUICPay開局時間チェック
                if(AppPreference.isMoneyQuicpay()) {
                    String stringOpeningQuicpay = AppPreference.getDatetimeOpeningQuicpay();
                    try {
                        Date datetimeOpeningQuicpay = dateFormat.parse(stringOpeningQuicpay);
                        if (datetimeOpeningQuicpay != null) {
                            String warningCode = getString(R.string.error_type_opening_quicpay_warning);
                            String errorCode = getString(R.string.error_type_opening_quicpay_error);
                            String closedCode = getString(R.string.error_type_quicpay_closed_error);
                            if(null == EmoneyOpeningInfo.getQuicpay()) {
                                // マネーは有効なのに開局できていない
                                errorList.add(closedCode);
                            }else {
                                long elapsedTime = currentDatetime.getTime() - datetimeOpeningQuicpay.getTime();

                                if (elapsedTime >= ELAPSED_TIME_ERROR) {
                                    errorList.add(errorCode);
                                } else if (elapsedTime >= ELAPSED_TIME_WANING) {
                                    errorList.add(warningCode);
                                } else {
                                    removeList.addAll(Arrays.asList(warningCode, errorCode, closedCode, openingEMoneyError));
                                }
                            }
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                //QR認証結果チェック
                if(AppPreference.isMoneyQr()) {
                    // QRは有効なのに認証できていない
                    String errorCode = getString(R.string.error_type_qr_3325);

                    if (MainApplication.getInstance().getQREnabledFlags() < 0) {
                        errorList.add(errorCode);
                    } else {
                        removeList.add(errorCode);
                    }

                    // QRは、認証からの経過時間の規定は無いので、経過時間チェックは行わない
                }

                //MC認証開局時間チェック
                String stringAuthenticationMc = AppPreference.getDatetimeAuthenticationMc();
                try {
                    Date datetimeAuthenticationMc = dateFormat.parse(stringAuthenticationMc);
                    if (datetimeAuthenticationMc != null) {
                        String warningCode = getString(R.string.error_type_authentication_mc_warning);
                        String errorCode = getString(R.string.error_type_authentication_mc_error);
                        long elapsedTime = currentDatetime.getTime() - datetimeAuthenticationMc.getTime();

                        if (elapsedTime >= ELAPSED_TIME_ERROR) {
                            errorList.add(errorCode);
                        } else if (elapsedTime >= ELAPSED_TIME_WANING) {
                            errorList.add(warningCode);
                        } else {
                            removeList.addAll(Arrays.asList(warningCode, errorCode));
                        }
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if (AppPreference.isMoneyOkica() == true){
                    removeList.add(getString(R.string.error_type_okica_force_deactivation_uri_exist_error));
                    removeList.add(getString(R.string.error_type_okica_force_deactivation_aggregate_exist_error));
                    removeList.add(getString(R.string.error_type_okica_not_available));
                    // 強制撤去を受信
                    if (OkicaMasterControl.force_deactivation_stat != FORCE_DEACT_NONE) {
                        switch (OkicaMasterControl.force_deactivation_stat) {
                            case FORCE_DEACT_EXIST_UNSENT:
                                errorList.add(getString(R.string.error_type_okica_force_deactivation_uri_exist_error));
                                break;
                            case FORCE_DEACT_EXIST_AGGREGATE:
                                errorList.add(getString(R.string.error_type_okica_force_deactivation_aggregate_exist_error));
                                break;
                            case FORCE_DEACT_END:
                                // スタックエラーは表示しない
                                break;
                            default:
                                Timber.e("force_deactivation_stat value error in PeriodicErrorCheckService");
                                break;
                        }
                    // OKICA有効フラグOFFを受信
                    } else if(OkicaMasterControl.force_okica_off == true) {
                        errorList.add(getString(R.string.error_type_okica_not_available));
                    } else {
                        // OKICA端末設置チェック
                        boolean okicaError = false;
                        if (Strings.isNullOrEmpty(AppPreference.getOkicaAccessToken())) {
                            errorList.add(getString(R.string.error_type_okica_not_installed_error));
                            okicaError = true;
                        } else {
                            removeList.add(getString(R.string.error_type_okica_not_installed_error));
                        }
                        // OKICAデータチェック
                        if (!AppPreference.isOkicaAvailable() || !OkicaNegaFile.isExistNegaList()) {
                            if (okicaError == false) {
                                errorList.add(getString(R.string.error_type_okica_data_error));
                                okicaError = true;
                            }
                        } else {
                            removeList.add(getString(R.string.error_type_okica_data_error));
                        }
                        // FeliCa SAM初期化チェック
                        if (!MainApplication.getInstance().isInitFeliCaSAM()) {
                            if (okicaError == false) {
                                errorList.add(getString(R.string.error_type_okica_sam_init_error));
                            }
                        } else {
                            removeList.add(getString(R.string.error_type_okica_sam_init_error));
                        }
                    }
                }
            }

        	if (!AppPreference.getIsExternalPrinter()) {
//CHG-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                //if (!IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) && (!IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D))) {
                if (!IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) && (!IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) && (!IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D))) {
//CHG-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                    // プリンター状態（用紙切れのみ）のチェック
                    int isPrinterSts = Printer.getInstance().getStatus();
                    if (Printer.PRINTER_STATUS_PAPER_LACK == isPrinterSts) {
                        // 用紙切れ状態 (-3)
                        errorList.add(getString(R.string.error_type_printer_sts_paper_lack));
                    } else {
                        // 用紙切れ以外の状態に変更された場合、用紙切れのスタックエラーを削除する
                        removeList.add(getString(R.string.error_type_printer_sts_paper_lack));
                    }
                }
            }

            //IM-A820連動チェック
            if (_viewModel.isAccOn_IMA820() && AppPreference.getIFBoxVersionInfo() != null && !_viewModel.isIFBoxConnected()) {
                errorList.add(getString(R.string.error_type_ifbox_connection_error));
            } else {
                removeList.add(getString(R.string.error_type_ifbox_connection_error));
            }

            //バッテリー残量チェック 15%以下になった場合のみ
            if (_isBatteryLow) {
                // バッテリー状態のチェック
                Intent batteryStatus = registerReceiver(null, BATTERY_INTENT_FILTER);
                int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

                if (batteryLevel <= 10) {
                    //10%以下 エラー
                    errorList.add(getString(R.string.error_type_low_battery_error));
                    removeList.add(getString(R.string.error_type_low_battery_warning));
                } else if (batteryLevel <= 15) {
                    //15%以下 警告
                    errorList.add(getString(R.string.error_type_low_battery_warning));
                    removeList.add(getString(R.string.error_type_low_battery_error));
                } else if (batteryLevel >= 20) {
                    //20%以上 定期チェックを終了
                    _isBatteryLow = false;
                } else {
                    //16% ～ 19% 定期チェックは継続
                    removeList.add(getString(R.string.error_type_low_battery_warning));
                    removeList.add(getString(R.string.error_type_low_battery_error));
                }
            } else {
                removeList.add(getString(R.string.error_type_low_battery_warning));
                removeList.add(getString(R.string.error_type_low_battery_error));
            }

            /*インボイス対応*/
            //インボイス情報取得チェック
            if (AppPreference.judgeInvoiceStack() && !AppPreference.isServicePos()){
                errorList.add(getString(R.string.error_type_invoice_info_error));
            }

            removeErrorStacking(removeList.toArray(new String[0]));
            errorStacking(errorList.toArray(new String[0]));
        }, 0, 30, TimeUnit.SECONDS); //チェック間隔 30秒

        //バッテリー残量チェック
        IntentFilter batteryLow = new IntentFilter(Intent.ACTION_BATTERY_LOW);  //15%以下
        registerReceiver(_batteryLowReceiver, batteryLow);

        //メーター状態チェック
        _meterDisposable = _viewModel.getMeterInfo();
        _viewModel.fetchMeter();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void errorStacking(String... errorList) {
        ErrorManage errorManage = ErrorManage.getInstance();
        ErrorStackingDao dao = DBManager.getErrorStackingDao();

        for (String errorCode : errorList) {
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

            // エラー情報を取得
            ErrorData errorData = errorManage.getErrorData(errorCode);

            if(errorData == null) {
                Timber.d("error dialog errorCode not found");
                continue;
            }

            // 送信用のログはスタックされるたびに作成
            errorData.detail = String.format(errorData.detail, detailCode);
            String message = String.format("MCエラーコード：%s", errorData.errorCode);
            if (errorData.detail.contains("詳細コード：")){
                message = message.concat(",").concat(errorData.detail.split("\n")[0]);
            }
            Timber.tag("エラー履歴").i(message);

            ErrorStackingData errorStackingData = dao.getErrorStackingData(errorData.errorCode);

            Date date = new Date();
            if (errorStackingData == null) {
                ErrorStackingData newErrorStackingData = new ErrorStackingData();
                newErrorStackingData.errorCode = errorData.errorCode;
                newErrorStackingData.title = errorData.title;
                newErrorStackingData.message = errorData.message;
                newErrorStackingData.detail = errorData.detail;
                newErrorStackingData.level = errorData.level;

                newErrorStackingData.date = date;
                dao.insertErrorData(newErrorStackingData);
            } else {
                dao.updateErrorStackingData(errorStackingData.id, date);
            }
        }
    }

    private void removeErrorStacking(String... errorCodeList) {
        ErrorStackingDao dao = DBManager.getErrorStackingDao();

        for (String errorCode : errorCodeList) {
            ErrorStackingData errorStackingData = dao.getErrorStackingData(errorCode);
            if (errorStackingData != null) {
                dao.deleteErrorStackingData(errorStackingData.id);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        _scheduledExecutor.shutdown();
        unregisterReceiver(_batteryLowReceiver);

        if (_meterDisposable != null) {
            _meterDisposable.dispose();
            _meterDisposable = null;
        }

        //Serviceの終了を通知
        Intent intent = new Intent();
        intent.setAction("SERVICE_STOP");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        Timber.d("Periodic Error Check Service End");
    }

    private final BroadcastReceiver _batteryLowReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onReceive(Context context, Intent intent) {
            _isBatteryLow = true;
            new Thread(() -> {
                Intent batteryStatus = registerReceiver(null, BATTERY_INTENT_FILTER);
                int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

                if (batteryLevel <= 10) {
                    errorStacking(getString(R.string.error_type_low_battery_error));
                    removeErrorStacking(getString(R.string.error_type_low_battery_warning));
                } else {
                    //15%以下
                    errorStacking(getString(R.string.error_type_low_battery_warning));
                    removeErrorStacking(getString(R.string.error_type_low_battery_error));
                }
            }).start();
        }
    };

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        return _viewModelStore;
    }
}
