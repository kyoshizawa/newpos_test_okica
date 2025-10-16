package jp.mcapps.android.multi_payment_terminal.database.ticket;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public abstract class TicketGateSettingsDao {
    // 改札設定取得（最新レコード取得）
    @Query("select * from ticket_gate_settings order by created_at desc limit 1")
    public abstract TicketGateSettingsData getTicketGateSettingsLatest();

    //のりば・おりば情報登録
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertTicketGateSettingsData(TicketGateSettingsData ticketGateSettingsData);

    //全件削除
    @Query("delete from ticket_gate_settings")
    public abstract void deleteAll();
}
