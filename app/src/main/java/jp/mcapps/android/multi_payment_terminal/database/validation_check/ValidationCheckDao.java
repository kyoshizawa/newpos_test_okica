package jp.mcapps.android.multi_payment_terminal.database.validation_check;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.Date;
import java.util.List;

@Dao
public abstract class ValidationCheckDao {
    //履歴を全件取得
    @Query("SELECT * FROM history_validation_check ORDER BY operation_date DESC")
    public abstract List<ValidationCheckHistoryData> getAllHistory();

    //POS未送信の売上を取得
    @Query("SELECT * FROM payment_validation_check WHERE pos_send = 0")
    public abstract List<ValidationCheckPaymentData> getUnsentPayment();

    @Query("SELECT * FROM payment_validation_check WHERE pos_send = 0 LIMIT :limit")
    public abstract List<ValidationCheckPaymentData> getUnsentPaymentWithLimit(int limit);

    //POS未送信件数を取得
    @Query("SELECT count(payment_id) FROM payment_validation_check WHERE pos_send = 0")
    public abstract int getUnsentCnt();

    //POS送信済の売上を削除
    @Query("DELETE FROM payment_validation_check WHERE pos_send = 1")
    public abstract void deletePosSentPayment();

    //POS送信済の売上を削除
    @Query("DELETE FROM payment_validation_check WHERE payment_id = :paymentId")
    public abstract void deletePaymentById(long paymentId);

    //指定した日時以前の履歴を削除
    @Query("DELETE FROM history_validation_check WHERE operation_date < :date")
    public abstract void deleteOldHistory(String date);

    //売上を挿入
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertPayment(ValidationCheckPaymentData payment);

    //履歴を挿入
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertHistory(ValidationCheckHistoryData history);

    //送信フラグを送信済みに更新
    @Query("UPDATE payment_validation_check SET pos_send = 1 WHERE payment_id = :paymentId")
    public abstract void posSent(long paymentId);

    @Transaction
    public void addRecord(ValidationCheckPaymentData paymentData, ValidationCheckHistoryData historyData) {
        insertPayment(paymentData);
        insertHistory(historyData);
    };

    //デモ用
    @Transaction
    public void addRecord(ValidationCheckHistoryData historyData) {
        insertHistory(historyData);
    };
}
