package jp.mcapps.android.multi_payment_terminal.database.ticket;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import jp.mcapps.android.multi_payment_terminal.database.pos.CategoryData;

@Dao
public abstract class TicketClassNameLangDao {
    //チケット分類に対応する名称を取得
    @Query("select name from ticket_class_name_lang where ticket_class_id = :id and lang = :name_lang")
    public abstract String getTicketClassName(long id, String name_lang);

    //チケット分類名称登録
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertTicketClassesNameLangData(List<TicketClassNameLangData> ticketClassNameLangData);

    // 対象の世代のレコードを全て削除する
    @Query("DELETE FROM ticket_class_name_lang WHERE generation_id = :generation_id")
    public abstract void deleteTicketClassesNameLangByGenerationId(int generation_id);

    // 世代IDを書き換える
    @Query("UPDATE ticket_class_name_lang SET generation_id = :generation_to WHERE generation_id = :generation_from")
    public abstract void swapTicketClassesNameLangGenerationId(int generation_from, int generation_to);

    //全件削除
    @Query("delete from ticket_class_name_lang")
    public abstract void deleteAll();
}
