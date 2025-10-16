package jp.mcapps.android.multi_payment_terminal.model;

import android.content.res.Resources;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.data.pos.CardBrand;
import jp.mcapps.android.multi_payment_terminal.data.pos.CardCategory;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.AmountParam;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.RefundParam;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.ResultParam;
import jp.mcapps.android.multi_payment_terminal.data.trans_param.SurveyParam;
import jp.mcapps.android.multi_payment_terminal.database.Converters;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipDao;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipDaoSecure;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriDao;
import jp.mcapps.android.multi_payment_terminal.database.history.uri.UriData;
import jp.mcapps.android.multi_payment_terminal.database.history.uriOkica.UriOkicaDao;
import jp.mcapps.android.multi_payment_terminal.database.history.uriOkica.UriOkicaData;
import jp.mcapps.android.multi_payment_terminal.iCAS.data.DeviceClient;
import jp.mcapps.android.multi_payment_terminal.thread.credit.data.CreditResult;
import jp.mcapps.android.multi_payment_terminal.ui.emoney.okica.EMoneyOkicaViewModel;
import jp.mcapps.android.multi_payment_terminal.webapi.grpc.data.TerminalInfo;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.WatariPoint;
import timber.log.Timber;

public class TransLogger {
    private UriData _uriData;
    private UriOkicaData _uriOkicaData;
    private SlipData _slipData;
    private final ResultParam _resultParam = new ResultParam();
    private final AmountParam _amountParam = new AmountParam();
    private final RefundParam _refundParam = new RefundParam();
    private final SurveyParam _surveyParam = new SurveyParam();
    private final ExecutorService _transExecutorService = Executors.newSingleThreadExecutor();
    private final boolean _isDemoMode = AppPreference.isDemoMode();
    private Resources _resources = MainApplication.getInstance().getResources();
    private String _purchasedTicketDealId = null;

    /**
     * 暗号化パターン
     * 0 : 暗号化なし
     * 1 : AES128
     */
    private final int _encryptType = 1;

    /**
     * クレジットの取引データを生成
     * デモモードでは売上データなし
     * @param result 取引結果
     */
    public void credit(CreditResult.Result result) {
        int termSequence = getTermSequence(false);   //端末通番を取得
        setAmountParam(String.valueOf(result.fare));   //金額情報を取得
        setAmountPaymented();
        if(_resultParam.transType == 0) {   //取消時はsetRefundParamにて設定済み
            setCashAmount();    //現金分割分を設定
            setTicketAmount();  //チケット金額を設定
            setEigyoCount();    //営業回数を設定
            setCompleteAmount();  //支払済み金額を設定
        }
        getLocation();  //位置情報を取得

        _uriData = _isDemoMode ? null : new UriData(result, _encryptType, termSequence, _resultParam, _refundParam, _amountParam, _surveyParam);
        _slipData = new SlipData(result, _encryptType, termSequence, _resultParam, _refundParam, _amountParam);
        if (AppPreference.isTicketTransaction()) setCardInfo();
        if (_slipData.transType == TransMap.TYPE_CANCEL) _slipData.purchasedTicketDealId = _purchasedTicketDealId;
    }

    /**
     * 交通系の取引データを生成
     * デモモードでは売上データなし
     * @param result RASからの応答
     */
    public void suica(DeviceClient.Result result) {
        final boolean doIncrement = !AppPreference.getPoweroffTrans();
        int termSequence = getTermSequence(doIncrement);   //端末通番を取得
        setAmountParam(result.value);   //金額情報を取得
        setAmountPaymented();
        if(_resultParam.transType == 0) {   //取消時はsetRefundParamにて設定済み
            setCashAmount();    //現金分割分を設定
            setTicketAmount();  //チケット金額を設定
            setEigyoCount();    //営業回数を設定
            setCompleteAmount();  //支払済み金額を設定
        }
        getLocation();  //位置情報を取得

        _uriData = _isDemoMode ? null : new UriData(result, _encryptType, termSequence, _resultParam, _refundParam, _amountParam, _surveyParam);
        _slipData = new SlipData(result, _encryptType, termSequence, _resultParam, _amountParam);
        if (AppPreference.isTicketTransaction()) setCardInfo();
        if (_slipData.transType == TransMap.TYPE_CANCEL) _slipData.purchasedTicketDealId = _purchasedTicketDealId;
    }

    /**
     * Edyの取引データを生成
     * デモモードでは売上データなし
     * @param result RASからの応答
     */
    public void edy(DeviceClient.ResultEdy result) {
        final boolean doIncrement = !AppPreference.getPoweroffTrans();
        int termSequence = getTermSequence(doIncrement);   //端末通番を取得
        setAmountParam(result.value);   //金額情報を取得
        setAmountPaymented();
        if(_resultParam.transType == 0) {   //取消時はsetRefundParamにて設定済み
            setCashAmount();    //現金分割分を設定
            setTicketAmount();  //チケット金額を設定
            setEigyoCount();    //営業回数を設定
            setCompleteAmount();  //支払済み金額を設定
        }
        getLocation();  //位置情報を取得

        _uriData = _isDemoMode ? null : new UriData(result, _encryptType, termSequence, _resultParam, _amountParam, _surveyParam);
        _slipData = new SlipData(result, _encryptType, termSequence, _resultParam, _amountParam);
        if (AppPreference.isTicketTransaction()) setCardInfo();
        if (_slipData.transType == TransMap.TYPE_CANCEL) _slipData.purchasedTicketDealId = _purchasedTicketDealId;
    }

