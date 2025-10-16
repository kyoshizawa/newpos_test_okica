package jp.mcapps.android.multi_payment_terminal.model;

import static jp.mcapps.android.multi_payment_terminal.data.okica.Constants.COMPANY_CODE_BUPPAN;
import static jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl.FORCE_DEACT_END;
import static jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl.FORCE_DEACT_EXIST_AGGREGATE;
import static jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl.FORCE_DEACT_EXIST_UNSENT;
import static jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl.FORCE_DEACT_NONE;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.view.View;

import com.google.common.base.Strings;
import com.pos.device.printer.Printer;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.data.CurrentRadio;
import jp.mcapps.android.multi_payment_terminal.data.EmoneyOpeningInfo;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.okica.ICMaster;
import jp.mcapps.android.multi_payment_terminal.data.okica.OkicaNegaFile;
import jp.mcapps.android.multi_payment_terminal.data.sam.Constants;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalData;
import jp.mcapps.android.multi_payment_terminal.devices.SamRW;
import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorCodes;
import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorMap;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApi;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.TicketPurchasedConfirm;
import jp.mcapps.android.multi_payment_terminal.thread.printer.EpsonPrinterProc;
import timber.log.Timber;

public class OkicaChecker {
    private static OkicaChecker _instance = null;
    private static final int ELAPSED_TIME_ERROR = 1000 * 60 * 60 * 24;
    private static final int BATTERY_LOWER_LIMIT = 10;  // パーセント

    public static OkicaChecker getInstance() {
        if(_instance == null){
            _instance = new OkicaChecker();
        }
        return _instance;
    }
//    private static IFBoxManager _ifBoxManager;
//    public void setIFBoxManager( IFBoxManager ifBoxManager) {
//        _ifBoxManager = ifBoxManager ;
//    }
    private static boolean _isABTCancelSuccess = false;
    private static int _errorCode = 0;

    public static String check(View view, BusinessType businessType) {
        return check(view, businessType, null);
    }
    public static String check(View view, BusinessType businessType, String purchasedTicketDealId) {
        Timber.d("Okica起動前チェック開始");

        //デモモードの場合のみチェックする項目
        if (AppPreference.isDemoMode()) {
            MainApplication app = MainApplication.getInstance();
            if (businessType != BusinessType.BALANCE) {
                if (businessType == BusinessType.PAYMENT) {
                    // 決済金額下限チェック
                    if (Amount.getFixedAmount() <= 0) {
                        return "2001";
                    }

                    // 決済金額上限チェック
                    boolean isBelowUpperLimit = Amount.getFixedAmount() <= 30_000;
                    if (!isBelowUpperLimit) {
                        return app.getString(R.string.error_type_okica_upper_limit_error);
                    }
                }

                if (AppPreference.getIsExternalPrinter()) {
                    /* つり銭機連動 */
                    EpsonPrinterProc epsonPrinterProc = EpsonPrinterProc.getInstance();
                    if (!epsonPrinterProc.checkConnect()) {
                        return app.getString(R.string.error_type_cashchanger_printer_connection_error);
                    }
                } else {
//CHG-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                    //if (!IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) && (!IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D))) {
                    if (!IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) && (!IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) && (!IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D))) {
//CHG-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                        // プリンター状態のチェック
                        int isPrinterSts = Printer.getInstance().getStatus();
                        switch (isPrinterSts) {
                            case Printer.PRINTER_OK: // 正常状態（0）
                                break;

                            case Printer.PRINTER_STATUS_BUSY: // ビジー状態（-1）
                                return app.getString(R.string.error_type_printer_sts_busy);

                            case Printer.PRINTER_STATUS_HIGHT_TEMP: // 高温状態（-2）
                                return app.getString(R.string.error_type_printer_sts_hight_temp);

                            case Printer.PRINTER_STATUS_PAPER_LACK: // 用紙切れ状態 (-3)
                                return app.getString(R.string.error_type_printer_sts_paper_lack);

                            case Printer.PRINTER_STATUS_NO_BATTERY: // バッテリー残量不足状態（-4）
                                return app.getString(R.string.error_type_printer_sts_no_battery);

                            case Printer.PRINTER_STATUS_FEED: // 用紙送り状態（-5）
                                return app.getString(R.string.error_type_printer_sts_feed);

                            case Printer.PRINTER_STATUS_PRINT: // 印刷状態（-6）
                                return app.getString(R.string.error_type_printer_sts_print);

                            case Printer.PRINTER_STATUS_FORCE_FEED: // 強制用紙送り状態（-7）
                                return app.getString(R.string.error_type_printer_sts_force_feed);

                            case Printer.PRINTER_STATUS_POWER_ON: // 電源ON処理中状態（-8）
                                return app.getString(R.string.error_type_printer_sts_power_on);

                            case Printer.PRINTER_TASKS_FULL: // 処理満載状態（-9）
                                return app.getString(R.string.error_type_printer_tasks_full);

                            default:
                                // プリンター異常状態（未定義）
                                return app.getString(R.string.error_type_printer_sts_undefined) + "@@@" + isPrinterSts + "@@@";
                        }
                    } else {
                        // LT27の場合IM-A820未接続状態であれば使わせない
//                        if (!_ifBoxManager.isConnected()) {
//                            return app.getString(R.string.error_type_ifbox_connection_error);
//                        }
                    }
                }
            }

