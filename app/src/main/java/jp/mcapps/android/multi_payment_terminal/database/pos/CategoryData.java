package jp.mcapps.android.multi_payment_terminal.database.pos;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "pos_categories",
        indices = {
                @Index(value = {"category_id", "generation_id"}, unique = true)
        })
public class CategoryData implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo
    @NotNull
    public Long category_id;

    @ColumnInfo
    @NotNull
    public String service_instance_id;

    @ColumnInfo
    @NotNull
    public String name;

    @ColumnInfo
    public String name_kana;

    @ColumnInfo
    public String name_short;

    @ColumnInfo
    public Integer status;

    @ColumnInfo
    public Long parent_id;

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
