package jp.mcapps.android.multi_payment_terminal.database.ticket;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public abstract class TicketReceiptDao {
    // レシートデータを1件追加する
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertReceipt(TicketReceiptData receiptTicketData);

    // slip_Idを指定してレシートデータを取得する
    @Query("SELECT * FROM ticket_receipts WHERE slip_id = :slip_id")
    public abstract TicketReceiptData getReceiptsBySlipId(int slip_id);

    // term_sequenceとtrans_dateを指定して最新のレシートデータを取得する
    @Query("SELECT * FROM ticket_receipts WHERE term_sequence = :term_sequence AND trans_date = :trans_date ORDER BY slip_id DESC")
    public abstract TicketReceiptData getReceiptsByTermSequenceAndTransDate(int term_sequence, String trans_date);

    // レシートデータを全て取得する
    @Query("SELECT * FROM ticket_receipts")
    public abstract TicketReceiptData[] getReceipts();

    // 全てのレシートデータを削除する
    @Query("DELETE FROM ticket_receipts")
    public abstract void deleteAllReceipts();

    // history_slipに残っていないデータを削除する
    @Query("DELETE FROM ticket_receipts WHERE slip_id NOT IN(:slip_ids)")
    public abstract void deleteBySlipId(List<Integer> slip_ids);

    // 領収書印字回数を更新
    @Query("UPDATE ticket_receipts SET print_cnt = print_cnt + 1 WHERE slip_id = :slip_id")
    public abstract void updatePrintCnt(int slip_id);

    // 取消フラグを更新
    @Query("UPDATE ticket_receipts SET canceled_trans = 1 WHERE slip_id = :slip_id")
    public abstract void updateCanceledTrans(int slip_id);
}
