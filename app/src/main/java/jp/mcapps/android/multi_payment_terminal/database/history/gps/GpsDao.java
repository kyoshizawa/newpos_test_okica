package jp.mcapps.android.multi_payment_terminal.database.history.gps;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Dao
public abstract class GpsDao {
    //指定した期間の履歴を取得
    @Query("select * from history_gps where date >= :start and date <= :end order by id desc")
    abstract List<GpsData> getAllWithinPeriod(Date start, Date end);

    //指定した期間より前の履歴を1件取得
    @Query("select * from history_gps where date < :date limit 1")
    abstract GpsData getPrevOne(Date date);

    //指定した期間より後の履歴を1件取得
    @Query("select * from history_gps where date > :date limit 1")
    abstract GpsData getNextOne(Date date);

    //全件削除
    @Query("delete from history_gps")
    public abstract void deleteAll();

    //最新の1件を取得
    @Query("select * from history_gps order by id desc limit 1")
    public abstract GpsData getLatestOne();

    //指定した日付以前のレコードを削除
    @Query("delete from history_gps where date < :date")
    public abstract void deleteOldRecords(Date date);

    //GPS履歴挿入
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertGpsData(GpsData gpsData);

    //期間を指定して履歴を取得
    //期間後1件、期間内(日時降順)、期間前1件の順
    @Transaction
    public List<GpsData> getAllByDateTime(Date start, Date end) {
        List<GpsData> res = new ArrayList<>();
        res.add(getNextOne(end));
        res.addAll(getAllWithinPeriod(start, end));
        res.add(getPrevOne(start));

        return res;
    }
}
