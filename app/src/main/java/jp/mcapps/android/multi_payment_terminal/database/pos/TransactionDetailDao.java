package jp.mcapps.android.multi_payment_terminal.database.pos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public abstract class TransactionDetailDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertTransactionDetails(List<TransactionDetailData> transactionDetails);

    @Query("DELETE FROM pos_transactions_detail WHERE payment_transaction_id in (:transaction_ids)")
    public abstract void deleteTransactionDetailsByTransactionIDs(List<Long> transaction_ids);

    @Query("SELECT * FROM pos_transactions_detail WHERE payment_transaction_id in (:transaction_ids) ORDER BY payment_transaction_id, id")
    public abstract TransactionDetailData[] getTransactionDetailsByTransactionIDs(List<Long> transaction_ids);
}
