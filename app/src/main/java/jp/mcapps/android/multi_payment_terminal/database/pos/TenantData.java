package jp.mcapps.android.multi_payment_terminal.database.pos;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "pos_tenants",
        indices = {
                @Index(value = {"tenant_id", "generation_id"}, unique = true)
        })
public class TenantData implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo
    @NotNull
    public Long tenant_id;

    @ColumnInfo
    @NotNull
    public String service_instance_id;

    @ColumnInfo
    @NotNull
    public String tenant_code; // 店舗コード

    @ColumnInfo
    @NotNull
    public Long merchant_id;

    @ColumnInfo
    @NotNull
    public String customer_code; // 取引先コード

    @ColumnInfo
    public String name;

    @ColumnInfo
    public String name_kana;

    @ColumnInfo
    public String zipcode;

    @ColumnInfo
    public Integer pref_cd;

    @ColumnInfo
    public String city;

    @ColumnInfo
    public String address_line1;

    @ColumnInfo
    public String address_line2;

    @ColumnInfo
    public String address_line3;

    @ColumnInfo
    public String kana_city;

    @ColumnInfo
    public String address_kana_line1;

    @ColumnInfo
    public String address_kana_line2;

    @ColumnInfo
    public String address_kana_line3;

    @ColumnInfo
    public String phone_number;

    @ColumnInfo
    public String fax;

    @ColumnInfo
    public String houjin_bangou;

    @ColumnInfo
    public String alphabet_name;

    @ColumnInfo
    public String parent_name;

    // TODO ... 加盟店情報で必要な情報はここに追加

    @ColumnInfo(defaultValue = "0")
    @NotNull
    public int generation_id; // 0:downloading 1:currently active

    public Date created_at;
}
