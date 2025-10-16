package jp.mcapps.android.multi_payment_terminal.database.history.slip;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.TransHead;

@Dao
public abstract class SlipDao {
    //取引履歴一覧表示に必要なカラムを取得
    @Transaction
    @Query("select id, trans_brand, trans_type, trans_result, trans_amount, trans_date, term_sequence from history_slip order by trans_date desc")
    public abstract List<TransHead> getTransHead();

    //最新の印字を1件取得
    @Query("select * from history_slip order by trans_date desc limit 1")
    public abstract SlipData getLatestOne();

    //特定のIDを持つ印字を1件取得
    @Query("select * from history_slip where id = :id")
    public abstract SlipData getOneById(int id);

    //日計印字前の履歴を取得
    @Transaction
    @Query("select * from history_slip where old_aggregate_order = 0")
    public abstract List<SlipData> getAggregate();

    //日計印字順を指定して履歴を取得※取引結果：失敗を除く
    @Transaction
    @Query("select * from history_slip where old_aggregate_order = :cnt and trans_result != 1")
    public abstract List<SlipData> getAggregate(int cnt);

    //印字回数を更新
    @Query("update history_slip set print_cnt = print_cnt + 1 where id = :id")
    public abstract void updatePrintCnt(int id);

    //日計印字順が5のデータを削除
    @Query("delete from history_slip where old_aggregate_order = 5")
    abstract void deleteByOldAggregateOrder();

    //指定された取引日時、機器通番のデータを削除
    @Query("delete from history_slip where trans_date = :date and term_sequence = :seq")
    public abstract void deleteSpecifiedData(String date, int seq);

    //全データの日計印字順をインクリメント
    @Query("update history_slip set old_aggregate_order = old_aggregate_order + 1, cancel_flg = null")
    abstract void updateOldAggregateOrder();

    //全データの日計印字順をインクリメント(POS用：キャンセルフラグは更新しない)
    @Query("update history_slip set old_aggregate_order = old_aggregate_order + 1")
    abstract void updateOldAggregateOrderStayCancelFlg();

    //transTypeCodeを更新
    @Query("update history_slip set trans_type_code = :code where id = :id")
    abstract void updateTransTypeCode(int id, String code);

    //全件削除
    @Query("delete from history_slip")
    public abstract void deleteAll();

    //取消可能な取引を検索
    @Query("select id from history_slip where cancel_flg is not null order by trans_date desc limit 1")
    abstract Integer getLatestCancelableOrder();

    //取消不可に変更
    @Query("update history_slip set cancel_flg = null where id = :id")
    public abstract void updateCancelUriId(int id);

    //5回前の日計で印字したデータを削除し、その他のデータの日計印字順をインクリメント
    @Transaction
    public void updateTableAfterAggregate() {
        deleteByOldAggregateOrder();

        if (AppPreference.isServicePos() || AppPreference.isServiceTicket()) {
            // POSまたTICKETの場合、全データの日計印字順をインクリメントし、取消不可にしない
            updateOldAggregateOrderStayCancelFlg();
        } else {
            // TAXIの場合、全データの日計印字順をインクリメントし、取消不可にする
            updateOldAggregateOrder();
        }
    }

    //ひとつ前の取消を無効にして印字履歴を挿入
    @Transaction
    public long insertSlipData(SlipData slipData) {

        if (AppPreference.isServicePos() || AppPreference.isServiceTicket()) {
            // POSまたTICKETの場合、ひとつ前の取引を取消不可にしない
        } else {
            // TAXIの場合、ひとつ前の取引を取消不可にする
/*
            Integer id = getLatestCancelableOrder();
            if (id != null) updateCancelUriId(id);
*/
        }
        return insert(slipData);
    }

    //印字履歴の挿入
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract long insert(SlipData slipData);

    //カード番号マスク更新
    @Query("update history_slip set card_id_merchant = :mask_card_id where id = :id")
    public abstract void updateMaskCardId(int id, String mask_card_id);

    // 登録されているすべてのidを取得
    @Query("select id from history_slip")
    public abstract List<Integer> getIds();

    // チケット販売IDと便予約IDを設定更新※チケット販売のみ使用
    @Query("update history_slip set purchased_ticket_deal_id = :purchased_ticket_deal_id, trip_reservation_id = :trip_reservation_id where id = :id")
    public abstract void updateTicketData(int id, String purchased_ticket_deal_id, String trip_reservation_id);

    // 未送信のチケット購入の取消を全件取得
    @Query("select * from history_slip where send_cancel_purchased_ticket = 1")
    public abstract List<SlipData> getUnsentCancelPurchasedTicketData();

    // チケット購入の取消を送信済み更新※ID指定
    @Query("update history_slip set send_cancel_purchased_ticket = 0 where id = :id")
    public abstract void updateSentCancelPurchasedTicketData(int id);
}
