package jp.mcapps.android.multi_payment_terminal.ui.emoney.okica;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.Bytes;
import jp.mcapps.android.multi_payment_terminal.data.MoneyType;
import jp.mcapps.android.multi_payment_terminal.data.TransMap;
import jp.mcapps.android.multi_payment_terminal.data.okica.AccessControlInfo;
import jp.mcapps.android.multi_payment_terminal.data.okica.CardBasicInfo;
import jp.mcapps.android.multi_payment_terminal.data.okica.IDi;
import jp.mcapps.android.multi_payment_terminal.data.okica.KaisatsuLogInfo;
import jp.mcapps.android.multi_payment_terminal.data.okica.OkicaExceptions;
import jp.mcapps.android.multi_payment_terminal.data.okica.SFBalanceInfo;
import jp.mcapps.android.multi_payment_terminal.data.okica.SFChargeInfo;
import jp.mcapps.android.multi_payment_terminal.data.okica.SFLogInfo;
import jp.mcapps.android.multi_payment_terminal.data.sam.Constants;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamExceptions;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamResponseTypes;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.devices.OkicaRW;
import jp.mcapps.android.multi_payment_terminal.devices.SamRW;
import jp.mcapps.android.multi_payment_terminal.error.SDKErrors;
import jp.mcapps.android.multi_payment_terminal.model.OptionalTicketTransFacade;
import jp.mcapps.android.multi_payment_terminal.model.OptionalTransFacade;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import jp.mcapps.android.multi_payment_terminal.model.TransLogger;
import jp.mcapps.android.multi_payment_terminal.ui.amount_input.AmountInputSeparationPayFDViewModel;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import timber.log.Timber;

abstract public class BaseEMoneyOkicaViewModel extends ViewModel {
    abstract public void withdrawal();
    abstract public void refund(int slipId);
    abstract public void balance();
    abstract public void charge(int amount);

    public enum States {
        None("アイドル"),
        Waiting("かざし待ち"),
        Processing("処理中"),
        Unprocessed("処理未了発生"),
        Success("取引成功"),
        SuccessBalance("残高照会成功"),
        Error("取引失敗"),
        NoOpTimeout("かざしタイムアウト"),
        Canceling("キャンセル中"),
        Canceled("キャンセル完了"),
        InsufficientBalance("残高不足"),
        WithCashOrCancel("現金併用・中止選択"),
        InsufficientBalanceCancel("残高不足キャンセル"),
        HistoryInquiry("履歴照会"),
        HistoryInquiryCancel("履歴照会キャンセル"),
        ;

        protected final String _state;
        States(String state) {
            _state = state;
        }

        @Override
        @NotNull
        public String toString() {
            return _state;
        }
    }

    protected static class Message {
        protected String line1 = "";
        protected String line2 = "";
        protected String line3 = "";

        public String withoutBalance() {
            return line1 + "\n" + "" + "\n" + line3;
        }

        public String withBalance() {
            return line1 + "\n" + line2 + "\n" + line3;
        }
    }

    protected static class RefundData {
        protected int cardAmount;
        protected int cashAmount;
        protected jp.mcapps.android.multi_payment_terminal.data.okica.IDi IDi;
    }

    public static class ReadData {
        public static final byte[] BLOCK_LIST = new Bytes()
                .add(0x80, 0x03)
                .add(0x81, 0x00)
                .add(0x82, 0x00)
                .add(0x84, 0x00)
                .add(0x86, 0x00)
                .toArray();

        public final CardBasicInfo.Block3 cardBasicInfoB3;  // カード基本情報ブロック3
        public CardBasicInfo.Block3 getCardBasicInfoB3() {
            return cardBasicInfoB3;
        }

        public final AccessControlInfo accessControlInfo;   // アクセス制御情報ブロック0
        public AccessControlInfo getAccessControlInfo() {
            return accessControlInfo;
        }

        public final SFBalanceInfo sfBalanceInfo;           // SF残額情報ブロック0
        public SFBalanceInfo getSFBalanceInfo() {
            return getSFBalanceInfo();
        }

        public final SFLogInfo sfLogInfo;                   // SFログ情報ブロック0
        public SFLogInfo getSFLogInfo() {
            return sfLogInfo;
        }

        public final KaisatsuLogInfo kaisatsuLogInfo;       // 改札ログ情報ブロック0
        public KaisatsuLogInfo getKaisatsuLogInfo() {
            return kaisatsuLogInfo;
        }

