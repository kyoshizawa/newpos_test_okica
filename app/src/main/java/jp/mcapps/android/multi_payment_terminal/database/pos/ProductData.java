package jp.mcapps.android.multi_payment_terminal.database.pos;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "pos_products",
        indices = {
                @Index(value = {"product_id", "generation_id"}, unique = true)
        })
public class ProductData implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo
    @NotNull
    public Long product_id;

    @ColumnInfo
    @NotNull
    public String service_instance_id;

    @ColumnInfo
    @NotNull
    public String product_code;

    @ColumnInfo
    public String name;

    @ColumnInfo
    public String name_kana;

    @ColumnInfo
    public String name_short;

    @ColumnInfo
    public Integer standard_unit_price;

    @ColumnInfo
    public Integer tax_type;

    @ColumnInfo
    public Integer reduce_tax_type;

    @ColumnInfo
    public Integer included_tax_type;

    @ColumnInfo
    public Date sale_start_at;

    @ColumnInfo
    public Date sale_end_at;

    @ColumnInfo
    public Integer status;

    @ColumnInfo
    public String remarks;

    @ColumnInfo
    public Long product_category_id;

    @ColumnInfo(defaultValue = "0")
    public int generation_id; // 0:downloading 1:currently active

    public Date created_at;

    // 表示用の名称を取得する
    public String getDisplayName() {
        if (name_short != null && !name_short.isEmpty()) {
            // 短縮名がある場合はそれを優先する
            return name_short;
        }
        return name;
    }
}
