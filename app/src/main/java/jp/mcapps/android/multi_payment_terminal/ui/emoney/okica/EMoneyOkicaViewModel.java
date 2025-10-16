package jp.mcapps.android.multi_payment_terminal.ui.emoney.okica;

import static jp.mcapps.android.multi_payment_terminal.data.okica.Constants.COMPANY_CODE_BUPPAN;
import static jp.mcapps.android.multi_payment_terminal.devices.OkicaRW.KEY_TOTAL;
import static jp.mcapps.android.multi_payment_terminal.devices.OkicaRW.SetStartTime;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.data.okica.AccessControlInfo;
import jp.mcapps.android.multi_payment_terminal.data.okica.ICMaster;
import jp.mcapps.android.multi_payment_terminal.data.okica.IDi;
import jp.mcapps.android.multi_payment_terminal.data.okica.OkicaCardResponse;
import jp.mcapps.android.multi_payment_terminal.data.okica.SFBalanceInfo;
import jp.mcapps.android.multi_payment_terminal.data.okica.SFChargeInfo;
import jp.mcapps.android.multi_payment_terminal.data.okica.SFLogInfo;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamResponseTypes.*;
import jp.mcapps.android.multi_payment_terminal.devices.OkicaRW;
import jp.mcapps.android.multi_payment_terminal.devices.SamRW;
import jp.mcapps.android.multi_payment_terminal.model.McTerminal;
import jp.mcapps.android.multi_payment_terminal.ui.Converters;
import timber.log.Timber;

public class EMoneyOkicaViewModel extends BaseEMoneyOkicaViewModel {

    private final MutableLiveData<String> _toastMessage = new MutableLiveData<>();
    public LiveData<String> getToastMessage() {
        return _toastMessage;
    }

