package jp.mcapps.android.multi_payment_terminal.database.ticket;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;

@Dao
public abstract class TicketGtfsDao {
    // データを1件追加する
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertGtfs(TicketGtfsData ticketGtfsData);

    //最新の印字を1件取得
    @Query("select * from ticket_gtfs order by created_at desc limit 1")
    public abstract TicketGtfsData getLatestOne();

    // 全てのレシートデータを削除する
    @Query("DELETE FROM ticket_gtfs")
    public abstract void deleteAll();
}
