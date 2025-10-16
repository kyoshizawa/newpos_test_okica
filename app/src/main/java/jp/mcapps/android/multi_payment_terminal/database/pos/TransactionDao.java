package jp.mcapps.android.multi_payment_terminal.database.pos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public abstract class TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract long insertTransaction(TransactionData transaction);

    @Query("UPDATE pos_transactions SET uploaded = 1 WHERE id in (:ids)")
    public abstract void updateTransactionsToUploaded(List<Long> ids);

    @Query("DELETE FROM pos_transactions WHERE id in (:ids)")
    public abstract void deleteTransactionsByIDs(List<Long> ids);

    @Query("SELECT * FROM pos_transactions ORDER BY transaction_at LIMIT :limit OFFSET :offset")
    public abstract TransactionData[] getTransactionsToUpload(int limit, int offset);

    @Query("SELECT * FROM pos_transactions WHERE uploaded = 1")
    public abstract TransactionData[] getTransactionsToDelete();

    @Query("SELECT * FROM pos_transactions WHERE uploaded = 0")
    public abstract TransactionData[] getTransactionsUnUpload();
}
