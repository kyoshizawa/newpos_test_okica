package jp.mcapps.android.multi_payment_terminal.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public abstract class PickedDateTimeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        Date pickedDateTime = new Date(bundle.getLong("date"));
        onPickedDateTimeReceive(pickedDateTime);
    }

    protected abstract void onPickedDateTimeReceive(Date date);
}
