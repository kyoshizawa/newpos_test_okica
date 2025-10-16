package jp.mcapps.android.multi_payment_terminal.webapi.gmo.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class GetOrders {
    public static class Request {
        @Expose
        public String date;

        @Expose
        public String dateFrom;

        @Expose
        public String dateTo;

        @Expose
        public String status;

        @Expose
        public String channel;

        @Expose
        public String limit;

        public Map<String, String> toMap() {
            final HashMap<String, String> map = new HashMap<>();
            map.put("date", date);
            map.put("dateFrom", dateFrom);
            map.put("dateTo", dateTo);
            map.put("status", status);
            map.put("channel", channel);
            map.put("limit", limit);

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
        @SerializedName("return_code")
        public String returnCode;

        @Expose
        public String orderDate;

        @Expose
        public Data data;
    }

    public static class Data {
        @Expose
        @SerializedName("record_type")
        public String recordType;

        @Expose
        public String status;

        @Expose
        @SerializedName("partner_order_id")
        public String partnerOrderId;

        @Expose
        public String currency;

        @Expose
        @SerializedName("create_time")
        public String createTime;

        @Expose
        public String channel;

        @Expose
        @SerializedName("real_fee")
        public String realFee;

        @Expose
        @SerializedName("sum_refund_fee")
        public String sumRefundFee;

        @Expose
        @SerializedName("pay_time")
        public String payTime;

        @Expose
        @SerializedName("total_fee")
        public String totalFee;

        @Expose
        @SerializedName("order_body")
        public String orderBody;

        @Expose
        @SerializedName("order_id")
        public String orderId;

        @Expose
        @SerializedName("partner_refund_id")
        public String partnerRefundId;

        @Expose
        @SerializedName("refund_id")
        public String refundId;

        @Expose
        @SerializedName("refund_fee")
        public String refundFee;

        @Expose
        public String wallet;
    }
}
