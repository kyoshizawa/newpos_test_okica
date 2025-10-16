package jp.mcapps.android.multi_payment_terminal.logger;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.kinesis.kinesisrecorder.KinesisFirehoseRecorder;
import com.amazonaws.mobileconnectors.kinesis.kinesisrecorder.KinesisRecorderConfig;
import com.amazonaws.regions.Regions;
import com.amplifyframework.auth.AuthUser;
import com.amplifyframework.core.Amplify;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.logger.extra.DeviceInfo;
import jp.mcapps.android.multi_payment_terminal.logger.extra.SimInfo;
import jp.mcapps.android.multi_payment_terminal.logger.extra.SourceInfo;
import jp.mcapps.android.multi_payment_terminal.util.DeviceUtils;
import jp.mcapps.android.multi_payment_terminal.util.SimUtils;
import timber.log.Timber;

public class EventLogger {
    private final KinesisFirehoseRecorder _recorder;
    //private static final String PRODUCT_NAME = "multi_payment_terminal";
    private static final String PRODUCT_NAME = BuildConfig.APPLICATION_ID;
    private static final String STREAM_REGION = BuildConfig.LOGGINNG_STREAM_REGION;
    private static final String STREAM_NAME = BuildConfig.LOGGING_STREAM_NAME;
    private static final long STORAGE_SIZE_MAX = 1024 * 1024 * 100L;
    private static final String TAG_APP_START = "c_app_start";
    private static final String TAG_APP_CRASH = "c_app_crash";

    public EventLogger() {
        Context context = MainApplication.getInstance();
        File dir = context.getCacheDir();
        Regions regions = Regions.fromName(STREAM_REGION);
        AWSMobileClient client = AWSMobileClient.getInstance();
        KinesisRecorderConfig config = new KinesisRecorderConfig().withMaxStorageSize(STORAGE_SIZE_MAX);
        _recorder = new KinesisFirehoseRecorder(dir, regions, client, config);
    }

    public void log(EventLog.Level level, String tag, String message, Throwable t) {
        EventLog eventLog = new EventLog(
                getTimestamp(),
                level,
                tag,
                message,
                getLocation(),
                getSourceInfo()
        );
        saveEvent(eventLog);
    }

    public void appStart() {
        EventLog eventLog = new EventLog(
                getTimestamp(),
                EventLog.Level.INFO,
                TAG_APP_START,
                "START",
                getLocation(),
                getDeviceInfo(),
                getSimInfo(),
                getSourceInfo()
        );
        saveEvent(eventLog);
    }

    public void appCrash(String message) {
        EventLog eventLog = new EventLog(
                getTimestamp(),
                EventLog.Level.FATAL,
                TAG_APP_CRASH,
                message,
                getLocation(),
                getDeviceInfo(),
                getSimInfo(),
                getSourceInfo()
        );
        saveEvent(eventLog);
    }

    //ログ送信　UIスレッドからの呼び出し不可
    public void submit() {
        try {
            _recorder.submitAllRecords();
            Log.d(this.getClass().getSimpleName(), "submit success");
        } catch (Exception e) {
            Log.w(this.getClass().getSimpleName(), e);
        }
    }

    private void saveEvent(EventLog log) {
        final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        String str = gson.toJson(log) + "\n";
        _recorder.saveRecord(str, STREAM_NAME);
    }

    private String getTimestamp() {
        final TimeZone timeZone = TimeZone.getTimeZone("UTC");
        final SimpleDateFormat format =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        format.setTimeZone(timeZone);
        Calendar calendar = Calendar.getInstance(timeZone);
        return format.format(calendar.getTime());
    }

    private String getLocation() {
        Context context = MainApplication.getInstance();
        if (Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //Android5または位置情報使用の権限がある場合
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                return String.format(Locale.JAPANESE, "%f,%f", location.getLatitude(), location.getLongitude());
            }
        }
        return null;
    }

    @SuppressLint("HardwareIds")
    private SourceInfo getSourceInfo() {
        String userId = null;
        String userName = AppPreference.getOrganizationId();
        try {
            AuthUser currentUser = Amplify.Auth.getCurrentUser();
            if (currentUser != null) {
                userId = currentUser.getUserId();
                //userName = currentUser.getUsername();
            }
        } catch (Throwable t) {
            //ログは出せない　無視する
        }

        SourceInfo source = new SourceInfo();
        source.productName = PRODUCT_NAME;
        source.userId = userId;
        source.userName = userName;
        source.deviceId = AppPreference.getMcTermId();
        source.carId = String.valueOf(AppPreference.getMcCarId());
        source.driverId = String.valueOf(AppPreference.getMcDriverId());
        source.serial = DeviceUtils.getSerial();
        source.organizationId = AppPreference.getOrganizationId();
        source.orgId = source.organizationId;

        return source;
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    private SimInfo getSimInfo() {
        TelephonyManager tm = (TelephonyManager) MainApplication.getInstance().getSystemService(Context.TELEPHONY_SERVICE);
        SimInfo sim = new SimInfo();
        sim.country = tm.getSimCountryIso();
        sim.operatorName = tm.getSimOperatorName();

        if (Build.VERSION.SDK_INT >= 23 && ActivityCompat.checkSelfPermission(MainApplication.getInstance(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // OS7での初回起動時などで権限がない場合はnullで送信
            // 初回起動時はSIM未挿入のためどちらにしろIccIDは取れない
            // MainActivityのonCreateで確認ダイアログが表示され、権限が付与される想定
            Timber.e("権限エラーREAD_PHONE_STATE");
        } else {
            // OS5 or 権限有の場合  SIM未挿入等で取れない場合は空文字
            // Android 13 で取得できなくなっている（アプリが落ちる）ため処理変更
            sim.iccId = SimUtils.getIccId(MainApplication.getInstance());
        }

        return sim;
    }

    private DeviceInfo getDeviceInfo() {
        @SuppressLint("HardwareIds")
        String id = Settings.Secure.getString(MainApplication.getInstance().getContentResolver(), Settings.Secure.ANDROID_ID);
        return new DeviceInfo(id);
    }
}
