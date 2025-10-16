package jp.mcapps.android.multi_payment_terminal.database.pos;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "pos_tax_calc")
public class TaxCalcData implements Serializable {
    @PrimaryKey
    public int id;

    @ColumnInfo
    public Integer standard_tax_rate; // 一般税率（%）

    @ColumnInfo
    public Integer reduced_tax_rate; // 軽減税率（%）

    @ColumnInfo
    public Integer total_amount; // 合計金額

    @ColumnInfo
    public Integer total_count; // 合計個数

    @ColumnInfo
    public Integer amount_tax_free; // 非課税合計

    @ColumnInfo
    public Integer amount_tax_reduced; // 軽減税率対象（サ）

    @ColumnInfo
    public Integer amount_tax_standard; // 一般税率対象（シ）

    @ColumnInfo
    public Integer amount_tax_reduced_only_tax; // 軽減税率対象の内消費税（ス）

    @ColumnInfo
    public Integer amount_tax_standard_only_tax; // 一般税率対象の内消費税（セ）

    @ColumnInfo
    public Integer amount_tax_exclusive_reduced; // 外税合計（軽減課税）

    @ColumnInfo
    public Integer amount_tax_exclusive_standard; // 外税合計（一般課税）

    @ColumnInfo
    public Integer amount_tax_exclusive_reduced_without_tax; // 外税合計（軽減課税）税抜

    @ColumnInfo
    public Integer amount_tax_exclusive_standard_without_tax; // 外税合計（一般課税）税抜

    @ColumnInfo
    public Integer amount_tax_exclusive_reduced_only_tax; // 外税合計（軽減課税）の内消費税

    @ColumnInfo
    public Integer amount_tax_exclusive_standard_only_tax; // 外税合計（一般課税）の内消費税

    @ColumnInfo
    public Integer amount_tax_inclusive_reduced; // 内税合計（軽減課税）

    @ColumnInfo
    public Integer amount_tax_inclusive_standard; // 内税合計（一般課税）
}
