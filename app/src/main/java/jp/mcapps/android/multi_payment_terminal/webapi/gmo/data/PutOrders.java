package jp.mcapps.android.multi_payment_terminal.webapi.gmo.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PutOrders {
    public static class Request {
        @Expose
        @SerializedName("order_id")
        public String orderId;

        @Expose
        public String serialNo;

        @Expose
        public String description;

        @Expose
        public String price;

        @Expose
        @SerializedName("auth_code")
        public String authCode;

        @Expose
        public String currency;

        @Expose
        public String operator;
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

        @Expose
        public String balanceAmount;
    }

    public static class Result {
        @Expose
        @SerializedName("partner_order_id")
        public String partnerOrderId;

        @Expose
        public String currency;

        @Expose
        @SerializedName("order_id")
        public String orderId;

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
        @SerializedName("real_fee")
        public String realFee;

        @Expose
        public String channel;

        @Expose
        @SerializedName("pay_time")
        public String payTime;

        @Expose
        @SerializedName("order_body")
        public String orderBody;

        @Expose
        public String wallet;
    }
}