    /**
     * iDの取引データを生成
     * デモモードでは売上データなし
     * @param result RASからの応答
     */
    public void id(DeviceClient.ResultID result) {
        final boolean doIncrement = !AppPreference.getPoweroffTrans();
        int termSequence = getTermSequence(doIncrement);   //端末通番を取得

        setAmountParam(result.value);   //金額情報を取得
        setAmountPaymented();
        if(_resultParam.transType == 0) {   //取消時はsetRefundParamにて設定済み
            setCashAmount();    //現金分割分を設定
            setTicketAmount();  //チケット金額を設定
            setEigyoCount();    //営業回数を設定
            setCompleteAmount();  //支払済み金額を設定
        }
        getLocation();  //位置情報を取得

        _uriData = _isDemoMode ? null : new UriData(result, _encryptType, termSequence, _resultParam, _refundParam, _amountParam, _surveyParam);
        _slipData = new SlipData(result, _encryptType, termSequence, _resultParam, _amountParam);
        if (AppPreference.isTicketTransaction()) setCardInfo();
        if (_slipData.transType == TransMap.TYPE_CANCEL) _slipData.purchasedTicketDealId = _purchasedTicketDealId;
    }

    /**
     * waonの取引データを生成
     * デモモードでは売上データなし
     * @param result RASからの応答
     */
    public void waon(DeviceClient.ResultWAON result) {
        final boolean doIncrement = !AppPreference.getPoweroffTrans();
        int termSequence = getTermSequence(doIncrement);   //端末通番を取得
        setAmountParam(result.value);   //金額情報を取得
        setAmountPaymented();
        if(_resultParam.transType == 0) {   //取消時はsetRefundParamにて設定済み
            setCashAmount();    //現金分割分を設定
            setTicketAmount();  //チケット金額を設定
            setEigyoCount();    //営業回数を設定
            setCompleteAmount();  //支払済み金額を設定
        }
        getLocation();  //位置情報を取得

        _uriData = _isDemoMode ? null : new UriData(result, _encryptType, termSequence, _resultParam, _refundParam, _amountParam, _surveyParam);
        _slipData = new SlipData(result, _encryptType, termSequence, _resultParam, _refundParam, _amountParam);
        if (AppPreference.isTicketTransaction()) setCardInfo();
        if (_slipData.transType == TransMap.TYPE_CANCEL) _slipData.purchasedTicketDealId = _purchasedTicketDealId;
    }

    /**
     * QUICPayの取引データを生成
     * デモモードでは売上データなし
     * @param result RASからの応答
     */
    public void qp(DeviceClient.ResultQUICPay result) {
        final boolean doIncrement = !AppPreference.getPoweroffTrans();
        int termSequence = getTermSequence(doIncrement);   //端末通番を取得
        setAmountParam(result.value);   //金額情報を取得
        setAmountPaymented();
        if(_resultParam.transType == 0) {   //取消時はsetRefundParamにて設定済み
            setCashAmount();    //現金分割分を設定
            setTicketAmount();  //チケット金額を設定
            setEigyoCount();    //営業回数を設定
            setCompleteAmount();  //支払済み金額を設定
        }
        getLocation();  //位置情報を取得

        _uriData = _isDemoMode ? null : new UriData(result, _encryptType, termSequence, _resultParam, _refundParam, _amountParam, _surveyParam);
        _slipData = new SlipData(result, _encryptType, termSequence, _resultParam, _amountParam);
        if (AppPreference.isTicketTransaction()) setCardInfo();
        if (_slipData.transType == TransMap.TYPE_CANCEL) _slipData.purchasedTicketDealId = _purchasedTicketDealId;
    }

    /**
     * nanacoの取引データを生成
     * デモモードでは売上データなし
     * @param result RASからの応答
     */
    public void nanaco(DeviceClient.Resultnanaco result) {
        final boolean doIncrement = !AppPreference.getPoweroffTrans();
        int termSequence = getTermSequence(doIncrement);   //端末通番を取得
        setAmountParam(result.value);   //金額情報を取得
        setAmountPaymented();
        if(_resultParam.transType == 0) {   //取消時はsetRefundParamにて設定済み
            setCashAmount();    //現金分割分を設定
            setTicketAmount();  //チケット金額を設定
            setEigyoCount();    //営業回数を設定
            setCompleteAmount();  //支払済み金額を設定
        }
        getLocation();  //位置情報を取得

        _uriData = _isDemoMode ? null : new UriData(result, _encryptType, termSequence, _resultParam, _refundParam, _amountParam, _surveyParam);
        _slipData = new SlipData(result, _encryptType, termSequence, _resultParam, _amountParam);
        if (AppPreference.isTicketTransaction()) setCardInfo();
        if (_slipData.transType == TransMap.TYPE_CANCEL) _slipData.purchasedTicketDealId = _purchasedTicketDealId;
    }

