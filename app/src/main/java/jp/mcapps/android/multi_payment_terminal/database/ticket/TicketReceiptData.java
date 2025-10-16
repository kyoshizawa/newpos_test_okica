package jp.mcapps.android.multi_payment_terminal.database.ticket;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.data.MoneyType;

@Entity(tableName = "ticket_receipts",
        indices = {
                @Index(value = {"slip_id"}, unique = true)
        })
public class TicketReceiptData implements Serializable{
    @PrimaryKey(autoGenerate = true)
    public int id;

    // 取引履歴ID
    @ColumnInfo
    @NotNull
    public Integer slip_id = 0;

    // 加盟店名
    @ColumnInfo
    @NotNull
    public String merchant_name = "";

    // 営業所名
    @ColumnInfo
    @NotNull
    public String organization_name = "";

    // 号機番号
    @ColumnInfo
    @NotNull
    public String car_no = "";

     // 電話番号
    @ColumnInfo
    @NotNull
    public String phone_number = "";

     // 住所
    @ColumnInfo
    @NotNull
    public String address = "";

    // 係員番号
    @ColumnInfo
    @NotNull
    public String staff_code = "";

     // 機器通番（端末通番）
    @ColumnInfo
    @NotNull
    public String term_sequence = "";

    // 決済日時
    @ColumnInfo
    @NotNull
    public String trans_date = "";

    // 機器通番（取消元）
    @ColumnInfo
    @NotNull
    public String old_term_sequence = "";

    // 決済日時（取消元）
    @ColumnInfo
    @NotNull
    public String old_trans_date = "";

    // 端末番号
    @ColumnInfo
    @NotNull
    public String terminal_number = "";

    // 合計金額
    @ColumnInfo
    @NotNull
    public Integer total_amount = 0;

    // 支払種別
    @ColumnInfo
    @NotNull
    public String money_type = "";

    // 支払種別の金額
    @ColumnInfo
    @NotNull
    public Integer trans_type_amount = 0;

    // 現金で支払った金額（現金併用 or 現金決済）
    @ColumnInfo
    @NotNull
    public Integer trans_cash_amount = 0;

    // お釣り
    @ColumnInfo
    @NotNull
    public Integer change_amount = 0;

    // インボイス番号
    @ColumnInfo
    @NotNull
    public String invoice_no = "";

    // 再印刷回数
    @ColumnInfo
    @NotNull
    public Integer print_cnt = 0;

    // 取消フラグ
    @ColumnInfo
    public Integer canceled_trans = 0;

    // チケット情報
    @ColumnInfo
    @NotNull
    public String ticket_detail = "";

    // 小計情報
    @ColumnInfo
    @NotNull
    public String subtotal_detail = "";

    public TicketReceiptData() {}

    /**
     * データ作成コンストラクタ
     * @param moneyType 支払種別
     * @param slipId 伝票番号
     * @param termSequence 機器通番
     * @param transDate 決済日時
     * @param oldTermSequence 機器通番（取消元）
     * @param oldTransDate 決済日時（取消元）
     * @param totalAmount 合計金額
     * @param transTypeAmount 支払種別の金額
     * @param transCashAmount 現金で支払った金額
     * @param changeAmount お釣り
     */
    public TicketReceiptData(
            MoneyType moneyType,
            int slipId,
            Integer termSequence,
            String transDate,
            Integer oldTermSequence,
            String oldTransDate,
            Integer totalAmount,
            Integer transTypeAmount,
            Integer transCashAmount,
            Integer changeAmount) {
        slip_id = slipId;
        merchant_name = AppPreference.getMerchantName();
        organization_name = AppPreference.getMerchantOffice();
        car_no = String.valueOf(AppPreference.getMcCarId());
        phone_number = AppPreference.getMerchantTelnumber();
        staff_code = String.valueOf(AppPreference.getMcDriverId());
        term_sequence = termSequence.toString();
        trans_date = transDate;
        old_term_sequence = oldTermSequence.toString();
        old_trans_date = oldTransDate;
        terminal_number = AppPreference.getMcTermId();
        total_amount = totalAmount;
        money_type = moneyType.toString();
        trans_type_amount = transTypeAmount;
        trans_cash_amount = transCashAmount;
        change_amount = changeAmount;
        ticket_detail = "";
        subtotal_detail = "";
        invoice_no = AppPreference.getInvoiceNo();
    }
}
