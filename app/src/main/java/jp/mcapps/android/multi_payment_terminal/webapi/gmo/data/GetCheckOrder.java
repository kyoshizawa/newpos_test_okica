package jp.mcapps.android.multi_payment_terminal.webapi.gmo.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class GetCheckOrder {
    public static class Request {
        @Expose
        public String storeOrderId;

        public Map<String, String> toMap() {
            final HashMap<String, String> map = new HashMap<>();
            map.put("storeOrderId", storeOrderId);

            return map;
        }
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
        @SerializedName("real_fee")
        public String realFee;

        @Expose
        public String channel;

        @Expose
        @SerializedName("create_time")
        public String createTime;

        @Expose
        @SerializedName("total_fee")
        public String totalFee;

        @Expose
        @SerializedName("pay_time")
        public String payTime;

        @Expose
        @SerializedName("refund_fee")
        public String refundFee;

        @Expose
        @SerializedName("order_body")
        public String orderBody;

        @Expose
        public String status;

        @Expose
        public String partialRefundFlag;

        @Expose
        public String wallet;
    }
}
