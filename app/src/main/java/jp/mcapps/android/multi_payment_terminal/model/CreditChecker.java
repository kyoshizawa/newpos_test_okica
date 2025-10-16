package jp.mcapps.android.multi_payment_terminal.model;

import static jp.mcapps.android.multi_payment_terminal.AppPreference.isDemoMode;
import static jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterConst.DuplexPrintResponseTimerSec;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.pos.device.emv.EMVHandler;
import com.pos.device.printer.Printer;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.SharedViewModel;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.data.CurrentRadio;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalData;
import jp.mcapps.android.multi_payment_terminal.error.CreditErrorCodes;
import jp.mcapps.android.multi_payment_terminal.error.McPosCenterErrorCodes;
import jp.mcapps.android.multi_payment_terminal.error.McPosCenterErrorMap;
import jp.mcapps.android.multi_payment_terminal.service.GetRadioService;
import jp.mcapps.android.multi_payment_terminal.thread.credit.CreditSettlement;
import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvCLProcess;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterConst;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputSeparationPayFDViewModel;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApi;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.TicketPurchasedConfirm;
import jp.mcapps.android.multi_payment_terminal.thread.printer.EpsonPrinterProc;
import timber.log.Timber;

public class CreditChecker {
    private static CreditChecker _instance = null;
    private static final int ELAPSED_TIME_ERROR = 1000 * 60 * 60 * 24;
    private static final int BATTERY_LOWER_LIMIT = 10;  // パーセント

    public static CreditChecker getInstance() {
        if(_instance == null){
            _instance = new CreditChecker();
        }
        return _instance;
    }
    private static IFBoxManager _ifBoxManager;
    public void setIFBoxManager( IFBoxManager ifBoxManager) {
        _ifBoxManager = ifBoxManager ;
    }

    //ADD-S BMT S.Oyama 2024/10/03 フタバ双方向向け改修
    private static Disposable _meterDataV4ErrorDisposable = null;
    private static Disposable _meterDataV4InfoDisposable = null;
    private SharedViewModel _sharedViewModel;

    private int _SeparationJobMode = 0;              //分別モード
    private int _Separation1stJobMode = 0;           //1st分別モード
    //ADD-E BMT S.Oyama 2024/10/03 フタバ双方向向け改修

    private CreditCheckListener _listener;
    public interface CreditCheckListener {
        void onFinished(String errCode);
    }

    public void setListener(CreditCheckListener listener) {
        _listener = listener;
    }

