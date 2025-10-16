package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data;

import com.google.gson.annotations.Expose;

/**
 * 以下のURLの定義を参照してください
 * https://github.com/googleapis/googleapis/blob/master/google/rpc/status.proto
 */
public class Status {

    /**
     * grpc status code:
     * https://github.com/googleapis/googleapis/blob/master/google/rpc/code.proto
     */
    @Expose
    public int code;

    /**
     * error message
     */
    @Expose
    public String message;
}
