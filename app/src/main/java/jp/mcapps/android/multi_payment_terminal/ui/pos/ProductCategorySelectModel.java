package jp.mcapps.android.multi_payment_terminal.ui.pos;

import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;

import jp.mcapps.android.multi_payment_terminal.database.pos.ProductData;

public class ProductCategorySelectModel {
    public long id;
    public String code;
    public String name;
    public boolean isCategory;
    public Integer unitPrice;
    public String  displayUnitPrice;
    public Integer taxType;
    public Integer reduceTaxType;
    public Integer includedTaxType;

    // 商品データへの参照
    @Nullable
    public ProductData productData;

    public static ProductCategorySelectModel newInstance(ProductData item) {

        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        ProductCategorySelectModel it = new ProductCategorySelectModel();

        it.isCategory = false;
        it.id = item.product_id;
        it.name = item.getDisplayName();
        it.code = item.product_code;
        it.unitPrice = item.standard_unit_price;
        it.displayUnitPrice = decimalFormat.format(item.standard_unit_price);
        it.taxType = item.tax_type;
        it.includedTaxType = item.included_tax_type;
        it.reduceTaxType = item.reduce_tax_type;
        it.productData = item;

        return it;
    }

    public static ProductCategorySelectModel newInstance(CategoryModel item) {

        ProductCategorySelectModel it = new ProductCategorySelectModel();

        it.isCategory = true;
        it.id = item.category_id;
        it.name = item.category_name;
        it.code = ""; // productにしかproduct_code無いですこれをカートに保存していきます

        return it;
    }
}
