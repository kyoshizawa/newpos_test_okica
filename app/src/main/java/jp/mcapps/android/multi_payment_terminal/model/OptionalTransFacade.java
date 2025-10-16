package jp.mcapps.android.multi_payment_terminal.model;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.data.MoneyType;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.AmountParam;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.RefundParam;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.ResultParam;
import jp.mcapps.android.multi_payment_terminal.database.Converters;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;
import jp.mcapps.android.multi_payment_terminal.database.history.uriOkica.UriOkicaData;
import jp.mcapps.android.multi_payment_terminal.database.pos.CartDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.CartData;
import jp.mcapps.android.multi_payment_terminal.database.pos.IncludedTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReceiptDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReceiptData;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReceiptProductDetail;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReceiptSubtotalDetail;
import jp.mcapps.android.multi_payment_terminal.database.pos.ReducedTaxTypes;
import jp.mcapps.android.multi_payment_terminal.database.pos.TaxCalcDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TaxCalcData;
import jp.mcapps.android.multi_payment_terminal.database.pos.TenantDao;
import jp.mcapps.android.multi_payment_terminal.database.pos.TenantData;
import jp.mcapps.android.multi_payment_terminal.model.pos.TransactionDetailLogger;
import jp.mcapps.android.multi_payment_terminal.model.pos.TransactionLogger;
import jp.mcapps.android.multi_payment_terminal.service.PosTransactionService;
import timber.log.Timber;

//　決済システムに送信する以外の取引データを作成するファザード
public class OptionalTransFacade {
    private MoneyType _moneyType = null;

    private UriData _uriData = null;
    private UriOkicaData _uriOkicaData = null;
    private ResultParam _resultParam = null;
    private AmountParam _amountParam = null;
    private RefundParam _refundParam = null;
    private String cardPrefix = "";

    // データ加工用の売上データを取得する
    public void setUriData(UriData uriData) {
        _uriData = uriData;
        if (_uriData != null && _uriData.cardId != null) {
            cardPrefix = _uriData.cardId.substring(0, 2); // 交通系のカード種別判別用
        }
    }

    // データ加工用の売上データを取得する（OKICA用）
    public void setUriOkicaData(UriOkicaData uriOkicaData) {
        _uriOkicaData = uriOkicaData;
    }

    // データ加工用の各データを取得する
    public void setResultParam(ResultParam resultParam) {
        _resultParam = resultParam;
    }
    public void setAmountParam(AmountParam amountParam) {
        _amountParam = amountParam;
    }
    public void setRefundParam(RefundParam refundParam) {
        _refundParam = refundParam;
    }

    private boolean _isPos = false;

    // POSサービス系
    private TransactionLogger _transactionLogger = null;
    private TransactionDetailLogger _transactionDetailLogger = null;
    private CartDao _cartDao = null;
    private TaxCalcDao _taxCalcDao = null;
    private ReceiptDao _receiptDao = null;
    private TenantDao _tenantDao = null;

    // コンストラクタ
    public OptionalTransFacade(MoneyType moneyType){
        LocalDatabase db = LocalDatabase.getInstance();
        _moneyType = moneyType;

        _isPos = AppPreference.isServicePos();

        if (_isPos){
            _transactionLogger = new TransactionLogger();
            _transactionDetailLogger = new TransactionDetailLogger();
            _cartDao = db.cartDao();
            _taxCalcDao = db.taxCalcDao();
            _receiptDao = db.receiptDao();
            _tenantDao = db.tenantDao();
        }
    }