        public ReadData(byte[][] readBlock) {
            cardBasicInfoB3 = new CardBasicInfo.Block3(readBlock[0]);

            if (readBlock.length > 1) accessControlInfo = new AccessControlInfo(readBlock[1]);
            else accessControlInfo = null;

            if (readBlock.length > 2) sfBalanceInfo = new SFBalanceInfo(readBlock[2]);
            else sfBalanceInfo = null;

            if (readBlock.length > 3) sfLogInfo = new SFLogInfo(readBlock[3]);
            else sfLogInfo = null;

            if (readBlock.length > 4) kaisatsuLogInfo = new KaisatsuLogInfo(readBlock[4]);
            else kaisatsuLogInfo = null;
        }
    }

    public static class ReadDataBalance {
        public static final byte[] BLOCK_LIST = new Bytes()
                .add(0x80, 0x03)
                .add(0x81, 0x00)
                .add(0x82, 0x00)
                .add(0x84, 0x00)
                .add(0x84, 0x01)
                .add(0x84, 0x02)
                .add(0x86, 0x00)
                .toArray();

        public final CardBasicInfo.Block3 cardBasicInfoB3;  // カード基本情報ブロック3
        public CardBasicInfo.Block3 getCardBasicInfoB3() {
            return cardBasicInfoB3;
        }

        public final AccessControlInfo accessControlInfo;   // アクセス制御情報ブロック0
        public AccessControlInfo getAccessControlInfo() {
            return accessControlInfo;
        }

        public final SFBalanceInfo sfBalanceInfo;           // SF残額情報ブロック0
        public SFBalanceInfo getSFBalanceInfo() {
            return getSFBalanceInfo();
        }


        public final SFLogInfo[] logs = new SFLogInfo[3];  // SFログ情報ブロック0
        public SFLogInfo[] getLogs() {
            return logs;
        }

        public final KaisatsuLogInfo kaisatsuLogInfo;       // 改札ログ情報ブロック0
        public KaisatsuLogInfo getKaisatsuLogInfo() {
            return kaisatsuLogInfo;
        }

        public ReadDataBalance(byte[][] readBlock) {
            cardBasicInfoB3 = new CardBasicInfo.Block3(readBlock[0]);
            accessControlInfo = new AccessControlInfo(readBlock[1]);
            sfBalanceInfo = new SFBalanceInfo(readBlock[2]);
            logs[0] = new SFLogInfo(readBlock[3]);
            logs[1] = new SFLogInfo(readBlock[4]);
            logs[2] = new SFLogInfo(readBlock[5]);
            kaisatsuLogInfo = new KaisatsuLogInfo(readBlock[6]);
        }
    }

    public static class WriteData {
        public static final byte[] BLOCK_LIST = new Bytes()
                .add(0x81, 0x00)
                .add(0x82, 0x00)
                .add(0x84, 0x00)
                .toArray();

        protected final byte[] writeBlock;

        protected final AccessControlInfo accessControlInfo;   // アクセス制御情報ブロック0
        public AccessControlInfo getAccessControlInfo() {
            return accessControlInfo;
        }

        protected final SFBalanceInfo sfBalanceInfo;           // SF残額情報ブロック0
        public SFBalanceInfo getSFBalanceInfo() {
            return sfBalanceInfo;
        }

        protected final SFLogInfo sfLogInfo;                   // SFログ情報ブロック0
        public SFLogInfo getSFLogInfo() {
            return sfLogInfo;
        }

        public WriteData(AccessControlInfo accessControlInfo, SFBalanceInfo sfBalanceInfo, SFLogInfo sfLogInfo) {
            this.accessControlInfo = accessControlInfo;
            this.sfBalanceInfo = sfBalanceInfo;
            this.sfLogInfo = sfLogInfo;

            writeBlock = new Bytes()
                    .add(accessControlInfo.getBlockData())
                    .add(sfBalanceInfo.getBlockData())
                    .add(sfLogInfo.getBlockData())
                    .toArray();
        }
    }

    public static class WriteChargeData {
        public static final byte[] BLOCK_LIST = new Bytes()
                .add(0x81, 0x00)
                .add(0x82, 0x00)
                .add(0x83, 0x00)
                .add(0x84, 0x00)
                .toArray();

        protected final byte[] writeBlock;

