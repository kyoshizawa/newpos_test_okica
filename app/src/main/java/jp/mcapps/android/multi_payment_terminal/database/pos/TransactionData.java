package jp.mcapps.android.multi_payment_terminal.database.pos;

import android.os.Build;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.data.pos.CardBrand;
import jp.mcapps.android.multi_payment_terminal.data.pos.CardCategory;
import jp.mcapps.android.multi_payment_terminal.data.pos.TransactionType;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.AmountParam;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.RefundParam;
import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;
import jp.mcapps.android.multi_payment_terminal.database.history.uriOkica.UriOkicaData;
import jp.mcapps.android.multi_payment_terminal.util.DeviceUtils;
import timber.log.Timber;

@Entity(tableName = "pos_transactions")
public class TransactionData implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo
    public String service_instance_id;

    @ColumnInfo
    public Integer transaction_type;

    @ColumnInfo
    public Date transaction_at;

    @ColumnInfo
    public String tid;

    @ColumnInfo
    public Long terminal_id;

    @ColumnInfo
    public String terminal_name;

    @ColumnInfo
    public Long merchant_id;

    @ColumnInfo
    public Long tenant_id;

    @ColumnInfo
    public String tenant_code;

    @ColumnInfo
    public String tenant_name;

    @ColumnInfo
    public String transaction_no;

    @ColumnInfo
    public String card_category;

    @ColumnInfo
    public String card_brand;

    @ColumnInfo
    public Integer amount;

    @ColumnInfo
    public Integer cash_amount;

    @ColumnInfo
    public String staff_code;

    @ColumnInfo
    public String staff_name;

    @ColumnInfo
    public Date org_transaction_at;

    @ColumnInfo
    public String org_transaction_id;

    @ColumnInfo
    public boolean is_unexecuted;

    // TODO ... 取引データに必要な情報はここに追加

    @ColumnInfo
    public boolean uploaded;

    public TransactionData() {}

    // データ作成コンストラクタ（UriDataから作成）
    public TransactionData(UriData uriData, TenantData tenant, TerminalData terminal, CardCategory category, CardBrand brand){
        service_instance_id = tenant.service_instance_id;
        if (uriData.transType == TransMap.TYPE_SALES) { // 支払の場合
            transaction_type = TransactionType.PROCEEDS.getInt();
        } else if (uriData.transType == TransMap.TYPE_CANCEL) { // 取消の場合
            transaction_type = TransactionType.CANCEL.getInt();
        }
        is_unexecuted = uriData.transResult == TransMap.RESULT_UNFINISHED; // 未了
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            transaction_at = sdf.parse(uriData.transDate);
        }catch (ParseException e) {
            throw new IllegalArgumentException("UriData の項目 transDate が不正");
        }
        tid = terminal.terminal_no;
        terminal_id = Long.parseLong(terminal.terminal_id);
        terminal_name = DeviceUtils.getSerial();
        merchant_id = tenant.merchant_id;
        tenant_id = tenant.tenant_id;
        tenant_code = tenant.tenant_code;
        tenant_name = tenant.name;
        transaction_no = String.valueOf(uriData.termSequence);
        card_category = category.name();
        card_brand = brand.get();
        amount = uriData.transAmount + uriData.transCashTogetherAmount;
        cash_amount = uriData.transCashTogetherAmount;
        staff_code = AppPreference.getPosStaffCode();
        staff_name = ""; // 使用されない
        if (uriData.transType == TransMap.TYPE_CANCEL) { // 取消の場合
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                if (uriData.oldTransDate != null) {
                    org_transaction_at = sdf.parse(uriData.oldTransDate);
                }
            }catch (ParseException e) {
                throw new IllegalArgumentException("UriData の項目 oldTransDate が不正");
            }
            org_transaction_id = String.valueOf(uriData.oldTermSequence);
        }
    }

    // データ作成コンストラクタ（UriOkicaDataから作成）
    public TransactionData(UriOkicaData uriOkicaData, RefundParam refundParam, Integer result, TenantData tenant, TerminalData terminal){
        service_instance_id = tenant.service_instance_id;
        if (uriOkicaData.okicaProcessType == 0x46) { // 支払の場合
            transaction_type = TransactionType.PROCEEDS.getInt();
        } else if (uriOkicaData.okicaProcessType == 0x4C) { // 取消の場合
            transaction_type = TransactionType.CANCEL.getInt();
        }
        is_unexecuted = result == TransMap.RESULT_UNFINISHED; // 未了
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            transaction_at = sdf.parse(uriOkicaData.okicaTransDate);
        }catch (ParseException e) {
            throw new IllegalArgumentException("UriData の項目 transDate が不正");
        }
        tid = terminal.terminal_no;
        terminal_id = Long.parseLong(terminal.terminal_id);
        terminal_name = DeviceUtils.getSerial();
        merchant_id = tenant.merchant_id;
        tenant_id = tenant.tenant_id;
        tenant_code = tenant.tenant_code;
        tenant_name = tenant.name;
        transaction_no = String.valueOf(uriOkicaData.okicaSequence);
        card_category = CardCategory.EMONEY_TRANSPORTATION.name();
        card_brand = CardBrand.EMoneyTransportation.OKICA.name();
        amount = uriOkicaData.transAmount + uriOkicaData.transCashTogetherAmount;
        cash_amount = uriOkicaData.transCashTogetherAmount;
        staff_code = AppPreference.getPosStaffCode();
        staff_name = ""; // 使用されない
        if (uriOkicaData.okicaProcessType == 0x4C) { // 取消の場合
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                org_transaction_at = sdf.parse(refundParam.oldTransDate);
            }catch (ParseException e) {
                throw new IllegalArgumentException("RefundParam の項目 oldTransDate が不正");
            }
            org_transaction_id = String.valueOf(refundParam.oldTermSequence);
        }
    }

    // データ作成コンストラクタ（現金用）
    public TransactionData(boolean isRepay, String transDate, String termSequence, AmountParam amountParam, RefundParam refundParam, TenantData tenant, TerminalData terminal, String categoryName){
        service_instance_id = tenant.service_instance_id;
        if (!isRepay) { // 支払の場合
            transaction_type = TransactionType.PROCEEDS.getInt();
        } else { // 取消の場合
            transaction_type = TransactionType.CANCEL.getInt();
        }
        is_unexecuted = false; // 未了なし
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            transaction_at = sdf.parse(transDate);
        }catch (ParseException e) {
            throw new IllegalArgumentException("transDate が不正");
        }
        tid = terminal.terminal_no;
        terminal_id = Long.parseLong(terminal.terminal_id);
        terminal_name = DeviceUtils.getSerial();
        merchant_id = tenant.merchant_id;
        tenant_id = tenant.tenant_id;
        tenant_code = tenant.tenant_code;
        tenant_name = tenant.name;
        transaction_no = termSequence;
        card_category = categoryName;
        card_brand = "";
        amount = amountParam.transAmount;
        cash_amount = amountParam.transAmount;
        staff_code = AppPreference.getPosStaffCode();
        staff_name = ""; // 使用されない
        if (isRepay) { // 取消の場合
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                org_transaction_at = sdf.parse(refundParam.oldTransDate);
            }catch (ParseException e) {
                throw new IllegalArgumentException("RefundParam の項目 oldTransDate が不正");
            }
            org_transaction_id = String.valueOf(refundParam.oldTermSequence);
        }
    }

}
