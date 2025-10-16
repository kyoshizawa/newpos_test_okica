package jp.mcapps.android.multi_payment_terminal.ui.emoney.okica;

import com.pos.device.SDKException;
import com.pos.device.picc.FeliCa;
import com.pos.device.picc.PiccReader;

import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.okica.AccessControlInfo;
import jp.mcapps.android.multi_payment_terminal.data.okica.IDi;
import jp.mcapps.android.multi_payment_terminal.data.okica.SFBalanceInfo;
import jp.mcapps.android.multi_payment_terminal.data.okica.SFChargeInfo;
import jp.mcapps.android.multi_payment_terminal.data.okica.SFLogInfo;
import jp.mcapps.android.multi_payment_terminal.ui.Converters;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import timber.log.Timber;

public class DemoEMoneyOkicaViewModel extends BaseEMoneyOkicaViewModel {
    private final PiccReader _piccReader = PiccReader.getInstance();

    private final byte[] COMMAND = new byte[]{0x06, 0x00, (byte) 0xff, (byte) 0xff, 0x01, 0x00};

    private final byte[] EMPTY_BLOCK = new byte[16];

    private final int BALANCE = 15001;

    private final IDi IDi = new IDi(new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x11, (byte) 0xD7});

    private boolean withBalanceFlag = false;

    /**
     * 引去を行います
     */
    public void withdrawal() {
        setState(States.Waiting);
        makeSound(R.raw.emoney_touch_default, false);

        _pool.submit(() -> {

            final TransactionData transData = new TransactionData();

            // デモモードでは未了は無し
            transData.isUnprocessed = false;

            transData.transAmount = Amount.getFixedAmount();

            if (withBalanceFlag) {
                transData.cardAmount = BALANCE;
                transData.cashAmount = transData.transAmount - BALANCE;
            } else {
                transData.cardAmount = transData.transAmount;
                transData.cashAmount = 0;
            }

            transData.IDi = IDi;

            final Message msg = new Message();

            msg.line1 = "OKICA　支払 " + Converters.integerToNumberFormat(transData.cardAmount) + "円";
            msg.line3 = "タッチしてください";

            setMessage(msg.withoutBalance());

            polling();

            if (_state.getValue() == States.Canceled || _state.getValue() == States.NoOpTimeout) {
                return;
            }

            setState(States.Processing);

            msg.line3 = "カードを離さないでください";
            setMessage(msg.withoutBalance());

            try {
                Thread.sleep(500);
            } catch (InterruptedException ignore) {
            }

            if (BALANCE - transData.cardAmount < 0) {
                withBalanceFlag = true;
                _balance = BALANCE;

                setState(States.InsufficientBalance);
                msg.line2 = "支払前残高" + Converters.integerToNumberFormat(BALANCE) + "円";
                msg.line3 = "残高不足です";
                setMessage(msg.withBalance());
                setErrorCode(R.string.error_type_okica_insufficient_balance_error);

                _handler.postDelayed(() -> {
                    setState(States.WithCashOrCancel);
                }, 5000);

                return;
            }

            final AccessControlInfo rAccessControlInfo = new AccessControlInfo(EMPTY_BLOCK)
                    .setPurseAmount(BALANCE);

            transData.readData = new ReadData(new byte[][] {
                    EMPTY_BLOCK, rAccessControlInfo.getBlockData(), EMPTY_BLOCK, EMPTY_BLOCK, EMPTY_BLOCK,
            });

            final AccessControlInfo wAccessControlInfo = new AccessControlInfo(EMPTY_BLOCK)
                    .setPurseAmount(BALANCE - transData.cardAmount);

            final SFBalanceInfo sfBalanceInfo = new SFBalanceInfo(EMPTY_BLOCK);

            final SFLogInfo sfLogInfo = new SFLogInfo(EMPTY_BLOCK);

            transData.writeData = new WriteData(wAccessControlInfo, sfBalanceInfo, sfLogInfo);

            setState(States.Success);

            msg.line2 = "残高" + Converters.integerToNumberFormat(withBalanceFlag ? 0 : BALANCE - transData.transAmount) + "円";
            msg.line3 = "ありがとうございました";

            setMessage(msg.withBalance());

            if (transData.cashAmount > 0) {
                _app.setCashValue(transData.cashAmount);
            }

            saveTransactionRecord(transData);
            Amount.reset();
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

            transData.transAmount = refundData.cardAmount;
            transData.cardAmount = refundData.cardAmount;
            transData.cashAmount = refundData.cashAmount;

            transData.IDi = IDi;
            transData.slipId = slipId;

            final Message msg = new Message();

            msg.line1 = "OKICA　支払取消" + Converters.integerToNumberFormat(refundData.cardAmount) + "円";
            msg.line3 = "タッチしてください";

            setMessage(msg.withoutBalance());

            polling();

            if (_state.getValue() == States.Canceled || _state.getValue() == States.NoOpTimeout) {
                return;
            }

            setState(States.Processing);

            msg.line3 = "カードを離さないでください";
            setMessage(msg.withoutBalance());

            try {
                Thread.sleep(500);
            } catch (InterruptedException ignore) {
            }

            final AccessControlInfo rAccessControlInfo = new AccessControlInfo(EMPTY_BLOCK)
                    .setPurseAmount(BALANCE - refundData.cardAmount);

            transData.readData = new ReadData(new byte[][] {
                    EMPTY_BLOCK, rAccessControlInfo.getBlockData(), EMPTY_BLOCK, EMPTY_BLOCK, EMPTY_BLOCK,
            });

            final AccessControlInfo wAccessControlInfo = new AccessControlInfo(EMPTY_BLOCK)
                    .setPurseAmount(BALANCE);

            final SFBalanceInfo sfBalanceInfo = new SFBalanceInfo(EMPTY_BLOCK);

            final SFLogInfo sfLogInfo = new SFLogInfo(EMPTY_BLOCK);

            transData.writeData = new WriteData(wAccessControlInfo, sfBalanceInfo, sfLogInfo);

            setState(States.Success);

            msg.line2 = "残高" + Converters.integerToNumberFormat(BALANCE) + "円";
            msg.line3 = "ありがとうございました";

            setMessage(msg.withBalance());

            transData.isUnprocessed = false;
            saveTransactionRecord(transData);
        });
    }

    /**
     * 残高照会を行います
     */
    public void balance() {
        setState(States.Waiting);
        makeSound(R.raw.emoney_touch_only, false);

        _pool.submit(() -> {

            final Message msg = new Message();

            msg.line1 = "OKICA　残高照会";
            msg.line3 = "タッチしてください";

            setMessage(msg.withoutBalance());

            polling();

            if (_state.getValue() == States.Canceled || _state.getValue() == States.NoOpTimeout) {
                return;
            }

            setState(States.Processing);

            msg.line3 = "カードを離さないでください";
            setMessage(msg.withoutBalance());

            try {
                Thread.sleep(500);
            } catch (InterruptedException ignore) {
            }

            setState(States.SuccessBalance);

            final ReadDataBalance readData = new ReadDataBalance(new byte[][] {
                    EMPTY_BLOCK,
                    EMPTY_BLOCK,
                    McUtils.hexStringToBytes("993A0000000000002E6D5CCC0300027B"),
                    McUtils.hexStringToBytes("C74C00002E2100000000993A00016CC0"),
                    McUtils.hexStringToBytes("C74600002E2100000000112700016BC0"),
                    McUtils.hexStringToBytes("C74900002E2100000000993A00016AC0"),
                    EMPTY_BLOCK,
            });

            _historyData = new HistoryData(IDi, readData.logs, readData.sfBalanceInfo);

            msg.line2 = "残高" + Converters.integerToNumberFormat(BALANCE) + "円";
            msg.line3 = "残高照会完了";

            setMessage(msg.withBalance());
        });
    }

    @Override
    public void charge(int amount) {
        setState(States.Waiting);
        makeSound(R.raw.emoney_touch_default, false);

        _pool.submit(() -> {

            final TransactionData transData = new TransactionData();

            // デモモードでは未了は無し
            transData.isUnprocessed = false;

            transData.transAmount = amount;
            transData.cardAmount = amount;
            transData.cashAmount = 0;

            transData.IDi = IDi;

            final Message msg = new Message();

            msg.line1 = "OKICA　チャージ" + Converters.integerToNumberFormat(amount) + "円";
            msg.line3 = "タッチしてください";

            setMessage(msg.withoutBalance());

            polling();

            if (_state.getValue() == States.Canceled || _state.getValue() == States.NoOpTimeout) {
                return;
            }

            setState(States.Processing);

            msg.line3 = "カードを離さないでください";
            setMessage(msg.withoutBalance());

            try {
                Thread.sleep(500);
            } catch (InterruptedException ignore) {
            }

            if (BALANCE + amount > 30_000) {
                setState(States.Error);
                msg.line3 = "残高上限額をオーバーします";
                setMessage(msg.withoutBalance());

                final String limitAmount = String.format("チャージ限度額は%s円です。\n", Converters.integerToNumberFormat(30_000));
                final String chargeAmountMsg = "チャージ金額　：" + Converters.integerToNumberFormat(amount) + "円\n";
                final String beforeBalanceMsg = "チャージ前残高：" + Converters.integerToNumberFormat(BALANCE) + "円";
                final String errorMsg = "@@@" + limitAmount + chargeAmountMsg + beforeBalanceMsg + "@@@";
                Timber.e(errorMsg);
                _app.setErrorCode(_app.getString(R.string.error_type_okica_charge_upper_limit_error) + errorMsg);

                return;
            }

            final AccessControlInfo rAccessControlInfo = new AccessControlInfo(EMPTY_BLOCK)
                    .setPurseAmount(BALANCE);

            transData.readData = new ReadData(new byte[][] {
                    EMPTY_BLOCK, rAccessControlInfo.getBlockData(), EMPTY_BLOCK, EMPTY_BLOCK, EMPTY_BLOCK,
            });

            final AccessControlInfo wAccessControlInfo = new AccessControlInfo(EMPTY_BLOCK)
                    .setPurseAmount(BALANCE + amount);

            final SFBalanceInfo sfBalanceInfo = new SFBalanceInfo(EMPTY_BLOCK);

            final SFChargeInfo sfChargeInfo = new SFChargeInfo(EMPTY_BLOCK);

            final SFLogInfo sfLogInfo = new SFLogInfo(EMPTY_BLOCK);

            transData.writeChargeData = new WriteChargeData(wAccessControlInfo, sfBalanceInfo, sfChargeInfo, sfLogInfo);

            setState(States.Success);

            msg.line2 = "残高" + Converters.integerToNumberFormat(BALANCE + amount) + "円";
            msg.line3 = "ありがとうございました";

            setMessage(msg.withBalance());

            saveTransactionRecord(transData);
        });
    }

    @Override
    public void cancel() {
        switch (_state.getValue()) {
            case Waiting:
                setState(States.Canceled);
                break;
            case WithCashOrCancel:
                setState(States.InsufficientBalanceCancel);
                break;
            case SuccessBalance:
                setState(States.HistoryInquiryCancel);
                break;
            default:
                Timber.i("キャンセル無効");
                break;
        }
    }

    public void polling() {
        _piccReader.selectCarrierType(PiccReader.MIF_TYPE_C1);
        long startTime = System.currentTimeMillis();
        boolean timeOutFlg = false;

        while (true) {

            if (System.currentTimeMillis() - startTime >= NOOP_TIMEOUT && timeOutFlg == false) {
                timeOutFlg = true;
                setState(States.NoOpTimeout);
                setErrorCode(R.string.error_type_okica_wait_timeout_error);
                Timber.d("time out");
            }

            try {
                if (_state.getValue() == States.Canceled
                        || _state.getValue() == States.InsufficientBalanceCancel
                        || _state.getValue() == States.HistoryInquiryCancel
                        || _state.getValue() == States.NoOpTimeout) {
                    return;
                }

                byte[] resp = FeliCa.getInstance().transmit(COMMAND, 0);
                if (resp != null) {
                    return;
                }
            } catch (SDKException ignore) {
            }
        }
    }
}
