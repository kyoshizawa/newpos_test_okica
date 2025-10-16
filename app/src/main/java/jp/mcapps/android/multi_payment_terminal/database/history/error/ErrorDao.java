package jp.mcapps.android.multi_payment_terminal.database.history.error;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Dao
public abstract class ErrorDao {
    //指定した期間内の履歴を取得
    @Query("select * from history_error where date >= :start and date <= :end order by date desc")
    public abstract List<ErrorData> getAllWithinPeriod(Date start, Date end);

    //指定した期間より前の履歴を1件取得
    @Query("select * from history_error where date < :date limit 1")
    abstract ErrorData getPrevOne(Date date);

    //指定した期間より後の履歴を1件取得
    @Query("select * from history_error where date > :date limit 1")
    abstract ErrorData getNextOne(Date date);

    //指定した日付以前のレコードを削除
    @Query("delete from history_error where date < :date")
    public abstract void deleteOldRecords(Date date);

    //エラー履歴の挿入
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertErrorData(ErrorData errorData);

    //期間を指定して履歴を取得
    //期間後1件、期間内(日時降順)、期間前1件の順
    @Transaction
    public List<ErrorData> getAllByDateTime(Date start, Date end) {
        List<ErrorData> res = new ArrayList<>();
        res.add(getNextOne(end));
        res.addAll(getAllWithinPeriod(start, end));
        res.add(getPrevOne(start));

        return res;
    }
}
