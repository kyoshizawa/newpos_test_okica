package jp.mcapps.android.multi_payment_terminal.webapi.jrem_ras.data;

import com.google.gson.annotations.Expose;

public class OpeningSuica {
    public static class Request {
        @Expose
        public Integer businessId = 145;
    }

    public static class Response extends OpeningInfoSuica {
    }
}
