package jp.mcapps.android.multi_payment_terminal.database.history.operation;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Dao
public abstract class OperationDao {
    //指定した期間内の履歴を取得
    @Query("select * from history_operation where operation_date >= :start and operation_date <= :end order by operation_date desc")
    abstract List<OperationData> getAllWithinPeriod(Date start, Date end);

    //指定した期間より前の履歴を1件取得
    @Query("select * from history_operation where operation_date < :date limit 1")
    abstract OperationData getPrevOne(Date date);

    //指定した期間より後の履歴を1件取得
    @Query("select * from history_operation where operation_date > :date limit 1")
    abstract OperationData getNextOne(Date date);

    //指定した日付以前のレコードを削除
    @Query("delete from history_operation where operation_date < :date")
    public abstract void deleteOldRecords(Date date);

    //操作履歴の挿入
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertOperationData(OperationData operationData);

    //期間を指定して履歴を取得
    //期間後1件、期間内(日時降順)、期間前1件の順
    @Transaction
    public List<OperationData> getAllByDateTime(Date start, Date end) {
        List<OperationData> res = new ArrayList<>();
        res.add(getNextOne(end));
        res.addAll(getAllWithinPeriod(start, end));
        res.add(getPrevOne(start));

        return res;
    }
}
