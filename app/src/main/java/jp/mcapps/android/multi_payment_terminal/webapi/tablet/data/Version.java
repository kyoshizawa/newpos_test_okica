package jp.mcapps.android.multi_payment_terminal.webapi.tablet.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Version {
    public static class Response {
        @SerializedName("application_id")
        @Expose
        public String applicationId;

        @SerializedName("version_name")
        @Expose
        public String versionName;

        @SerializedName("version_code")
        @Expose
        public Integer versionCode;

        @SerializedName("device_id")
        @Expose
        public String deviceId;
    }
}
