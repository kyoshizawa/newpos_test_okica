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
import jp.mcapps.android.multi_payment_terminal.data.BusinessType;
import jp.mcapps.android.multi_payment_terminal.data.CurrentRadio;
import jp.mcapps.android.multi_payment_terminal.error.GmoErrorMap;
import jp.mcapps.android.multi_payment_terminal.service.GetRadioService;
import timber.log.Timber;

public class ValidationCheckChecker {
    private static final int BATTERY_LOWER_LIMIT = 10;  // パーセント
    public static String check(View view) {
        Timber.d("有効性確認起動前チェック開始");

        final MainApplication app = MainApplication.getInstance();

        //通常モードの場合のみチェックする項目
        if (!AppPreference.isDemoMode()) {
            //電波強度が強or中じゃない場合は使わせない、エラーコード1001
            if (CurrentRadio.getImageLevel() == 0) {
                return MainApplication.getInstance().getString(R.string.error_type_comm_reception);
            } else if (CurrentRadio.getImageLevel() == GetRadioService.AIRPLANE_MODE) {
                //機内モード状態の場合は使わせない、エラーコード1003
                return MainApplication.getInstance().getString(R.string.error_type_airplane_mode);
            }

            //係員設定チェック
            if (AppPreference.isDriverCodeInput() && AppPreference.getDriverCode().equals("")) {
                return app.getString(R.string.error_type_payment_system_2013);
            }
        }

        //通常モード・デモモード共通でチェックする項目
        // 決済金額下限チェック
        if (Amount.getFixedAmount() <= 0) {
            return "2001";
        }
        Timber.d("決済金額下限値チェックOK");

        // バッテリー状態のチェック
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = view.getContext().registerReceiver(null, intentFilter);
        int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

        if (batteryLevel <= BATTERY_LOWER_LIMIT) {
            return app.getString(R.string.error_type_low_battery_error);
        }

        return null;
    }
}
