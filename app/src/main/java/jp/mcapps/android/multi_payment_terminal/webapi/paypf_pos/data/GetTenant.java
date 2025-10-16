package jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.data;

import com.google.gson.annotations.Expose;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.type.Tenant;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.type.TenantProduct;

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
