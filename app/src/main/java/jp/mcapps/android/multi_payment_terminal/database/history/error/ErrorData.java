package jp.mcapps.android.multi_payment_terminal.database.history.error;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;


@Entity(tableName = "history_error")
public class ErrorData implements Cloneable {
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

    @ColumnInfo(index = true)
    public Date date;

    @NonNull
    @Override
    public ErrorData clone() {
        ErrorData data = null;
        try {
            data = (ErrorData) super.clone();
            if (date != null) {
                data.date = (Date) date.clone();
            }
        } catch (CloneNotSupportedException ignore) { /* Cloneableを実装しているためここには来ない */ }

        assert data != null; // nullのクローンは出来ない
        return data;
    }
}