    public void check(View view, BusinessType type) {
        check(view, type, null);
    }
    public void check(View view, BusinessType type, String purchasedTicketDealId) {
        if (_listener == null) {
            Timber.e("リスナー未設定");
            return;
        }

        new Thread(() -> {
            MainApplication app = MainApplication.getInstance();

            if (!isDemoMode()) {
                //通常モードの場合のみチェックする項目
                //電波強度が強or中じゃない場合は使わせない、エラーコード1001
                if (CurrentRadio.getImageLevel() == 0) {
                    _listener.onFinished(app.getString(R.string.error_type_comm_reception));
                    return;
                } else if (CurrentRadio.getImageLevel() == GetRadioService.AIRPLANE_MODE){
                    //機内モード状態の場合は使わせない、エラーコード1003
                    _listener.onFinished(app.getString(R.string.error_type_airplane_mode));
                    return;
                }
                Timber.d("電波強度チェックOK");

                CreditSettlement creditSettlement = CreditSettlement.getInstance();
                // MC認証チェック
                if (creditSettlement._mcCenterCommManager.getTerOpePort() <= 0) {
                    _listener.onFinished(app.getString(R.string.error_type_payment_system_2094));
                    return;
                }

                //正常認証から24時間経過してたら使わせない
                final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
                Date authDate;

                try {
                    authDate = dateFormat.parse(AppPreference.getDatetimeAuthenticationMc());
                } catch (ParseException e) {
                    Timber.e(e);
                    _listener.onFinished(app.getString(R.string.error_type_payment_system_internal_error));
                    return;
                }

                final Date currentDatetime = new Date();

                if (authDate != null) {

                    long elapsedTime = currentDatetime.getTime() - authDate.getTime();

                    Timber.d("MC認証経過時間: %s分", elapsedTime / 1000 / 60);

                    //認証から24時間経過
                    if (elapsedTime >= ELAPSED_TIME_ERROR) {
                        _listener.onFinished(app.getString(R.string.error_type_authentication_mc_error));
                        return;
                    }
                } else {
                    _listener.onFinished(app.getString(R.string.error_type_authentication_mc_error));
                    return;
                }

                // 疎通確認
                if (CreditSettlement.k_OK != creditSettlement._mcCenterCommManager.echo()) {
                    _listener.onFinished(creditSettlement.getCreditErrorCode());
                    return;
                } else if (!AppPreference.isAvailable()) {
                    //疎通確認で利用停止の応答が返ってきた場合
                    _listener.onFinished(app.getString(R.string.error_type_not_available));
                    return;
                }

                // クレジットCA公開鍵DLチェック
                if (EMVHandler.getInstance().getCAPublicKeyNum() <= 0) {
                    // DLできてない場合は、再度、クレジットCA公開鍵DLを行う
                    String errCode = new McCredit().getCAKey();
                    if (errCode != null) {
                        _listener.onFinished(errCode);
                        return;
                    }
                }

                // 非接触が有効な場合リスク管理パラメータを取得チェック
                if (AppPreference.isMoneyContactless() && app.getRiskManagementParameter() == null) {
                    String errCode = new McCredit().getRiskParameterContactless();
                    if (errCode != null) {
                        _listener.onFinished(errCode);
                        return;
                    }

                    EmvCLProcess.emvInit();
                }

                /* カードデータ保護用公開鍵取得 */
                if (CreditSettlement.k_OK != creditSettlement._mcCenterCommManager.creditGetKey()) {
                    _listener.onFinished(creditSettlement.getCreditErrorCode());
                    return;
                }

                // チケット購入取消可能チェック
                if (AppPreference.isTicketTransaction() && type == BusinessType.REFUND) {

                    if (purchasedTicketDealId == null || purchasedTicketDealId.equals("")) {
                        // チケット購入IDが存在しない場合、
                        Timber.i("チケット取消確認不要(%s)", purchasedTicketDealId);
                    } else {
                        // チケットの取消時はABTセンターで取消ができるか確認が必要
                        final TicketSalesApi ticketSalesApiClient = TicketSalesApiImpl.getInstance();
                        final TerminalDao terminalDao = LocalDatabase.getInstance().terminalDao();
                        final TerminalData terminalData = terminalDao.getTerminal();
                        try {
                            // ABTに取消確認実施
                            TicketPurchasedConfirm.Response confirmResponse = ticketSalesApiClient.TicketPurchasedConfirm(terminalData.service_instance_abt, purchasedTicketDealId);

                        } catch (TicketSalesStatusException e) {
                            Timber.e(e);
                            int errorCode = e.getCode();
                            String errorMessage = e.getMessage();
                            if (404 == errorCode || 4007 == errorCode) {
                                Timber.i("取消許可(エラーコード：%s　内容：%s)", errorCode, errorMessage);
                            } else if (4008 == errorCode) {
                                _listener.onFinished(app.getString(R.string.error_type_ticket_8161));
                                return;
                            } else if (4009 == errorCode) {
                                _listener.onFinished(app.getString(R.string.error_type_ticket_8162));
                                return;
                            } else if (4010 == errorCode) {
                                _listener.onFinished(app.getString(R.string.error_type_ticket_8163));
                                return;
                            } else {
                                _listener.onFinished(app.getString(R.string.error_type_ticket_8160) + "@@@" + errorCode + "@@@");
                                return;
                            }
                        } catch (HttpStatusException e) {
                            Timber.e(e);
                            int statusCode = e.getStatusCode();
                            _listener.onFinished(app.getString(R.string.error_type_ticket_8160) + "@@@" + statusCode + "@@@");
                            return;
                        } catch (Exception e) {
                            Timber.e(e);
                            _listener.onFinished(app.getString(R.string.error_type_ticket_8097));
                            return;
                        }
                    }
                }
            }

            if (type == BusinessType.PAYMENT) {
                // 決済金額下限チェック
                if (Amount.getFixedAmount() <= 0) {
                    _listener.onFinished(app.getString(R.string.error_type_payment_system_2001));
                    return;
                }
                Timber.d("決済金額下限値チェックOK");

                // 決済金額上限チェック
                if (!McUtils.isCheckMaxAmount(Amount.getFixedAmount())) {
                    _listener.onFinished(app.getString(R.string.error_type_credit_3090));
                    return;
                }
                Timber.d("決済金額上限値チェックOK");
            }

            if (AppPreference.getIsExternalPrinter()) {
                /* つり銭機連動 */
                EpsonPrinterProc epsonPrinterProc = EpsonPrinterProc.getInstance();
                if (!epsonPrinterProc.checkConnect()) {
                    _listener.onFinished(app.getString(R.string.error_type_cashchanger_printer_connection_error));
                    return;
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
                            _listener.onFinished(app.getString(R.string.error_type_printer_sts_busy));
                            return;
                        case Printer.PRINTER_STATUS_HIGHT_TEMP: // 高温状態（-2）
                            _listener.onFinished(app.getString(R.string.error_type_printer_sts_hight_temp));
                            return;
                        case Printer.PRINTER_STATUS_PAPER_LACK: // 用紙切れ状態 (-3)
                            _listener.onFinished(app.getString(R.string.error_type_printer_sts_paper_lack));
                            return;
                        case Printer.PRINTER_STATUS_NO_BATTERY: // バッテリー残量不足状態（-4）
                            _listener.onFinished(app.getString(R.string.error_type_printer_sts_no_battery));
                            return;
                        case Printer.PRINTER_STATUS_FEED: // 用紙送り状態（-5）
                            _listener.onFinished(app.getString(R.string.error_type_printer_sts_feed));
                            return;
                        case Printer.PRINTER_STATUS_PRINT: // 印刷状態（-6）
                            _listener.onFinished(app.getString(R.string.error_type_printer_sts_print));
                            return;
                        case Printer.PRINTER_STATUS_FORCE_FEED: // 強制用紙送り状態（-7）
                            _listener.onFinished(app.getString(R.string.error_type_printer_sts_force_feed));
                            return;
                        case Printer.PRINTER_STATUS_POWER_ON: // 電源ON処理中状態（-8）
                            _listener.onFinished(app.getString(R.string.error_type_printer_sts_power_on));
                            return;
                        case Printer.PRINTER_TASKS_FULL: // 処理満載状態（-9）
                            _listener.onFinished(app.getString(R.string.error_type_printer_tasks_full));
                            return;
                        default:
                            // プリンター異常状態（未定義）
                            _listener.onFinished(app.getString(R.string.error_type_printer_sts_undefined) + "@@@" + isPrinterSts + "@@@");
                            return;
                    }
                    Timber.d("プリンターチェックOK");
                } else {
                    // LT27の場合IM-A820未接続状態であれば使わせない
                    if (!_ifBoxManager.isConnected()) {
                        _listener.onFinished(app.getString(R.string.error_type_ifbox_connection_error));
                        return;
                    }
                }
            }

            // バッテリー状態のチェック
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = view.getContext().registerReceiver(null, intentFilter);
            int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            Timber.i("battery %d %%", batteryLevel);

            if (batteryLevel <= BATTERY_LOWER_LIMIT) {
                _listener.onFinished(app.getString(R.string.error_type_low_battery_error));
                return;
            }
            Timber.d("バッテリーチェックOK");

            if (!isDemoMode()) {
                //通常モードの場合のみチェックする項目

                //係員設定チェック
                if (AppPreference.isDriverCodeInput() && AppPreference.getDriverCode().equals("")) {
                    _listener.onFinished(app.getString(R.string.error_type_payment_system_2013));
                    return;
                }

                //未送信売り上げの送信、決済前送信はクレジットのときのみとする
                //クレジットの売上送信に失敗した場合は使わせない
                String mcTerminalErrCode = new McTerminal().postPayment();
                if (mcTerminalErrCode != null) {
                    Timber.e("売上情報送信失敗：%s", mcTerminalErrCode);
                    //未送信のクレジット売上データがあるか確認
                    if (DBManager.getUriDao().getUnsentCreditData() != null) {
                        //未送信の売上データあり
                        if(mcTerminalErrCode.equals(McPosCenterErrorCodes.E0098)
                        || mcTerminalErrCode.equals(McPosCenterErrorCodes.E0901)) {
                            _listener.onFinished(McPosCenterErrorMap.get(mcTerminalErrCode));
                        } else {
                            _listener.onFinished(app.getString(R.string.error_type_credit_3006));
                        }
                        return;
                    }
                }
            }

            //ADD-S BMT S.Oyama 2024/10/03 フタバ双方向向け改修
            //決済選択キーコードの送出部（分別時は送出無し）
            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {

                if (_ifBoxManager.getIsConnected820() == false)             //820未接続の場合
                {
                    _listener.onFinished("6030");                       //IFBOX接続エラー
                    return;
                }

                IFBoxManager.SendMeterDataInfo_FutabaD tmpSend820Info = new IFBoxManager.SendMeterDataInfo_FutabaD();
                tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
                tmpSend820Info.ErrorCode820 = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;

                _meterDataV4InfoDisposable = _ifBoxManager.getMeterDataV4().subscribeOn(
                        Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(meter -> {
                    Timber.i("[FUTABA-D]CreditChecker:750<-820 meter_data event cmd:%d ", meter.meter_sub_cmd);
                    if(meter.meter_sub_cmd == 9) {              //ファンクション通知を受信
                        Timber.i("[FUTABA-D]CreditChecker:Function Req event");
                        tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;             //ACKが返ってきた場合
                    }
                });

                _meterDataV4ErrorDisposable = _ifBoxManager.getMeterDataV4Error().subscribeOn(
                        Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(error -> {         //送信中にエラー受信(タイムアウト，切断)
                    Timber.e("[FUTABA-D]CreditChecker:Error event ErrCD:%d 820ErrCD:%d ", error.ErrorCode, error.ErrorCode820);
                    tmpSend820Info.StatusCode = error.ErrorCode;
                    tmpSend820Info.ErrorCode820 = error.ErrorCode820;
                });

//                if (_Separation1stJobMode == AmountInputSeparationPayFDViewModel.AMOUNTINPUT_SEPARATIONMODE_TICKET) {        //初回分別がチケット時
//                    _ifBoxManager.send820_SeparateTicketJobFix_KeyCode();        //分別完了時のセットキーコード送信
//                    for(int i = 0; i < 5; i++)
//                    {
//                        try {
//                            Thread.sleep(100);
//                        } catch (InterruptedException e) {
//                        }
//                    }
//                }

                if (type == BusinessType.REFUND) {              //取り消し時
                    _ifBoxManager.send820_SettlementSelectMode(IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_CREDIT, true);                //決済選択モードを送信 クレジット取り消し
                } else {
                    _ifBoxManager.send820_SettlementSelectMode(IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_CREDIT, false);               //決済選択モードを送信 クレジット
                }

                boolean tmpLoopBreakOut = false;
                for(int i = 0; i < (DuplexPrintResponseTimerSec + 1) * 10; i++)        //最大26秒ほど待ってみる
                {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }

                    if (tmpSend820Info.StatusCode != IFBoxManager.SendMeterDataStatus_FutabaD.NONE)         //状態に変化が出たら直ちに抜ける
                    {
                        tmpLoopBreakOut = true;
                        break;
                    }
                }

                if (_meterDataV4InfoDisposable != null) {       //コールバック系を後始末
                    _meterDataV4InfoDisposable.dispose();
                    _meterDataV4InfoDisposable = null;
                }

                if (_meterDataV4ErrorDisposable != null)        //コールバック系を後始末
                {
                    _meterDataV4ErrorDisposable.dispose();
                    _meterDataV4ErrorDisposable = null;
                }

                if (tmpLoopBreakOut == false) {                             //820から何も返却されなかった場合のループアウト
                    _listener.onFinished("6030");                       //IFBOX接続エラー
                    return;
                }
                else
                {
                    switch(tmpSend820Info.StatusCode)                       //ステータスコードのチェック
                    {
                        case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_NOTCONNECTED:       //切断
                            _listener.onFinished("6030");                       //IFBOX接続エラー
                            return;
                        case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_TIMEOUT:           //タイムアウト
                            _listener.onFinished("6030");                       //IFBOX接続エラー
                            return;
                        case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_820NACK:              //820内でが返ってきた場合
                            Timber.e("[FUTABA-D]820 Inner error! ErrCD:%d", tmpSend820Info.ErrorCode820);
                            //ADD-S BMT S.Oyama 2025/01/29 フタバ双方向向け改修
                            if (tmpSend820Info.ErrorCode820 == IFBoxManager.Send820Status_Error_FutabaD.ERROR_STATUS820_PAPERLACKING)       //用紙無しエラー
                            {
                                _listener.onFinished("");                           //用紙なしの場合は空文字を入れる(NULLはNG　9110を入れると２重でエラーが出る)
                            }
                            else
                            {
                                _listener.onFinished("6030");                       //IFBOX接続エラー
                            }
                            //ADD-E BMT S.Oyama 2025/01/29 フタバ双方向向け改修
                            return;
                        default:
                            //ここに到達する場合は，エラー無しで決済選択モードが送信されたことを意味する
                            break;
                    }
                }
            }
            //ADD-E BMT S.Oyama 2024/10/03 フタバ双方向向け改修

            _listener.onFinished(null);
        }).start();
    }
}
