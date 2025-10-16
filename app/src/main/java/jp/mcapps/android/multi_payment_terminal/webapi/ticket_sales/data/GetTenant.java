package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data;

import com.google.gson.annotations.Expose;

import org.jetbrains.annotations.Nullable;

import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.data.Status;
import jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales.type.Tenant;

/**
 * 以下のURLに定義されているProductCategoryを参照してください
 * https://github.com/MobileCreate/pay_pf_grpc/blob/main/proto/paypf/pos/product/product-query-service.proto
 */
public class GetTenant {

    public static class Response {

        @Expose
        @Nullable
        public Tenant data;

        @Expose
        @Nullable
        public Status error;
    }
}
