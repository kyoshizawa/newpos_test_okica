package jp.mcapps.android.multi_payment_terminal.webapi.tablet.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SignedIn {
    public static class Response {
        @SerializedName("id")
        @Expose
        public String id;

        @SerializedName("office_id")
        @Expose
        public String officeId;

        @SerializedName("code")
        @Expose
        public String code;

        @SerializedName("name")
        @Expose
        public String name;
    }
}
