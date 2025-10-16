package jp.mcapps.android.multi_payment_terminal.database.pos;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;
import jp.mcapps.android.multi_payment_terminal.database.history.uriOkica.UriOkicaData;
import jp.mcapps.android.multi_payment_terminal.util.JSON;

@Entity(tableName = "pos_transactions_detail")
public class TransactionDetailData implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo
    public long payment_transaction_id;

    @ColumnInfo
    public Date transaction_at;

    @ColumnInfo
    public long product_id;

    @ColumnInfo
    public String product_code;

    @ColumnInfo
    public String product_name;

    @ColumnInfo
    public Integer unit_price;

    @ColumnInfo
    public Integer count;

    @ColumnInfo
    public Integer product_tax_type;

    @ColumnInfo
    public Integer reduced_tax_type;

    @ColumnInfo
    public Integer included_tax_type;

    @ColumnInfo
    public Boolean is_manual;

    @ColumnInfo
    public Integer org_unit_price;

    @ColumnInfo
    public String cart_data_json;

    public TransactionDetailData() {}

    // データ作成コンストラクタ
    public TransactionDetailData(UriData uriData, CartData cart, long transactionId) {
        payment_transaction_id = transactionId;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            transaction_at = sdf.parse(uriData.transDate);
        }catch (ParseException e) {
            throw new IllegalArgumentException("UriData の項目 transDate が不正");
        }
        if(cart.product_id == null){
            // 手動明細
            product_id = 0;
        }else{
            product_id = cart.product_id;
        }
        product_code = cart.product_code;
        product_name = cart.product_name;
        count = cart.count;
        product_tax_type = cart.tax_type;
        reduced_tax_type = cart.reduce_tax_type;
        included_tax_type = cart.included_tax_type;
        is_manual = cart.is_manual;
        if(cart.is_custom_price) {
            unit_price = cart.custom_unit_price;       // 変更後金額
            org_unit_price = cart.standard_unit_price; // 変更前金額
        }else{
            unit_price = cart.standard_unit_price;
            org_unit_price = null;
        }
        cart_data_json = JSON.stringify(cart);
    }

    // データ作成コンストラクタ（UriOkicaDataから作成）
    public TransactionDetailData(UriOkicaData uriOkicaData, CartData cart, long transactionId){
        payment_transaction_id = transactionId;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            transaction_at = sdf.parse(uriOkicaData.okicaTransDate);
        }catch (ParseException e) {
            throw new IllegalArgumentException("UriData の項目 transDate が不正");
        }
        if(cart.product_id == null){
            // 手動明細
            product_id = 0;
        }else{
            product_id = cart.product_id;
        }
        product_code = cart.product_code;
        product_name = cart.product_name;
        count = cart.count;
        product_tax_type = cart.tax_type;
        reduced_tax_type = cart.reduce_tax_type;
        included_tax_type = cart.included_tax_type;
        is_manual = cart.is_manual;
        if(cart.is_custom_price) {
            unit_price = cart.custom_unit_price;       // 変更後金額
            org_unit_price = cart.standard_unit_price; // 変更前金額
        }else{
            unit_price = cart.standard_unit_price;
            org_unit_price = null;
        }
        cart_data_json = JSON.stringify(cart);
    }

    // データ作成コンストラクタ（UriDataなしで作成）
    public TransactionDetailData(String transDate, CartData cart, long transactionId){
        payment_transaction_id = transactionId;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            transaction_at = sdf.parse(transDate);
        }catch (ParseException e) {
            throw new IllegalArgumentException("UriData の項目 transDate が不正");
        }
        if(cart.product_id == null){
            // 手動明細
            product_id = 0;
        }else{
            product_id = cart.product_id;
        }
        product_code = cart.product_code;
        product_name = cart.product_name;
        count = cart.count;
        product_tax_type = cart.tax_type;
        reduced_tax_type = cart.reduce_tax_type;
        included_tax_type = cart.included_tax_type;
        is_manual = cart.is_manual;
        if(cart.is_custom_price) {
            unit_price = cart.custom_unit_price;       // 変更後金額
            org_unit_price = cart.standard_unit_price; // 変更前金額
        }else{
            unit_price = cart.standard_unit_price;
            org_unit_price = null;
        }
        cart_data_json = JSON.stringify(cart);
    }
}
