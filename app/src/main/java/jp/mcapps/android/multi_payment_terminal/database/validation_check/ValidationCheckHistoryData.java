package jp.mcapps.android.multi_payment_terminal.database.validation_check;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history_validation_check")
public class ValidationCheckHistoryData {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "history_id")
    public long historyId;

    @ColumnInfo(name = "validation_name")
    public String validationName; //名称

    @ColumnInfo(name = "print_flg")
    public Integer printFlg; //印刷可否

    @ColumnInfo(name = "print_cnt")
    public Integer printCnt; //印刷回数

    @ColumnInfo(name = "merchant_name")
    public String merchantName; //加盟店名

    @ColumnInfo(name = "merchant_office")
    public String merchantOffice; //加盟店営業所名

    @ColumnInfo(name = "merchant_telnumber")
    public String merchantTelnumber; //加盟店電話番号

    @ColumnInfo(name = "term_id")
    public String termId; //端末番号

    @ColumnInfo(name = "driver_id")
    public int driverId; //乗務員コード

    @ColumnInfo(name = "term_sequence")
    public int termSequence; //機器通番

    @ColumnInfo(index = true, name = "operation_date")
    public String operationDate; //操作日時

    @ColumnInfo(name = "term_result")
    public int termResult; //処理結果

    @ColumnInfo(name = "validation_id")
    public String validationId; //有効性確認ID

    @ColumnInfo(name = "error_code")
    public String errorCode; //エラーコード

    @ColumnInfo(name = "trans_amount")
    public int transAmount; //取引金額

    @ColumnInfo(name = "encrypt_type")
    public int encryptType; //暗号化パターン

    @ColumnInfo(name = "card_id")
    public String cardId; //カード番号
}
