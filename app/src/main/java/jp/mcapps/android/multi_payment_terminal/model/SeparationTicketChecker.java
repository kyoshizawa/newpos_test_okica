package jp.mcapps.android.multi_payment_terminal.model;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.view.View;

import com.pos.device.printer.Printer;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
//import jp.mcapps.android.multi_payment_terminal.database.DBManager;
//import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketReceiptDao;
//import jp.mcapps.android.multi_payment_terminal.devices.GloryCashChanger;
import timber.log.Timber;

public class SeparationTicketChecker {
    private static final int BATTERY_LOWER_LIMIT = 10;  // パーセント

    public static String check(View view) {
        Timber.d("分別チケット起動前チェック開始");

        // 通常モードの場合のみチェックする項目
        if(!AppPreference.isDemoMode()) {
            // 係員設定チェック
            if(AppPreference.isDriverCodeInput() && AppPreference.getDriverCode().equals("")) {
                return "2013";
            }
        }

        // 通常モード・デモモード共通でチェックする項目
        if (AppPreference.isTicketTransaction()) {
            // チケット販売の場合は下限チェックなし
        } else {
            // 決済金額下限チェック
            if (Amount.getTotalAmount() <= 0) {
                return "2001";
            }
            Timber.d("決済金額下限値チェックOK");
        }

        // バッテリー状態のチェック
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = view.getContext().registerReceiver(null, intentFilter);
        int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

        if(batteryLevel <= BATTERY_LOWER_LIMIT) {
            return "6021";
        }

        if (AppPreference.getIsCashChanger()) {
            /* つり銭機連動 */
//            GloryCashChanger gloryCashChanger = GloryCashChanger.getInstance();
//            if (gloryCashChanger == null) {
//                return "6102";
//            }
//            if (gloryCashChanger.connect() == false) {
//                return "6102";
//            }
            // CashChangerPaymentViewModel.start()におけるconnectを速くするため、
            // あえてdisconnectは行わない
        } else {
            MainApplication app = MainApplication.getInstance();
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
                //Timber.e("分別チケットは双方向系で使う予定はないので現状未対応");
                //return app.getString(R.string.error_type_ifbox_connection_error);
            }
        }

        return null;
    }
}
