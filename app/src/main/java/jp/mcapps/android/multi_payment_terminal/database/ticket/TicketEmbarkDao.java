package jp.mcapps.android.multi_payment_terminal.database.ticket;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public abstract class TicketEmbarkDao {
    //指定チケット分類の全停留所名称取得（表示順: 名称の昇順） 重複名称はまとめる※仕様より
    @Query("select * from ticket_embark where stop_type = :stop_type and ticket_class_id = :ticket_class_id group by stop_name order by stop_name asc")
    public abstract List<TicketEmbarkData> getAllTicketEmbarkByTicketId(long ticket_class_id, String stop_type); // stop_type : embark or disembark

    //指定チケット分類の指定のりばと同じ路線の全降車停留所名称取得（表示順: 名称の昇順） 重複名称はまとめる※仕様より
    @Query("select * from ticket_embark where stop_type = :stop_type and ticket_class_id = :ticket_class_id and route_id = :route_id group by stop_name order by stop_name asc")
    public abstract List<TicketEmbarkData> getAllTicketEmbarkByRouteId(long ticket_class_id, String stop_type, String route_id); // stop_type : embark or disembark

    //指定チケット分類、停留所の全経路ID取得
    @Query("select route_id from ticket_embark where stop_type = :stop_type and ticket_class_id = :ticket_class_id and stop_id = :stop_id group by route_id")
    public abstract List<String> getAllRouteEmbarkByStopId(long ticket_class_id, String stop_type, String stop_id); // stop_type : embark or disembark

    //のりば・おりば情報登録
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertTicketEmbarkData(List<TicketEmbarkData> ticketEmbarkData);

    // 対象の世代のレコードを全て削除する
    @Query("DELETE FROM ticket_embark WHERE generation_id = :generation_id")
    public abstract void deleteTicketEmbarkByGenerationId(int generation_id);

    // 世代IDを書き換える
    @Query("UPDATE ticket_embark SET generation_id = :generation_to WHERE generation_id = :generation_from")
    public abstract void swapTicketEmbarkGenerationId(int generation_from, int generation_to);

    //全件削除
    @Query("delete from ticket_embark")
    public abstract void deleteAll();
}