    /**
     * 引去を行います
     */
    public void withdrawal() {
        setState(States.Waiting);
        makeSound(R.raw.emoney_touch_default, false);

        _pool.submit(() -> {
            final TransactionData transData = _tmpTransData != null
                    ? _tmpTransData
                    : new TransactionData();

            transData.isUnprocessed = true;
            transData.isNegaCheckError = false;

            transData.transAmount = Amount.getFixedAmount();

            if (_balance > 0) {
                // 現金併用の時
                if (AppPreference.isWishcash1yenEnabled()) {
                    /* 現金併用１円単位設定：有効 */
                    transData.cardAmount = _balance;
                    transData.cashAmount = _tmpTransData.transAmount - _tmpTransData.cardAmount;
                } else {
                    /* 現金併用１円単位設定：無効 */
                    transData.cardAmount = _balance/10*10;
                    transData.cashAmount = _tmpTransData.transAmount - _tmpTransData.cardAmount/10*10;
                }

            } else {
                transData.cardAmount = _tmpTransData.transAmount;
                transData.cashAmount = 0;
            }

            // 支払前残額 初回のRead時に値をセット
            int beforeBalance = 0;
            int afterBalance = 0;

            final Message msg = new Message();

            msg.line1 = "OKICA　支払 " + Converters.integerToNumberFormat(transData.cardAmount) + "円";
            msg.line3 = "タッチしてください";

            setMessage(msg.withoutBalance());

            // 処理未了発生フラグ
            boolean unprocessedFlg = false;

            boolean isFirstContact = true;

            startTimer(NOOP_TIMEOUT);

            while (true) {

                final OkicaCardResponse<Polling> pollingResp = OkicaRW.polling();

                if (pollingResp.hasError()) {
                    if (retryOrError(pollingResp.getError(), msg, unprocessedFlg)) {
                        continue;
                    } else {
                        if (unprocessedFlg) {
                            saveTransactionRecord(transData);
                        }
                        break;
                    }
                }

                // setState(States.Processing);

                SetStartTime(KEY_TOTAL, System.currentTimeMillis());

                msg.line3 = "カードを離さないでください";
                setMessage(msg.withoutBalance());

                final byte[] IDm = pollingResp.getData().getIDmPMm()[0].getIDm();

                final OkicaCardResponse<MutualAuthenticationRWSAM> authResp = OkicaRW.authentication(IDm);

                if (authResp.hasError()) {
                    if (retryOrError(authResp.getError(), msg, unprocessedFlg)) {
                        continue;
                    } else {
                        if (unprocessedFlg) {
                            saveTransactionRecord(transData);
                        }
                        break;
                    }
                }

//                if (transData.IDi != null && !transData.IDi.equals(authResp.getData().getIDi())) {
//                    Timber.e("別カードタッチを検出");
//
//                    if (unprocessedFlg) {
//                        setState(States.Error);
//                        msg.line3 = "お取扱いできません";
//                        setMessage(msg.withBalance());
//                        setErrorCode(R.string.error_type_okica_unprocessed_another_card_touch_error);
//
//                        saveTransactionRecord(transData);
//                    } else {
//                        // ここに入るのは現金併用時に別カードタッチされた時
//                        setState(States.Error);
//                        msg.line3 = "お取扱いできません";
//                        setMessage(msg.withoutBalance());
//                        setErrorCode(R.string.error_type_okica_another_card_touch_error);
//                    }
//
//                    break;
//                }

                final byte[] IDt = authResp.getData().getIDt();

                final OkicaCardResponse<ReadBlock> rResp = OkicaRW.read(IDt, ReadData.BLOCK_LIST);

                if (rResp.hasError()) {
                    if (retryOrError(rResp.getError(), msg, unprocessedFlg)) {
                        continue;
                    } else {
                        if (unprocessedFlg) {
                            saveTransactionRecord(transData);
                        }
                        break;
                    }
                }

                final ReadData readData = new ReadData(rResp.getData().getBlockData());

//                Timber.d("------ 読取データ -----");
//                Timber.d("%s", readData.cardBasicInfoB3);
//                Timber.d("%s", readData.accessControlInfo);
//                Timber.d("%s", readData.sfBalanceInfo);
//                Timber.d("%s", readData.sfLogInfo);
//                Timber.d("%s", readData.kaisatsuLogInfo);

                if (transData.IDi == null) {
                    // 最初に読取したカードのIDiを保存する(別カードタッチ検出のため)
                    transData.IDi = new IDi(authResp.getData().getIDi());
                }

                // ブロック変えている場合は落ちるので飛ばす
                if (OkicaRW.GetWriteDisable() == false) {

                    if (isFirstContact) {
                        isFirstContact = false;

                        // 最初にカード読込みしたら処理未了タイマーにセットしなおす
                        restartTimer(UNPROCESSED_TIMEOUT);

                        transData.readData = readData;

                        beforeBalance = readData.accessControlInfo.getPurseAmount();
                        afterBalance = beforeBalance - transData.cardAmount;
                        msg.line2 = "支払前残高" + Converters.integerToNumberFormat(beforeBalance) + "円";
                    } else if (readData.accessControlInfo.getPurseAmount() == (beforeBalance - transData.cardAmount)) {
                        // 処理未了後に引去済みを確認
                        setState(States.Success);

                        msg.line2 = "残高" + Converters.integerToNumberFormat(beforeBalance - transData.cardAmount) + "円";
                        msg.line3 = "ありがとうございました";
                        setMessage(msg.withBalance());

                        if (transData.cashAmount > 0) {
                            _app.setCashValue(transData.cashAmount);
                        }

                        transData.isUnprocessed = false;
                        saveTransactionRecord(transData);

                        break;
                    }

                    final CommonJudge.Result commonJudgeResult = CommonJudge.execute(
                            pollingResp.getData(),
                            authResp.getData(),
                            rResp.getData(),
                            readData.cardBasicInfoB3,
                            readData.accessControlInfo,
                            readData.sfBalanceInfo,
                            readData.sfLogInfo,
                            readData.kaisatsuLogInfo,
                            true);

                    if (commonJudgeResult != CommonJudge.Result.Success) {
                        Timber.e("共通判定　総合判定NG %s", commonJudgeResult.getMessage());
                        if (commonJudgeResult == CommonJudge.Result.NegaCheckError) {
                            transData.isNegaCheckError = true;
                            fallNega(authResp.getData(), readData.accessControlInfo);
                        }

                        setState(States.Error);
                        msg.line3 = "お取扱いできません";
                        setMessage(msg.withoutBalance());

                        _app.setErrorCode(commonJudgeResult.getErrorCode());

                        transData.isUnprocessed = false;
                        if (transData.isNegaCheckError == true) {
                            saveTransactionRecord(transData);
                        }
                        break;
                    }


                    // 残額0
                    if (beforeBalance <= 0) {
                        setState(States.Error);
                        msg.line2 = "支払前残高" + Converters.integerToNumberFormat(beforeBalance) + "円";
                        msg.line3 = "残高がありません";
                        setMessage(msg.withBalance());
                        setErrorCode(R.string.error_type_okica_insufficient_balance_error);
                        break;
                    }

                    // 残額不足
                    if (afterBalance < 0) {

                        // 現金併用１円単位設定：無効 且つ 残額10円未満
                        if (AppPreference.isWishcash1yenEnabled() == false && beforeBalance < 10) {
                            setState(States.Error);
                            msg.line2 = "支払前残高" + Converters.integerToNumberFormat(beforeBalance) + "円";
                            msg.line3 = "残高不足です";
                            setMessage(msg.withBalance());
                            setErrorCode(R.string.error_type_okica_insufficient_balance_error);
                            break;
                        }

                        _tmpTransData = transData;
                        _balance = beforeBalance;

                        setState(States.InsufficientBalance);
                        msg.line3 = "残高不足です";
                        setMessage(msg.withBalance());
                        setErrorCode(R.string.error_type_okica_insufficient_balance_error);

                        _handler.postDelayed(() -> {
                            setState(States.WithCashOrCancel);
                        }, 5000);
                        break;
                    }

                    final AccessControlInfo wAccessControlInfo = readData.accessControlInfo
                            .copy()
                            .setPurseAmount(afterBalance)  // 読取値 - 引去額
                            .incrementIkkenMeisaiId();     // 読み取り値+1

                    final SFBalanceInfo wSFBalanceInfo = readData.sfBalanceInfo
                            .copy()
                            .setBalance(afterBalance)  // 処理後パース金額
                            .incrementExecId();        // 読取値+1

                    // SFログデータ生成
                    final SFLogInfo wSFLogInfo = new SFLogInfo()
                            .setTypeCode(1)                                 // 種別コード  1: 関連事業分野
                            .setModelCode(71)                               // 機種コード 71: 係員操作型物販端末
                            .setSFUchiwake(0)                               // SF内訳 0固定
                            .setProcessingType(70)                          // 処理種別詳細 70: 物販利用
                            .setDepositClass(0)                             // 入金区分 0: 現金
                            .setStationType(0)                              // 利用駅種別 0: SF利用
                            .setCurrentDate()                               // 当日年月日
                            .setStation1(0)                                 // 利用駅1(未使用)
                            .setStation2(0)                                 // 利用駅2(未使用)
                            .setBalance(afterBalance)                       // 処理後パース金額
                            .setSFLogId(wSFBalanceInfo.getExecId())         // SFログID (実行ID+1)
                            .setRegionCodeStation1(3)                       // 地域識別コード 利用駅1(未使用)
                            .setRegionCodeStation2(0);                      // 地域識別コード 利用駅1(未使用)

                    final WriteData writeData = new WriteData(wAccessControlInfo, wSFBalanceInfo, wSFLogInfo);

                    transData.writeData = writeData;

                    final OkicaCardResponse<WriteBlock> wResp = OkicaRW.write(IDt, WriteData.BLOCK_LIST, writeData.writeBlock);

                    if (wResp.hasError()) {
                        unprocessedFlg = true;

                        if (retryOrError(wResp.getError(), msg, unprocessedFlg)) {
                            continue;
                        } else {
                            if (unprocessedFlg) {
                                // テスト的に印刷させないようにコメント
                                // saveTransactionRecord(transData);
                            }
                            break;
                        }
                    }

                }

                // 処理時間計測用
                OkicaRW.SetEndTime(OkicaRW.KEY_TOTAL, System.currentTimeMillis());

//                Timber.d("------ 書込データ -----");
//                Timber.d("%s", writeData.accessControlInfo);
//                Timber.d("%s", writeData.sfBalanceInfo);
//                Timber.d("%s", writeData.sfLogInfo);

                String msg2 = "READ (" + SamRW.GetBytes(SamRW.READ_BLOCK) + ")";
                msg2 += " WRITE (" + SamRW.GetBytes(SamRW.WRITE_BLOCK) + ")";
                msg2 += " Elapse(Read:" +  OkicaRW.getElapsed(OkicaRW.KEY_READ_TOTAL) +  "," ;
                msg2 += " Write:" +  OkicaRW.getElapsed(OkicaRW.KEY_WRITE_TOTAL) +  "," ;
                msg2 += " Total:" +  OkicaRW.getElapsed(OkicaRW.KEY_TOTAL) + ")";

                Timber.i("◆◆◆◆◆◆ " + msg2);

                _toastMessage.postValue(msg2);


                // 処理時間計測用
                OkicaRW.PrintTime();
                OkicaRW.clearAll();
                SamRW.PrintTime();
                SamRW.clearAll();


                setState(States.Success);

                msg.line2 = "残高" + Converters.integerToNumberFormat(afterBalance) + "円";
                msg.line3 = "ありがとうございました";

                setMessage(msg.withBalance());

                if (transData.cashAmount > 0) {
                    _app.setCashValue(transData.cashAmount);
                }

                transData.isUnprocessed = false;

                // テスト的に印刷させないようにコメント
                //saveTransactionRecord(transData);
                //Amount.reset();

                break;
            }
        });
    }

