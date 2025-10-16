package jp.mcapps.android.multi_payment_terminal.database.pos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public abstract class TerminalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertTerminals(TerminalData... terminals);

    // 対象の世代のレコードを全て削除する
    @Query("DELETE FROM pos_terminals WHERE generation_id = :generation_id")
    public abstract void deleteTerminalsByGenerationId(int generation_id);

    // 世代IDを書き換える
    @Query("UPDATE pos_terminals SET generation_id = :generation_to WHERE generation_id = :generation_from")
    public abstract void swapTerminalsGenerationId(int generation_from, int generation_to);

    // 現在アクティブな端末情報を取得する
    @Query("SELECT * FROM pos_terminals WHERE generation_id = 1")
    public abstract TerminalData[] getTerminals();

    public TerminalData getTerminal() {
        TerminalData[] items = getTerminals();
        if (0 < items.length) {
            return items[0];
        }
        return null;
    }
}