    // 明細データを作る（UriData前提）
    public void CreateByUriData() {
        // POS機能が無効の場合は明細データ作成しない
        if (!_isPos) return;

        if (_uriData == null){
            throw new IllegalStateException("CreateByUriData を使用する場合は先に UriData を set してください。");
        }

        new Thread(() -> {

            if (_isPos) {
                // 正常終了 or 処理未了 の場合データを作成する（念のためここでもチェック）
                if (_uriData.transResult == TransMap.RESULT_SUCCESS || _uriData.transResult == TransMap.RESULT_UNFINISHED){
                    Timber.i("POS 送信用の取引データを作成: %s", _uriData.transResult);

                    // 取引データと取引明細データを登録
                    _transactionLogger.CreateByUriData(_uriData, _moneyType, cardPrefix);
                    long id = _transactionLogger.Insert();

                    if (_uriData.transType == TransMap.TYPE_SALES) { // 取消の場合は明細は不要
                        _transactionDetailLogger.CreateByUriData(_uriData, id);
                        _transactionDetailLogger.Insert();

                        if (_resultParam.transResult == TransMap.RESULT_SUCCESS) {
                            // 売上の場合のみカートの中身を削除
                            Timber.i("取引データの格納が完了したのでカートの中身を削除");
                            _cartDao.deleteAll();
                        }
                    }

                    // 取引データ送信サービスを起動する
                    PosTransactionService.startService();
                }
            }
        }).start();
    }

    // 明細データを作る（UriOkicaData前提）
    public void CreateByUriOkicaData() {
        // POS機能が無効の場合は明細データ作成しない
        if (!_isPos) return;

        if (_uriOkicaData == null) {
            throw new IllegalStateException("CreateByUriOkicaData を使用する場合は先に UriOkicaData を set してください。");
        }

        new Thread(() -> {

            if (_isPos) {
                // 正常終了 or 処理未了 の場合データを作成する（念のためここでもチェック）
                if (_resultParam.transResult == TransMap.RESULT_SUCCESS || _resultParam.transResult == TransMap.RESULT_UNFINISHED) {
                    // 決済 or 取消 の場合のみデータを作成する（チャージの時はPOSに上げない）
                    if(_uriOkicaData.okicaProcessType == 0x46 || _uriOkicaData.okicaProcessType == 0x4C) {
                        Timber.i("POS 送信用の取引データを作成（OKICA）: %s", _resultParam.transResult);
                        _transactionLogger.CreateByUriOkicaData(_uriOkicaData, _refundParam, _resultParam.transResult);
                        long id = _transactionLogger.Insert();

                        if (_uriOkicaData.okicaProcessType == 0x46) { // 取消の場合は明細は不要
                            _transactionDetailLogger.CreateByUriOkicaData(_uriOkicaData, id);
                            _transactionDetailLogger.Insert();

                            if (_resultParam.transResult == TransMap.RESULT_SUCCESS) {
                                // 売上の場合のみカートの中身を削除
                                Timber.i("取引データの格納が完了したのでカートの中身を削除");
                                _cartDao.deleteAll();
                            }
                        }

                        // 取引データ送信サービスを起動する
                        PosTransactionService.startService();
                    }
                }
            }
        }).start();
    }

    // 現金用の明細データを作る
    public void CreateCash(boolean isRepay, String transDate, String termSequence) {

        new Thread(() -> {

            if (_isPos) {
                Timber.i("POS 送信用の取引データを作成（現金）");
                _transactionLogger.CreateCash(isRepay, transDate, termSequence, _amountParam, _refundParam);
                long id = _transactionLogger.Insert();

                if (!isRepay){ // 取消の場合は明細は不要
                    _transactionDetailLogger.Create(transDate, id);
                    _transactionDetailLogger.Insert();

                    // 売上の場合のみカートの中身を削除
                    Timber.i("取引データの格納が完了したのでカートの中身を削除");
                    _cartDao.deleteAll();
                }

                // 取引データ送信サービスを起動する
                PosTransactionService.startService();
            }
        }).start();
    }

    // 為替類用の明細データを作る
    public void CreatePostalOrder(boolean isRepay, String transDate, String termSequence) {

        new Thread(() -> {

            if (_isPos) {
                Timber.i("POS 送信用の取引データを作成（為替類）");
                _transactionLogger.CreatePostalOrder(isRepay, transDate, termSequence, _amountParam, _refundParam);
                long id = _transactionLogger.Insert();

                if (!isRepay){ // 取消の場合は明細は不要
                    _transactionDetailLogger.Create(transDate, id);
                    _transactionDetailLogger.Insert();

                    // 売上の場合のみカートの中身を削除
                    Timber.i("取引データの格納が完了したのでカートの中身を削除");
                    _cartDao.deleteAll();
                }

                // 取引データ送信サービスを起動する
                PosTransactionService.startService();
            }
        }).start();
    }

