package jp.mcapps.android.multi_payment_terminal.webapi.paypf_activation.data;

import com.google.gson.annotations.Expose;



public class GetToken {
    public static class Request {
        @Expose
        public String model_code;

        @Expose
        public String serial_no;

        @Expose
        public Boolean use_in_test;
    }

    public static class Response {

        @Expose
        public String terminal_id;

        @Expose
        public Integer expires_in;

        @Expose
        public String token_type;

        @Expose
        public String access_token;

        @Expose
        public String refresh_token;

    }
}
