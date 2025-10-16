package jp.mcapps.android.multi_payment_terminal.database.history.radio;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "history_radio")
public class RadioData implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo
    public int level;

    @ColumnInfo
    public Integer asu;

    @ColumnInfo
    public Integer rsrp;

    @ColumnInfo
    public Integer rsrq;

    @ColumnInfo
    public Integer rssnr;

    //@ColumnInfo
    //public Integer cqi;

    //@ColumnInfo(name = "timing_advance")
    //public int timingAdvance;

    @ColumnInfo(name = "signal_strength")
    public int signalStrength;

    @ColumnInfo(index = true)
    public Date date;

    @ColumnInfo(name = "network_type")
    public String networkType;

    @ColumnInfo(name = "network_type_code")
    public int networkTypeCode;
}
