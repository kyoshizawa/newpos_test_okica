package jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Version {
    public static class Response {
        @SerializedName("app_name")
        public String appName;

        @SerializedName("app_model")
        @Expose
        public String appModel;

        @SerializedName("app_version")
        @Expose
        public String appVersion;

        @SerializedName("mc_serial")
        @Expose
        public String mcSerial;

        @SerializedName("mc_version")
        @Expose
        public String mcVersion;

        @Override
        public String toString() {
            return "Response{" +
                    "appName='" + appName + '\'' +
                    ", appModel='" + appModel + '\'' +
                    ", appVersion='" + appVersion + '\'' +
                    ", mcSerial='" + mcSerial + '\'' +
                    ", mcVersion='" + mcVersion + '\'' +
                    '}';
        }
    }
}