        protected final AccessControlInfo accessControlInfo;   // アクセス制御情報ブロック0
        public AccessControlInfo getAccessControlInfo() {
            return accessControlInfo;
        }

        protected final SFBalanceInfo sfBalanceInfo;           // SF残額情報ブロック0
        public SFBalanceInfo getSFBalanceInfo() {
            return sfBalanceInfo;
        }

        protected final SFChargeInfo sfChargeInfo;              // SFロ積増報ブロック0
        public SFChargeInfo getSFChargeInfo() {
            return sfChargeInfo;
        }

        protected final SFLogInfo sfLogInfo;                   // SFログ情報ブロック0
        public SFLogInfo getSFLogInfo() {
            return sfLogInfo;
        }

        public WriteChargeData(AccessControlInfo accessControlInfo, SFBalanceInfo sfBalanceInfo, SFChargeInfo sfChargeInfo, SFLogInfo sfLogInfo) {
            this.accessControlInfo = accessControlInfo;
            this.sfBalanceInfo = sfBalanceInfo;
            this.sfChargeInfo = sfChargeInfo;
            this.sfLogInfo = sfLogInfo;

            writeBlock = new Bytes()
                    .add(accessControlInfo.getBlockData())
                    .add(sfBalanceInfo.getBlockData())
                    .add(sfChargeInfo.getBlockData())
                    .add(sfLogInfo.getBlockData())
                    .toArray();
        }
    }

    public static class HistoryData {
        public final IDi IDi;
        public final SFLogInfo[] logs;
        public final SFBalanceInfo sfBalanceInfo;

        public HistoryData(IDi IDi, SFLogInfo[] logs, SFBalanceInfo sfBalanceInfo) {
            this.IDi = IDi;
            this.logs = logs;
            this.sfBalanceInfo = sfBalanceInfo;
        }
    }

    /**
     * DBに保存するために必要なデータ
     */
    public static class TransactionData {
        protected final long startTime;

        public int getProcTime() {
            return (int) (System.currentTimeMillis() - startTime);
        }

        protected int transAmount;
        public int getTransAmount() {
            return transAmount;
        }

        protected int cardAmount;
        public int getCardAmount() {
            return cardAmount;
        }

        protected int cashAmount;
        public int getCashAmount() {
            return cashAmount;
        }

        protected IDi IDi;
        public IDi getIDi() {
            return IDi;
        }

        protected String transactionDate;
        public String getTransactionDate() {
            return transactionDate;
        }

        protected Integer slipId = null;

        protected boolean isUnprocessed;
        public boolean isUnprocessed() {
            return isUnprocessed;
        }

        protected boolean isNegaCheckError;
        public boolean isNegaCheckError() { return isNegaCheckError; }

        protected ReadData readData;
        public ReadData getReadData() {
            return readData;
        }

        protected WriteData writeData;
        public WriteData getWriteData() {
            return writeData;
        }

        protected WriteChargeData writeChargeData;
        public WriteChargeData getWriteChargeData() {
            return writeChargeData;
        }

        public TransactionData() {
            startTime = System.currentTimeMillis();
        }
    }

    protected static final ExecutorService _pool = Executors.newSingleThreadExecutor();

    // カードタッチされなかった時のタイムアウト時間[ms]
    protected static final int NOOP_TIMEOUT = 30_000;  // 30秒

    // 処理未了発生後のタイムアウト時間[ms]
    protected static final int UNPROCESSED_TIMEOUT = 30_000;  // 30秒

    protected final MainApplication _app = MainApplication.getInstance();
    protected final Handler _handler = new Handler(Looper.getMainLooper());
    protected final SoundManager _soundManager = SoundManager.getInstance();
    protected final float _soundVolume = AppPreference.getSoundPaymentVolume() / 10f;
    protected Integer _soundId = null;

    // 現金併用時の一時退避用 欲しい情報は処理開始時間とIDi
    public TransactionData _tmpTransData = new TransactionData();

    protected Integer _slipId = null;
    public Integer getSlipId() {
        return _slipId;
    }

    public int _balance = 0;
    public int getBalance() {
        return _balance;
    }

    protected HistoryData _historyData;
    public HistoryData getHitoryData() {
        return _historyData;
    }

