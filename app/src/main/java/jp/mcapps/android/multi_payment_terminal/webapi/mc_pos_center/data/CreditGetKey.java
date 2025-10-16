package jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data;

import com.google.gson.annotations.Expose;

public class CreditGetKey {
    public static class Request {
        @Expose
        public int keyType;

        @Expose
        public int keyVersion;
    }

    public static class Response {
        @Expose
        public Boolean result;

        @Expose
        public String errorCode;

        @Expose
        public int keytype;

        @Expose
        public int keyVersion;

        @Expose
        public String keyData;
    }
}