    /**
     * 取消を行います
     *
     * @param slipId 伝票ID
     */
    public void refund(int slipId) {
        setState(States.Waiting);
        makeSound(R.raw.emoney_touch_default, false);

        _pool.submit(() -> {
            final RefundData refundData = getRefundData(slipId);

            TransactionData transData = new TransactionData();
            transData.isUnprocessed = true;
            transData.isNegaCheckError = false;

            transData.transAmount = refundData.cardAmount;
            transData.cardAmount = refundData.cardAmount;
            transData.cashAmount = refundData.cashAmount;

            transData.IDi = refundData.IDi;
            transData.slipId = slipId;

            // 取消前残額 初回のRead時に値をセット
            int beforeBalance = 0;

            final Message msg = new Message();

            msg.line1 = "OKICA　支払取消" + Converters.integerToNumberFormat(refundData.cardAmount) + "円";
            msg.line3 = "タッチしてください";

            setMessage(msg.withoutBalance());

            // 処理未了発生フラグ
            boolean unprocessedFlg = false;

            boolean detectCard = false;

            startTimer(NOOP_TIMEOUT);

            while (true) {

                final OkicaCardResponse<Polling> pollingResp = OkicaRW.polling();

                if (pollingResp.hasError()) {
                    if (retryOrError(pollingResp.getError(), msg, unprocessedFlg)) {
                        continue;
                    } else {
                        if (unprocessedFlg) {
                            saveTransactionRecord(transData);
                        }
                        break;
                    }
                }

                setState(States.Processing);

                msg.line3 = "カードを離さないでください";
                setMessage(msg.withoutBalance());

                final byte[] IDm = pollingResp.getData().getIDmPMm()[0].getIDm();

                final OkicaCardResponse<MutualAuthenticationRWSAM> authResp = OkicaRW.authentication(IDm);

                if (authResp.hasError()) {
                    if (retryOrError(authResp.getError(), msg, unprocessedFlg)) {
                        continue;
                    } else {
                        break;
                    }
                }

                // 別カード検出
                if (!refundData.IDi.equals(authResp.getData().getIDi())) {
                    Timber.e("別カードタッチを検出");

                    if (unprocessedFlg) {
                        setState(States.Error);
                        msg.line3 = "お取扱いできません";
                        setMessage(msg.withBalance());
                        setErrorCode(R.string.error_type_okica_unprocessed_another_card_touch_error);
                        saveTransactionRecord(transData);
                    } else {
                        setState(States.Error);
                        msg.line3 = "お取扱いできません";
                        setMessage(msg.withoutBalance());
                        setErrorCode(R.string.error_type_okica_another_card_touch_error);
                    }

                    break;
                }

                final byte[] IDt = authResp.getData().getIDt();

                final OkicaCardResponse<ReadBlock> rResp = OkicaRW.read(IDt, ReadData.BLOCK_LIST);

                if (rResp.hasError()) {
                    if (retryOrError(rResp.getError(), msg, unprocessedFlg)) {
                        continue;
                    } else {
                        if (unprocessedFlg) {
                            saveTransactionRecord(transData);
                        }
                        break;
                    }
                }

                final ReadData readData = new ReadData(rResp.getData().getBlockData());

                Timber.d("------ 読取データ -----");
                Timber.d("%s", readData.cardBasicInfoB3);
                Timber.d("%s", readData.accessControlInfo);
                Timber.d("%s", readData.sfBalanceInfo);
                Timber.d("%s", readData.sfLogInfo);
                Timber.d("%s", readData.kaisatsuLogInfo);


                if (!detectCard) {
                    detectCard = true;
                    transData.readData = readData;
                    beforeBalance = readData.accessControlInfo.getPurseAmount();
                    // 最初にカード読込みしたら処理未了タイマーにセットしなおす
                    restartTimer(UNPROCESSED_TIMEOUT);
                    msg.line2 = "取消前残高" + Converters.integerToNumberFormat(beforeBalance) + "円";
                } else {
                    if (readData.accessControlInfo.getPurseAmount() == (beforeBalance - refundData.cardAmount)) {
                        // 処理未了後に取消済みを確認
                        setState(States.Success);

                        msg.line2 = "残高" + Converters.integerToNumberFormat(beforeBalance - refundData.cardAmount) + "円";
                        msg.line3 = "ありがとうございました";

                        setMessage(msg.withBalance());

                        transData.isUnprocessed = false;
                        saveTransactionRecord(transData);

                        break;
                    }
                }

                final CommonJudge.Result commonJudgeResult = CommonJudge.execute(
                        pollingResp.getData(),
                        authResp.getData(),
                        rResp.getData(),
                        readData.cardBasicInfoB3,
                        readData.accessControlInfo,
                        readData.sfBalanceInfo,
                        readData.sfLogInfo,
                        readData.kaisatsuLogInfo,
                        true);

                if (commonJudgeResult != CommonJudge.Result.Success) {
                    Timber.e("共通判定　総合判定NG %s", commonJudgeResult.getMessage());
                    if (commonJudgeResult == CommonJudge.Result.NegaCheckError) {
                        transData.isNegaCheckError = true;
                        fallNega(authResp.getData(), readData.accessControlInfo);
                    }

                    setState(States.Error);
                    msg.line3 = "お取扱いできません";
                    setMessage(msg.withoutBalance());

                    _app.setErrorCode(commonJudgeResult.getErrorCode());

                    transData.isUnprocessed = false;
                    if (transData.isNegaCheckError == true) {
                        saveTransactionRecord(transData);
                    }
                    break;
                }

                final SfJudge.Result sfJudgeResult = SfJudge.execute(
                        TransMap.TYPE_CANCEL,
                        readData.sfBalanceInfo,
                        readData.sfLogInfo);

                if (sfJudgeResult != SfJudge.Result.Success) {
                    Timber.e("SF判定　総合判定NG %s", sfJudgeResult.getMessage());

                    setState(States.Error);
                    msg.line3 = "お取扱いできません";
                    setMessage(msg.withoutBalance());

                    _app.setErrorCode(sfJudgeResult.getErrorCode());
                    break;
                }

                int afterBalance = beforeBalance + refundData.cardAmount;

                final AccessControlInfo wAccessControlInfo = readData.accessControlInfo
                        .copy()
                        .setPurseAmount(afterBalance)  // 読取値 + 引去額
                        .incrementIkkenMeisaiId();     // 読み取り値+1

                final SFBalanceInfo wSFBalanceInfo = readData.sfBalanceInfo
                        .copy()
                        .setBalance(afterBalance)  // 処理後パース金額
                        .incrementExecId();        // 読取値+1

                // SFログデータ生成
                final SFLogInfo wSFLogInfo = new SFLogInfo()
                        .setTypeCode(1)                                 // 種別コード  1: 関連事業分野
                        .setModelCode(71)                               // 機種コード 71: 係員操作型物販端末
                        .setSFUchiwake(0)                               // SF内訳 0固定
                        .setProcessingType(76)                          // 処理種別詳細 76: 物販利用取消
                        .setDepositClass(0)                             // 入金区分 0: 現金
                        .setStationType(0)                              // 利用駅種別 0: SF利用
                        .setCurrentDate()                               // 当日年月日
                        .setStation1(0)                                 // 利用駅1(未使用)
                        .setStation2(0)                                 // 利用駅2(未使用)
                        .setBalance(afterBalance)                       // 処理後パース金額
                        .setSFLogId(wSFBalanceInfo.getExecId())         // SFログID (実行ID+1)
                        .setRegionCodeStation1(3)                       // 地域識別コード 利用駅1(未使用)
                        .setRegionCodeStation2(0);                      // 地域識別コード 利用駅1(未使用)

                final WriteData writeData = new WriteData(wAccessControlInfo, wSFBalanceInfo, wSFLogInfo);

                transData.writeData = writeData;

                final OkicaCardResponse<WriteBlock> wResp = OkicaRW.write(IDt, WriteData.BLOCK_LIST, writeData.writeBlock);

                if (wResp.hasError()) {
                    unprocessedFlg = true;

                    if (retryOrError(wResp.getError(), msg, unprocessedFlg)) {
                        continue;
                    } else {
                        if (unprocessedFlg) {
                            saveTransactionRecord(transData);
                        }
                        break;
                    }
                }

                Timber.d("------ 書込データ -----");
                Timber.d("%s", writeData.accessControlInfo);
                Timber.d("%s", writeData.sfBalanceInfo);
                Timber.d("%s", writeData.sfLogInfo);

                setState(States.Success);

                msg.line2 = "残高" + Converters.integerToNumberFormat(afterBalance) + "円";
                msg.line3 = "ありがとうございました";

                setMessage(msg.withBalance());

                transData.isUnprocessed = false;
                saveTransactionRecord(transData);

                break;
            }
        });
    }