    /**
     * 取引明細書、取消票、領収書のデータを作成
     * <br>
     * @param slipId 伝票ID
     */
    public void CreateReceiptData(int slipId) {
        // POS機能が無効の場合は取引明細書、取消票、領収書のデータを作成しない
        if (!_isPos) return;

        Timber.d("CreateReceiptData slipId=%d", slipId);
        // 正常終了or未了でデータを作成する
        if ((_resultParam.transResult == TransMap.RESULT_SUCCESS) || (_resultParam.transResult == TransMap.RESULT_UNFINISHED)) {
            new Thread(() -> {
                if (_moneyType == MoneyType.OKICA) {
                    if(_resultParam.transType == TransMap.TYPE_SALES) {
                        // 取引明細書、領収書の印字データ作成
                        ReceiptData(
                                slipId,
                                _uriOkicaData.okicaSequence,
                                Converters.dateFormat(_uriOkicaData.okicaTransDate),
                                _uriOkicaData.transAmount,
                                _uriOkicaData.transCashTogetherAmount,
                                0
                        );
                    } else if(_resultParam.transType == TransMap.TYPE_CANCEL) {
                        // 取消票の印字データ作成
                        CancelData(
                                slipId,
                                _uriOkicaData.okicaSequence,
                                Converters.dateFormat(_uriOkicaData.okicaTransDate),
                                _refundParam.oldTermSequence,
                                _refundParam.oldTransDate,
                                _uriOkicaData.transAmount,
                                _uriOkicaData.transCashTogetherAmount);
                    }
                } else {
                    if(_resultParam.transType == TransMap.TYPE_SALES) {
                        // 取引明細書、領収書の印字データ作成
                        ReceiptData(
                                slipId,
                                _uriData.termSequence,
                                _uriData.transDate,
                                _uriData.transAmount,
                                _uriData.transCashTogetherAmount,
                                0);
                    } else if(_resultParam.transType == TransMap.TYPE_CANCEL){
                        // 取消票の印字データ作成
                        CancelData(
                                slipId,
                                _uriData.termSequence,
                                _uriData.transDate,
                                _refundParam.oldTermSequence,
                                _refundParam.oldTransDate,
                                _uriData.transAmount,
                                _uriData.transCashTogetherAmount);
                    }
                }
            }).start();
        }
    }

    /**
     * 取引明細書、取消票、領収書のデータを作成
     * <br>
     * @param slipId 伝票ID
     * @param transDate 取引日時
     * @param termSequence 機器通番（端末通番）
     * @param cashAmount 現金の金額
     * @param changeAmount お釣りの金額
     */
    public void CreateReceiptData(int slipId, String transDate, String termSequence, int cashAmount, int changeAmount) {
        Timber.d("CreateReceiptData slipId=%d", slipId);
        if(_resultParam.transType == TransMap.TYPE_SALES) {
            ReceiptData(
                    slipId,
                    Integer.parseInt(termSequence),
                    transDate,
                    0,
                    cashAmount,
                    changeAmount);
        } else {
            CancelData(
                    slipId,
                    Integer.parseInt(termSequence),
                    transDate,
                    _refundParam.oldTermSequence,
                    _refundParam.oldTransDate,
                    0,
                    cashAmount);
        }
    }

