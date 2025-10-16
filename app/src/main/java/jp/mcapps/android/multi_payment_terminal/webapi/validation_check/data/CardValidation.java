package jp.mcapps.android.multi_payment_terminal.webapi.validation_check.data;

import com.google.gson.annotations.Expose;

import androidx.room.ColumnInfo;

public class CardValidation {
    public static class Request {
        @Expose
        public String terminalNo;

        @Expose
        public Integer carNo;

        @Expose
        public Integer driverCd;

        @Expose
        public Integer procNo;

        @Expose
        public String operationTime;

        @Expose
        public Integer mediaType;

        @Expose
        public MediaInfo mediaInfo;

        /* APIのリクエストには含めないがDBに保存するデータ */
        public String driverName;

        public Integer fare;

        public String termLatitude; //位置情報(緯度)

        public String termLongitude; //位置情報(経度)

        public String termNetworkType; //ネットワーク種別

        public Integer termRadioLevel; //電波状況(レベル)
    }

    public static class Response {
        @Expose
        public Boolean result;

        @Expose
        public String errorCode;


        @Expose
        public String validationID;

        @Expose
        public Integer procNo;

        @Expose
        public Integer mediaResultType;

        @Expose
        public MediaResultInfo mediaResultInfo;

    }

    public static class MediaInfo {
        @Expose
        public Integer encryptType;

        @Expose
        public String encryptTrack1;

        @Expose
        public String encryptTrack2;

        @Expose
        public String encryptTrack3;
    }

    public static class MediaResultInfo {
        @Expose
        public Boolean isValid;

        @Expose
        public String reason;

        @Expose
        public Integer encryptType;

        @Expose
        public String encryptCardNo;
    }
}