    /**
     * 残高照会を行います
     */
    public void balance() {
        setState(States.Waiting);
        makeSound(R.raw.emoney_touch_only, false);

        _pool.submit(() -> {
            try {
                final Message msg = new Message();

                msg.line1 = "OKICA　残高照会";
                msg.line3 = "タッチしてください";

                setMessage(msg.withoutBalance());

                // 残照のタイムアウトは30秒
                startTimer(NOOP_TIMEOUT);

                while (true) {

                    final OkicaCardResponse<Polling> pollingResp = OkicaRW.polling();

                    if (pollingResp.hasError()) {
                        if (retryOrError(pollingResp.getError(), msg, false)) {
                            continue;
                        } else {
                            break;
                        }
                    }

                    setState(States.Processing);

                    msg.line3 = "カードを離さないでください";
                    setMessage(msg.withoutBalance());

                    final byte[] IDm = pollingResp.getData().getIDmPMm()[0].getIDm();

                    final OkicaCardResponse<MutualAuthenticationRWSAM> authResp = OkicaRW.authentication(IDm);

                    if (authResp.hasError()) {
                        if (retryOrError(authResp.getError(), msg, false)) {
                            continue;
                        } else {
                            break;
                        }
                    }

                    final byte[] IDt = authResp.getData().getIDt();

                    final OkicaCardResponse<ReadBlock> rResp = OkicaRW.read(IDt, ReadDataBalance.BLOCK_LIST);

                    if (rResp.hasError()) {
                        if (retryOrError(rResp.getError(), msg, false)) {
                            continue;
                        } else {
                            break;
                        }
                    }

                    final ReadDataBalance readData = new ReadDataBalance(rResp.getData().getBlockData());

                    Timber.d("------ 読取データ -----");
                    Timber.d("%s", readData.cardBasicInfoB3);
                    Timber.d("%s", readData.accessControlInfo);
                    Timber.d("%s", readData.sfBalanceInfo);
                    Timber.d("%s", readData.logs[0]);
                    Timber.d("%s", readData.logs[1]);
                    Timber.d("%s", readData.logs[2]);
                    Timber.d("%s", readData.kaisatsuLogInfo);

                    final CommonJudge.Result commonJudgeResult = CommonJudge.execute(
                            pollingResp.getData(),
                            authResp.getData(),
                            rResp.getData(),
                            readData.cardBasicInfoB3,
                            readData.accessControlInfo,
                            readData.sfBalanceInfo,
                            readData.logs[0],
                            readData.kaisatsuLogInfo,
                            false);

                    if (commonJudgeResult != CommonJudge.Result.Success) {
                        Timber.e("共通判定　総合判定NG %s", commonJudgeResult.getMessage());

                        setState(States.Error);
                        msg.line3 = "お取扱いできません";
                        setMessage(msg.withoutBalance());

                        _app.setErrorCode(commonJudgeResult.getErrorCode());
                        break;
                    }

                    final IDi IDi = new IDi(authResp.getData().getIDi());
                    _historyData = new HistoryData(IDi, readData.logs, readData.sfBalanceInfo);

                    setState(States.SuccessBalance);

                    msg.line2 = "残高" + Converters.integerToNumberFormat(readData.accessControlInfo.getPurseAmount()) + "円";
                    msg.line3 = "残高照会完了";

                    setMessage(msg.withBalance());

                    break;
                }

            } catch (Exception e) {
                Timber.e(e);
            }
        });
    }

