package jp.mcapps.android.multi_payment_terminal;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.journeyapps.barcodescanner.CaptureManager;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketGateSettingsData;
import jp.mcapps.android.multi_payment_terminal.service.PeriodicGateCheckService;
import jp.mcapps.android.multi_payment_terminal.ui.pin.PinInputViewModel;
import jp.mcapps.android.multi_payment_terminal.ui.ticket.TicketGateRoute;
import timber.log.Timber;

public class TicketScannerViewModel extends ViewModel {

    public TicketScannerViewModel() { super(); }

    private final TicketGateSettingsDao _ticketGateSettingsDao = DBManager.getTicketGateSettingsDao();

    private List<TicketGateRoute> _ticketGateRouteList = null;

//    private CaptureManager _capture;

    private Activity _activity;

    private LocalBroadcastManager _lbm;

    private void insertTicketGateSettingsData(TicketGateSettingsData data) {
        _ticketGateSettingsDao.insertTicketGateSettingsData(data);
    }
    private TicketGateSettingsData getTicketGateSettingsData() {
        return _ticketGateSettingsDao.getTicketGateSettingsLatest();
    }

    private MutableLiveData<Boolean> _useLight = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> useLight() {
        return _useLight;
    }
    public void useLight(boolean b) {
        _useLight.setValue(b);
    }

//    public void setCapture(CaptureManager cm) { _capture = cm; }

    public void setTicketGateRouteList(List<TicketGateRoute> data) {
        _ticketGateRouteList = data;
    }

    public void registerReceiver(Activity activity) {
        IntentFilter intentFilter = new IntentFilter("TICKET_GATE_CHECK");
        _activity = activity;
        _lbm = LocalBroadcastManager.getInstance(activity);
        _lbm.registerReceiver(_gateCheckReceiver, intentFilter);
    }

    public void unregisterReceiver() {
        _lbm.unregisterReceiver(_gateCheckReceiver);
    }