    /**
     * OKICAの取引データを生成
     * デモモードでは売上データなし
     * @param transData カード取引結果
     */
    public void okica(EMoneyOkicaViewModel.TransactionData transData) {
        final boolean doIncrement = !AppPreference.getPoweroffTrans();
        int termSequence = getTermSequence(doIncrement);   //端末通番を取得

        TerminalInfo.Response terminalInfoRes = new TerminalInfo.Response();
        if (_isDemoMode == false) {
            terminalInfoRes.terminalId = AppPreference.getOkicaTerminalInfo().terminalId;
            terminalInfoRes.machineId = AppPreference.getOkicaTerminalInfo().machineId;
            terminalInfoRes.machineModelCd = AppPreference.getOkicaTerminalInfo().machineModelCd;
        }

        setAmountParam(Integer.toString(transData.getCardAmount()));   //金額情報を取得
        setAmountPaymented();

        if(_resultParam.transType == 0) {   //取消時はsetRefundParamにて設定済み
            setCashAmount();    //現金分割分を設定
            setTicketAmount();  //チケット金額を設定
            setEigyoCount();    //営業回数を設定
            setCompleteAmount();  //支払済み金額を設定
        }else if(_resultParam.transType == 4){
            setCashAmount();    //現金分割分を設定
            setTicketAmount();  //チケット金額を設定
            setEigyoCount();    //営業回数を設定
            setCompleteAmount();  //支払済み金額を設定
        }

        getLocation();  //位置情報を取得

        _uriOkicaData = _isDemoMode ? null : new UriOkicaData(transData, _encryptType, terminalInfoRes, termSequence, _resultParam, _amountParam, _surveyParam);
        if (_resultParam.transResult != TransMap.RESULT_NEGA_CHECK_ERROR) {
            _slipData = new SlipData(transData, _encryptType, termSequence, _resultParam, _refundParam, _amountParam);
        }
        if (AppPreference.isTicketTransaction()) setCardInfo();
        if (_slipData.transType == TransMap.TYPE_CANCEL) _slipData.purchasedTicketDealId = _purchasedTicketDealId;
    }

    /**
     * QRの取引データを生成
     * デモモードでは売上データなし
     * @param result GMOからのレスポンス
     */
    public void qr(QRSettlement.ResultSummary result) {
        final boolean doIncrement = !AppPreference.getPoweroffTrans();
        int termSequence = getTermSequence(doIncrement);   //端末通番を取得
        setAmountParam(result.totalFee);   //金額情報を取得
        setAmountPaymented();
        if(_resultParam.transType == 0) {   //取消時はsetRefundParamにて設定済み
            setCashAmount();    //現金分割分を設定
            setTicketAmount();  //チケット金額を設定
            setEigyoCount();    //営業回数を設定
            setCompleteAmount();  //支払済み金額を設定
        }
        getLocation();  //位置情報を取得

        _uriData = _isDemoMode ? null : new UriData(result, _encryptType, termSequence,_resultParam, _refundParam, _amountParam, _surveyParam);
        _slipData = new SlipData(result, _encryptType, termSequence, _resultParam, _amountParam);
        if (AppPreference.isTicketTransaction()) setCardInfo();
        if (_slipData.transType == TransMap.TYPE_CANCEL) _slipData.purchasedTicketDealId = _purchasedTicketDealId;
    }

    /**
     * 現金での取引データを生成
     * レシートのみ
     * @param payTime
     */
    public void cash(String payTime, int amount, Boolean isFixedAmountPostalOrder){
        final boolean doIncrement = !AppPreference.getPoweroffTrans();
        int termSequence = getTermSequence(doIncrement);   //端末通番を取得
        setTransResult(TransMap.RESULT_SUCCESS, TransMap.RESULT_SUCCESS); // 取引種別と結果をセット
        setAmountParam(String.valueOf(amount)); // 金額をセット
        if(_resultParam.transType == 0) {   //一応・取消時はsetRefundParamにて設定済み
            setCashAmount();    //現金分割分を設定
            setTicketAmount();  //チケット金額を設定
            setEigyoCount();    //営業回数を設定
            setCompleteAmount();  //支払済み金額を設定
        }
        getLocation();  //位置情報を取得

        _slipData = new SlipData(payTime, termSequence, _resultParam, _refundParam, _amountParam, isFixedAmountPostalOrder);
        if (AppPreference.isTicketTransaction()) setCardInfo();
        if (_slipData.transType == TransMap.TYPE_CANCEL) _slipData.purchasedTicketDealId = _purchasedTicketDealId;
    }

    /**
     * ポイントでの取引データを作成
     * レシートのみ
     */
    public void point(WatariSettlement.WatariResult result){
        final boolean doIncrement = !AppPreference.getPoweroffTrans();
        int termSequence = getTermSequence(doIncrement);   //端末通番を取得
        int watariSlipNo = AppPreference.getSlipNoWatari(); // 和多利用伝票番号を取得
        setAmountParamForPoint(String.valueOf(result.amount));   //金額情報を取得
        if(_resultParam.transType == TransMap.TYPE_POINT) {   //取消時はsetRefundParamにて設定済み
            setCashAmount();    //現金分割分を設定
            setTicketAmount();  //チケット金額を設定
            setEigyoCount();    //営業回数を設定
            setCompleteAmount();  //支払済み金額を設定
        }

        _slipData = new SlipData(result, termSequence, watariSlipNo, _resultParam, _refundParam, _amountParam);
    }

