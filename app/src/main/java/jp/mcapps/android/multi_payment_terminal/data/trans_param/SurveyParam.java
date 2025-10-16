package jp.mcapps.android.multi_payment_terminal.data.trans_param;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.data.CurrentRadio;
import jp.mcapps.android.multi_payment_terminal.database.history.radio.RadioData;

public class SurveyParam {
    public Integer transTime;   //取引処理時間
    public Integer transInputPinTime;   //暗証番号入力時間
    public String termLatitude; //位置情報（緯度）
    public String termLongitude;    //位置情報（経度）
    public String termNetworkType;  //ネットワーク種別
    public Integer termRadioLevel;  //電波状況（レベル）

    public void setAntennaLevel() {
        RadioData data = CurrentRadio.getData();
        if (data != null) {
            this.termNetworkType = data.networkType; //ネットワーク種別
            this.termRadioLevel = data.level;   //電波レベル
        } else {
            //nullの場合は取得できていないとみなす
            this.termNetworkType = "NONE"; //ネットワーク種別
            this.termRadioLevel = 0;   //電波レベル
        }
    }

    public void setLocation() {
        Context context = MainApplication.getInstance();
        if (Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //Android5または位置情報使用の権限がある場合
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                this.termLatitude = String.valueOf(location.getLatitude());  //位置情報（緯度）
                this.termLongitude = String.valueOf(location.getLongitude());    //位置情報（経度）
            }
        }
    }
}