    public void fetch() {

        Observable.fromCallable(() -> {
                    return getTicketGateSettingsData();
                })
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            Timber.d("on fetch data");
                            // 自動更新サービスが停止していたら開始
                            boolean foundGateCheckService = false;
                            ActivityManager am = (ActivityManager) _activity.getSystemService(Context.ACTIVITY_SERVICE);
                            List<ActivityManager.RunningServiceInfo> serviceInfoList = am.getRunningServices(Integer.MAX_VALUE);
                            for (ActivityManager.RunningServiceInfo runningServiceInfo : serviceInfoList) {
                                //サービスが起動しているかを確認
                                if (runningServiceInfo.service.getClassName().equals(PeriodicGateCheckService.class.getName())) {
                                    foundGateCheckService = true;
                                    break;
                                }
                            }
                            if (!foundGateCheckService) {
                                /* 改札設定画面から遷移 */
                                Intent periodicService = new Intent(MainApplication.getInstance().getApplicationContext(), PeriodicGateCheckService.class);
                                periodicService.putExtra("ticketRouteList", (Serializable) _ticketGateRouteList);
                                final MainApplication app = MainApplication.getInstance();
                                app.startService(periodicService);
                                Timber.i("便自動検札開始");
                                setTicketGateSettingsDataInit(result);

//                                // QRリーダー停止
//                                _capture.onPause();
                            } else {
                                /* QRかざし結果画面からの遷移 */
                                if (null != AppPreference.getNextTripGateCheckStartTime()) {
                                    // 手続き開始待機状態時にQR判定結果画面から戻った際、QR読取開始画面にならないように修正対応
                                    String time = AppPreference.getNextTripGateCheckStartTime();
                                    setTicketGateSettingsData(AppPreference.getTicketGateSettingsData());

                                    LinearLayout layout = _activity.findViewById(R.id.ticket_gate_layout);
                                    layout.setBackgroundColor(0xFF000000);
                                    // メッセージ変更
                                    String msg = _activity.getResources().getString(R.string.text_attention_1) + time + _activity.getResources().getString(R.string.text_attention_2);
                                    String msgEn = _activity.getResources().getString(R.string.text_attention_en) + " " + time;
                                    setMessage(msg);
                                    setMessageEnglish(msgEn);
                                    Timber.i("QR読込停止:\n" + msg + "\n" + msgEn);
                                } else {
                                    setTicketGateSettingsData(result);
                                    // メッセージ変更
                                    setMessage(_activity.getResources().getString(R.string.text_Information));
                                    setMessageEnglish(_activity.getResources().getString(R.string.text_Information_en));
                                }
                            }


                        },
                        error -> {
                            // エラーハンドリングの処理
                            Timber.e(error);
                        }
                );
    }

    public void setTicketGateSettingsDataInit(TicketGateSettingsData SettingsData) {
        setTicketGateSettingsData(SettingsData);

        // メッセージ変更
        String msg = _activity.getResources().getString(R.string.text_Init);
        String msgEn = _activity.getResources().getString(R.string.text_Init_en);
        setMessage(msg);
        setMessageEnglish(msgEn);
    }

    public void setTicketGateSettingsData(TicketGateSettingsData SettingsData) {
        TicketGateSettingsData ticketGateSettingsDao = SettingsData;
        setLocationName(ticketGateSettingsDao.stop_name);
        setTicketEmbarkName(ticketGateSettingsDao.start_stop_name);
        setTicketEmbarkTime(ticketGateSettingsDao.start_departure_time);
        setTicketDisembarkName(ticketGateSettingsDao.end_stop_name);
        setTicketDisembarkTime(ticketGateSettingsDao.end_arrival_time);
        setAutoUpdate(ticketGateSettingsDao.auto_gate_check);
        setLastAutoTime(new Date());
    }

    // 設置場所(名称)
    private final MutableLiveData<String> _locationName = new MutableLiveData<String>("");
    public MutableLiveData<String> getLocationName() {
        return _locationName;
    }
    public void setLocationName(String value) {
        if (value == null) return;
        String LocationName = "【" + value + "】";
        _locationName.setValue(LocationName);
    }

    // のりば(名称)
    private final MutableLiveData<String> _ticketEmbarkName = new MutableLiveData<String>("");
    public MutableLiveData<String> getTicketEmbarkName() { return _ticketEmbarkName; }
    public void setTicketEmbarkName(String value) {
        if (value == null) return;
        _ticketEmbarkName.setValue(value);
    }

    // のりば(出発時刻)
    private final MutableLiveData<String> _ticketEmbarkTime = new MutableLiveData<String>("");
    public MutableLiveData<String> getTicketEmbarkTime() { return _ticketEmbarkTime; }
    public void setTicketEmbarkTime(String value) {
        if (value == null || value.length() < 4) return;
        String time = value.substring(0, 2) + ":" + value.substring(3, 5) + " 発";
        _ticketEmbarkTime.setValue(time);
    }

    // おりば(名称)
    private final MutableLiveData<String> _ticketDisembarkName = new MutableLiveData<String>("");
    public MutableLiveData<String> getTicketDisembarkName() { return _ticketDisembarkName; }
    public void setTicketDisembarkName(String value) {
        if (value == null) return;
        _ticketDisembarkName.setValue(value);
    }

    // おりば(到着時刻)
    private final MutableLiveData<String> _ticketDisembarkTime = new MutableLiveData<String>("");
    public MutableLiveData<String> getTicketDisembarkTime() { return _ticketDisembarkTime; }
    public void setTicketDisembarkTime(String value) {
        if (value == null || value.length() < 4) return;
        String time = value.substring(0, 2) + ":" + value.substring(3, 5) + " 着";
        _ticketDisembarkTime.setValue(time);
    }

    // 更新種別(アイコン)
    private final MutableLiveData<Boolean> isAutoUpdate = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isAutoUpdate() {
        return isAutoUpdate;
    }
    public void setAutoUpdate(boolean b) {
        isAutoUpdate.setValue(b);
    }

    // 最終更新(時刻)
    private final MutableLiveData<String> _lastAutoTime = new MutableLiveData<String>("");
    public MutableLiveData<String> getLastAutoTime() { return _lastAutoTime; }
    public void setLastAutoTime(Date value) {
        if (value == null) return;
        SimpleDateFormat dateFmt = new SimpleDateFormat("HH:mm", Locale.JAPANESE);
        String time = dateFmt.format(value);
        _lastAutoTime.setValue(time);
    }

    // 検札メッセージ
    private final MutableLiveData<String> _message = new MutableLiveData<String>("");
    public MutableLiveData<String> getMessage() { return _message; }
    public void setMessage(String msg) {
        if (msg == null) return;
        _message.setValue(msg);
    }

    // 検札メッセージ(英語)
    private final MutableLiveData<String> _messageEnglish = new MutableLiveData<String>("");
    public MutableLiveData<String> getMessageEnglish() { return _messageEnglish; }
    public void setMessageEnglish(String msg) {
        if (msg == null) return;
        _messageEnglish.setValue(msg);
    }

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

    private final BroadcastReceiver _gateCheckReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            TicketGateSettingsData data = (TicketGateSettingsData)bundle.getSerializable("settingData");
            String nextTripGateCheckStartTime = bundle.getString("nextTripGateCheckStartTime");

            Timber.i("便自動更新(%s):\n%s %s～%s %s～%s\n(経路:%s 便ID:%s 検札開始時刻:%s)", data.auto_gate_check, data.stop_name, data.start_stop_name, data.end_stop_name, data.start_departure_time, data.end_arrival_time, data.route_name, data.trip_id, nextTripGateCheckStartTime);
            setTicketGateSettingsData(data);

            if (data.trip_id.equals("")) {
                // 便がない場合カメラ停止
                LinearLayout layout = _activity.findViewById(R.id.ticket_gate_layout);
//                layout.setAlpha(1.0f);
                layout.setBackgroundColor(0xFF000000);
                // メッセージ変更
                String msg = _activity.getResources().getString(R.string.text_attention_1) + nextTripGateCheckStartTime + _activity.getResources().getString(R.string.text_attention_2);
                String msgEn = _activity.getResources().getString(R.string.text_attention_en) + " " + nextTripGateCheckStartTime;
                setMessage(msg);
                setMessageEnglish(msgEn);
                Timber.i("QR読込停止:\n" + msg + "\n" + msgEn);
//                // QRリーダー停止
//                _capture.onPause();
                AppPreference.setNextTripGateCheckStartTime(nextTripGateCheckStartTime);
                AppPreference.setTicketGateSettingsData(data);
            } else {
                // 便がある場合はカメラ開始
                LinearLayout layout = _activity.findViewById(R.id.ticket_gate_layout);
//                layout.setAlpha(0.35f);
                layout.setBackgroundColor(0x0);

                Observable.fromCallable(() -> {
                            try {
                                TicketGateSettingsData settings = new TicketGateSettingsData();

                                settings.stop_id = data.stop_id;
                                settings.stop_name = data.stop_name;
                                settings.stop_type = data.stop_type;
                                settings.gate_check_start_time = data.gate_check_start_time;
                                settings.auto_gate_check = data.auto_gate_check;
                                settings.auto_gate_check_interval = data.auto_gate_check_interval;
                                settings.route_id = data.route_id;
                                settings.route_name = data.route_name;
                                settings.trip_id = data.trip_id;
                                settings.start_stop_name = data.start_stop_name;
                                settings.start_departure_time = data.start_departure_time;
                                settings.end_stop_name = data.end_stop_name;
                                settings.end_arrival_time = data.end_arrival_time;
                                settings.created_at = new Date();

                                insertTicketGateSettingsData(settings);
                                return true;
                            } catch (Exception e) {
                                Timber.e(e, "TicketGateSettingsData insert error");
                                return false;
                            }
                        })
                        .subscribeOn(Schedulers.single())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                result -> {
                                    Timber.d("TicketGateSettingsData insert success");
                                },
                                error -> {
                                    // エラーハンドリングの処理
                                }
                        );

                // メッセージ変更
                String msg = _activity.getResources().getString(R.string.text_Information);
                String msgEn = _activity.getResources().getString(R.string.text_Information_en);
                setMessage(msg);
                setMessageEnglish(msgEn);
                Timber.i("QR読込開始:\n" + msg + "\n" + msgEn);
//                // QRリーダー開始
//                _capture.onResume();
                AppPreference.setNextTripGateCheckStartTime(null);
                AppPreference.setTicketGateSettingsData(null);
             }

        }
    };
}
