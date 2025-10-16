package jp.mcapps.android.multi_payment_terminal.webapi.gmo.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

// これはたぶん使わないと思う
public class PostOrdersForPartner {
    public static class Request {
        @Expose
        public String dateFrom;

        @Expose
        public String dateTo;

        @Expose
        public String merchantIds;

        @Expose
        public String status;

        @Expose
        public String channel;

        @Expose
        public String limit;

        @Expose
        public String pageNumber;
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
        public String dataCount;

        @Expose
        public String orderCount;

        @Expose
        public String refundCount;

        @Expose
        public Data data;
    }

    public static class Data {
        @Expose
        public String storeOrderId;

        @Expose
        public String procType;

        @Expose
        public String orgStoreOrderId;

        @Expose
        public String merchantId;

        @Expose
        public String storeId;

        @Expose
        public String settlementAmount;

        @Expose
        @SerializedName("sum_refund_fee")
        public String sumRefundFee;

        @Expose
        public String settlementDatetime;

        @Expose
        public String payType;

        @Expose
        public String terminalId;

        @Expose
        public String operatorId;

        @Expose
        public String transactionId;

        @Expose
        public String orgTransactionId;

        @Expose
        @SerializedName("order_body")
        public String orderBody;

        @Expose
        public String wallet;
    }
}
