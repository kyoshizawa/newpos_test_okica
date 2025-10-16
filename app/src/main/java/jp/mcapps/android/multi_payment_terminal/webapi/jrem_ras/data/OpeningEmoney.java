package jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;

public class OpeningEmoney {
    public static class Request {
        @Expose
        public Integer businessId = 245;
    }

    public static class Response {
        @Expose
        public Boolean result;

        @Expose
        public ArrayList<Object> money;
    }
}
