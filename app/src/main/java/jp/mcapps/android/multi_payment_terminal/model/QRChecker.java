package jp.mcapps.android.multi_payment_terminal.model;

import static jp.mcapps.android.multi_payment_terminal.AppPreference.isDemoMode;
import static jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterConst.DuplexPrintResponseTimerSec;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.view.View;

import com.pos.device.printer.Printer;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.data.CurrentRadio;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalData;
import jp.mcapps.android.multi_payment_terminal.error.GmoErrorMap;
import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorCodes;
import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorMap;
import jp.mcapps.android.multi_payment_terminal.service.GetRadioService;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApi;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.TicketPurchasedConfirm;
import jp.mcapps.android.multi_payment_terminal.thread.printer.EpsonPrinterProc;
import timber.log.Timber;

public class QRChecker {
    private static QRChecker _instance = null;
    private static final int ELAPSED_TIME_ERROR = 1000 * 60 * 60 * 24;
    private static final int BATTERY_LOWER_LIMIT = 10;  // パーセント
    public static QRChecker getInstance() {
        if(_instance == null){
            _instance = new QRChecker();
        }
        return _instance;
    }
    private static IFBoxManager _ifBoxManager;
    public void setIFBoxManager( IFBoxManager ifBoxManager) {
        _ifBoxManager = ifBoxManager ;
    }
    private static boolean _isABTCancelSuccess = false;
    private static int _errorCode = 0;

    //ADD-S BMT S.Oyama 2024/10/07 フタバ双方向向け改修
    private static Disposable _meterDataV4ErrorDisposable = null;
    private static Disposable _meterDataV4InfoDisposable = null;
    //ADD-E BMT S.Oyama 2024/10/07 フタバ双方向向け改修

    public static String check(View view, BusinessType businessType) {
        return check(view, businessType, null);
    }

