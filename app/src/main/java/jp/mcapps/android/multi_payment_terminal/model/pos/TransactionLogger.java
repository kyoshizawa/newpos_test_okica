package jp.mcapps.android.multi_payment_terminal.model.pos;

import java.util.Date;

import jp.mcapps.android.multi_payment_terminal.data.MoneyType;
import jp.mcapps.android.multi_payment_terminal.data.pos.CardBrand;
import jp.mcapps.android.multi_payment_terminal.data.pos.CardCategory;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.AmountParam;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.RefundParam;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;
import jp.mcapps.android.multi_payment_terminal.database.history.uriOkica.UriOkicaData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TenantDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TenantData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TerminalData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TransactionDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TransactionData;

// POS取引情報の記録用モデル
public class TransactionLogger {

    private TransactionData _transactionData = null;

    private TransactionDao _transactionDao = null;
    private TenantDao _tenantDao = null;
    private TerminalDao _terminalDao = null;

    public TransactionLogger() {
        LocalDatabase db = LocalDatabase.getInstance();
        _tenantDao = db.tenantDao();
        _terminalDao = db.terminalDao();
        _transactionDao = db.transactionDao();
    }

    // UriDataを元に取引を作成 cardPrefixは交通系でしか使用しないので注意
    public void CreateByUriData(UriData uriData, MoneyType moneyType, String cardPrefix) {
        TenantData tenantData = _tenantDao.getTenant();
        TerminalData terminalData = _terminalDao.getTerminal();

        CardCategory category = null;
        CardBrand brand = new CardBrand();
        if (moneyType == MoneyType.CREDIT) {
            CardBrand.Credit cre_brand = CardBrand.Credit.Convert(uriData.creditBrandId);
            category = CardCategory.CREDIT;
            brand.set(cre_brand);
        } else if (moneyType == MoneyType.UNIONPAY){
            category = CardCategory.CREDIT;
            brand.set(CardBrand.Credit.GINREN);
        } else if (moneyType == MoneyType.ID) {
            category = CardCategory.EMONEY_COMMERCIAL;
            brand.set(CardBrand.EMoneyCommercial.ID);
        } else if (moneyType == MoneyType.QUICKPAY) {
            category = CardCategory.EMONEY_COMMERCIAL;
            brand.set(CardBrand.EMoneyCommercial.QUICKPAY);
        } else if (moneyType == MoneyType.EDY) {
            category = CardCategory.EMONEY_COMMERCIAL;
            brand.set(CardBrand.EMoneyCommercial.EDY);
        } else if (moneyType == MoneyType.NANACO) {
            category = CardCategory.EMONEY_COMMERCIAL;
            brand.set(CardBrand.EMoneyCommercial.NANACO);
        } else if (moneyType == MoneyType.WAON) {
            category = CardCategory.EMONEY_COMMERCIAL;
            brand.set(CardBrand.EMoneyCommercial.WAON);
        } else if (moneyType == MoneyType.JR) {
            CardBrand.EMoneyTransportation et_brand = CardBrand.EMoneyTransportation.Convert(cardPrefix);
            category = CardCategory.EMONEY_TRANSPORTATION;
            brand.set(et_brand);
        } else if (moneyType == MoneyType.QR) {
            CardBrand.QR qr_brand = CardBrand.QR.Convert(uriData.codetransPayTypeCode);
            category = CardCategory.QR;
            brand.set(qr_brand);
        }
        _transactionData = new TransactionData(uriData, tenantData, terminalData, category, brand);
    }

    // UriOkicaDataを元に取引を作成
    public void CreateByUriOkicaData(UriOkicaData uriOkicaData, RefundParam refundParam, Integer result) {
        if (uriOkicaData.okicaProcessType == 0x4C && refundParam == null) { // 取消の場合は必須
            throw new IllegalStateException("取消の場合に CreateByUriOkicaData を使用する場合は refundParam が必須です。");
        }

        TenantData tenantData = _tenantDao.getTenant();
        TerminalData terminalData = _terminalDao.getTerminal();
        _transactionData = new TransactionData(uriOkicaData, refundParam, result, tenantData, terminalData);
    }

    // 現金の取引を作成
    public void CreateCash(boolean isRepay, String transDate, String termSequence, AmountParam amountParam, RefundParam refundParam){
        if (isRepay && refundParam == null) { // 取消の場合
            throw new IllegalStateException("取消の場合に CreateCash を使用する場合は refundParam が必須です。");
        }

        TenantData tenantData = _tenantDao.getTenant();
        TerminalData terminalData = _terminalDao.getTerminal();

        _transactionData = new TransactionData(isRepay, transDate, termSequence, amountParam, refundParam, tenantData, terminalData, "");

    }

    // 為替類の取引を作成
    public void CreatePostalOrder(boolean isRepay, String transDate, String termSequence, AmountParam amountParam, RefundParam refundParam){
        if (isRepay && refundParam == null) { // 取消の場合
            throw new IllegalStateException("取消の場合に CreateCash を使用する場合は refundParam が必須です。");
        }

        TenantData tenantData = _tenantDao.getTenant();
        TerminalData terminalData = _terminalDao.getTerminal();

        // 為替類の場合はカテゴリ名を設定
        String categoryName = CardCategory.POSTAL_ORDER.name();
        _transactionData = new TransactionData(isRepay, transDate, termSequence, amountParam, refundParam, tenantData, terminalData, categoryName);
    }

    // DB格納
    public long Insert(){
        if (_transactionData == null) {
            throw new IllegalStateException("Insert を行う TransactionData インスタンスがありません。");
        }

        return _transactionDao.insertTransaction(_transactionData);
    }
}
