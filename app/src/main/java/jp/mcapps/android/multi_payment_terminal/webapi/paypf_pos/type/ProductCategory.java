package jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.type;

import com.google.gson.annotations.Expose;

import java.util.Date;

/**
 * 以下のURLに定義されているProductCategoryを参照してください
 * https://github.com/MobileCreate/pay_pf_grpc/blob/main/proto/paypf/pos/product/product-data.proto
 */
public class ProductCategory {

    @Expose
    public long id;

    @Expose
    public String name;

    @Expose
    public String name_kana;

    @Expose
    public String name_short;

    @Expose
    public String status;

    @Expose
    public Long parent_id;

    @Expose
    public String parent_name;

    @Expose
    public Date created_at;

    @Expose
    public Date updated_at;
}
