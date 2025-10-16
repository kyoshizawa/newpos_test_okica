package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data;

import com.google.gson.annotations.Expose;

public class GetKey {
    public static class Request {
    }

    public static class Response {
        @Expose
        public Boolean result;

        @Expose
        public String errorCode;

        @Expose
        public Integer spec;

        @Expose
        public Integer ver;

        @Expose
        public String data;
    }
}
