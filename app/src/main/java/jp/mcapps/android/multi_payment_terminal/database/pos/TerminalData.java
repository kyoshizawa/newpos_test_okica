package jp.mcapps.android.multi_payment_terminal.database.pos;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "pos_terminals",
        indices = {
                @Index(value = {"generation_id"}, unique = true)
        })
public class TerminalData implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    // 端末ID
    @ColumnInfo
    @NotNull
    public String terminal_id;

    // 顧客ID
    @ColumnInfo
    @NotNull
    public String customer_id;

    // 端末識別番号
    @ColumnInfo
    @NotNull
    public String terminal_no;

    // サービスインスタンスID (ABT)
    @ColumnInfo
    @NotNull
    public String service_instance_abt;

    // サービスインスタンスID (POS)
    @ColumnInfo
    @NotNull
    public String service_instance_pos;

    // TODO ... 必要な情報はここに追加

    @ColumnInfo(defaultValue = "0")
    @NotNull
    public int generation_id; // 0:downloading 1:currently active

    public Date created_at;
}