    //ADD-S BMT S.Oyama 2024/09/19 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  分別：チケット向け取引データを作成（フタバ双方向用）
     * @note   分別：チケット向け取引データを作成　レシートのみ
     * @param [in] なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void separateTicket(String payTime, int ticketamount){
        final boolean doIncrement = !AppPreference.getPoweroffTrans();
        int termSequence = getTermSequence(doIncrement);   //端末通番を取得
        setTransResult(TransMap.RESULT_SUCCESS, TransMap.RESULT_SUCCESS); // 取引種別と結果をセット
        setAmountParam(String.valueOf(ticketamount)); // 金額をセット
        if(_resultParam.transType == 0) {   //一応・取消時はsetRefundParamにて設定済み
            setCashAmount();    //現金分割分を設定
            setTicketAmount();  //チケット金額を設定
            setEigyoCount();    //営業回数を設定
            setCompleteAmount();  //支払済み金額を設定
        }
        getLocation();  //位置情報を取得

        _slipData = new SlipData(payTime, termSequence, _resultParam, _refundParam, _amountParam, false);              //現金モードで作成
        _slipData.transBrand = "分別チケット";  //ブランド名を差し替える

        //if (AppPreference.isTicketTransaction()) setCardInfo();
        //if (_slipData.transType == TransMap.TYPE_CANCEL) _slipData.purchasedTicketDealId = _purchasedTicketDealId;
    }
    //ADD-E BMT S.Oyama 2024/09/19 フタバ双方向向け改修
    public void getSlipDataInfo(String[] transDate, int[] termSequence) {
        transDate[0] = _slipData.transDate;
        termSequence[0] = _slipData.termSequence;
    }

