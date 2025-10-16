package jp.mcapps.android.multi_payment_terminal.model;

import android.content.res.Resources;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.MoneyType;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.AmountParam;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.RefundParam;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.ResultParam;
import jp.mcapps.android.multi_payment_terminal.database.Converters;
import jp.mcapps.android.multi_payment_terminal.database.LocalDatabase;
import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;
import jp.mcapps.android.multi_payment_terminal.database.history.uriOkica.UriOkicaData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketReceiptDao;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketReceiptData;
import jp.mcapps.android.multi_payment_terminal.database.ticket.TicketReceiptDetail;
import jp.mcapps.android.multi_payment_terminal.ui.ticket.TicketSearchResults;
import timber.log.Timber;

public class OptionalTicketTransFacade {
    private MoneyType _moneyType = null;
    private UriData _uriData = null;
    private UriOkicaData _uriOkicaData = null;
    private ResultParam _resultParam = null;
    private AmountParam _amountParam = null;
    private RefundParam _refundParam = null;
    private TicketReceiptDao _ticketReceiptDao = null;
    private TicketSearchResults _ticketSearchResults = AppPreference.getTicketSearchResults();
    private Resources _resources = MainApplication.getInstance().getResources();

    // データ加工用の売上データを取得する
    public void setUriData(UriData uriData) {
        _uriData = uriData;
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

    // コンストラクタ
    public OptionalTicketTransFacade(MoneyType moneyType){
        LocalDatabase db = LocalDatabase.getInstance();
        _moneyType = moneyType;

        _ticketReceiptDao = db.ticketReceiptDao();
    }

    /**
     * 各決済時の取引明細書、取消票、領収書のデータを作成
     * <br>
     * @param slipId 伝票ID
     */
    public void CreateReceiptData(int slipId) {

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
     * 現金支払時の取引明細書、取消票、領収書のデータを作成
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

        // レシート印字用のデータ作成
        TicketReceiptData ticketReceiptData = new TicketReceiptData(
                _moneyType, slipId, termSequence, transDate, 0, "",
                transAmount + cashAmount, transAmount, cashAmount, changeAmount
        );

        Gson gson = new Gson();

        // チケット情報
        List<TicketReceiptDetail> ticketReceiptDetailList = new ArrayList<>();
        int categoryCount = _ticketSearchResults.categoryData.size();

        for(int i = 0; i < categoryCount ; i++) {
            TicketReceiptDetail ticketReceiptDetail = new TicketReceiptDetail();

            // カテゴリ名
            ticketReceiptDetail.categoryType = getCategoryTypeName(
                    _ticketSearchResults.categoryData.get(i).categoryType,
                    _ticketSearchResults.categoryData.get(i).ticketsNumber);
            // 単価
            ticketReceiptDetail.price = _ticketSearchResults.categoryData.get(i).amount / _ticketSearchResults.categoryData.get(i).quantity;
            // 数量
            ticketReceiptDetail.count = _ticketSearchResults.categoryData.get(i).quantity;
            // 税率
            ticketReceiptDetail.reducedTax = "";
            // 税種：税込固定
            ticketReceiptDetail.taxType = "込";
            // 小計
            ticketReceiptDetail.total = _ticketSearchResults.categoryData.get(i).amount;
            ticketReceiptDetailList.add(ticketReceiptDetail);
        }
        ticketReceiptData.ticket_detail = gson.toJson(ticketReceiptDetailList);

        // 小計情報
        ticketReceiptData.subtotal_detail = String.valueOf(_ticketSearchResults.totalAmount);

        new Thread(() -> {
            _ticketReceiptDao.insertReceipt(ticketReceiptData);
        }).start();
    }

    /**
     * 取消票のデータを作成
     * @param slipId 伝票ID
     * @param termSequence 機器通番（端末通番）
     * @param transDate 取引日時
     * @param oldTermSequence 機器通番（取消元）
     * @param oldTransDate 取引日時（取消元）
     * @param transAmount 支払種別の金額
     * @param cashAmount 現金の金額
     */
    private void CancelData(int slipId, Integer termSequence, String transDate, Integer oldTermSequence, String oldTransDate, Integer transAmount, Integer cashAmount) {

        // 取消印字用のデータ作成
        TicketReceiptData ticketReceiptData = new TicketReceiptData(
                _moneyType, slipId, termSequence, transDate, oldTermSequence, oldTransDate,
                transAmount + cashAmount, transAmount, cashAmount, 0);

        new Thread(() -> {
            _ticketReceiptDao.insertReceipt(ticketReceiptData);
            _ticketReceiptDao.updateCanceledTrans(_refundParam.oldSlipId);
        }).start();
    }

    /**
     * カテゴリタイプ名を取得（印字用）
     * @param categoryType カテゴリ名（API名称）
     * @param ticketsNumber 回数券（ｎ枚券）
     */
    private String getCategoryTypeName(String categoryType, Integer ticketsNumber) {
        String Name = "";

        // 乗客カテゴリ名
        if (categoryType != null) {
            if (categoryType.equals("unknown")) {
                Name = _resources.getString(R.string.text_ticket_adult);
            } else if (categoryType.equals("child")) {
                Name = _resources.getString(R.string.text_ticket_child);
            } else if (categoryType.equals("disabled")) {
                Name = _resources.getString(R.string.text_ticket_adult_disability);
            } else if (categoryType.equals("child_disabled")) {
                Name = _resources.getString(R.string.text_ticket_child_disability);
            } else if (categoryType.equals("carer")) {
                Name = _resources.getString(R.string.text_ticket_caregiver);
            } else if (categoryType.equals("baby")) {
                Name = _resources.getString(R.string.text_ticket_baby);
            } else {
                Timber.e("乗客カテゴリ名(想定外):%s", categoryType);
            }
        } else {
            Timber.e("乗客カテゴリ名 = null");
        }

        // 回数券のセット枚数
        if (ticketsNumber != null) {
            if (ticketsNumber > 1) {
                /* セット券あり */
                Name += String.format("%s%s", ticketsNumber, _resources.getString(R.string.text_tickets_number));
            }
        } else {
            Timber.e("回数券のセット枚数 = null");
        }

        return Name;
    }
}
