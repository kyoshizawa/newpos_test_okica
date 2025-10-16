package jp.mcapps.android.multi_payment_terminal.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

@Entity
public class TransHead {
    @ColumnInfo
    public int id;
    @ColumnInfo(name = "trans_brand")
    public String transBrand;
    @ColumnInfo(name = "trans_type")
    public Integer transType;
    @ColumnInfo(name = "trans_result")
    public Integer transResult;
    @ColumnInfo(name = "trans_amount")
    public Integer transAmount;
    @ColumnInfo(name = "trans_date")
    public String transDate;
    @ColumnInfo(name = "term_sequence")
    public Integer termSequence;
}
