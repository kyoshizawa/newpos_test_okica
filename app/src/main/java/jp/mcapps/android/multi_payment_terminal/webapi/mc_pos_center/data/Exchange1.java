package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data;

import com.google.gson.annotations.Expose;

public class Exchange1 {
    public static class Request {
        @Expose
        public Integer spec;

        @Expose
        public Integer ver;

        @Expose
        public String key1;

        @Expose
        public String sessionKey1;
    }

    public static class Response {
        @Expose
        public Boolean result;

        @Expose
        public String errorCode;

        @Expose
        public String sessionKey1;

        @Expose
        public String sessionKey2;
    }
}
