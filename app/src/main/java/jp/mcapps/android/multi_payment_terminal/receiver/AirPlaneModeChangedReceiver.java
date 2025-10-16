package jp.mcapps.android.multi_payment_terminal.receiver;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.data.CurrentRadio;
import timber.log.Timber;

public class AirPlaneModeChangedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (1 == Settings.System.getInt(getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0)) {
            Timber.i("機内モード変更（OFF ⇒ ON）");
            CurrentRadio.setAirplaneMode();
        } else {
            Timber.i("機内モード変更（ON ⇒ OFF）");
            CurrentRadio.cancelAirplaneMode();
        }
    }

    private ContentResolver getContentResolver() {
        return MainApplication.getInstance().getContentResolver();
    }
}
