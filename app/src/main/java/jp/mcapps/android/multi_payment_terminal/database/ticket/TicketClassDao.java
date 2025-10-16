package jp.mcapps.android.multi_payment_terminal.database.ticket;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public abstract class TicketClassDao {
    //全件取得（表示順: 名称の昇順）
    @Query("select * from ticket_classes order by ticket_class_name asc")
    public abstract List<TicketClassData> getAllTicketClasses();

    //1件取得（チケット分類名称の昇順トップのみ）
    @Query("select * from ticket_classes order by ticket_class_name asc limit 1")
    public abstract TicketClassData getInitTicketClassData();

    //チケット分類登録
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertTicketClassesData(List<TicketClassData> ticketClassesData);

    // 対象の世代のレコードを全て削除する
    @Query("DELETE FROM ticket_classes WHERE generation_id = :generation_id")
    public abstract void deleteTicketClassesByGenerationId(int generation_id);

    // 世代IDを書き換える
    @Query("UPDATE ticket_classes SET generation_id = :generation_to WHERE generation_id = :generation_from")
    public abstract void swapTicketClassesGenerationId(int generation_from, int generation_to);

    //全件削除
    @Query("delete from ticket_classes")
    public abstract void deleteAll();
}
