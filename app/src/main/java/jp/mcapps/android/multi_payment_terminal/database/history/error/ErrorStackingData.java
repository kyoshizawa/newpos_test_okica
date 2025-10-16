package jp.mcapps.android.multi_payment_terminal.database.history.error;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;


@Entity(tableName = "history_error_stacking")
public class ErrorStackingData {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "error_code")
    public String errorCode;

    @ColumnInfo
    public String title;

    @ColumnInfo
    public String message;

    @ColumnInfo
    public String detail;

    @ColumnInfo
    public int level;

    @ColumnInfo
    public Date date;
}
