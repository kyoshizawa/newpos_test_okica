package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data;

import com.google.gson.annotations.Expose;

public class Echo {
    public static class Request {
        @Expose
        public Boolean detachJR;

        @Expose
        public Boolean detachQR;
    }

    public static class Response {
        @Expose
        public Boolean result;

        @Expose
        public String errorCode;

        @Expose
        public Boolean useable;
    }
}
