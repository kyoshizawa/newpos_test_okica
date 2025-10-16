package jp.mcapps.android.multi_payment_terminal.database.ticket;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Date;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.data.MoneyType;

@Entity(tableName = "ticket_gtfs")
public class TicketGtfsData implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    // GTFSバージョン
    public String gtfs_version;

    // 追加された時刻
    public Date created_at;

    // 更新された時刻
    public Date updated_at;

    public TicketGtfsData() {
    }
}
