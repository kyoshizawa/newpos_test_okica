package jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Ems {
    public static class Response {
        @SerializedName("status")
        @Expose
        public String status;

        @SerializedName("sampled_at")
        @Expose
        public Integer sampledAt;
    }
}
