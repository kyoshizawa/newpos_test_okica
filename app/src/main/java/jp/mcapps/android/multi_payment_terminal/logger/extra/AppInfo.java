package jp.mcapps.android.multi_payment_terminal.logger.extra;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import jp.mcapps.android.multi_payment_terminal.BuildConfig;

public class AppInfo {
    @Expose
    public String stage = BuildConfig.BUILD_TYPE;
    @SerializedName("package_name")
    @Expose
    public String packageName = BuildConfig.APPLICATION_ID;
    @SerializedName("version_code")
    @Expose
    public int versionCode = BuildConfig.VERSION_CODE;
    @SerializedName("version_name")
    @Expose
    public String versionName = BuildConfig.VERSION_NAME;
}
