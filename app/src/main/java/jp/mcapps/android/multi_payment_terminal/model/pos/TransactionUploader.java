package jp.mcapps.android.multi_payment_terminal.model.pos;

import android.annotation.SuppressLint;

import com.google.common.base.Strings;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import jp.mcapps.android.multi_payment_terminal.data.pos.CardCategory;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.pos.CartData;
import jp.mcapps.android.multi_payment_terminal.database.pos.IncludedTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ProductTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReducedTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.TransactionDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TransactionData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TransactionDetailDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TransactionDetailData;
import jp.mcapps.android.multi_payment_terminal.util.JSON;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.PaypfPosApi;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.PaypfPosApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos.data.CreateManyTransaction;
import timber.log.Timber;

public class TransactionUploader {

    public TransactionUploader() {
        transactionDao = LocalDatabase.getInstance().transactionDao();
        transactionDetailDao = LocalDatabase.getInstance().transactionDetailDao();
    }

    // network
    private final PaypfPosApi _posApiClient = PaypfPosApiImpl.getInstance();

    // dao
    private final TransactionDao transactionDao;
    private final TransactionDetailDao transactionDetailDao;

    public void uploadTransactions() throws IOException {

        final LocalDatabase db = LocalDatabase.getInstance();
        final int uploadLimit = 5;

        // 送信済みデータをクリーンアップ
        db.runInTransaction(() -> cleanupTransactions());

        // 取引データを送信する
        for (int i=0; true; i++) {

            // 取引データを取得する
            final int uploadOffset = i * uploadLimit;
            TransactionData[] transData = transactionDao.getTransactionsToUpload(uploadLimit, uploadOffset);

            // データなし
            if (transData.length == 0) {
                break;
            }

            // 取引明細を取得する
            final List<Long> transIDs = new ArrayList<>();
            for (TransactionData it : transData) {
                transIDs.add(it.id);
            }
            TransactionDetailData[] transDetails = transactionDetailDao.getTransactionDetailsByTransactionIDs(transIDs);

            // サーバーに送信
            CreateManyTransaction.Request req = new CreateManyTransaction.Request();
            req.items = createRequestItems(transData, transDetails);
            CreateManyTransaction.Response resp = _posApiClient.createTransactions(req);

            // 送信済みにする（OKなやつだけ）
            List<Long> succeededIDs = new ArrayList<>();
            for (CreateManyTransaction.ResponseTransactionResult it: resp.items) {
                if (it.error != null && it.error.code != 0) {
                    // NG (error)
                    Timber.e("(paypf) transaction '%s' is failed to upload: (code: %s) %s", it.transaction_no, it.error.code, it.error.message);

                } else {
                    // OK (Success)
                    Timber.d("(paypf) transaction '%s' is successfully uploaded", it.transaction_no);

                    for (TransactionData trans : transData) {
                        if (Objects.equals(trans.transaction_no, it.transaction_no)) {
                            succeededIDs.add(trans.id);
                            break;
                        }
                    }
                }
            }
            if (0 < succeededIDs.size()) {
                transactionDao.updateTransactionsToUploaded(succeededIDs);
            }
        }
    }

    private void cleanupTransactions() {
        // 送信済みデータ取得
        TransactionData[] deleting = transactionDao.getTransactionsToDelete();
        List<Long> ids = new ArrayList<>();
        for (TransactionData it : deleting) {
            ids.add(it.id);
        }
        if (ids.size() == 0) {
            return;
        }
        // 取引明細削除
        transactionDetailDao.deleteTransactionDetailsByTransactionIDs(ids);
        // 取引データ削除
        transactionDao.deleteTransactionsByIDs(ids);
    }

    private CreateManyTransaction.RequestTransactionItem[] createRequestItems(TransactionData[] transData, TransactionDetailData[] transDetails) {

        @SuppressLint("SimpleDateFormat")
        final SimpleDateFormat formatter =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        // リクエストデータに変換する
        // - 取引明細データ
        final HashMap<Long, List<CreateManyTransaction.RequestTransactionDetail>> detailDict = new HashMap<>();
        for (TransactionDetailData it : transDetails) {
            if (it.count > 0) { // 販売個数=1以上のデータをリクエストデータに含める
                CreateManyTransaction.RequestTransactionDetail detail = new CreateManyTransaction.RequestTransactionDetail();

                detail.product_id = it.product_id;
                detail.product_code = it.product_code;
                detail.product_name = it.product_name;
                detail.product_tax_type = ProductTaxTypes.valueOf(it.product_tax_type).key;
                detail.reduced_tax_type = ReducedTaxTypes.valueOf(it.reduced_tax_type).key;
                detail.included_tax_type = IncludedTaxTypes.valueOf(it.included_tax_type).key;
                detail.unit_price = it.unit_price;
                detail.count = it.count;
                detail.is_manual = it.is_manual;
                detail.org_unit_price = it.org_unit_price;

                if (!Strings.isNullOrEmpty(it.cart_data_json)) {
                    // カートデータがある場合は、JSONをパースして追加する
                    CartData cart = JSON.parse(it.cart_data_json, CartData.class);

                    // バーコード情報
                    if (!Strings.isNullOrEmpty(cart.barcode_type)) {
                        detail.barcode_info = new CreateManyTransaction.RequestBarcodeInformation();
                        detail.barcode_info.barcode_type = cart.barcode_type;
                        detail.barcode_info.scanned_content = cart.barcode_text;
                    }
                }

                List<CreateManyTransaction.RequestTransactionDetail> list = detailDict.get(it.payment_transaction_id);
                if (list == null) {
                    list = new ArrayList<>();
                    detailDict.put(it.payment_transaction_id, list);
                }
                list.add(detail);
            }
        }
        // - 取引データ
        final List<CreateManyTransaction.RequestTransactionItem> items = new ArrayList<>();
        for (TransactionData it : transData) {
            CreateManyTransaction.RequestTransactionItem item = new CreateManyTransaction.RequestTransactionItem();

            item.service_instance_id = it.service_instance_id;
            item.transaction_type = it.transaction_type;
            item.transaction_at = formatter.format(it.transaction_at);
            item.tid = it.tid;
            item.terminal_id = it.terminal_id;
            item.terminal_name = it.terminal_name;
            item.merchant_id = it.merchant_id;
            item.tenant_id = it.tenant_id;
            item.tenant_code = it.tenant_code;
            item.tenant_name = it.tenant_name;
            item.transaction_no = it.transaction_no;
            if (!Strings.isNullOrEmpty(it.card_category)) {
                item.card_category = CardCategory.valueOf(it.card_category).getInt();
            }
            item.card_brand = it.card_brand;
            item.amount = it.amount.longValue();
            item.amount_cash = it.cash_amount.longValue();
            item.staff_code = it.staff_code;
            item.staff_name = it.staff_name;
            if (!Strings.isNullOrEmpty(it.org_transaction_id)) {
                // 取消のみ
                item.org_transaction_no = it.org_transaction_id;
                item.org_transaction_at = formatter.format(it.org_transaction_at);
            }
            item.is_unexecuted = it.is_unexecuted;

            List<CreateManyTransaction.RequestTransactionDetail> details = detailDict.get(it.id);
            if (details != null) {
                item.transaction_details = details.toArray(new CreateManyTransaction.RequestTransactionDetail[0]);
            }

            items.add(item);
        }

        return items.toArray(items.toArray(new CreateManyTransaction.RequestTransactionItem[0]));
    }
}
