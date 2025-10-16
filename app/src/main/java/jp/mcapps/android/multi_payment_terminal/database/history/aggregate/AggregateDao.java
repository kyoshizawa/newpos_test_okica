package jp.mcapps.android.multi_payment_terminal.database.history.aggregate;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public abstract class AggregateDao {
    //過去の集計履歴を全て取得
    @Query("select * from history_aggregate where aggregate_history_order > 0 order by aggregate_history_order asc")
    public abstract List<AggregateData> getAggregateHistory();

    //特定の集計履歴を取得
    @Query("select * from history_aggregate where aggregate_history_order = :order")
    public abstract AggregateData getAggregateHistoryByOrder(int order);

    //最新の集計履歴を取得
    @Query("select * from history_aggregate where aggregate_history_order = 0")
    public abstract AggregateData getCurrentAggregate();

    //集計期間終了時刻を挿入
    @Query("update history_aggregate set aggregate_end_datetime = :datetime where aggregate_history_order = 0")
    public abstract void updateAggregateEnd(String datetime);

    //集計履歴順をインクリメント
    @Query("update history_aggregate set aggregate_history_order = aggregate_history_order + 1")
    abstract void updateAggregateHistoryOrder();

    //現在の集計履歴を削除
    @Query("delete from history_aggregate where aggregate_history_order = 0")
    public abstract void deleteNowHistory();

    //5回前の集計履歴を削除
    @Query("delete from history_aggregate where aggregate_history_order = 5")
    abstract void deleteOldHistory();

    //集計期間開始時刻を挿入
    @Query("insert into history_aggregate (aggregate_history_order, aggregate_start_datetime) values (0, :datetime)")
    abstract void createData(String datetime);

    //最新の集計履歴を確認し、データがなければ集計期間開始時刻を挿入
    @Transaction
    public void insertAggregateStart(String datetime) {
        AggregateData data = getCurrentAggregate();
        if (data == null) {
            createData(datetime);
        }
    }

    //5回前の集計履歴を削除して集計履歴順をインクリメント
    @Transaction
    public void updateAggregateHistory() {
        deleteOldHistory();
        updateAggregateHistoryOrder();
    }
}
