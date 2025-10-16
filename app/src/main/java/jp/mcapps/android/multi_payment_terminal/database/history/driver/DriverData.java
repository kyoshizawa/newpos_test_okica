package jp.mcapps.android.multi_payment_terminal.database.history.driver;

import java.util.Date;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity (tableName = "driver_history")
public class DriverData {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "driver_code")
    public String driverCode;

    @ColumnInfo(name = "driver_name")
    public  String driverName;

    @ColumnInfo(name = "created_at")
    public Date createdAt;
}
