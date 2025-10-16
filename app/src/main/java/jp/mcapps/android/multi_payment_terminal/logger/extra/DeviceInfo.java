package jp.mcapps.android.multi_payment_terminal.logger.extra;

import android.os.Build;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DeviceInfo {
    @Expose
    public String id;
    @Expose
    public String manufacturer;
    @Expose
    public String model;
    @Expose
    public String platform;
    @SerializedName("sdk_int")
    @Expose
    public int sdkInt;
    @Expose
    public String version;

    public DeviceInfo(String id) {
        this.id = id;
        manufacturer = Build.MANUFACTURER;
        model = Build.MODEL;
        platform = "android";
        sdkInt = Build.VERSION.SDK_INT;
        version = Build.VERSION.RELEASE;
    }
}