    /**
     * 取引明細書、領収書のデータを作成
     * @param slipId 伝票ID
     * @param termSequence 機器通番（端末通番）
     * @param transDate 取引日時
     * @param transAmount 支払種別の金額
     * @param cashAmount 現金の金額
     * @param changeAmount お釣りの金額
     */
    private void ReceiptData(int slipId, Integer termSequence, String transDate, Integer transAmount, Integer cashAmount, Integer changeAmount) {
        // マスタデータの取得
        TenantData tenantData = _tenantDao.getTenant();

        // レシート印字用のデータ作成
        ReceiptData receiptData = new ReceiptData(
                _moneyType, slipId, termSequence, transDate, 0, "",
                transAmount + cashAmount, transAmount, cashAmount, changeAmount,
                tenantData);

        Gson gson = new Gson();
        // 商品情報
        List<CartData> cartDataList = _cartDao.getAllProduct();
        List<ReceiptProductDetail> productDetailList = new ArrayList<>();
        for(int i=0; i<cartDataList.size(); i++) {
            ReceiptProductDetail productDetail = new ReceiptProductDetail();
            productDetail.name = cartDataList.get(i).product_name;
            if(cartDataList.get(i).is_custom_price) {
                productDetail.price = cartDataList.get(i).custom_unit_price;
            } else {
                productDetail.price = cartDataList.get(i).standard_unit_price;
            }
            productDetail.count = cartDataList.get(i).count;
            if(ReducedTaxTypes.valueOf(cartDataList.get(i).reduce_tax_type) == ReducedTaxTypes.EXEMPTION) {
                // 非課税
                productDetail.taxType = "非";
            } else {
                if(ReducedTaxTypes.valueOf(cartDataList.get(i).reduce_tax_type) == ReducedTaxTypes.REDUCED) {
                    // 軽減税率
                    productDetail.reducedTax = "*";
                }

                if(cartDataList.get(i).included_tax_type == IncludedTaxTypes.INCLUDED.value) {
                    // 内税
                    productDetail.taxType = "込";
                }
            }
            productDetail.total = productDetail.price * cartDataList.get(i).count;
            productDetailList.add(productDetail);
        }
        receiptData.product_detail = gson.toJson(productDetailList);

        // 小計情報
        TaxCalcData _taxCalcData = _taxCalcDao.getData();
        ReceiptSubtotalDetail subtotalDetail = new ReceiptSubtotalDetail();
        subtotalDetail.reduced_tax_rate = _taxCalcData.reduced_tax_rate;
        subtotalDetail.standard_tax_rate = _taxCalcData.standard_tax_rate;
        subtotalDetail.amount_tax_exclusive_reduced_without_tax = _taxCalcData.amount_tax_exclusive_reduced_without_tax;
        subtotalDetail.amount_tax_exclusive_reduced_only_tax = _taxCalcData.amount_tax_exclusive_reduced_only_tax;
        subtotalDetail.amount_tax_exclusive_standard_without_tax = _taxCalcData.amount_tax_exclusive_standard_without_tax;
        subtotalDetail.amount_tax_exclusive_standard_only_tax = _taxCalcData.amount_tax_exclusive_standard_only_tax;
        subtotalDetail.amount_tax_inclusive_reduced = _taxCalcData.amount_tax_inclusive_reduced;
        subtotalDetail.amount_tax_inclusive_standard = _taxCalcData.amount_tax_inclusive_standard;
        subtotalDetail.amount_tax_free = _taxCalcData.amount_tax_free;
        subtotalDetail.amount_tax_reduced = _taxCalcData.amount_tax_reduced;
        subtotalDetail.amount_tax_standard = _taxCalcData.amount_tax_standard;
        subtotalDetail.amount_tax_reduced_only_tax = _taxCalcData.amount_tax_reduced_only_tax;
        subtotalDetail.amount_tax_standard_only_tax = _taxCalcData.amount_tax_standard_only_tax;
        receiptData.subtotal_detail = gson.toJson(subtotalDetail);

        new Thread(() -> {
            _receiptDao.insertReceipt(receiptData);
        }).start();
    }

    private void CancelData(int slipId, Integer termSequence, String transDate, Integer oldTermSequence, String oldTransDate, Integer transAmount, Integer cashAmount) {
        // マスタデータの取得
        TenantData tenantData = _tenantDao.getTenant();

        // 取消印字用のデータ作成
        ReceiptData receiptData = new ReceiptData(
                _moneyType, slipId, termSequence, transDate, oldTermSequence, oldTransDate,
                transAmount + cashAmount, transAmount, cashAmount, 0,
                tenantData);

        new Thread(() -> {
            _receiptDao.insertReceipt(receiptData);
            _receiptDao.updateCanceledTrans(_refundParam.oldSlipId);
        }).start();
    }
}
