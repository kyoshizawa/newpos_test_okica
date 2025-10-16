package jp.mcapps.android.multi_payment_terminal.database.pos;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "pos_service_functions",
        indices = {
                @Index(value = {"generation_id"}, unique = true)
        })
public class ServiceFunctionData implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    // 取引先コード
    @ColumnInfo
    @NotNull
    public String customer_code;

    // 商品カテゴリでグルーピングするかどうか
    public boolean is_product_category;

    // 領収書発行ボタンを表示するかどうか
    public boolean is_pos_receipt;

    // 金額の手入力を可能にするかどうか
    public boolean is_manual_amount;

    // レシートに表示するタイトル文字列
    @ColumnInfo(defaultValue = "")
    @NotNull
    public String slip_title;

    // 消費税計算後に端数が発生した場合の、処理方法 (0:切り捨て 1:切り上げ 2:四捨五入)
    @ColumnInfo(defaultValue = "0")
    public int tax_rounding;

    // 標準税率
    public String standard_tax_rate;

    // 軽減税率
    public String reduced_tax_rate;

    // レシート印字枚数
    public int receipt_count;

    // TODO ... 必要な情報はここに追加

    @ColumnInfo(defaultValue = "0")
    public int generation_id; // 0:downloading 1:currently active

    public Date created_at;
}
