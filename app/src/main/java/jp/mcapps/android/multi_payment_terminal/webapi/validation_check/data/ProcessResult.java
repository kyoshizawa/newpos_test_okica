package jp.mcapps.android.multi_payment_terminal.webapi.validation_check.data;

import com.google.gson.annotations.Expose;

public class ProcessResult {
    public static class Request {
        @Expose
        public RequestRecord[] records;
    }

    public static class Response {
        @Expose
        public Boolean result;

        @Expose
        public String errorCode;

        @Expose
        public ResponseRecord[] records;

    }

    public static class RequestRecord {
        @Expose
        public Integer recordType;

        @Expose
        public String terminalNo;

        @Expose
        public Integer carNo;

        @Expose
        public Integer driverCd;

        @Expose
        public String driverName;

        @Expose
        public Integer procNo;

        @Expose
        public String operationTime;

        @Expose
        public String validationID;

        @Expose
        public Boolean terminalResult;

        @Expose
        public String reason;

        @Expose
        public Integer fare;
    }

    public static class ResponseRecord {
        @Expose
        public Boolean tranResult;

        @Expose
        public String terminalNo;

        @Expose
        public Integer procNo;

        @Expose
        public String operationTime;
    }
}
