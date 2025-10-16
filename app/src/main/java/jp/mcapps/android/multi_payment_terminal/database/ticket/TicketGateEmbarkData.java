package jp.mcapps.android.multi_payment_terminal.database.ticket;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "ticket_gate_embark")
public class TicketGateEmbarkData implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo
    public String stop_id;

    @ColumnInfo
    public String stop_name;

    @ColumnInfo
    public String stop_type;

    @ColumnInfo(defaultValue = "0")
    public int generation_id; // 0:downloading 1:currently active

    // 追加された時刻
    public Date created_at;

    // 更新された時刻
    public Date updated_at;

    public TicketGateEmbarkData() {
    }
}
