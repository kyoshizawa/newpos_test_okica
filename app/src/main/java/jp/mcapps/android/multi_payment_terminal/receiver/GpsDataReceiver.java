package jp.mcapps.android.multi_payment_terminal.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import jp.mcapps.android.multi_payment_terminal.database.history.gps.GpsData;

public abstract class GpsDataReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        GpsData gpsData = (GpsData)bundle.getSerializable("gps");
        onGpsDataReceive(gpsData);
    }

    protected abstract void onGpsDataReceive(GpsData gpsData);
}
