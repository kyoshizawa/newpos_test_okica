package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data;

import com.google.gson.annotations.Expose;

public class Driver {
    public static class Request {
        @Expose
        public Integer driverCd;

        @Expose
        public boolean update;

        @Expose
        public String driverName;
    }

    public static class Response {
        @Expose
        public Boolean result;

        @Expose
        public String errorCode;

        @Expose
        public String driverName;
    }
}
