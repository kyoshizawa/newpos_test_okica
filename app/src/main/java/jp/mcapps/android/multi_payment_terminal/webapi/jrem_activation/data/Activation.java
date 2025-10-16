package jp.mcapps.android.multi_payment_terminal.webapi.jrem_activation.data;

import com.google.gson.annotations.Expose;

public class Activation {
    public static class Request {
        @Expose
        public String uniqueId;
    }

    public static class Response {
        @Expose
        public String resultCode;
        @Expose
        public ErrorObject errorObject;
    }

    public static class ErrorObject {
        @Expose
        public String errorMessage;
        @Expose
        public String printBugReport;
        @Expose
        public String errorType;
        @Expose
        public String errorCode;
        @Expose
        public String errorLevel;
    }
}