    protected final MutableLiveData<States> _state = new MutableLiveData<>(States.None);
    public MutableLiveData<States> getState() {
        return _state;
    }
    public void setState(States state) {

        Timber.i("OKICA取引状態: %s ⇒ %s", _state.getValue(), state);
        /* OKICA取引状態によってタイマーを停止 */
        timerStopForState(state);

        // Fragment側で監視処理が多重で発動する可能性があるので状態は変わった時だけセットする
        if (_state.getValue() != state) {
            // 処理未了になったら再タッチ音を案内を鳴らす
            if (state == States.Unprocessed) {
                makeSound(R.raw.okica_unfinished, true);
            }

            // 処理未了から状態が変わったら音を止める
            if (_state.getValue() == States.Unprocessed && _soundId != null) {
                _soundManager.stop(_soundId);
            }

            if (state == States.Success) {
                _app.setErrorCode(null);
                makeSound(R.raw.complete_okica, false);
            }

            if (state == States.Error || state == States.InsufficientBalance) {
                makeSound(R.raw.okica_ng, false);
            }

            _handler.post(() -> {
                _state.setValue(state);
            });
        }
    }

    protected final MutableLiveData<String> _message = new MutableLiveData<>("");
    public MutableLiveData<String> getMessage() {
        return _message;
    }
    public void setMessage(String msg) {
        Timber.i(msg);
        _handler.post(() -> {
            _message.setValue(msg);
        });
    }

