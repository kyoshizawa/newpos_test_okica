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
//import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
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

        }

        return null;
    }
}
