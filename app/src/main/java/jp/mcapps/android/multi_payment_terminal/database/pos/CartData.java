package jp.mcapps.android.multi_payment_terminal.database.pos;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Date;

import jp.mcapps.android.multi_payment_terminal.error.DomainErrors;

@Entity(tableName = "pos_carts")
public class CartData implements Serializable {

    public static final String BARCODE_TYPE_GS1_128 = "GS1_128"; // (GS1-128) コンビニ収納用バーコード
    public static final String BARCODE_TYPE_EL_QR = "EL_QR"; // (eL-QR) 地方統一税QRコード

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo
    public Long product_id;

    @ColumnInfo
    public Long product_category_id;

    @ColumnInfo
    public Integer count;

    @ColumnInfo
    public String product_name;

    @ColumnInfo
    public String product_name_short;

    @ColumnInfo
    public String product_code;

    @ColumnInfo
    public Integer standard_unit_price;

    @ColumnInfo
    public Integer custom_unit_price;

    @ColumnInfo
    public Integer tax_type;

    @ColumnInfo
    public Integer reduce_tax_type;

    @ColumnInfo
    public Integer included_tax_type;

    @ColumnInfo
    public Boolean is_manual;

    @ColumnInfo
    public Boolean is_custom_price;

    @ColumnInfo
    public String barcode_type; // GS1-128, eL-QR, etc.

    @ColumnInfo
    public String barcode_text; // バーコードの値

    @ColumnInfo
    public Date filing_due_date; // 納期限

    @ColumnInfo
    public Boolean is_filing_overdue; // 納期限切れ

    @ColumnInfo
    public Date payment_due_date; // 支払期限

    @ColumnInfo
    public Boolean is_payment_overdue; // 支払期限切れ

    // カートに追加された時刻
    public Date created_at;

    // カート情報が更新された時刻
    public Date update_at;

    {
        barcode_type = "";
        barcode_text = "";
        filing_due_date = null;
        is_filing_overdue = false;
        payment_due_date = null;
        is_payment_overdue = false;
    }

    public CartData() {
    }

    public CartData(@NotNull ProductData productData) {
        product_id = productData.product_id;
        product_category_id = productData.product_category_id;
        product_name = productData.name;
        product_name_short = productData.name_short;
        product_code = productData.product_code;
        tax_type = productData.tax_type;
        reduce_tax_type = productData.reduce_tax_type;
        included_tax_type = productData.included_tax_type;
        standard_unit_price = productData.standard_unit_price;
        custom_unit_price = 0;
        count = 1;
        is_manual = false;
        is_custom_price = false;
        created_at = new Date();
        update_at = new Date();
    }

    public CartData(
            Integer standardUnitPrice,
            int taxType,
            int reduceTaxType,
            int includedTaxType
    ) {
        standard_unit_price = standardUnitPrice;
        custom_unit_price = 0;
        tax_type = taxType;
        reduce_tax_type = reduceTaxType;
        included_tax_type = includedTaxType;
        count = 1;
        product_name = "手動明細";
        product_code = null;
        is_manual = true;
        is_custom_price = false;
        created_at = new Date();
        update_at = new Date();
    }

    // 表示用の商品名を取得する
    public String getDisplayProductName() {
        if (product_name_short != null && !product_name_short.isEmpty()) {
            // 短縮名がある場合はそれを優先する
            return product_name_short;
        }
        return product_name;
    }

    // インクリメントする
    public void Increment() throws DomainErrors.Exception {
        int n = count + 1;
        if (999 < n) {
            // 個数は0~999まで
            DomainErrors.OUT_OF_RANGE.raise();
        }
        this.count = n;
    }

    // デクリメントする
    public void Decrement() throws DomainErrors.Exception {
        int n = count - 1;
        if (n < 0) {
            // 個数は0~999まで
            DomainErrors.OUT_OF_RANGE.raise();
        }
        this.count = n;
    }
}
