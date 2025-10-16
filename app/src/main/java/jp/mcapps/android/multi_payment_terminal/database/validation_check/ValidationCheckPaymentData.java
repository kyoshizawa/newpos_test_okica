package jp.mcapps.android.multi_payment_terminal.database.validation_check;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "payment_validation_check")
public class ValidationCheckPaymentData {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "payment_id")
    public long paymentId;

    @ColumnInfo(name = "pos_send")
    public int posSend; //送信状態

    @ColumnInfo(name = "term_id")
    public String termId; //端末番号

    @ColumnInfo(name = "car_id")
    public int carId; //車番

    @ColumnInfo(name = "driver_id")
    public int driverId; //乗務員コード

    @ColumnInfo(name = "driver_name")
    public String driverName; //乗務員名

    @ColumnInfo(name = "term_sequence")
    public int termSequence; //機器通番

    @ColumnInfo(index = true, name = "operation_date")
    public String operationDate; //操作日時

    @ColumnInfo(name = "term_result")
    public int termResult; //処理結果

    @ColumnInfo(name = "invalid_reason")
    public String invalidReason; // 無効理由

    @ColumnInfo(name = "validation_id")
    public String validationId; //有効性確認ID

    @ColumnInfo(name = "error_code")
    public String errorCode; //エラーコード

    @ColumnInfo(name = "trans_amount")
    public int transAmount; //取引金額

    @ColumnInfo(name = "term_latitude")
    public String termLatitude; //位置情報(緯度)

    @ColumnInfo(name = "term_longitude")
    public String termLongitude; //位置情報(経度)

    @ColumnInfo(name = "term_network_type")
    public String termNetworkType; //ネットワーク種別

    @ColumnInfo(name = "term_radio_level")
    public Integer termRadioLevel; //電波状況(レベル)
}
