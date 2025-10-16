package jp.mcapps.android.multi_payment_terminal.database.history.error;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.Date;
import java.util.List;

import io.reactivex.Flowable;
import jp.mcapps.android.multi_payment_terminal.database.history.operation.OperationData;
import jp.mcapps.android.multi_payment_terminal.database.history.radio.RadioData;

@Dao
public interface ErrorStackingDao {
    @Query("select * from history_error_stacking where error_code == :errorCode order by date desc limit 1")
    public ErrorStackingData getErrorStackingData(String errorCode);

    @Query("select * from history_error_stacking order by date desc")
    public List<ErrorStackingData> getAll();

    @Query("select * from history_error_stacking order by date desc")
    public Flowable<List<ErrorStackingData>> getAllFlowable();

    @Query("update history_error_stacking set date = :date where id = :id")
    public void updateErrorStackingData(int id, Date date);

    @Query("delete from history_error_stacking where id = :id")
    public void deleteErrorStackingData(int id);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract void insertErrorData(ErrorStackingData errorStackingData);
}
