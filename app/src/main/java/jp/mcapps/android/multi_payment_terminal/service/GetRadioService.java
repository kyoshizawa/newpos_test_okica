package jp.mcapps.android.multi_payment_terminal.service;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthLte;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.data.CurrentRadio;
import jp.mcapps.android.multi_payment_terminal.database.history.radio.RadioDao;
import jp.mcapps.android.multi_payment_terminal.database.history.radio.RadioData;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import timber.log.Timber;

public class GetRadioService extends Service {
    private TelephonyManager _tm;
    private ConnectivityManager _cm;
    private WifiManager _wm;
    private RadioData _radioData = new RadioData();
    private String _networkType = "NONE";
    private int _networkTypeCode;
    private Handler _handler;
    private Runnable _run;
    private final ScheduledExecutorService _executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> _future;
    private RadioData _prevData;

    private static boolean _isAirplaneMode = false;

    private final String TYPE_LTE = "LTE";
    private final String TYPE_3G = "3G";
    private final String TYPE_WIFI = "WIFI";
    private final String TYPE_OTHER = "OTHER";
    private final String TYPE_NONE = "NONE";

    private final int RADIO_LEVEL_LOW       = 0;    //アンテナレベル弱
    private final int RADIO_LEVEL_MIDDLE    = 1;    //アンテナレベル中
    private final int RADIO_LEVEL_HIGH      = 2;    //アンテナレベル強
    public static final int AIRPLANE_MODE   = 3;    //機内モード

    private MyPhoneStateListener _phoneStateListener = new MyPhoneStateListener();
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        //設定から取得したDB保存の間隔 デフォルト10秒
        int interval = intent != null ? intent.getIntExtra("INTERVAL", 10) * 1000 : 10000;

        _tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        _wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        _cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        _tm.registerTelephonyCallback(getMainExecutor(), _phoneStateListener);

        HandlerThread thread = new HandlerThread("radioServiceThread");
        thread.start();

        _handler = new Handler(thread.getLooper());
        _run = new Runnable() {
            @Override
            public void run() {
                //ネットワーク接続を確認
                _networkType = checkNetworkType();
                switch (_networkType) {
                    case TYPE_LTE:
                    case TYPE_3G:
                        //LTE, 3G
                        //_radioData.date = new Date();
                        _radioData.networkType = _networkType;
                        _radioData.networkTypeCode = _networkTypeCode;
                        break;
                    case TYPE_WIFI:
                        //WIFI
                        getWifiInfo();
                        break;
                    default :
                        //未接続、2G、5G
                        _radioData = new RadioData();
                        _radioData.networkType = _networkType;
                        _radioData.networkTypeCode = _networkTypeCode;
                        _radioData.level = 0;
                        _radioData.signalStrength = 0;
                        _radioData.date = new Date();
                        break;
                }

                try {
                    sendBroadcast();

                    if (AppPreference.isAntlogEnabled()) {
                        LocalDatabase db = LocalDatabase.getInstance();
                        RadioDao dao = db.radioDao();
                        dao.insertRadioData(_radioData);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Timber.d("電波履歴保存エラー");
                }
                _handler.postDelayed(this, interval);
            }
        };
        _handler.post(_run);

        registerReceiver(_screenOn, new IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(_screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF));

        return START_NOT_STICKY;
    }

    class MyPhoneStateListener extends TelephonyCallback implements TelephonyCallback.SignalStrengthsListener {