            // バッテリー状態のチェック
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = view.getContext().registerReceiver(null, intentFilter);
            int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

            if (batteryLevel <= BATTERY_LOWER_LIMIT) {
                return app.getString(R.string.error_type_low_battery_error);
            }

            return null;
        }

        //通常モードの場合のみチェックする項目

        MainApplication app = MainApplication.getInstance();

        if (OkicaMasterControl.force_deactivation_stat != FORCE_DEACT_NONE) {
            switch(OkicaMasterControl.force_deactivation_stat) {
                // 強制撤去を受信
                case FORCE_DEACT_EXIST_UNSENT:
                    return app.getString(R.string.error_type_okica_force_deactivation_uri_exist_error);
                case FORCE_DEACT_EXIST_AGGREGATE:
                    return app.getString(R.string.error_type_okica_force_deactivation_aggregate_exist_error);
                case FORCE_DEACT_END:
                    return app.getString(R.string.error_type_okica_not_installed_error);
                default:
                    Timber.e("force_deactivation_stat value error in OkicaChecker");
            }
        }

        // OKICA有効フラグOFFを受信
        if(OkicaMasterControl.force_okica_off == true) {
            return app.getString(R.string.error_type_okica_not_available);
        }

        // 端末設置チェック
        if (Strings.isNullOrEmpty(AppPreference.getOkicaAccessToken())) {
            return app.getString(R.string.error_type_okica_not_installed_error);
        }

        // マスタデータの保持期限チェック
        OkicaMasterControl _okicaMasterCtrl = new OkicaMasterControl();
        _okicaMasterCtrl.okicaCheckMasterTimeLimit();

        // データチェック
        if (!AppPreference.isOkicaAvailable() || !OkicaNegaFile.isExistNegaList()) {
            return app.getString(R.string.error_type_okica_data_error);
        }

        // IC運用マスタをファイルから読み込む（圏外で起動した時の対応）
        if (AppPreference.getOkicaICMasterInfo() != null && MainApplication.getInstance().getOkicaICMaster() == null) {
            ICMaster master = ICMaster.load();
            if (master != null) {
                MainApplication.getInstance().setOkicaICMaster(master);
            }
        }

        // マスタ１の有効開始年月日時分 ＜ マスタ２の有効開始年月日時分
        // 実日付時刻 ＜ マスタ２の有効開始年月日時分
        if (app.getOkicaICMaster().getData() == null) {
            Timber.e("OkicaChecker マスタデータ判定異常");
            return app.getString(R.string.error_type_okica_data_error);
        }

        // IC運用マスタは存在するが、事業者コード0x0C02（沖縄ＩＣカード物販）のマスタが無い場合
        if (app.getOkicaICMaster().getData().getActivator(COMPANY_CODE_BUPPAN) == null) {
            Timber.e("OkicaChecker 事業者コード0x%04Xのマスタデータなし", COMPANY_CODE_BUPPAN);
            return app.getString(R.string.error_type_okica_data_error);
        }

        if (businessType != BusinessType.BALANCE) {
            if (businessType == BusinessType.PAYMENT) {
                // 決済金額下限チェック
                if (Amount.getFixedAmount() <= 0) {
                    return "2001";
                }
                Timber.d("決済金額下限値チェックOK");

                // 決済金額上限チェック
                boolean isBelowUpperLimit = Amount.getFixedAmount() <= 99_999;

                if (!isBelowUpperLimit) {
                    return app.getString(R.string.error_type_okica_upper_limit_error);
                }

                Timber.d("決済金額上限値チェックOK");
            }

            if (AppPreference.getIsExternalPrinter()) {
                /* つり銭機連動 */
                EpsonPrinterProc epsonPrinterProc = EpsonPrinterProc.getInstance();
                if (!epsonPrinterProc.checkConnect()) {
                    return app.getString(R.string.error_type_cashchanger_printer_connection_error);
                }
            } else {
//CHG-S BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                //if (!IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) && (!IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D))) {
                if (!IFBoxAppModels.isMatch(IFBoxAppModels.YAZAKI_LT27_D) && (!IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) && (!IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D))) {
//CHG-E BMT S.Oyama 2024/09/24 フタバ双方向向け改修
                    // プリンター状態のチェック
                    int isPrinterSts = Printer.getInstance().getStatus();
                    switch (isPrinterSts) {
                        case Printer.PRINTER_OK: // 正常状態（0）
                            break;

                        case Printer.PRINTER_STATUS_BUSY: // ビジー状態（-1）
                            // return app.getString(R.string.error_type_printer_sts_busy);
                            break;

                        case Printer.PRINTER_STATUS_HIGHT_TEMP: // 高温状態（-2）
                            return app.getString(R.string.error_type_printer_sts_hight_temp);

                        case Printer.PRINTER_STATUS_PAPER_LACK: // 用紙切れ状態 (-3)
                            // return app.getString(R.string.error_type_printer_sts_paper_lack);
                            break;

                        case Printer.PRINTER_STATUS_NO_BATTERY: // バッテリー残量不足状態（-4）
                            return app.getString(R.string.error_type_printer_sts_no_battery);

                        case Printer.PRINTER_STATUS_FEED: // 用紙送り状態（-5）
                            return app.getString(R.string.error_type_printer_sts_feed);

                        case Printer.PRINTER_STATUS_PRINT: // 印刷状態（-6）
                            return app.getString(R.string.error_type_printer_sts_print);

                        case Printer.PRINTER_STATUS_FORCE_FEED: // 強制用紙送り状態（-7）
                            return app.getString(R.string.error_type_printer_sts_force_feed);

                        case Printer.PRINTER_STATUS_POWER_ON: // 電源ON処理中状態（-8）
                            return app.getString(R.string.error_type_printer_sts_power_on);

                        case Printer.PRINTER_TASKS_FULL: // 処理満載状態（-9）
                            return app.getString(R.string.error_type_printer_tasks_full);

                        default:
                            // プリンター異常状態（未定義）
                            return app.getString(R.string.error_type_printer_sts_undefined) + "@@@" + isPrinterSts + "@@@";
                    }
                } else {
//                    // LT27の場合IM-A820未接続状態であれば使わせない
//                    if (!_ifBoxManager.isConnected()) {
//                        return app.getString(R.string.error_type_ifbox_connection_error);
//                    }
                }
            }
        }

        // バッテリー状態のチェック
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = view.getContext().registerReceiver(null, intentFilter);
        int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        Timber.i("battery %d %%", batteryLevel);

        if (batteryLevel <= BATTERY_LOWER_LIMIT) {
            return app.getString(R.string.error_type_low_battery_error);
        }

        //係員設定チェック
        if (AppPreference.isDriverCodeInput() && AppPreference.getDriverCode().equals("")) {
            return app.getString(R.string.error_type_payment_system_2013);
        }

        // FeliCa SAM初期化チェック
        if (!app.isInitFeliCaSAM()) {
            Timber.e("FeliCa SAM 初期化未実行");
            app.isInitFeliCaSAM(SamRW.open(Constants.MC_NORMAL_KEY, SamRW.States.Normal) == SamRW.OpenResult.SUCCESS);
            if (!app.isInitFeliCaSAM()) {
                return app.getString(R.string.error_type_okica_sam_init_error);
            }
            Timber.i("FeliCa SAM 初期化実行成功");
        }

        /*
         * シーケンス番号チェック
         * シーケンス番号の残りが1000h(4096)未満なら再認証する
         * Writeまで行う場合一度の取引で最小で8加算される(読取エラーが出た場合は上乗せ)
         *
         * --- SAM仕様書より抜粋
         * シーケンス番号（Snr）が上限値（FFFFh）に達すると、以後の通信はシンタックスエラーを返します
         * この場合、相互認証からの処理を再度実行する必要があります。
         */
        if (SamRW.getSnr() > (0xEFFF)) {
            Timber.i("FeliCa SAM シーケンス番号が上限超過");
            app.isInitFeliCaSAM(SamRW.open(Constants.MC_NORMAL_KEY, SamRW.States.Normal) == SamRW.OpenResult.SUCCESS);
            if (!app.isInitFeliCaSAM()) {
                return app.getString(R.string.error_type_okica_sam_init_error);
            }
            Timber.i("FeliCa SAM シーケンス番号をリセット成功");
        }

        // チケットの取消時はABTセンターで取消ができるか確認が必要
        if (AppPreference.isTicketTransaction() && businessType == BusinessType.REFUND) {
            _isABTCancelSuccess = false;
            _errorCode = 0;

            if (purchasedTicketDealId == null || purchasedTicketDealId.equals("")) {
                // チケット購入IDが存在しない場合、取消確認不要
                Timber.i("チケット取消確認不要(%s)", purchasedTicketDealId);
                return null;
            } else {
                // チケットの取消時はABTセンターで取消ができるか確認が必要
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final TicketSalesApi ticketSalesApiClient = TicketSalesApiImpl.getInstance();
                        final TerminalDao terminalDao = LocalDatabase.getInstance().terminalDao();
                        final TerminalData terminalData = terminalDao.getTerminal();

                        try {
                            // ABTに取消確認実施
                            TicketPurchasedConfirm.Response confirmResponse = ticketSalesApiClient.TicketPurchasedConfirm(terminalData.service_instance_abt, purchasedTicketDealId);
                            _isABTCancelSuccess = true;

                        } catch (TicketSalesStatusException e) {
                            Timber.e(e);
                            _errorCode = e.getCode();
                            if (404 == _errorCode || 4007 == _errorCode) {
                                _isABTCancelSuccess = true;
                            }
                        } catch (HttpStatusException e) {
                            Timber.e(e);
                            _errorCode = e.getStatusCode();
                            if (404 == _errorCode || 4007 == _errorCode) {
                                _isABTCancelSuccess = true;
                            }
                        } catch (Exception e) {
                            Timber.e(e);
                            _errorCode = 99999;
                        }
                    }
                });
                thread.start();

                try {
                    thread.join();

                    if (!_isABTCancelSuccess) {
                        if (4008 == _errorCode) {
                            return app.getString(R.string.error_type_ticket_8161);
                        } else if (4009 == _errorCode) {
                            return app.getString(R.string.error_type_ticket_8162);
                        } else if (4010 == _errorCode) {
                            return app.getString(R.string.error_type_ticket_8163);
                        } else if (99999 == _errorCode) {
                            return app.getString(R.string.error_type_ticket_8097);
                        } else {
                            return app.getString(R.string.error_type_ticket_8160) + "@@@" + _errorCode + "@@@";
                        }
                    }
                } catch (Exception e) {
                    Timber.e(e);
                    return app.getString(R.string.error_type_ticket_8097);
                }
            }
        }

        return null;
    }
}