    public static String check(View view, BusinessType businessType, String purchasedTicketDealId) {
        Timber.d("QR起動前チェック開始");

        final MainApplication app = MainApplication.getInstance();

        //チェックする項目
        if (!AppPreference.isDemoMode()) {
            //通常モードの場合のみチェックする項目

            //電波強度が強or中じゃない場合は使わせない、エラーコード1001（※通常モードの場合のみ）
            if (CurrentRadio.getImageLevel() == 0 && !AppPreference.isDemoMode()) {
                return MainApplication.getInstance().getString(R.string.error_type_comm_reception);
            } else if (CurrentRadio.getImageLevel() == GetRadioService.AIRPLANE_MODE && !AppPreference.isDemoMode()) {
                //機内モード状態の場合は使わせない、エラーコード1003（※通常モードの場合のみ）
                return MainApplication.getInstance().getString(R.string.error_type_airplane_mode);
            }

            // 認証チェック（※通常モードの場合のみ）
            // 初期値は-1でセット。値が負なら認証できていない
            if (app.getQREnabledFlags() < 0 && !AppPreference.isDemoMode()) {
                return GmoErrorMap.INTERNAL_ERROR_CODE;
            }

            //ID、パスワードが設置解除済の場合やデフォルト値の場合は使わせない
            boolean unAuth = ( AppPreference.getQrUserId().equals("") && AppPreference.getQrPassword().equals("") ) //ID、パスワードが空文字
                    || ( AppPreference.getQrUserId().equals( app.getString( R.string.setting_default_qr_userid)) && AppPreference.getQrPassword().equals( app.getString( R.string.setting_default_qr_password))); //ID、パスワードがデフォルト設定のまま
            if (unAuth){
                return app.getString(R.string.error_type_qr_3326);
            }

            Timber.d("認証チェックOK");

            //疎通確認（※通常モードの場合のみ）
            if (!AppPreference.isDemoMode()) {
                final String[] errCode = {null};
                McTerminal terminal = new McTerminal();
                Thread thread = new Thread(() -> errCode[0] = terminal.echo());

                thread.start();
                try {
                    thread.join();
                    if (errCode[0] != null && errCode[0].equals(app.getString(R.string.error_type_not_available))) {
                        //利用許可状態のみ確認する 疎通確認自体が失敗する場合は無視
                        return errCode[0];
                    }
                } catch (InterruptedException e) {
                    Timber.e(e);
                }
            }

            if (AppPreference.isTicketTransaction() && businessType == BusinessType.REFUND) {
                _isABTCancelSuccess = false;
                _errorCode = 0;

                if (purchasedTicketDealId == null || purchasedTicketDealId.equals("")) {
                    // チケット購入IDが存在しない場合、取消確認不要
                    Timber.i("チケット取消確認不要(%s)", purchasedTicketDealId);
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
        }

        if (businessType == BusinessType.PAYMENT) {
            // 決済金額下限チェック
            if (Amount.getFixedAmount() <= 0) {
                return "2001";
            }
            Timber.d("決済金額下限値チェックOK");
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
                if (!_ifBoxManager.isConnected()) {
                    return app.getString(R.string.error_type_ifbox_connection_error);
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

        if (!AppPreference.isDemoMode()) {
            //通常モードの場合のみチェックする項目

            //係員設定チェック（※通常モードの場合のみ）
            if (AppPreference.isDriverCodeInput() && AppPreference.getDriverCode().equals("") && !AppPreference.isDemoMode()) {
                return app.getString(R.string.error_type_payment_system_2013);
            }
        }

        //ADD-S BMT S.Oyama 2024/10/07 フタバ双方向向け改修
        if ( (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true)) {

            if (_ifBoxManager.getIsConnected820() == false)             //820未接続の場合
            {
                return "6030";                       //IFBOX接続エラー
            }

            IFBoxManager.SendMeterDataInfo_FutabaD tmpSend820Info = new IFBoxManager.SendMeterDataInfo_FutabaD();
            tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
            tmpSend820Info.IsLoopBreakOut = false;
            tmpSend820Info.ErrorCode820 = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;

            _meterDataV4InfoDisposable = _ifBoxManager.getMeterDataV4().subscribeOn(
                    Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(meter -> {                 //AndroidSchedulers.mainThread()
                Timber.i("[FUTABA-D]QRChecker:750<-820 meter_data event cmd:%d ", meter.meter_sub_cmd);
                if(meter.meter_sub_cmd == 9) {              //ファンクション通知を受信
                    tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;             //ACKが返ってきた場合
                }
            });

            _meterDataV4ErrorDisposable = _ifBoxManager.getMeterDataV4Error().subscribeOn(
                    Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(error -> {         //送信中にエラー受信(タイムアウト，切断)      AndroidSchedulers.mainThread()
                Timber.e("[FUTABA-D]QRChecker:Error event ErrCD:%d 820ErrCD:%d ", error.ErrorCode, error.ErrorCode820);
                tmpSend820Info.StatusCode = error.ErrorCode;
                tmpSend820Info.ErrorCode820 = error.ErrorCode820;

            });

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    _ifBoxManager.send820_SettlementSelectMode(IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_QR, false);             //決済選択モードを送信 QR

                    for(int i = 0; i < (DuplexPrintResponseTimerSec + 1) * 10; i++)        //最大26秒ほど待ってみる
                    {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }

                        if (tmpSend820Info.StatusCode != IFBoxManager.SendMeterDataStatus_FutabaD.NONE)         //状態に変化が出たら直ちに抜ける
                        {
                            tmpSend820Info.IsLoopBreakOut = true;
                            break;
                        }
                    }

                }
            });
            thread.start();

            try {
                thread.join();

                if (_meterDataV4InfoDisposable != null) {       //コールバック系を後始末
                    _meterDataV4InfoDisposable.dispose();
                    _meterDataV4InfoDisposable = null;
                }

                if (_meterDataV4ErrorDisposable != null)        //コールバック系を後始末
                {
                    _meterDataV4ErrorDisposable.dispose();
                    _meterDataV4ErrorDisposable = null;
                }

                if (tmpSend820Info.IsLoopBreakOut == false) {                             //820から何も返却されなかった場合のループアウト
                    return "6030";                       //IFBOX接続エラー
                }
                else
                {
                    switch(tmpSend820Info.StatusCode)                       //ステータスコードのチェック
                    {
                        case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_NOTCONNECTED:       //切断
                            return "6030";                       //IFBOX接続エラー
                        case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_TIMEOUT:           //タイムアウト
                            return "6030";                       //IFBOX接続エラー
                        case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_820NACK:              //820内でが返ってきた場合
                            Timber.e("[FUTABA-D]820 Inner error! ErrCD:%d", tmpSend820Info.ErrorCode820);
                            //ADD-S BMT S.Oyama 2025/01/29 フタバ双方向向け改修
                            if (tmpSend820Info.ErrorCode820 == IFBoxManager.Send820Status_Error_FutabaD.ERROR_STATUS820_PAPERLACKING)       //用紙無しエラー
                            {
                                return "";                          //用紙なしの場合は空文字を入れる(NULLはNG　9110を入れると２重でエラーが出る)
                            }
                            else {
                                return "6030";                       //IFBOX接続エラー
                            }
                            //ADD-E BMT S.Oyama 2025/01/29 フタバ双方向向け改修
                        default:
                            //ここに到達する場合は，エラー無しで決済選択モードが送信されたことを意味する
                            break;
                    }
                }
            } catch (Exception e) {
                Timber.e(e);
                return app.getString(R.string.error_type_ticket_8097);
            }

        }
        //ADD-E BMT S.Oyama 2024/10/07 フタバ双方向向け改修


        return null;
    }
}
