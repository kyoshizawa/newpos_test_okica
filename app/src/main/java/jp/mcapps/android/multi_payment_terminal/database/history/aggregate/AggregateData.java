package jp.mcapps.android.multi_payment_terminal.database.history.aggregate;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history_aggregate")
public class AggregateData {
    @PrimaryKey
    public int id;

    @ColumnInfo(name = "aggregate_history_order")
    public int aggregateHistoryOrder;

    @ColumnInfo(name = "aggregate_start_datetime")
    public String aggregateStartDatetime;

    @ColumnInfo(name = "aggregate_end_datetime")
    public String aggregateEndDatetime;
}
