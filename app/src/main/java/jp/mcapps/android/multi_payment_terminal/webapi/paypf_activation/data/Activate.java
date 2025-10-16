package jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.data;

import com.google.gson.annotations.Expose;

public class Activate {
    public static class Request {
        @Expose
        public String model_code;

        @Expose
        public String serial_no;

        @Expose
        public String unit_id;

        @Expose
        public Boolean use_payment;

        @Expose
        public String tid;

        @Expose
        public String customer_code;
    }

    public static class Response {

        @Expose
        public int code;

        @Expose
        public String message;

    }
}
