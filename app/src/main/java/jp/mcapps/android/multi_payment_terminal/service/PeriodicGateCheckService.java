package jp.mcapps.android.multi_payment_terminal.service;

import static jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl.FORCE_DEACT_END;
import static jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl.FORCE_DEACT_EXIST_AGGREGATE;
import static jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl.FORCE_DEACT_EXIST_UNSENT;
import static jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl.FORCE_DEACT_NONE;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.common.base.Strings;
import com.pos.device.printer.Printer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.disposables.Disposable;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.EmoneyOpeningInfo;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.data.okica.OkicaNegaFile;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorData;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingDao;
import jp.mcapps.android.multi_payment_terminal.database.history.error.ErrorStackingData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsData;
import jp.mcapps.android.multi_payment_terminal.error.ErrorManage;
import jp.mcapps.android.multi_payment_terminal.model.OkicaMasterControl;
import jp.mcapps.android.multi_payment_terminal.ui.ticket.TicketGateRoute;
import timber.log.Timber;

public class PeriodicGateCheckService extends Service implements ViewModelStoreOwner {
    private PeriodicGateCheckServiceViewModel _viewModel;
    private final ViewModelStore _viewModelStore = new ViewModelStore();

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate() {
        super.onCreate();
        _viewModel = new ViewModelProvider(this, MainApplication.getViewModelFactory()).get(PeriodicGateCheckServiceViewModel.class);
        Timber.d("Periodic Gate Check Service Start");

        _viewModel.fetch(this);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        List<TicketGateRoute> list = (List<TicketGateRoute>) bundle.getSerializable("ticketRouteList");
        _viewModel.setTicketGateRoutes(list);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        _viewModel.shutdown();

        //Serviceの終了を通知
        Intent intent = new Intent();
        intent.setAction("SERVICE_STOP");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        Timber.d("Periodic Gate Check Service End");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        return _viewModelStore;
    }
}