    protected boolean _doCancel = false;
    public void cancel() {
        switch (_state.getValue()) {
            case Waiting:
                setState(States.Canceling);
                _doCancel = true;
                stopTimer();
                OkicaRW.terminate();
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

    protected Timer _timer = null;
    protected void startTimer(long delay) {
        _timer = new Timer();
        _timer.schedule(createTimerTask(), delay);
    }

    protected void stopTimer() {
        if (_timer != null) {
            _timer.cancel();
            _timer = null;
        }
    }

    protected void restartTimer(long delay) {
        stopTimer();
        startTimer(delay);
    }

    // cancelするとTimerTaskは使えなくなるので新しいインスタンスを返す
    protected TimerTask createTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                while (true) {
                    if (_state.getValue() == States.Waiting || _state.getValue() == States.Unprocessed) {
                        OkicaRW.terminate();
                        break;
                    }
                }
            }
        };
    }

    /**
     * OKICA取引状態によってタイマーを停止
     *
     * @param state 取引状態
     */
    protected void timerStopForState(States state) {

        if(state == States.Success ||                   //取引成功
                state == States.SuccessBalance ||       //残高照会成功
                state == States.Error ||                //取引失敗
                state == States.Canceling ||            //キャンセル中
                state == States.InsufficientBalance ){  //残高不足
            /* タイマー停止 */
            stopTimer();
        }
    }

    /**
     * 取引結果をDBに保存します
     *
     * @param transData 取引データ
     */
    protected void saveTransactionRecord(TransactionData transData) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPANESE);
        transData.transactionDate = sdf.format(new Date());

        final TransLogger transLogger = new TransLogger();

        // オフラインなのでアンテナレベルの情報は必要ないが他のマネーに体裁を合わせる
        transLogger.setAntennaLevel();
        transLogger.setProcTime(transData.getProcTime(), 0);

        if (transData.isUnprocessed) {
            transLogger.setTransResult(TransMap.RESULT_UNFINISHED, TransMap.DETAIL_UNFINISHED);
        } else if (transData.isNegaCheckError) {
            transLogger.setTransResult(TransMap.RESULT_NEGA_CHECK_ERROR, TransMap.DETAIL_NEGA_CHECK_ERROR);
        } else {
            transLogger.setTransResult(TransMap.RESULT_SUCCESS, TransMap.DETAIL_NORMAL);   //取引結果
        }

        if (transData.cashAmount > 0) {
            transLogger.setCashTogetherAmount(transData.cardAmount);
        }

        // 取消の場合
        if (transData.slipId != null) {
            transLogger.setRefundParam(transData.slipId);
        }

        transLogger.okica(transData);

        _slipId = transLogger.insertOkica();
        if (AppPreference.isPosTransaction()) {
            // 通常の取引レコード以外の取引情報を作成する
            OptionalTransFacade optionalTransFacade = new OptionalTransFacade(MoneyType.OKICA);
            optionalTransFacade = transLogger.setDataForFacade(optionalTransFacade);
            optionalTransFacade.CreateReceiptData(_slipId); // 取引明細書、取消票、領収書のデータを作成
            optionalTransFacade.CreateByUriOkicaData(); // DBにセット
        }

        if (AppPreference.isTicketTransaction()) {
            // チケット販売時の取引情報を作成する
            OptionalTicketTransFacade optionalTicketTransFacade = new OptionalTicketTransFacade(MoneyType.OKICA);
            optionalTicketTransFacade = transLogger.setTicketDataForFacade(optionalTicketTransFacade);
            optionalTicketTransFacade.CreateReceiptData(_slipId); // 取引明細書、取消票、領収書のデータを作成
        }
    }

    /**
     * クリーンアップを行います
     * FragmentのonDestroyView()で呼び出します
     */
    public void cleanup() {
        stopTimer();
    }

    /**
     * ネガカード化します
     *
     * @param auth カード相互認証レスポンス
     * @param accessControlInfo 読取したアクセス制御情報
     *
     */
    protected boolean fallNega(SamResponseTypes.MutualAuthenticationRWSAM auth, AccessControlInfo accessControlInfo) {
        byte[] wBlockList = new Bytes()
                .add(AccessControlInfo.SERVICE_CODE, 0x00)
                .toArray();

        byte[] blockData = accessControlInfo
                .copy()
                .setNegaBit()
                .incrementIkkenMeisaiId()
                .getBlockData();

        OkicaRW.write(auth.getIDt(), wBlockList, blockData);
        return false;
    }

    /**
     * 伝票IDから取消データを取得します
     *
     * @param slipId 伝票ＩＤ
     *
     * @return 取消データ
     */
    protected RefundData getRefundData(int slipId) {
        final SlipData slipData = DBManager.getSlipDao().getOneById(slipId);

        final RefundData refundData = new RefundData();
        refundData.cardAmount = slipData.transAmount;
        refundData.cashAmount = slipData.transCashTogetherAmount;

        refundData.IDi = new IDi(McUtils.hexStringToBytes(slipData.transId));

        return refundData;
    }

    /**
     * リトライするかエラー終了するかどうかを返します
     * エラーの場合は本メソッド内でエラー処理をおこない呼び出し元は処理を終了させます
     *
     * @param e エラーオブジェクト
     *
     * @return リトライ可能な場合はtrue
     */
    protected boolean retryOrError(Throwable e, Message msg, boolean isUnprocessed) {
        Timber.e("エラーオブジェクト: %s", e.getClass().getSimpleName());

        msg.line3 = "お取扱いできません";

        // 未了発生の場合
        if (isUnprocessed) {
            if (e instanceof OkicaExceptions.RWTimeoutException) {
                // カード読取のタイムアウトはリトライ
                setState(States.Unprocessed);
                msg.line3 = "もう一度、タッチしてください";
                setMessage(msg.withoutBalance());
                return true;
            }

            setState(States.Error);
            setMessage(msg.withBalance());

            if (e instanceof OkicaExceptions.TerminationException) {
                // 処理未了タイムアウトエラー
                setErrorCode(R.string.error_type_okica_unprocessed_timeout_error);
            } else {
                // 処理未了発生後の予期せぬエラー
                setErrorCode(R.string.error_type_okica_unprocessed_unexpected_error);
            }

            return false;
        }

        // キャンセルされた場合
        if (_doCancel) {
            setState(States.Canceled);
            return false;
        }

        if (e instanceof OkicaExceptions.RWTimeoutException) {
            msg.line3 = "タッチしてください";
            setMessage(msg.withoutBalance());
            setState(States.Waiting);
            return true;
        }

        else if (e instanceof OkicaExceptions.OkicaSDKException) {
            int code = ((OkicaExceptions.OkicaSDKException) e).getCode();

            Timber.e("OKICA SDKエラーコード: %s", code);

            setMessage(msg.withoutBalance());
            setState(States.Error);

            // SDKの仕様が不明なので実際に試して発生したエラーを元にエラー処理を行っています
            if (code == SDKErrors.ECOMM) {
                // 複数枚かざした時に発生したエラー
                // 本来は共通判定の処理枚数判定で行うが先に例外が発生してしまうのでここで処理する
                _app.setErrorCode(CommonJudge.Result.MultipleCardsError.getErrorCode());
            } else {
                setErrorCode(R.string.error_type_okica_card_sdk_error);
            }

            return false;
        }
        else if (e instanceof OkicaExceptions.TerminationException) {
            // ここにくるまでに未了発生タイムアウトとキャンセルの処理はしているのでこの場合はカードタッチタイムアウト
            setState(States.NoOpTimeout);
            setErrorCode(R.string.error_type_okica_wait_timeout_error);

            return false;
        }
        else if (e instanceof OkicaExceptions.SamCommandException) {
            setMessage(msg.withoutBalance());
            setState(States.Error);

            final Throwable inEx = ((OkicaExceptions.SamCommandException) e).getInnerException();

            Timber.e("SAMエラーオブジェクト: %s", inEx.getClass().getSimpleName());

            if (inEx instanceof SamExceptions.SamSDKException) {
                int code = ((SamExceptions.SamSDKException) inEx).getCode();

                Timber.e("SAM SDKエラーコード: %s", code);

                // 次回以降通信できなくなる可能性があるのでSAM認証をやり直す
                _app.isInitFeliCaSAM(SamRW.open(Constants.MC_NORMAL_KEY, SamRW.States.Normal) == SamRW.OpenResult.SUCCESS);

                setErrorCode(R.string.error_type_okica_sam_sdk_error);
            }
            else if (inEx instanceof SamExceptions.RWStatusException) {
                // この例外は共通判定の読取判定エラーに該当
                Timber.d(inEx.toString());
                _app.setErrorCode(CommonJudge.Result.ReadError.getErrorCode());
            }
            else if (inEx instanceof SamExceptions.SyntaxErrorException) {
                setErrorCode(R.string.error_type_okica_transaction_error);
                // 次回以降通信できなくなる可能性があるのでSAM認証をやり直す
                _app.isInitFeliCaSAM(SamRW.open(Constants.MC_NORMAL_KEY, SamRW.States.Normal) == SamRW.OpenResult.SUCCESS);
            }
            else if (inEx instanceof SamExceptions.SequenceMisMatchException) {
                // SAMの応答のシーケンス番号が不一致だった場合
                setErrorCode(R.string.error_type_okica_transaction_error);
                // 次回以降通信できなくなる可能性があるのでSAM認証をやり直す
                _app.isInitFeliCaSAM(SamRW.open(Constants.MC_NORMAL_KEY, SamRW.States.Normal) == SamRW.OpenResult.SUCCESS);
            }
            else if (inEx instanceof SamExceptions.IllegalStatusWordException) {
                // これはコマンドデータの不整合なのでバグが無い限り発生しない
                setErrorCode(R.string.error_type_okica_transaction_error);
            } else {
                /*
                 * ハンドルしていないエラー
                 * ここに到達する場合はバグ
                 */
                Timber.e("予期せぬ例外の発生 :%s", e.getMessage());
                setErrorCode(R.string.error_type_okica_transaction_error);
            }

            return false;
        }
        else {
            setState(States.Error);
            setMessage(msg.withoutBalance());
            setErrorCode(R.string.error_type_okica_transaction_error);

            return false;
        }
    }

    /**
     * Stringリソースを指定してエラーコードをセットします
     *
     * @param id
     */
    protected void setErrorCode(@StringRes int id) {
        _app.setErrorCode(_app.getString(id));
    }

    /**
     * 音声を再生します
     *
     * @param id 音声ファイルのraw resource
     * @param repeat リピートするかどうか
     */
    protected void makeSound(@RawRes int id, boolean repeat) {
        _soundId = _soundManager.load(_app, id, 1);
        _soundManager.setOnLoadCompleteListener((soundPool, soundId, status) -> {
            // soundPool.play(soundId, _soundVolume, _soundVolume, 1, repeat ? -1 : 0, 1);
        });
    }

    //ADD-S BMT S.Oyama 2024/09/19 フタバ双方向向け改修
    private MutableLiveData<AmountInputSeparationPayFDViewModel> _amountInputSeparationPayFDViewModel = new MutableLiveData<>(null);
    public MutableLiveData<AmountInputSeparationPayFDViewModel> getAmountInputSeparationPayFDViewModel() {
        return _amountInputSeparationPayFDViewModel;
    }
    public void setAmountInputSeparationPayFDViewModel(AmountInputSeparationPayFDViewModel viewModel) {
        _amountInputSeparationPayFDViewModel.setValue( viewModel);
    }
    //ADD-E BMT S.Oyama 2024/09/19 フタバ双方向向け改修

}