    @Override
    public void charge(int amount) {
        setState(States.Waiting);
        makeSound(R.raw.emoney_touch_default, false);

        _pool.submit(() -> {
            TransactionData transData = new TransactionData();
            transData.isUnprocessed = true;
            transData.isNegaCheckError = false;

            transData.cardAmount = amount;

            // チャージ前残額 初回のRead時に値をセット
            int beforeBalance = 0;

            final Message msg = new Message();

            msg.line1 = "OKICA　チャージ" + Converters.integerToNumberFormat(amount) + "円";
            msg.line3 = "タッチしてください";

            setMessage(msg.withoutBalance());

            // 処理未了発生フラグ
            boolean unprocessedFlg = false;

            boolean detectCard = false;

            startTimer(NOOP_TIMEOUT);

            while (true) {

                final OkicaCardResponse<Polling> pollingResp = OkicaRW.polling();

                if (pollingResp.hasError()) {
                    if (retryOrError(pollingResp.getError(), msg, unprocessedFlg)) {
                        continue;
                    } else {
                        if (unprocessedFlg) {
                            saveTransactionRecord(transData);
                        }
                        break;
                    }
                }

                setState(States.Processing);

                msg.line3 = "カードを離さないでください";
                setMessage(msg.withoutBalance());

                final byte[] IDm = pollingResp.getData().getIDmPMm()[0].getIDm();

                final OkicaCardResponse<MutualAuthenticationRWSAM> authResp = OkicaRW.authentication(IDm);

                if (authResp.hasError()) {
                    if (retryOrError(authResp.getError(), msg, unprocessedFlg)) {
                        continue;
                    } else {
                        if (unprocessedFlg) {
                            saveTransactionRecord(transData);
                        }
                        break;
                    }
                }

                if (transData.IDi != null && !transData.IDi.equals(authResp.getData().getIDi())) {
                    Timber.e("別カードタッチを検出");

                    if (unprocessedFlg) {
                        setState(States.Error);
                        msg.line3 = "お取扱いできません";
                        setMessage(msg.withBalance());
                        setErrorCode(R.string.error_type_okica_unprocessed_another_card_touch_error);

                        saveTransactionRecord(transData);
                    }

                    break;
                }

                final byte[] IDt = authResp.getData().getIDt();

                final OkicaCardResponse<ReadBlock> rResp = OkicaRW.read(IDt, ReadData.BLOCK_LIST);

                if (rResp.hasError()) {
                    if (retryOrError(rResp.getError(), msg, unprocessedFlg)) {
                        continue;
                    } else {
                        if (unprocessedFlg) {
                            saveTransactionRecord(transData);
                        }
                        break;
                    }
                }

                final ReadData readData = new ReadData(rResp.getData().getBlockData());

                Timber.d("------ 読取データ -----");
                Timber.d("%s", readData.cardBasicInfoB3);
                Timber.d("%s", readData.accessControlInfo);
                Timber.d("%s", readData.sfBalanceInfo);
                Timber.d("%s", readData.sfLogInfo);
                Timber.d("%s", readData.kaisatsuLogInfo);

                if (transData.IDi == null) {
                    // 最初に読取したカードのIDiを保存する(別カードタッチ検出のため)
                    transData.IDi = new IDi(authResp.getData().getIDi());
                }

                if (!detectCard) {
                    detectCard = true;
                    transData.readData = readData;
                    beforeBalance = readData.accessControlInfo.getPurseAmount();
                    // 最初にカード読込みしたら処理未了タイマーにセットしなおす
                    restartTimer(UNPROCESSED_TIMEOUT);
                    msg.line2 = "チャージ前残高" + Converters.integerToNumberFormat(beforeBalance) + "円";
                } else {
                    if (readData.accessControlInfo.getPurseAmount() == (beforeBalance + amount)) {
                        // 処理未了後にチャージ済みを確認
                        setState(States.Success);

                        msg.line2 = "残高" + Converters.integerToNumberFormat(beforeBalance + amount) + "円";
                        msg.line3 = "ありがとうございました";

                        setMessage(msg.withBalance());

                        transData.isUnprocessed = false;
                        saveTransactionRecord(transData);

                        break;
                    }
                }

                final CommonJudge.Result commonJudgeResult = CommonJudge.execute(
                        pollingResp.getData(),
                        authResp.getData(),
                        rResp.getData(),
                        readData.cardBasicInfoB3,
                        readData.accessControlInfo,
                        readData.sfBalanceInfo,
                        readData.sfLogInfo,
                        readData.kaisatsuLogInfo,
                        true);

                if (commonJudgeResult != CommonJudge.Result.Success) {
                    Timber.e("共通判定　総合判定NG %s", commonJudgeResult.getMessage());
                    if (commonJudgeResult == CommonJudge.Result.NegaCheckError) {
                        transData.isNegaCheckError = true;
                        fallNega(authResp.getData(), readData.accessControlInfo);
                    }

                    setState(States.Error);
                    msg.line3 = "お取扱いできません";
                    setMessage(msg.withoutBalance());

                    _app.setErrorCode(commonJudgeResult.getErrorCode());

                    transData.isUnprocessed = false;
                    if (transData.isNegaCheckError == true) {
                        saveTransactionRecord(transData);
                    }
                    break;
                }

                final ICMaster.Activator activator = _app.getOkicaICMaster().getData().getActivator(COMPANY_CODE_BUPPAN);

                // 残高上限額をオーバー
                if (beforeBalance + amount > activator.getPurseLimitAmount()) {
                    setState(States.Error);
                    msg.line3 = "残高上限額をオーバーします";
                    setMessage(msg.withoutBalance());

                    final String limitAmount = String.format("チャージ限度額は%s円です。\n", Converters.integerToNumberFormat(activator.getMaxCardAmount()));
                    final String chargeAmountMsg = "チャージ金額　：" + Converters.integerToNumberFormat(amount) + "円\n";
                    final String beforeBalanceMsg = "チャージ前残高：" + Converters.integerToNumberFormat(beforeBalance) + "円";
                    final String errorMsg = "@@@" + limitAmount + chargeAmountMsg + beforeBalanceMsg + "@@@";
                    Timber.e(errorMsg);
                    _app.setErrorCode(_app.getString(R.string.error_type_okica_charge_upper_limit_error) + errorMsg);
                    break;
                }

                int afterBalance = beforeBalance + amount;

                final AccessControlInfo wAccessControlInfo = readData.accessControlInfo
                        .copy()
                        .setPurseAmount(afterBalance)  // 読取値 + 引去額
                        .incrementIkkenMeisaiId();     // 読み取り値+1

                int userCode = activator.getCompanyCode() & 0xFF;
                final SFBalanceInfo wSFBalanceInfo = readData.sfBalanceInfo
                        .copy()
                        .setBalance(afterBalance)  // 処理後パース金額
                        .setCurrentDate()          // 当日年月日時分
                        .setRegionNo(12)           // 地域番号 12: 物販
                        .setUserCode(userCode)     // ユーザコード
                        .setDepositClass(0)        // 入金区分
                        .incrementExecId();        // 読取値+1

                long chargePointCode = Integer.valueOf(AppPreference.getOkicaTerminalInfo().machineId);
                final SFChargeInfo wSFChargeInfo = new SFChargeInfo()
                        .setTypeCode(1)                  // 種別コード  1: 関連事業分野
                        .setModelCode(71)                // 機種コード 71: 係員操作型物販端末
                        .setChargePointCode(chargePointCode)  // チャージ箇所コード(物販端末ID)
                        .setChargeAmount(amount);        // 積増額(チャージ金額)

                // SFログデータ生成
                final SFLogInfo wSFLogInfo = new SFLogInfo()
                        .setTypeCode(1)                                 // 種別コード  1: 関連事業分野
                        .setModelCode(71)                               // 機種コード 71: 係員操作型物販端末
                        .setSFUchiwake(0)                               // SF内訳 0固定
                        .setProcessingType(73)                          // 処理種別詳細 73: 物販チャージ
                        .setDepositClass(0)                             // 入金区分 0: 現金
                        .setStationType(0)                              // 利用駅種別 0: SF利用
                        .setCurrentDate()                               // 当日年月日
                        .setStation1(0)                                 // 利用駅1(未使用)
                        .setStation2(0)                                 // 利用駅2(未使用)
                        .setBalance(afterBalance)                       // 処理後パース金額
                        .setSFLogId(wSFBalanceInfo.getExecId())         // SFログID (実行ID+1)
                        .setRegionCodeStation1(3)                       // 地域識別コード 利用駅1(未使用)
                        .setRegionCodeStation2(0);                      // 地域識別コード 利用駅1(未使用)

                final WriteChargeData writeChargeData = new WriteChargeData(wAccessControlInfo, wSFBalanceInfo ,wSFChargeInfo ,wSFLogInfo);

                transData.writeChargeData = writeChargeData;

                final OkicaCardResponse<WriteBlock> wResp = OkicaRW.write(IDt, WriteChargeData.BLOCK_LIST, writeChargeData.writeBlock);

                if (wResp.hasError()) {
                    unprocessedFlg = true;

                    if (retryOrError(wResp.getError(), msg, unprocessedFlg)) {
                        continue;
                    } else {
                        if (unprocessedFlg) {
                            saveTransactionRecord(transData);
                        }
                        break;
                    }
                }

                Timber.d("------ 書込データ -----");
                Timber.d("%s", writeChargeData.accessControlInfo);
                Timber.d("%s", writeChargeData.sfBalanceInfo);
                Timber.d("%s", writeChargeData.sfChargeInfo);
                Timber.d("%s", writeChargeData.sfLogInfo);

                setState(States.Success);

                msg.line2 = "残高" + Converters.integerToNumberFormat(afterBalance) + "円";
                msg.line3 = "ありがとうございました";

                setMessage(msg.withBalance());

                transData.isUnprocessed = false;
                saveTransactionRecord(transData);

                break;
            }
        });
    }
}
