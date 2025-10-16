package jp.mcapps.android.multi_payment_terminal.webapi.gmo.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PutCreateQRCode {
    public static class Request {
        @Expose
        @SerializedName("order_reservation_id")
        public String orderReservationId;

        @Expose
        public String serialNo;

        @Expose
        public String description;

        @Expose
        public String price;

        @Expose
        public String currency;

        @Expose
        public String operator;

        @Expose
        public String channel;

        @Expose
        @SerializedName("pay_individual_area")
        public String payIndividualArea;
    }

    public static class Response {
        @Expose
        public String returnCode;

        @Expose
        public String returnMessage;

        @Expose
        public String msgSummaryCode;

        @Expose
        public String msgSummary;

        @Expose
        public Result result;
    }

    public static class Result {
        @Expose
        @SerializedName("partner_order_reservation_id")
        public String partnerOrderReservationId;

        @Expose
        public String currency;

        @Expose
        @SerializedName("order_reservation_id")
        public String orderReservationId;

        @Expose
        @SerializedName("return_code")
        public String returnCode;

        @Expose
        @SerializedName("result_code")
        public String resultCode;

        @Expose
        @SerializedName("create_time")
        public String createTime;

        @Expose
        @SerializedName("total_fee")
        public String totalFee;

        @Expose
        public String channel;

        @Expose
        @SerializedName("order_body")
        public String orderBody;

        @Expose
        @SerializedName("pay_individual_area")
        public String payIndividualArea;
    }
}
