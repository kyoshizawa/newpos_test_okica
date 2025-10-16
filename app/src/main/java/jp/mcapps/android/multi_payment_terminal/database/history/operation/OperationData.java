package jp.mcapps.android.multi_payment_terminal.database.history.operation;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "history_operation")
public class OperationData {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "operation_type")
    public int operationType;

    @NonNull
    @ColumnInfo(name = "screen_name")
    public String screenName;

    @ColumnInfo(name = "dialog_name")
    public String dialogName;

    @NonNull
    @ColumnInfo(name = "operation_name")
    public String operationName;

    @ColumnInfo(name = "input_data")
    public String inputData;

    @ColumnInfo(name = "input_digit_number")
    public Integer inputDigitNumber;

    @ColumnInfo(name = "operation_date", index = true)
    public Date operationDate;
}
