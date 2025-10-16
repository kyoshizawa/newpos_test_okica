package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsData;
import jp.mcapps.android.multi_payment_terminal.service.PeriodicGateCheckService;
import jp.mcapps.android.multi_payment_terminal.ui.pin.PinInputViewModel;
import timber.log.Timber;

public class TicketGateQrScanViewModel extends PinInputViewModel {

    public TicketGateQrScanViewModel() { super(); }

    {_pinDigits = 10;}

    // パスワード入力
    private MutableLiveData<Boolean> _isPinInput = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isPinInput() {
        return _isPinInput;
    }
    public void isPinInput(boolean b) {
        _isPinInput.setValue(b);
    }

    private final MutableLiveData<String> _display = new MutableLiveData<>("パスワードを入力してください。");
    public final MutableLiveData<String> getDisplay() {
        return _display;
    }

    @Override
    public boolean enter() {
        enterBeep();    //効果音
        return _pin.equals("8888888888");
    }

    @Override
    public void cancel() {

    }
}