    /**
     * DBに保存
     * デモモードでは売上データなし
     * @return 伝票ID
     */
    public int insert() {
        Future<Integer> future = _transExecutorService.submit(() -> {
            UriDao uriDao = DBManager.getUriDao();
            SlipDao slipDao = DBManager.getSlipDao();

            if(_uriData != null) uriDao.insertUriData(_uriData);

            return (int)slipDao.insertSlipData(_slipData);
        });

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            //現状発生しない例外
            Timber.e(e);
            return -1;
        }
    }

    public int delete(String transDate, int termSequence) {
        Future<Integer> future = _transExecutorService.submit(() -> {
            UriDao uriDao = DBManager.getUriDao();
            SlipDao slipDao = DBManager.getSlipDao();
            uriDao.posSendCompleted(transDate, termSequence);
            slipDao.deleteSpecifiedData(transDate, termSequence);
            return 0;
        });

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            //現状発生しない例外
            Timber.e(e);
            return -1;
        }
    }

    /**
     * 取引結果の情報を設定
     * @param transResult 取引結果
     * @param transResultDetail 取引結果詳細
     */
    public void setTransResult(int transResult, int transResultDetail) {
        _resultParam.transType = getTransType();    //取引種別
        _resultParam.transResult = transResult; //取引結果
        _resultParam.transResultDetail = transResultDetail; //取引結果詳細
    }

    /**
     * 取消時に保存する情報を設定
     * 元伝票番号、元取消日時、元端末通番、元決済ID、取引金額（RASから取得できない場合）、現金併用金額
     * @param slipId 伝票ID
     */
    public void setRefundParam(int slipId) {
        final SlipData[] oldData = {new SlipData()};
        Thread thread = new Thread(() -> {
            oldData[0] = DBManager.getSlipDao().getOneById(slipId);
        });
        thread.start();

        try {
            thread.join();
            if (oldData[0] != null) {
                _refundParam.oldSlipId = slipId;
                _refundParam.oldSlipNumber = oldData[0].slipNumber;  //元伝票番号
                _refundParam.oldTransDate = oldData[0].transDate; //元取引日時
                _refundParam.oldTermSequence = oldData[0].termSequence;   //元端末通番
                _refundParam.oldTransId = oldData[0].transId; //元決済ID
                _refundParam.oldTransAmount = oldData[0].transAmount;   //取引金額 RASから取得できなかった場合はこの値を使う
                _amountParam.transCashTogetherAmount = oldData[0].transCashTogetherAmount;  //現金併用金額
                _amountParam.transTicketAmount = oldData[0].transOtherAmountOne;    //チケット金額
                _amountParam.transMeterAmount = oldData[0].transMeterAmount;    // メーター金額
                _amountParam.transEigyoCount =  oldData[0].freeCountOne ;       // 営業回数
                _amountParam.transCompleteAmount = oldData[0].transCompleteAmount;  //支払済み金額
                _purchasedTicketDealId = oldData[0].purchasedTicketDealId;      // チケット購入ID
            }
        } catch (InterruptedException e) {
            Timber.e(e);
        }
    }

    /**
     * 金額情報を設定
     * 取引金額、定額、メータ金額、増減額
     * @param value RASから応答された取引金額
     */
    private void setAmountParam(String value) {
        switch (_resultParam.transType) {
            case TransMap.TYPE_SALES:
                //支払
                _amountParam.transAmount = value != null
                        ? Converters.stringToInteger(value)
                        : Amount.getFixedAmount(); //取引金額 RASからの応答がなければ端末で持っている値を保存

                if (Amount.getFlatRateAmount() < 0 || Amount.getFlatRateAmount() > 999999) {
                    Timber.w("定額異常：%d -> %d", Amount.getFlatRateAmount(), 0);
                    _amountParam.transSpecifiedAmount = 0;  //定額 範囲の場合は0にする
                } else {
                    _amountParam.transSpecifiedAmount = Amount.getFlatRateAmount();
                }

                _amountParam.transMeterAmount = Amount.getMeterCharge();  //メータ金額

                if (Amount.getTotalChangeAmount() < -999999 || Amount.getTotalChangeAmount() > 999999) {
                    Timber.w("増減額異常：%d -> %d", Amount.getTotalChangeAmount(), 0);
                    _amountParam.transAdjAmount = 0;  //増減額 範囲の場合は0にする
                } else {
                    _amountParam.transAdjAmount = Amount.getTotalChangeAmount();
                }

                if (   _amountParam.transSpecifiedAmount == 0
                    && _amountParam.transMeterAmount == 0
                    && _amountParam.transAdjAmount == 0
                    && Amount.getPosAmount() == 0) {

                    Timber.w("金額情報異常: 定額 0, メーター金額 0, 増減額 0");
                }

                break;
            case TransMap.TYPE_CANCEL:
                //取消
                _amountParam.transAmount = value != null
                        ? Converters.stringToInteger(value)
                        : _refundParam.oldTransAmount; //取引金額 RASからの応答がなければ取消対象の履歴から取得
/*  メーター料金の値は決済時のものをセットするためここではクリアしない
                _amountParam.transMeterAmount = 0;  //メーター料金
*/
                _amountParam.transSpecifiedAmount = 0;  //定額料金
                _amountParam.transAdjAmount = 0;  //増減額
                break;
            case TransMap.TYPE_CACHE_CHARGE:
                //現金チャージ
                _amountParam.transAmount = value != null
                        ? Converters.stringToInteger(value)
                        : Amount.getFixedAmount();
                _amountParam.transMeterAmount = Amount.getMeterCharge();  //メータ金額
                _amountParam.transSpecifiedAmount = 0;  //定額料金
                _amountParam.transAdjAmount = 0;  //増減額
                break;
        }
    }

    /**
     * 金額情報を設定（ポイント用）
     * 取引金額、定額、メータ金額、増減額
     * @param value RASから応答された取引金額
     */
    private void setAmountParamForPoint(String value) {
        switch (_resultParam.transType) {
            case TransMap.TYPE_POINT:
                //ポイント付与
                _amountParam.transAmount = value != null
                        ? Converters.stringToInteger(value)
                        : Amount.getFixedAmount(); //取引金額 RASからの応答がなければ端末で持っている値を保存

                if (Amount.getFlatRateAmount() < 0 || Amount.getFlatRateAmount() > 999999) {
                    Timber.w("定額異常：%d -> %d", Amount.getFlatRateAmount(), 0);
                    _amountParam.transSpecifiedAmount = 0;  //定額 範囲の場合は0にする
                } else {
                    _amountParam.transSpecifiedAmount = Amount.getFlatRateAmount();
                }

                _amountParam.transMeterAmount = Amount.getMeterCharge();  //メータ金額

                if (Amount.getTotalChangeAmount() < -999999 || Amount.getTotalChangeAmount() > 999999) {
                    Timber.w("増減額異常：%d -> %d", Amount.getTotalChangeAmount(), 0);
                    _amountParam.transAdjAmount = 0;  //増減額 範囲の場合は0にする
                } else {
                    _amountParam.transAdjAmount = Amount.getTotalChangeAmount();
                }

                if (   _amountParam.transSpecifiedAmount == 0
                        && _amountParam.transMeterAmount == 0
                        && _amountParam.transAdjAmount == 0
                        && Amount.getPosAmount() == 0) {

                    Timber.w("金額情報異常: 定額 0, メーター金額 0, 増減額 0");
                }

                break;
            case TransMap.TYPE_POINT_CANCEL:
                //ポイント付与取消
                _amountParam.transAmount = value != null
                        ? Converters.stringToInteger(value)
                        : _refundParam.oldTransAmount; //取引金額 RASからの応答がなければ取消対象の履歴から取得
/*  メーター料金の値は決済時のものをセットするためここではクリアしない
                _amountParam.transMeterAmount = 0;  //メーター料金
*/
                _amountParam.transSpecifiedAmount = 0;  //定額料金
                _amountParam.transAdjAmount = 0;  //増減額
                break;
        }
    }

    /**
     * 現金併用金額を設定
     * 確定した合計金額と決済金額の差額
     * @param value 決済金額
     */
    public void setCashTogetherAmount(Integer value) {
        //初期値で0が入っているためnullや範囲外の場合は初期値のまま
        if (value != null) {
            int cash = Amount.getFixedAmount() - value;
            if (cash < 0 || cash > 999999) {
                Timber.w("現金併用額異常：%d -> %d", cash, 0);
            } else {
                _amountParam.transCashTogetherAmount = cash;    //現金併用金額
            }
        }
    }

    /**
     * チケット金額を設定
     */
    public void setTicketAmount() {
        int ticket = Amount.getTicketAmount();
        if (ticket < 0 || ticket > 999999) {
            Timber.w("チケット金額異常：%d -> %d", ticket, 0);
        } else {
            _amountParam.transTicketAmount = ticket;    //チケット金額
        }
    }

    /**
     * 現金分割金額を設定
     */
    public void setCashAmount() {
        int cash = Amount.getCashAmount();
        if (cash < 0 || cash > 999999) {
            Timber.w("現金分割金額異常：%d -> %d", cash, 0);
        } else {
            _amountParam.transCashTogetherAmount += cash;    //現金分割金額（現金併用分に加算）
        }
    }

    /**
     * 営業回数を設定
     */
    public void setEigyoCount() {
        int eigyoCount = Amount.getFixedEigyoCount();
        _amountParam.transEigyoCount = eigyoCount;    //チケット金額
    }

    /**
     * 支払済み金額を設定
     */
    public void setCompleteAmount() {
        int completeAmount = Amount.getPaidAmount();
        _amountParam.transCompleteAmount = completeAmount;    //支払済み金額
    }

    /**
     * 支払済みフラグを設定
     */
    public void setAmountPaymented() {
        if (_resultParam.transType == TransMap.TYPE_SALES && _resultParam.transResult == TransMap.RESULT_SUCCESS) {
            // 支払成功時
            Amount.setPaymented(1);
        }
        else
        {
            // 支払成功時以外は支払済みフラグを設定しない
            Amount.setPaymented(0);
        }
    }

    /**
     * 計測した時間を設定
     * @param procTime 処理時間
     * @param pinTime 暗証番号入力時間
     */
    public void setProcTime(int procTime, int pinTime) {
        if (_uriData != null) {
            //売上データ生成済 値を直接更新
            _uriData.transTime = procTime;  //取引処理時間
            _uriData.transInputPinTime = pinTime;  //暗証番号入力時間
        } else {
            //売上データ未生成 SurveyParamに保存
            _surveyParam.transTime = procTime;  //取引処理時間
            _surveyParam.transInputPinTime = pinTime;   //暗証番号入力時間
        }

    }

    /**
     * 電波状況を設定
     */
    public void setAntennaLevel() {
        _surveyParam.setAntennaLevel();
    }

    /**
     * 端末通番を取得
     * @param increment インクリメントするかのフラグ
     * @return 端末通番
     */
    private int getTermSequence(boolean increment) {
        int termSequence = AppPreference.getTermSequence();
        if (increment) {
            termSequence = termSequence < 999
                         ? termSequence + 1
                         : 1;
            AppPreference.setTermSequence(termSequence);
        }
        return termSequence;
    }

    /**
     * ビジネスタイプをDBに保存する取引種別に変換
     * ビジネスタイプはMainApplicationから取得
     * @return 取引種別
     */
    private Integer getTransType() {
        switch (MainApplication.getInstance().getBusinessType()) {
            case PAYMENT:
            case RECOVERY_PAYMENT:
                return TransMap.TYPE_SALES;    //支払
            case REFUND:
            case RECOVERY_REFUND:
                return TransMap.TYPE_CANCEL;   //取消
            case CHARGE:
                return TransMap.TYPE_CACHE_CHARGE;   //現金チャージ
            case POINT_ADD:
                return TransMap.TYPE_POINT; // ポイント付与
            case POINT_REFUND:
                return TransMap.TYPE_POINT_CANCEL; // ポイント取消
        }
        return null;
    }

    /**
     * 位置情報を取得
     */
    private void getLocation() {
        _surveyParam.setLocation();
    }

    /**
     * DBに保存（OKICA）
     * デモモードでは売上データなし
     * @return 伝票ID
     */
    public int insertOkica() {
        Future<Integer> future = _transExecutorService.submit(() -> {
            UriOkicaDao uriOkicaDao = DBManager.getUriOkicaDao();
            SlipDao slipDao = DBManager.getSlipDao();

            if(_uriOkicaData != null) uriOkicaDao.insertUriData(_uriOkicaData);

            if (_resultParam.transResult != TransMap.RESULT_NEGA_CHECK_ERROR) {
                return (int) slipDao.insertSlipData(_slipData);
            } else {
                return null;
            }
        });

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            //現状発生しない例外
            Timber.e(e);
            return -1;
        }
    }

    /**
     * 計測した時間を設定（OKICA）
     * @param procTime 処理時間
     * @param pinTime 暗証番号入力時間
     */
    public void setProcTimeOkica(int procTime, int pinTime) {
        if (_uriOkicaData != null) {
            //売上データ生成済 値を直接更新
            _uriOkicaData.transTime = procTime;  //取引処理時間
        } else {
            //売上データ未生成 SurveyParamに保存
            _surveyParam.transTime = procTime;  //取引処理時間
        }

    }

    /**
     * 他サービス連携用に各種データをFacadeにセットする
     * @return facade
     */
    public OptionalTransFacade setDataForFacade(OptionalTransFacade facade) {
        facade.setUriData(_uriData);
        facade.setUriOkicaData(_uriOkicaData);
        facade.setResultParam(_resultParam);
        facade.setAmountParam(_amountParam);
        facade.setRefundParam(_refundParam);
        return facade;
    }

    /**
     * 取消の金額取得（画面表示用）
     * @return
     */
    public int getRefundAmount(){
        return _refundParam.oldTransAmount;
    }

    /**
     * 取消の情報取得
     */
    public RefundParam getRefundParam() {
        return _refundParam;
    }

    /**
     * DBに保存
     * POSの時のみ
     * @return 伝票ID
     */
    public void updateCancelFlg() {
        SlipDaoSecure slipDao = DBManager.getSlipDao();

        // 念のため、もう一度POSだけに絞る
        if(AppPreference.isServicePos() && _refundParam.oldSlipId != null) {
            Thread thread = new Thread(() -> {
                slipDao.updateCancelUriId(_refundParam.oldSlipId);
            });
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                // 例外処理
                e.printStackTrace();
            }
        }
    }

    /**
     * チケットサービス連携用に各種データをFacadeにセットする
     * @return facade
     */
    public OptionalTicketTransFacade setTicketDataForFacade(OptionalTicketTransFacade facade) {
        facade.setUriData(_uriData);
        facade.setUriOkicaData(_uriOkicaData);
        facade.setResultParam(_resultParam);
        facade.setAmountParam(_amountParam);
        facade.setRefundParam(_refundParam);
        return facade;
    }

    /**
     * チケット販売時のカード情報をセットする
     * @return facade
     */
    private void setCardInfo() {

        // ブランド名
        if(_slipData.transBrand != null) {
            if (_slipData.transBrand.equals(_resources.getString(R.string.print_brand_credit))) {
                /* クレジット */
                _slipData.cardCategory = String.valueOf(CardCategory.CREDIT.getInt());
                CardBrand.Credit Credit = CardBrand.Credit.Convert(_uriData.creditBrandId);
                switch (Credit) {
                    case VISA:
                        _slipData.cardBrandCode = CardBrand.VISA;
                        _slipData.cardBrandName = CardBrand.VISA_NAME;
                        break;
                    case MASTER:
                        _slipData.cardBrandCode = CardBrand.MASTER;
                        _slipData.cardBrandName = CardBrand.MASTER_NAME;
                        break;
                    case JCB:
                        _slipData.cardBrandCode = CardBrand.JCB;
                        _slipData.cardBrandName = CardBrand.JCB_NAME;
                        break;
                    case AMEX:
                        _slipData.cardBrandCode = CardBrand.AMEX;
                        _slipData.cardBrandName = CardBrand.AMEX_NAME;
                        break;
                    case DINERS:
                        _slipData.cardBrandCode = CardBrand.DINERS;
                        _slipData.cardBrandName = CardBrand.DINERS_NAME;
                        break;
                    case DISCOVER:
                        _slipData.cardBrandCode = CardBrand.DISCOVER;
                        _slipData.cardBrandName = CardBrand.DISCOVER_NAME;
                        break;
                    case UNKNOWN:
                    default:
                        break;
                }
            } else if (_slipData.transBrand.equals(_resources.getString(R.string.print_brand_suica))) {
                /* 交通系電子マネー */
                _slipData.cardCategory = String.valueOf(CardCategory.EMONEY_TRANSPORTATION.getInt());
                CardBrand.EMoneyTransportation EMoneyTransportation = CardBrand.EMoneyTransportation.Convert(_uriData.cardId.substring(0, 2));
                switch (EMoneyTransportation) {
                    case SUICA:
                        _slipData.cardBrandCode = CardBrand.SUICA;
                        _slipData.cardBrandName = CardBrand.SUICA_NAME;
                        break;
                    case SUGOCA:
                        _slipData.cardBrandCode = CardBrand.SUGOCA;
                        _slipData.cardBrandName = CardBrand.SUGOCA_NAME;
                        break;
                    case KITACA:
                        _slipData.cardBrandCode = CardBrand.KITACA;
                        _slipData.cardBrandName = CardBrand.KITACA_NAME;
                        break;
                    case PASMO:
                        _slipData.cardBrandCode = CardBrand.PASMO;
                        _slipData.cardBrandName = CardBrand.PASMO_NAME;
                        break;
                    case MANACA:
                        _slipData.cardBrandCode = CardBrand.MANACA;
                        _slipData.cardBrandName = CardBrand.MANACA_NAME;
                        break;
                    case TOICA:
                        _slipData.cardBrandCode = CardBrand.TOICA;
                        _slipData.cardBrandName = CardBrand.TOICA_NAME;
                        break;
                    case PITAPA:
                        _slipData.cardBrandCode = CardBrand.PITAPA;
                        _slipData.cardBrandName = CardBrand.PITAPA_NAME;
                        break;
                    case ICOCA:
                        _slipData.cardBrandCode = CardBrand.ICOCA;
                        _slipData.cardBrandName = CardBrand.ICOCA_NAME;
                        break;
                    case HAYAKAKEN:
                        _slipData.cardBrandCode = CardBrand.HAYAKAKEN;
                        _slipData.cardBrandName = CardBrand.HAYAKAKEN_NAME;
                        break;
                    case NIMOCA:
                        _slipData.cardBrandCode = CardBrand.NIMOCA;
                        _slipData.cardBrandName = CardBrand.NIMOCA_NAME;
                        break;
                    case UNKNOWN:
                    default:
                        break;
                }
            } else if (_slipData.transBrand.equals(_resources.getString(R.string.print_brand_waon))) {
                /* WAON */
                _slipData.cardCategory = String.valueOf(CardCategory.EMONEY_COMMERCIAL.getInt());
                _slipData.cardBrandCode = CardBrand.WAON;
                _slipData.cardBrandName = CardBrand.WAON_NAME;
            } else if (_slipData.transBrand.equals(_resources.getString(R.string.print_brand_edy_rakute))) {
                /* 楽天Edy */
                _slipData.cardCategory = String.valueOf(CardCategory.EMONEY_COMMERCIAL.getInt());
                _slipData.cardBrandCode = CardBrand.EDY;
                _slipData.cardBrandName = CardBrand.EDY_NAME;
            } else if (_slipData.transBrand.equals(_resources.getString(R.string.print_brand_nanaco))) {
                /* nanaco */
                _slipData.cardCategory = String.valueOf(CardCategory.EMONEY_COMMERCIAL.getInt());
                _slipData.cardBrandCode = CardBrand.NANACO;
                _slipData.cardBrandName = CardBrand.NANACO_NAME;
            } else if (_slipData.transBrand.equals(_resources.getString(R.string.print_brand_id))) {
                /* iD */
                _slipData.cardCategory = String.valueOf(CardCategory.EMONEY_COMMERCIAL.getInt());
                _slipData.cardBrandCode = CardBrand.ID;
                _slipData.cardBrandName = CardBrand.ID_NAME;
            } else if (_slipData.transBrand.equals(_resources.getString(R.string.print_brand_quicpay))) {
                /* QUICPay */
                _slipData.cardCategory = String.valueOf(CardCategory.EMONEY_COMMERCIAL.getInt());
                _slipData.cardBrandCode = CardBrand.QUICKPAY;
                _slipData.cardBrandName = CardBrand.QUICKPAY_NAME;
            } else if (_slipData.transBrand.equals(_resources.getString(R.string.print_brand_okica))) {
                /* OKICA */
                // TODO:仕様確認必要
            } else if (_slipData.transBrand.equals(_resources.getString(R.string.print_brand_codetrans))) {
                /* コード決済 */
                _slipData.cardCategory = String.valueOf(CardCategory.QR.getInt());
                CardBrand.QR QR = CardBrand.QR.Convert(_uriData.codetransPayTypeCode);
                switch (QR) {
                    case PAYPAY:
                        _slipData.cardBrandCode = CardBrand.PAYPAY;
                        _slipData.cardBrandName = CardBrand.PAYPAY_NAME;
                        break;
                    case DOCOMO:
                        _slipData.cardBrandCode = CardBrand.DOCOMO;
                        _slipData.cardBrandName = CardBrand.DOCOMO_NAME;
                        break;
                    case ALIPAY:
                        _slipData.cardBrandCode = CardBrand.ALIPAY;
                        _slipData.cardBrandName = CardBrand.ALIPAY_NAME;
                        break;
                    case ALIPAYPLUS:
                        _slipData.cardBrandCode = CardBrand.ALIPAYPLUS;
                        _slipData.cardBrandName = CardBrand.ALIPAYPLUS_NAME;
                        break;
                    case WECHAT:
                        _slipData.cardBrandCode = CardBrand.WECHAT;
                        _slipData.cardBrandName = CardBrand.WECHATPAY_NAME;
                        break;
                    case MERPAY:
                        _slipData.cardBrandCode = CardBrand.MERPAY;
                        _slipData.cardBrandName = CardBrand.MERUPAY_NAME;
                        break;
                    case RAKUTENPAY:
                        _slipData.cardBrandCode = CardBrand.RAKUTENPAY;
                        _slipData.cardBrandName = CardBrand.RAKUTENPAY_NAME;
                        break;
                    case AUPAY:
                        _slipData.cardBrandCode = CardBrand.AUPAY;
                        _slipData.cardBrandName = CardBrand.AUPAY_NAME;
                        break;
                    case GINKOPAY:
                        _slipData.cardBrandCode = CardBrand.GINKOPAY;
                        _slipData.cardBrandName = CardBrand.GINKOUPAY_NAME;
                        break;
                    case UNKNOWN:
                    default:
                        break;
                }
            } else if (_slipData.transBrand.equals(_resources.getString(R.string.print_brand_cash))) {
                /* 現金決済 */
                _slipData.cardCategory = String.valueOf(CardCategory.CASH.getInt());

            } else {
                // 想定外
            }
        } else {
            // 想定外
        }
    }
}
