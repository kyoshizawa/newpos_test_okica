package jp.mcapps.android.multi_payment_terminal.webapi.gmo.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class GetCheckRefunds {
    public static class Request {
        public String storeRefundId;

        public String storeOrderId;

        public Map<String, String> toMap() {
            final HashMap<String, String> map = new HashMap<>();
            map.put("storeRefundId", storeRefundId);
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
        @SerializedName("partner_refund_id")
        public String partnerRefundId;

        @Expose
        @SerializedName("refund_id")
        public String refundId;

        @Expose
        public String currency;

        @Expose
        @SerializedName("return_code")
        public String returnCode;

        @Expose
        @SerializedName("result_code")
        public String resultCode;

        @Expose
        @SerializedName("partner_order_id")
        public String partnerOrderId;

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
        @SerializedName("pay_time")
        public String payTime;

        @Expose
        @SerializedName("total_fee")
        public String totalFee;

        @Expose
        @SerializedName("real_fee")
        public String realFee;

        @Expose
        public String wallet;
    }
}
