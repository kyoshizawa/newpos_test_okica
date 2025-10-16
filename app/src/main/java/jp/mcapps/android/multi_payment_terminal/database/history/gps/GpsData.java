package jp.mcapps.android.multi_payment_terminal.database.history.gps;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "history_gps")
public class GpsData implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo
    public double latitude;

    @ColumnInfo
    public double longitude;

    @ColumnInfo
    public Float accuracy;

    @ColumnInfo
    public double altitude;

    @ColumnInfo
    public Float bearing;

    @ColumnInfo
    public Float speed;

    @ColumnInfo
    public int satellites;

    @ColumnInfo(index = true)
    public Date date;

    @ColumnInfo(name = "gps_date")
    public Date gpsDate;

    @ColumnInfo(name = "elapsed_real_time")
    public long elapsedRealTime;
}
