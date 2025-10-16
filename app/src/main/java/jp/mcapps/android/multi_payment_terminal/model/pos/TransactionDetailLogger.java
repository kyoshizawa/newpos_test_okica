package jp.mcapps.android.multi_payment_terminal.model.pos;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;
import jp.mcapps.android.multi_payment_terminal.database.history.uriOkica.UriOkicaData;
import jp.mcapps.android.multi_payment_terminal.database.pos.CartDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.CartData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TransactionData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TransactionDetailDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TransactionDetailData;
import timber.log.Timber;

// POS取引明細情報の記録用モデル
public class TransactionDetailLogger {

    private List<TransactionDetailData> _transactionDetailDatas = null;

    private TransactionDetailDao _transactionDetailDao = null;
    private CartDao _cartDao = null;

    // コンストラクタ
    public TransactionDetailLogger () {
        LocalDatabase db = LocalDatabase.getInstance();
        _cartDao = db.cartDao();
        _transactionDetailDao = db.transactionDetailDao();
    }

    // UriDataを元に取引を作成
    public void CreateByUriData(UriData uriData, long transactionId) {
        List<CartData> carts = _cartDao.getAllProduct();

        _transactionDetailDatas = new ArrayList<TransactionDetailData>();
        for (CartData item : carts) {
            _transactionDetailDatas.add(new TransactionDetailData(uriData, item, transactionId));
        }
    }

    // UriOkicaDataを元に取引を作成
    public void CreateByUriOkicaData(UriOkicaData uriOkicaData, long transactionId) {
        List<CartData> carts = _cartDao.getAllProduct();

        _transactionDetailDatas = new ArrayList<TransactionDetailData>();
        for (CartData item : carts) {
            _transactionDetailDatas.add(new TransactionDetailData(uriOkicaData, item, transactionId));
        }
    }

    // UriData無しで取引を作成
    public void Create(String transDate, long transactionId){
        List<CartData> carts = _cartDao.getAllProduct();

        _transactionDetailDatas = new ArrayList<TransactionDetailData>();
        for (CartData item : carts) {
            _transactionDetailDatas.add(new TransactionDetailData(transDate, item, transactionId));
        }
    }

    // DB格納
    public void Insert(){
        if (_transactionDetailDatas == null || _transactionDetailDatas.size() == 0) {
            Timber.i("Insert を行う TransactionDetailData インスタンスがありません");
            return;
//            throw new IllegalStateException("Insert を行う TransactionDetailData インスタンスがありません。");
        }

        _transactionDetailDao.insertTransactionDetails(_transactionDetailDatas);
    }
}
