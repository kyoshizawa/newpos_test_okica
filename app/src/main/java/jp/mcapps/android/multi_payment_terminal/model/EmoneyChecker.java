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
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.data.CurrentRadio;
import jp.mcapps.android.multi_payment_terminal.data.EmoneyOpeningInfo;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsData;
import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorCodes;
import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorMap;
import jp.mcapps.android.multi_payment_terminal.service.GetRadioService;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.webapi.HttpStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApi;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.TicketSalesStatusException;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.TicketPurchasedConfirm;
import jp.mcapps.android.multi_payment_terminal.thread.printer.EpsonPrinterProc;
import timber.log.Timber;

public class EmoneyChecker {
    private static EmoneyChecker _instance = null;
    private static final int ELAPSED_TIME_ERROR = 1000 * 60 * 60 * 24;
    private static final int BATTERY_LOWER_LIMIT = 10;  // パーセント

    public static EmoneyChecker getInstance() {
        if(_instance == null){
            _instance = new EmoneyChecker();
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

    public static String check(View view, String moneyBrand, BusinessType businessType) {
        return check(view, moneyBrand, businessType, null);
    }

    public static String check(View view, String moneyBrand, BusinessType businessType, String purchasedTicketDealId) {
        Timber.d("電マネ起動前チェック開始");

        //デモモードの場合のみチェックする項目
        if (AppPreference.isDemoMode()) {
            MainApplication app = MainApplication.getInstance();
            String upperLimitErrorCode = null;
            if (businessType != BusinessType.BALANCE) {
                if (businessType == BusinessType.PAYMENT) {
                    // 決済金額下限チェック
                    if (Amount.getFixedAmount() <= 0) {
                        return "2001";
                    }

                    // 決済金額上限チェック
                    boolean isBelowUpperLimit = moneyBrand.equals(app.getString(R.string.money_brand_suica))
                            ? Amount.getFixedAmount() <= 99999    // Suicaは上限99,999円
                            : moneyBrand.equals(app.getString(R.string.money_brand_waon))
                            ? Amount.getFixedAmount() <= 100000    // waonは上限100,000円
                            : Amount.getFixedAmount() <= 999999;  // それ以外は999,999円

                    if (!isBelowUpperLimit) {
                        if (moneyBrand.equals(app.getString(R.string.money_brand_suica))) {
                            upperLimitErrorCode = app.getString(R.string.error_type_suica_upper_limit_error);
                        }
                        else if (moneyBrand.equals(app.getString(R.string.money_brand_id))) {
                            upperLimitErrorCode = app.getString(R.string.error_type_id_upper_limit_error);
                        }
                        else if (moneyBrand.equals(app.getString(R.string.money_brand_waon))) {
                            upperLimitErrorCode = app.getString(R.string.error_type_waon_upper_limit_error);
                        }
                        else if (moneyBrand.equals(app.getString(R.string.money_brand_nanaco))) {
                            upperLimitErrorCode = app.getString(R.string.error_type_nanaco_upper_limit_error);
                        }
                        else if (moneyBrand.equals(app.getString(R.string.money_brand_qp))) {
                            upperLimitErrorCode = app.getString(R.string.error_type_quicpay_upper_limit_error);
                        }
                        else if (moneyBrand.equals(app.getString(R.string.money_brand_edy))) {
                            upperLimitErrorCode = app.getString(R.string.error_type_edy_upper_limit_error);
                        }

                        return upperLimitErrorCode;
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
                        if (!_ifBoxManager.isConnected()) {
                            return app.getString(R.string.error_type_ifbox_connection_error);
                        }
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

            //ADD-S BMT S.Oyama 2024/10/07 フタバ双方向向け改修
            if ( (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) && (businessType != BusinessType.BALANCE)) { // 2025.02.14 t.wada 残照はキー通知しない

                if (_ifBoxManager.getIsConnected820() == false)             //820未接続の場合
                {
                    return "6030";                       //IFBOX接続エラー
                }

                IFBoxManager.SendMeterDataInfo_FutabaD tmpSend820Info = new IFBoxManager.SendMeterDataInfo_FutabaD();
                tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
                tmpSend820Info.IsLoopBreakOut = false;
                tmpSend820Info.ErrorCode820 = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;

                _meterDataV4InfoDisposable = _ifBoxManager.getMeterDataV4().subscribeOn(
                        Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(meter -> {     //AndroidSchedulers.mainThread()
                    Timber.i("[FUTABA-D]EmoneyChecker:750<-820 meter_data event cmd:%d zandaka_flg:%d", meter.meter_sub_cmd, meter.zandaka_flg);
                    if(meter.meter_sub_cmd == 9) {              //ファンクション通知を受信
                        Timber.i("[FUTABA-D]EmoneyChecker:Function Req event");
                        if (businessType == BusinessType.BALANCE) {                 //残高照会時
                            if ((meter.zandaka_flg != null) && (meter.zandaka_flg == 1)) {             //残高照会時の残高情報を受信
                                tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;
                            }
                            else {
                                tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SENDNG;           //残高照会時の残高情報を受信できない
                            }
                        } else {
                            tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;             //ACKが返ってきた場合
                        }
                    }
                });

                _meterDataV4ErrorDisposable = _ifBoxManager.getMeterDataV4Error().subscribeOn(
                        Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(error -> {         //送信中にエラー受信(タイムアウト，切断)
                    Timber.e("[FUTABA-D]EmoneyChecker:Error event ErrCD:%d 820ErrCD:%d ", error.ErrorCode, error.ErrorCode820);
                    tmpSend820Info.StatusCode = error.ErrorCode;
                    tmpSend820Info.ErrorCode820 = error.ErrorCode820;

                });

                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {

                        if (businessType == BusinessType.BALANCE) {                 //残高照会時
//                            if (moneyBrand.equals(app.getString(R.string.money_brand_suica))) {         //決済ブランドにより決済選択モードを送信
//                                _ifBoxManager.send820_BalanceInquiryMode(IFBoxManager.SendMeterDataStatus_FutabaD.BALANCEINQUIRY_SUICA);            //残高照会モードを送信 SUICA
//                            } else if (moneyBrand.equals(app.getString(R.string.money_brand_waon))) {
//                                _ifBoxManager.send820_BalanceInquiryMode(IFBoxManager.SendMeterDataStatus_FutabaD.BALANCEINQUIRY_WAON);             //残高照会モードを送信 WAON
//                            } else if (moneyBrand.equals(app.getString(R.string.money_brand_nanaco))) {
//                                _ifBoxManager.send820_BalanceInquiryMode(IFBoxManager.SendMeterDataStatus_FutabaD.BALANCEINQUIRY_NANACO);           //残高照会モードを送信 NANACO
//                            } else if (moneyBrand.equals(app.getString(R.string.money_brand_edy))) {
//                                _ifBoxManager.send820_BalanceInquiryMode(IFBoxManager.SendMeterDataStatus_FutabaD.BALANCEINQUIRY_EDY);              //残高照会モードを送信 EDY
//                            } else {
//                                tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SELECTMODE;             //決済ブランドが不明
//                            }
                        }
                        else                                                        //それ以外：決済，決済取消
                        {
                            if (moneyBrand.equals(app.getString(R.string.money_brand_suica))) {         //決済ブランドにより決済選択モードを送信
                                _ifBoxManager.send820_SettlementSelectMode(IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_SUICA, false);             //決済選択モードを送信 SUICA
                            } else if (moneyBrand.equals(app.getString(R.string.money_brand_id))) {
                                if (businessType == BusinessType.REFUND) {              //取り消し時
                                    _ifBoxManager.send820_SettlementSelectMode(IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_ID, true);                //決済選択モードを送信 iD
                                } else {
                                    _ifBoxManager.send820_SettlementSelectMode(IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_ID, false);                //決済選択モードを送信 iD
                                }
                            } else if (moneyBrand.equals(app.getString(R.string.money_brand_waon))) {
                                _ifBoxManager.send820_SettlementSelectMode(IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_WAON, false);              //決済選択モードを送信 waon
                            } else if (moneyBrand.equals(app.getString(R.string.money_brand_nanaco))) {
                                _ifBoxManager.send820_SettlementSelectMode(IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_NANACO, false);            //決済選択モードを送信 nanaco
                            } else if (moneyBrand.equals(app.getString(R.string.money_brand_qp))) {
                                _ifBoxManager.send820_SettlementSelectMode(IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_QUICPAY, false);           //決済選択モードを送信 quicpay
                            } else if (moneyBrand.equals(app.getString(R.string.money_brand_edy))) {
                                _ifBoxManager.send820_SettlementSelectMode(IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_EDY, false);               //決済選択モードを送信 EDY
                            } else {
                                tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SELECTMODE;             //決済ブランドが不明
                            }
                        }

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
                            case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_TIMEOUT:            //タイムアウト
                                return "6030";                       //IFBOX接続エラー
                            case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SELECTMODE:         //選択モードエラー
                                return "6030";                       //IFBOX接続エラー
                            case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SENDNG:             //zandaka_flg送信エラー(1が返ってきていない)
                                return "6030";                       //IFBOX接続エラー
                            case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_820NACK:              //820内でが返ってきた場合
                                Timber.e("[FUTABA-D](demo)820 Inner error! ErrCD:%d", tmpSend820Info.ErrorCode820);
                                //ADD-S BMT S.Oyama 2025/01/29 フタバ双方向向け改修
                                if (tmpSend820Info.ErrorCode820 == IFBoxManager.Send820Status_Error_FutabaD.ERROR_STATUS820_PAPERLACKING)       //用紙無しエラー
                                {
                                    return "9110";                       //用紙なしエラー（クレカと違ってここではエラーコードを返す：サブメニューに居るためHOMEメニューの用紙切れエラーを検知できない）
                                }
                                else
                                {
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

        //通常モードの場合のみチェックする項目

        //電波強度が強or中じゃない場合は使わせない、エラーコード1001
        if (CurrentRadio.getImageLevel() == 0) {
            return MainApplication.getInstance().getString(R.string.error_type_comm_reception);
        } else if (CurrentRadio.getImageLevel() == GetRadioService.AIRPLANE_MODE) {
            //機内モード状態の場合は使わせない、エラーコード1003
            return MainApplication.getInstance().getString(R.string.error_type_airplane_mode);
        }

        // 電子マネー認証されてない場合は使わせない
        final File certFile = new File(
                MainApplication.getInstance().getFilesDir(), BuildConfig.JREM_CLIENT_CERTIFICATE);

        if (!certFile.exists()) {
            // 証明書が存在していない
            // 直前に確認しているため通常ありえないルートとなる
            return JremRasErrorMap.get(JremRasErrorCodes.E4901);
        }
        Timber.d("証明書チェックOK");

        // 開閉局チェック
        MainApplication app = MainApplication.getInstance();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);

        Date openingDate = null;
        String openingErrorCode = null;
        String upperLimitErrorCode = null;
        String closingErrorCode = null;

        try {
            /**/ if (moneyBrand.equals(app.getString(R.string.money_brand_suica))) {
                openingDate = dateFormat.parse(AppPreference.getDatetimeOpeningSuica());
                openingErrorCode = app.getString(R.string.error_type_opening_suica_error);
                upperLimitErrorCode = app.getString(R.string.error_type_suica_upper_limit_error);
                if(EmoneyOpeningInfo.getSuica() == null || !EmoneyOpeningInfo.getSuica().result) {
                    closingErrorCode = app.getString(R.string.error_type_suica_closed_error);
                }
            }
            else if (moneyBrand.equals(app.getString(R.string.money_brand_id))) {
                openingDate = dateFormat.parse(AppPreference.getDatetimeOpeningId());
                openingErrorCode = app.getString(R.string.error_type_opening_id_error);
                upperLimitErrorCode = app.getString(R.string.error_type_id_upper_limit_error);
                if(EmoneyOpeningInfo.getId() == null || !EmoneyOpeningInfo.getId().mresult) {
                    closingErrorCode = app.getString(R.string.error_type_id_closed_error);
                }
            }
            else if (moneyBrand.equals(app.getString(R.string.money_brand_waon))) {
                openingDate = dateFormat.parse(AppPreference.getDatetimeOpeningWaon());
                openingErrorCode = app.getString(R.string.error_type_opening_waon_error);
                upperLimitErrorCode = app.getString(R.string.error_type_waon_upper_limit_error);
                if(EmoneyOpeningInfo.getWaon() == null || !EmoneyOpeningInfo.getWaon().mresult) {
                    closingErrorCode = app.getString(R.string.error_type_waon_closed_error);
                }
            }
            else if (moneyBrand.equals(app.getString(R.string.money_brand_nanaco))) {
                openingDate = dateFormat.parse(AppPreference.getDatetimeOpeningNanaco());
                openingErrorCode = app.getString(R.string.error_type_opening_nanaco_error);
                upperLimitErrorCode = app.getString(R.string.error_type_nanaco_upper_limit_error);
                if(EmoneyOpeningInfo.getNanaco() == null || !EmoneyOpeningInfo.getNanaco().mresult) {
                    closingErrorCode = app.getString(R.string.error_type_nanaco_closed_error);
                }
            }
            else if (moneyBrand.equals(app.getString(R.string.money_brand_qp))) {
                openingDate = dateFormat.parse(AppPreference.getDatetimeOpeningQuicpay());
                openingErrorCode = app.getString(R.string.error_type_opening_quicpay_error);
                upperLimitErrorCode = app.getString(R.string.error_type_quicpay_upper_limit_error);
                if(EmoneyOpeningInfo.getQuicpay() == null || !EmoneyOpeningInfo.getQuicpay().mresult) {
                    closingErrorCode = app.getString(R.string.error_type_quicpay_closed_error);
                }
            }
            else if (moneyBrand.equals(app.getString(R.string.money_brand_edy))) {
                openingDate = dateFormat.parse(AppPreference.getDatetimeOpeningEdy());
                openingErrorCode = app.getString(R.string.error_type_opening_edy_error);
                upperLimitErrorCode = app.getString(R.string.error_type_edy_upper_limit_error);
                if(EmoneyOpeningInfo.getEdy() == null || !EmoneyOpeningInfo.getEdy().mresult) {
                    closingErrorCode = app.getString(R.string.error_type_edy_closed_error);
                }
            }
        } catch (ParseException e) {
            Timber.e(e);
            return "";
        }

        final Date currentDatetime = new Date();

        if (openingDate != null) {

            long elapsedTime = currentDatetime.getTime() - openingDate.getTime();

            Timber.d("開局経過時間: %s分", elapsedTime / 1000 / 60);

            //開局から24時間経過でエラーをスタック
            if (elapsedTime >= ELAPSED_TIME_ERROR) {
                return openingErrorCode;
            }
        } else {
            return openingErrorCode;
        }


        if (closingErrorCode != null) {
            return closingErrorCode;
        }

        Timber.d("開閉局チェックOK");

        if (businessType != BusinessType.BALANCE) {
            if (businessType == BusinessType.PAYMENT) {
                // 決済金額下限チェック
                if (Amount.getFixedAmount() <= 0) {
                    return "2001";
                }
                Timber.d("決済金額下限値チェックOK");

                // 決済金額上限チェック
                boolean isBelowUpperLimit = moneyBrand.equals(app.getString(R.string.money_brand_suica))
                        ? Amount.getFixedAmount() <= 99999    // Suicaは上限99,999円
                        : moneyBrand.equals(app.getString(R.string.money_brand_waon))
                        ? Amount.getFixedAmount() <= 100000    // waonは上限100,000円
                        : Amount.getFixedAmount() <= 999999;  // それ以外は999,999円

                if (!isBelowUpperLimit) {
                    return upperLimitErrorCode;
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

        //利用許可状態チェック
        //電マネ決済前に疎通確認は行っていないためRAMを参照
        //起動後に疎通確認を行っていない場合はエラーとしない
        if(AppPreference.isMcEcho() && !AppPreference.isAvailable()) {
            return app.getString(R.string.error_type_not_available);
        }

        // チケット購入取消可能チェック
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

        //ADD-S BMT S.Oyama 2024/10/07 フタバ双方向向け改修
        if ( (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) && (businessType != BusinessType.BALANCE) ) { // 2025.02.14 t.wada 残照はキー通知しない

            if (_ifBoxManager.getIsConnected820() == false)             //820未接続の場合
            {
                return "6030";                       //IFBOX接続エラー
            }

            IFBoxManager.SendMeterDataInfo_FutabaD tmpSend820Info = new IFBoxManager.SendMeterDataInfo_FutabaD();
            tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;
            tmpSend820Info.IsLoopBreakOut = false;
            tmpSend820Info.ErrorCode820 = IFBoxManager.SendMeterDataStatus_FutabaD.NONE;

            _meterDataV4InfoDisposable = _ifBoxManager.getMeterDataV4().subscribeOn(
                    Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(meter -> {
                Timber.i("[FUTABA-D]EmoneyChecker:750<-820 meter_data event cmd:%d zandaka_flg:%d", meter.meter_sub_cmd, meter.zandaka_flg);
                if(meter.meter_sub_cmd == 9) {              //ファンクション通知を受信
                    Timber.i("[FUTABA-D]EmoneyChecker:Function Req event");
                    if (businessType == BusinessType.BALANCE) {                 //残高照会時
                        if (meter.zandaka_flg == 1) {             //残高照会時の残高情報を受信
                            tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;
                        }
                        else {
                            tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SENDNG;           //残高照会時の残高情報を受信できない
                        }
                    } else {
                        tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.SENDOK;             //ACKが返ってきた場合
                    }
                }
            });

            _meterDataV4ErrorDisposable = _ifBoxManager.getMeterDataV4Error().subscribeOn(
                    Schedulers.io()).observeOn(Schedulers.newThread()).subscribe(error -> {         //送信中にエラー受信(タイムアウト，切断)
                Timber.e("[FUTABA-D]EmoneyChecker:Error event ErrCD:%d 820ErrCD:%d ", error.ErrorCode, error.ErrorCode820);
                tmpSend820Info.StatusCode = error.ErrorCode;
                tmpSend820Info.ErrorCode820 = error.ErrorCode820;

            });

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    if (businessType == BusinessType.BALANCE) {                 //残高照会時
//                        if (moneyBrand.equals(app.getString(R.string.money_brand_suica))) {         //決済ブランドにより決済選択モードを送信
//                            _ifBoxManager.send820_BalanceInquiryMode(IFBoxManager.SendMeterDataStatus_FutabaD.BALANCEINQUIRY_SUICA);            //残高照会モードを送信 SUICA
//                        } else if (moneyBrand.equals(app.getString(R.string.money_brand_waon))) {
//                            _ifBoxManager.send820_BalanceInquiryMode(IFBoxManager.SendMeterDataStatus_FutabaD.BALANCEINQUIRY_WAON);             //残高照会モードを送信 WAON
//                        } else if (moneyBrand.equals(app.getString(R.string.money_brand_nanaco))) {
//                            _ifBoxManager.send820_BalanceInquiryMode(IFBoxManager.SendMeterDataStatus_FutabaD.BALANCEINQUIRY_NANACO);           //残高照会モードを送信 NANACO
//                        } else if (moneyBrand.equals(app.getString(R.string.money_brand_edy))) {
//                            _ifBoxManager.send820_BalanceInquiryMode(IFBoxManager.SendMeterDataStatus_FutabaD.BALANCEINQUIRY_EDY);              //残高照会モードを送信 EDY
//                        } else {
//                            tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SELECTMODE;             //決済ブランドが不明
//                        }
                    }
                    else                                                        //それ以外：決済，決済取消
                    {
                        if (moneyBrand.equals(app.getString(R.string.money_brand_suica))) {         //決済ブランドにより決済選択モードを送信
                            _ifBoxManager.send820_SettlementSelectMode(IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_SUICA, false);             //決済選択モードを送信 SUICA
                        } else if (moneyBrand.equals(app.getString(R.string.money_brand_id))) {
                            if (businessType == BusinessType.REFUND) {              //取り消し時
                                _ifBoxManager.send820_SettlementSelectMode(IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_ID, true);                //決済選択モードを送信 iD
                            } else {
                                _ifBoxManager.send820_SettlementSelectMode(IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_ID, false);                //決済選択モードを送信 iD
                            }
                        } else if (moneyBrand.equals(app.getString(R.string.money_brand_waon))) {
                            _ifBoxManager.send820_SettlementSelectMode(IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_WAON, false);              //決済選択モードを送信 waon
                        } else if (moneyBrand.equals(app.getString(R.string.money_brand_nanaco))) {
                            _ifBoxManager.send820_SettlementSelectMode(IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_NANACO, false);            //決済選択モードを送信 nanaco
                        } else if (moneyBrand.equals(app.getString(R.string.money_brand_qp))) {
                            _ifBoxManager.send820_SettlementSelectMode(IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_QUICPAY, false);           //決済選択モードを送信 quicpay
                        } else if (moneyBrand.equals(app.getString(R.string.money_brand_edy))) {
                            _ifBoxManager.send820_SettlementSelectMode(IFBoxManager.SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_EDY, false);               //決済選択モードを送信 EDY
                        } else {
                            tmpSend820Info.StatusCode = IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SELECTMODE;             //決済ブランドが不明
                        }
                    }

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
                        case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_TIMEOUT:            //タイムアウト
                            return "6030";                       //IFBOX接続エラー
                        case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SELECTMODE:         //選択モードエラー
                            return "6030";                       //IFBOX接続エラー
                        case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_SENDNG:             //zandaka_flg送信エラー(1が返ってきていない)
                            return "6030";                       //IFBOX接続エラー
                        case IFBoxManager.SendMeterDataStatus_FutabaD.ERROR_820NACK:              //820内でが返ってきた場合
                            Timber.e("[FUTABA-D]820 Inner error! ErrCD:%d", tmpSend820Info.ErrorCode820);
                            if (tmpSend820Info.ErrorCode820 == IFBoxManager.Send820Status_Error_FutabaD.ERROR_STATUS820_PAPERLACKING)       //用紙無しエラー
                            {
                                return "9110";                       //用紙なしエラー（クレカと違ってここではエラーコードを返す：サブメニューに居るためHOMEメニューの用紙切れエラーを検知できない）
                            }
                            else
                            {
                                return "6030";                       //IFBOX接続エラー
                            }
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
