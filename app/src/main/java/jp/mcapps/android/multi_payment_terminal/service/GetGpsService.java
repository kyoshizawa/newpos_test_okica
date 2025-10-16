package jp.mcapps.android.multi_payment_terminal.service;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Date;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.history.gps.GpsDao;
import jp.mcapps.android.multi_payment_terminal.database.history.gps.GpsData;
import timber.log.Timber;

public class GetGpsService extends Service implements LocationListener{
    private LocationManager _lm;
    private Thread _thread;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        //時間の更新間隔 デフォルト3分
        int timeInterval = intent != null ? intent.getIntExtra("TIME_INTERVAL", 180) * 1000 : 1000 * 60 * 3;
        //距離の更新間隔 デフォルト100m
        int distanceInterval = intent != null ? intent.getIntExtra("DISTANCE_INTERVAL", 100) : 100;

        _lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Timber.e("権限エラーACCESS_FINE_LOCATION");
        } else {
            _lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    timeInterval,
                    distanceInterval,
                    this
            );
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendBroadcast(GpsData gpsData) {
        Intent intent = new Intent();
        intent.setAction("SEND_GPS_DATA");
        intent.putExtra("gps", gpsData);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //リスナーの解除
        _lm.removeUpdates(this);

        //スレッドの状態を確認
        if (_thread != null) {
            try {
                //スレッドが終了するまで待機
                _thread.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Serviceの終了を通知
        Intent intent = new Intent();
        intent.setAction("SERVICE_STOP");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        Timber.d("Gps Service End");
    }

    @Override
    public void onLocationChanged(Location location) {
        _thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Date date = new Date();
                GpsData gpsData = new GpsData();

                gpsData.latitude = location.getLatitude();
                gpsData.longitude = location.getLongitude();
                gpsData.accuracy = location.getAccuracy();
                gpsData.altitude = location.getAltitude();
                gpsData.bearing = location.getBearing();
                gpsData.speed = location.getSpeed();
                gpsData.satellites = location.getExtras().getInt("satellites");
                gpsData.gpsDate = new Date(location.getTime());
                gpsData.elapsedRealTime = location.getElapsedRealtimeNanos();
                gpsData.date = date;

                if (AppPreference.isGpslogEnabled()) {
                    try {
                        GpsDao dao = LocalDatabase.getInstance().gpsDao();
                        dao.insertGpsData(gpsData);
                        sendBroadcast(gpsData);
                    } catch (Exception e) {
                        Timber.d("GPS履歴保存エラー");
                    }
                }
            }
        });
        _thread.start();
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}