package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data;

import com.google.gson.annotations.Expose;

public class Car {
    public static class Request {
        @Expose
        public Integer carNo;
    }

    public static class Response {
        @Expose
        public Boolean result;

        @Expose
        public String errorCode;
    }
}
