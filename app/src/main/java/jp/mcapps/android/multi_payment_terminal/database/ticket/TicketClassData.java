package jp.mcapps.android.multi_payment_terminal.database.ticket;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Date;

import jp.mcapps.android.multi_payment_terminal.database.pos.ProductData;
import jp.mcapps.android.multi_payment_terminal.error.DomainErrors;

@Entity(tableName = "ticket_classes")
public class TicketClassData implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo
    public Long ticket_class_id;

    @ColumnInfo
    public String ticket_class_name;

    @ColumnInfo
    public String service_instance_id;

    @ColumnInfo
    public String reserve_type;

    @ColumnInfo
    public Boolean enable_route_judge;

    @ColumnInfo
    public Boolean enable_trip_judge;

    @ColumnInfo
    public Boolean enable_stop_judge;

    @ColumnInfo
    public String ticket_class_created_at;

    @ColumnInfo
    public Long ticket_class_created_auth_server_id;

    @ColumnInfo
    public String ticket_class_created_user_id;

    @ColumnInfo
    public String ticket_class_created_user_name;

    @ColumnInfo
    public String ticket_class_updated_at;

    @ColumnInfo
    public Long ticket_class_updated_auth_server_id;

    @ColumnInfo
    public String ticket_class_updated_user_id;

    @ColumnInfo
    public String ticket_class_updated_user_name;

    @ColumnInfo(defaultValue = "0")
    public int generation_id; // 0:downloading 1:currently active

    // 追加された時刻
    public Date created_at;

    // 更新された時刻
    public Date updated_at;

    public TicketClassData() {
    }
}
