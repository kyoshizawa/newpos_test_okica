package jp.mcapps.android.multi_payment_terminal.database.history.uriOkica;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public abstract class UriOkicaDao {
    // 未送信の売上を取得
    @Transaction
    @Query("select * from history_uri_okica where okica_send = 0")
    public abstract List<UriOkicaData> getUnsentData();

    // 未送信売上データの件数を取得
    @Query("select count(id) from history_uri_okica where okica_send = 0")
    public abstract int getUnsentCnt();

    //送信フラグを送信済みに更新
    @Query("update history_uri_okica set okica_send = 1 where okica_trans_date = :date and okica_sequence = :seq")
    public abstract void setOkicaSent(String date, int seq);

    // 全件削除
    @Query("delete from history_uri_okica")
    public abstract void deleteAll();

    // 送信済フラグがたっているデータを削除
    @Query("delete from history_uri_okica where okica_send = 1")
    public abstract void deleteOkicaSentData();

    // 売上履歴の挿入
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertUriData(UriOkicaData uriOkicaData);

    // 送信済みの売上データを削除し、紐づいた印字データを取消不可に変更
    @Query("delete from history_uri_okica where okica_trans_date = :date and okica_sequence = :seq and okica_send = 1")
    public abstract void posSendCompleted(String date, int seq);
}
