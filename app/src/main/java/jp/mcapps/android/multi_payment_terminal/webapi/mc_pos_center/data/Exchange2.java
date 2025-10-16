package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data;

import com.google.gson.annotations.Expose;

public class Exchange2 {
    public static class Request {
        @Expose
        public Integer spec;

        @Expose
        public Integer ver;

        @Expose
        public String sessionKey2;
    }

    public static class Response {
        @Expose
        public Boolean result;

        @Expose
        public String errorCode;

        @Expose
        public Integer forward;
    }
}
