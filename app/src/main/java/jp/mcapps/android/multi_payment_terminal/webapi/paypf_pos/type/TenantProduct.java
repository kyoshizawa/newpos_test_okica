package jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.type;

import com.google.gson.annotations.Expose;

import java.util.Date;

/**
 * 以下のURLに定義されているTenantProductを参照してください
 * https://github.com/MobileCreate/pay_pf_grpc/blob/main/proto/paypf/pos/product/product-data.proto
 */
public class TenantProduct {

    @Expose
    public long id;

    @Expose
    public String product_code;

    @Expose
    public String name;

    @Expose
    public String name_kana;

    @Expose
    public String name_short;

    @Expose
    public int standard_unit_price;

    @Expose
    public String tax_type;

    @Expose
    public String reduced_tax_type;

    @Expose
    public String included_tax_type;

    @Expose
    public Date sale_start_at;

    @Expose
    public Date sale_end_at;

    @Expose
    public String status;

    @Expose
    public String remarks;

    @Expose
    public Long product_category_id;

    @Expose
    public String product_category_name;

    @Expose
    public Date created_at;

    @Expose
    public Date updated_at;
}
