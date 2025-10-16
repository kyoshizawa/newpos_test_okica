package jp.mcapps.android.multi_payment_terminal.database.history.radio;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Dao
public abstract class RadioDao {
    //指定した期間内の履歴を取得
    @Query("select * from history_radio where date >= :start and date <= :end order by date desc")
    abstract List<RadioData> getAllWithinPeriod(Date start, Date end);

    //指定した期間より前の履歴を1件取得
    @Query("select * from history_radio where date < :date limit 1")
    abstract RadioData getPrevOne(Date date);

    //指定した期間より後の履歴を1件取得
    @Query("select * from history_radio where date > :date limit 1")
    abstract RadioData getNextOne(Date date);

    //指定した期間内のアンテナレベルを取得
    @Query("select level from history_radio where date >= :start and date <= :end")
    abstract List<Integer> getLevelWithinPeriod(Date start, Date end);

    //指定した期間より前のアンテナレベルを1件取得
    @Query("select level from history_radio where date < :date limit 1")
    abstract Integer getPrevLevel(Date date);

    //指定した期間より後のアンテナレベルを1件取得
    @Query("select level from history_radio where date > :date limit 1")
    abstract Integer getNextLevel(Date date);

    //最新の履歴を1件取得
    @Query("select * from history_radio order by id desc limit 1")
    public abstract RadioData getLatestOne();

    //電波履歴の全件削除
    @Query("delete from history_radio")
    public abstract void deleteAll();

    //指定した日付以前のレコードを削除
    @Query("delete from history_radio where date < :date")
    public abstract void deleteOldRecords(Date date);

    //電波履歴の挿入
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertRadioData(RadioData radioData);

    //期間を指定してアンテナレベルを取得
    //期間後1件、期間内(日時降順)、期間前1件の順
    @Transaction
    public List<RadioData> getAllByDateTime(Date start, Date end) {
        List<RadioData> res = new ArrayList<>();
        res.add(getNextOne(end));
        res.addAll(getAllWithinPeriod(start, end));
        res.add(getPrevOne(start));

        return res;
    }

    //期間を指定してアンテナレベルを取得
    //期間後1件、期間内、期間前1件の順
    @Transaction
    public List<Integer> getLevelByDateTime(Date start, Date end) {
        List<Integer> res = new ArrayList<>();
        res.add(getNextLevel(end));
        res.addAll(getLevelWithinPeriod(start, end));
        res.add(getPrevLevel(start));

        return res;
    }
}
