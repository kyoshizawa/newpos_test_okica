package jp.mcapps.android.multi_payment_terminal.database.ticket;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "ticket_gate_settings")
public class TicketGateSettingsData implements Serializable, Cloneable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo
    public String stop_id;

    @ColumnInfo
    public String stop_name;

    @ColumnInfo
    public String stop_type;

    @ColumnInfo
    public Integer gate_check_start_time;

    @ColumnInfo
    public Boolean auto_gate_check;

    @ColumnInfo
    public Integer auto_gate_check_interval;

    @ColumnInfo
    public String route_id;

    @ColumnInfo
    public String route_name;

    @ColumnInfo
    public String trip_id;

    @ColumnInfo
    public String start_stop_name;

    @ColumnInfo
    public String start_departure_time;

    @ColumnInfo
    public String end_stop_name;

    @ColumnInfo
    public String end_arrival_time;

    // 追加された時刻
    public Date created_at;

    // 更新された時刻
    public Date updated_at;

    public TicketGateSettingsData() {
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();    //浅いコピーを返します
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        TicketGateSettingsData dst = (TicketGateSettingsData) obj;
        if (dst.auto_gate_check) {
            if (this.stop_id.equals(dst.stop_id)
            && this.stop_name.equals(dst.stop_name)
            && this.stop_type.equals(dst.stop_type)
            && this.gate_check_start_time == dst.gate_check_start_time
            && this.auto_gate_check == dst.auto_gate_check
            && this.auto_gate_check_interval == dst.auto_gate_check_interval
            && this.route_id.equals(dst.route_id)
            && this.route_name.equals(dst.route_name)
            // ここは参考程度に保持しているだけなのでノーチェック
            //        &&  this.trip_id.equals(dst.trip_id)
            //        &&  this.start_stop_name.equals(dst.start_stop_name)
            //&& this.start_departure_time.equals(dst.start_departure_time)
            //&& this.end_arrival_time.equals(dst.end_arrival_time)
            ) {
                return true;
            } else {
                return false;
            }
        } else {
            if (this.stop_id.equals(dst.stop_id)
            && this.stop_name.equals(dst.stop_name)
            && this.stop_type.equals(dst.stop_type)
            && this.gate_check_start_time == dst.gate_check_start_time
            && this.auto_gate_check == dst.auto_gate_check
            && this.auto_gate_check_interval == dst.auto_gate_check_interval
            && this.route_id.equals(dst.route_id)
            && this.route_name.equals(dst.route_name)
            // ここは参考程度に保持しているだけなのでノーチェック
            //        &&  this.trip_id.equals(dst.trip_id)
            //        &&  this.start_stop_name.equals(dst.start_stop_name)
            && this.start_departure_time.equals(dst.start_departure_time)
            && this.end_arrival_time.equals(dst.end_arrival_time)
            ) {
                return true;
            } else {
                return false;
            }
        }
    }
}
