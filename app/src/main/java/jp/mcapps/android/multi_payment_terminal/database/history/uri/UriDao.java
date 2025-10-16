package jp.mcapps.android.multi_payment_terminal.database.history.uri;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public abstract class UriDao {
    //POS未送信の売上を取得
    @Transaction
    @Query("select * from history_uri where pos_send = 0")
    public abstract List<UriData> getUnsentData();

    //未送信のクレジット売上データがないか確認
    @Query("select id from history_uri where pos_send = 0 and trans_brand = 'クレジット' limit 1")
    public abstract Integer getUnsentCreditData();

    //未送信売上データの件数を取得
    @Query("select count(id) from history_uri where pos_send = 0")
    public abstract int getUnsentCnt();

    //全件削除
    @Query("delete from history_uri")
    public abstract void deleteAll();

    //POS送信済フラグがたっているデータを削除 ダミー決済データ
    @Query("delete from history_uri where pos_send = 1")
    public abstract void deletePosSentData();

    //売上履歴の挿入
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertUriData(UriData uriData);

    //送信済みの売上データを削除し、紐づいた印字データを取消不可に変更
    @Query("delete from history_uri where trans_date = :date and term_sequence = :seq")
    public abstract void posSendCompleted(String date, int seq);

    //不正なブランド名が入っているデータを削除
    @Delete
    public abstract void deleteUriDataList(List<UriData> dataList);
}