        @Override
        public void onSignalStrengthsChanged(@NonNull SignalStrength signalStrength) {

            _radioData = new RadioData();
            _radioData.date = new Date();
            _radioData.networkType = _networkType;
            _radioData.networkTypeCode = _networkTypeCode;

            switch (checkNetworkType()) {
                case TYPE_LTE :
                    for (CellSignalStrength ss : signalStrength.getCellSignalStrengths()) {
                        if (ss instanceof CellSignalStrengthLte) {
                            CellSignalStrengthLte lte = (CellSignalStrengthLte) ss;
                            _radioData.level = lte.getLevel();
                            _radioData.rsrp = lte.getRsrp();
                            _radioData.rsrq = lte.getRsrq();
                            _radioData.rssnr = lte.getRssnr();
                            _radioData.signalStrength = lte.getDbm();
                            _radioData.asu = lte.getAsuLevel();
                            break;
                        }
                    }
                    break;
                case TYPE_3G :
                    try {
                        _radioData.level = signalStrength.getLevel();
                        List<CellSignalStrength> strengths = signalStrength.getCellSignalStrengths();
                        for (CellSignalStrength ss : strengths) {
                            if (!(ss instanceof CellSignalStrengthLte)) {  // LTE以外を対象
                                _radioData.signalStrength = ss.getDbm();
                                _radioData.asu = ss.getAsuLevel();
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Timber.e(e, "3G Signal parse failed");
                    }
                    break;
                default :
                    // その他は無視
                    return;
            }

            sendBroadcast();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendBroadcast() {
        Intent changeRadio = new Intent().setAction("CHANGE_RADIO_DATA");
        Intent changeImage = new Intent().setAction("CHANGE_RADIO_LEVEL_IMAGE");

        int currentImageLevel = CurrentRadio.getImageLevel();
        int newImageLevel = getNetworkAntLevel(_radioData.level, _radioData.networkType, _radioData.rsrp);

        String currentState = (currentImageLevel == RADIO_LEVEL_MIDDLE || currentImageLevel == RADIO_LEVEL_HIGH) ? "通信可" : "通信不可";
        final String newState = (newImageLevel == RADIO_LEVEL_MIDDLE || newImageLevel == RADIO_LEVEL_HIGH) ? "通信可" : "通信不可";

        if(_radioData.networkType == "NONE") {
            /* networkType が "NONE" の場合、表示レベルは変化なしの扱いにする */
            //Timber.d("NetworkType is NONE");
            newImageLevel = currentImageLevel;
        }

        if (currentImageLevel == AIRPLANE_MODE) {
            if (_future != null) {
                _future.cancel(true);
                _future = null;
            }
            //機内モード状態の場合、画像を即時更新
            if (!_isAirplaneMode) {
                _isAirplaneMode = true;
                CurrentRadio.setImageLevel(AIRPLANE_MODE);
                LocalBroadcastManager.getInstance(this).sendBroadcast(changeImage);
                Timber.tag("networkImage").i("AirplaneMode");
            }
        } else if (_prevData == null || currentImageLevel < newImageLevel || _isAirplaneMode == true) {
            _isAirplaneMode = false;
            if (_future != null) {
                _future.cancel(true);
                _future = null;
            }
            //表示レベルが上がる場合、画像を即時更新
            CurrentRadio.setImageLevel(newImageLevel);
            LocalBroadcastManager.getInstance(this).sendBroadcast(changeImage);

            if (!currentState.equals(newState)) {
                Timber.tag("networkImage").i(newState);
            }
        } else if (currentImageLevel > newImageLevel) {
            //表示レベルが下がった場合、5秒下がったままなら画像更新
            if (_future == null) {
                Runnable run = () -> {
                    CurrentRadio.setImageLevel(getNetworkAntLevel(_radioData.level, _radioData.networkType, _radioData.rsrp));
                    LocalBroadcastManager.getInstance(this).sendBroadcast(changeImage);

                    if (!currentState.equals(newState)) {
                        Timber.tag("networkImage").i(newState);
                    }
                };
                _future = _executor.schedule(run, 5, TimeUnit.SECONDS);
            }
        } else {
            //表示レベルが変わらないor5秒以内に回復した場合
            if (_future != null) {
                _future.cancel(true);
                _future = null;
            }
        }

        CurrentRadio.setData(_radioData);
        _prevData = _radioData;
        LocalBroadcastManager.getInstance(this).sendBroadcast(changeRadio);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //リスナーの解除
        _tm.unregisterTelephonyCallback(_phoneStateListener);
        _handler.removeCallbacks(_run);

        unregisterReceiver(_screenOn);
        unregisterReceiver(_screenOff);

        //Serviceの終了を通知
        Intent intent = new Intent();
        intent.setAction("SERVICE_STOP");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        Timber.d("Radio Service End");
    }

    private String checkNetworkType() {
        _networkTypeCode = TelephonyManager.NETWORK_TYPE_UNKNOWN;

        Network activeNetwork = _cm.getActiveNetwork();
        if (activeNetwork == null) {
            _networkType = TYPE_NONE;
            return TYPE_NONE;
        }

        NetworkCapabilities capabilities = _cm.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) {
            _networkType = TYPE_NONE;
            return TYPE_NONE;
        }

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                Timber.e("権限エラーREAD_PHONE_STATE");
                _networkType = TYPE_NONE;
                return TYPE_NONE;
            }
            _networkTypeCode = _tm.getDataNetworkType();
            switch (_networkTypeCode) {
                case TelephonyManager.NETWORK_TYPE_LTE:
                    _networkType = TYPE_LTE;
                    return TYPE_LTE;
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_IWLAN:
                case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    _networkType = TYPE_3G;
                    return TYPE_3G;
                default:
                    _networkType = TYPE_OTHER;
                    return TYPE_OTHER;
            }
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            _networkType = TYPE_WIFI;
            return TYPE_WIFI;
        } else {
            _networkType = TYPE_OTHER;
            return TYPE_OTHER;
        }
    }

    @SuppressWarnings("deprecation")
    private void getWifiInfo() {
        _radioData = new RadioData();
        _radioData.networkType = TYPE_WIFI;
        _radioData.networkTypeCode = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        _radioData.date = new Date();

        // 非推奨マークが出るが、代替方法がない & 今のまま動作するため見送り
        int rssi = _wm.getConnectionInfo().getRssi();
        _radioData.signalStrength = rssi;
        _radioData.level = WifiManager.calculateSignalLevel(rssi, 5);

        sendBroadcast();
    }

    private int getNetworkAntLevel(int level, String type, Integer rsrp) {
        switch (type) {
            case "3G":
                if (level == 0 || level == 1) {
                    return RADIO_LEVEL_LOW;
                } else if (level == 2 || level == 3) {
                    return RADIO_LEVEL_MIDDLE;
                } else {
                    return RADIO_LEVEL_HIGH;
                }
            case "LTE":
                if (level == 0) {
                    return RADIO_LEVEL_LOW;
                } else if (level == 1) {
                    if((rsrp != null) && (rsrp <= -120)){
                        // LTEのアンテナレベル1でも電波強度が-120dBm以下の場合は弱として決済させないようにする
                        return RADIO_LEVEL_LOW;
                    } else {
                        return RADIO_LEVEL_MIDDLE;
                    }
                } else {
                    return RADIO_LEVEL_HIGH;
                }
            case "WIFI":
                if (level == 0) {
                    return RADIO_LEVEL_LOW;
                } else if (level == 1 || level == 2) {
                    return RADIO_LEVEL_MIDDLE;
                } else {
                    return RADIO_LEVEL_HIGH;
                }
            default :
                return RADIO_LEVEL_LOW;
        }
    }

    private final BroadcastReceiver _screenOn = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            _handler.post(_run); //スクリーンON 履歴保存を再開
        }
    };

    private final BroadcastReceiver _screenOff = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            _handler.removeCallbacks(_run); //スクリーンOFF 履歴保存を停止
        }
    };
}
