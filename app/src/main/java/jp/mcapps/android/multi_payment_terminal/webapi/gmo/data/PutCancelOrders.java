package jp.mcapps.android.multi_payment_terminal.webapi.gmo.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PutCancelOrders {
    public static class Request {
        @Expose
        @SerializedName("order_id")
        public String orderId;

        @Expose
        public String serialNo;

        @Expose
        public String fee;
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
        @SerializedName("partner_order_id")
        public String partnerOrderId;

        @Expose
        public String currency;

        @Expose
        @SerializedName("return_code")
        public String returnCode;

        @Expose
        @SerializedName("result_code")
        public String resultCode;

        @Expose
        public String amount;

        @Expose
        public String channel;

        @Expose
        @SerializedName("order_id")
        public String orderId;

        @Expose
        @SerializedName("create_time")
        public String createTime;

        @Expose
        @SerializedName("total_fee")
        public String totalFee;

        @Expose
        public String wallet;
    }
}
