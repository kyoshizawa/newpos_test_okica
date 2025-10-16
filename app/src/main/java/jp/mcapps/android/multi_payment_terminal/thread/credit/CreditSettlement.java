package jp.mcapps.android.multi_payment_terminal.thread.credit;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.RawRes;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

import com.pos.device.SDKException;
import com.pos.device.emv.TerminalAidInfo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.database.DBManager;
import jp.mcapps.android.multi_payment_terminal.database.history.slip.SlipData;
import jp.mcapps.android.multi_payment_terminal.error.CreditErrorCodes;
import jp.mcapps.android.multi_payment_terminal.error.CreditErrorMap;
import jp.mcapps.android.multi_payment_terminal.thread.credit.data.CreditResult;
import jp.mcapps.android.multi_payment_terminal.thread.emv.Constants.*;
import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvCLProcess;
import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvProcessing;
import jp.mcapps.android.multi_payment_terminal.thread.emv.ParamConfig;
import jp.mcapps.android.multi_payment_terminal.thread.emv.utils.ISOUtil;
import jp.mcapps.android.multi_payment_terminal.thread.emv.utils.TLVUtil;
import jp.mcapps.android.multi_payment_terminal.ui.credit_card.CreditCardScanFragment;
import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.CardAnalyze;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.OnlineAuth;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.OnlineAuthCancel;
import timber.log.Timber;

import static jp.mcapps.android.multi_payment_terminal.thread.credit.CardType.INMODE_IC;
import static jp.mcapps.android.multi_payment_terminal.thread.credit.CardType.INMODE_MAG;
import static jp.mcapps.android.multi_payment_terminal.thread.credit.CardType.INMODE_NFC;

public class CreditSettlement {
    public interface CreditSettlementListener {
        void OnProcStart();
        void OnProcEnd();
        void OnError(String errorCode);
        void OnSound(int id);
        void selectApplication(String[] applications);
        void timeoutWaitCard(String errorCode);
        void cancelPin();
    }

    public interface DemoEventListener {
        void onPinInputRequired();
    }

    private final String LOGTAG = "クレジットメイン";

    /* クレジット決済処理状態 */
    public final static int k_CREDIT_STAT_IDLE = 0;                 // IDLE
    public final static int k_CREDIT_STAT_PROCESSING = 1;           // クレジット決済処理中
    public final static int k_CREDIT_STAT_CANCEL_PROCESSING = 2;    // クレジット取消処理中
    public final static int k_CREDIT_STAT_ONLINE_OK = 3;            // オンライン処理成功
    public final static int k_CREDIT_STAT_ONLINE_CANCEL_OK = 4;     // オンライン取消処理成功
    public final static int k_CREDIT_STAT_ONLINE_NG = -1;           // オンライン処理失敗
    public final static int k_CREDIT_STAT_ONLINE_CANCEL_NG = -2;    // オンライン取消処理失敗
    public final static int k_CREDIT_STAT_OFFLINE = -3;             // オフライン判定
    public final static int k_CREDIT_STAT_ERROR = -4;               // エラー
    public final static int k_CREDIT_STAT_TIMEOUT_WAIT_CARD = -5;   // カード読み込み待ちタイムアウト
    public final static int k_CREDIT_STAT_CANCEL_PIN = -6;          // PIN入力キャンセル
    /* カード読み込み待ちタイムアウト(ms) */
    public final static int k_WAIT_CARD_TIMEOUT = 30000;
    /* クレジットカードの各トラックのデータサイズ */
    public final static int k_JIS1_TRACK1_SIZE = 79;
    public final static int k_JIS1_TRACK2_SIZE = 40;
    public final static int k_JIS2_SIZE = 72;
    /* 鍵種別 */
    public final static int k_KEYTYPE_RSA3072 = 1;              // RSA-3072
    /* マネー区分 */
    public final static int k_MONEY_KBN_CREDIT = 0;             // クレジット
    public final static int k_MONEY_KBN_GINREN = 1;             // 銀聯
    /* 磁気・IC区分 */
    public final static int k_MSIC_KBN_UNKNOWN = -1;
    public final static int k_MSIC_KBN_MS = 0;                  // 磁気
    public final static int k_MSIC_KBN_IC = 1;                  // 接触IC
    public final static int k_MSIC_KBN_CONTACTLESS_IC = 2;      // 非接触IC（予約）
    /* POSエントリモード */
    public final static String k_POSEMODE_IC = "05";            // ICカード（接触）
    public final static String k_POSEMODE_MS = "90";            // 磁気ストライプ

    public final static String k_POSEMODE_CL = "072";            // 磁気ストライプ
    public final static String k_POSEMODE_PIN_EXISTS = "1";     // PINあり
    public final static String k_POSEMODE_PIN_NOTEXISTS = "2";  // PINなし
    /* オンオフ区分 */
    public final static int k_ONOFF_KBN_OFFLINE = 0;            // オフライン
    public final static int k_ONOFF_KBN_ONLINE = 1;             // オンライン
    public final static int k_ONOFF_KBN_ONLINE_IMPOSSIBLE = 2;  // オンライン不能
    /* チップコンディションコード */
    public final static String k_CHIPCC_MS = "00";              // IC対応端末における磁気ストライプ取引
    public final static String k_CHIPCC_FB1 = "01";             // IC読取に成功したが磁気ストライプへフォールバック
    public final static String k_CHIPCC_FB2 = "02";             // IC読取に失敗し磁気ストライプへフォールバック
    public final static String k_CHIPCC_IC = "FF";              // IC取引
    /* サイン欄印字指定 */
    public final static int k_SIGNATURE_UNNECESSARY = 0;        // サイン不要
    public final static int k_SIGNATURE_NECESSARY = 1;          // サイン必要
    public final static int k_SIGNATURE_SPACE_NONE = 2;         // 署名欄なし

    public final static int k_OK = 0;
    public final static int k_ERROR_UNKNOWN = -1;

    public final static int k_ERROR_READ_AGAIN = -2;  // カード読み込み失敗(非接触IC)

    public final static int k_ERROR_READ_AGAIN_AND_SEEPHONE = -3;  // 携帯電話の指示にしたがってください

    /* メッセージ */
    public enum CreditHandlMessage {
        START_CREDIT,
        START_IC_PROC,
        START_CL_PROC,
        START_MS_AUTH,
        START_CREDIT_CANCEL,
        START_INPUT_PIN,
        START_ANNOUNCE_SIGNATURE,
        START_SELECT_APP,
        RETRY_WAIT_CARD,
        RETRY_WAIT_CARD_AND_SEEPHONE,
        END_CREDIT,
        START_DEMO_IC_PROC,
        START_DEMO_CL_PROC,
        START_DEMO_AUTH,
    }

    /* オーソリNG要因 */
    public enum AuthErrorReason {
        NONE,
        WAITRES_TOUT,                                           // オーソリ応答待ちタイムアウト
        RES_NG,                                                 // オーソリNG
        RES_VERI_NG,                                            // オーソリ応答検証NG
    }

    private int _creditProcStatus = k_CREDIT_STAT_IDLE;         // クレジット決済処理状態
    private String _creditErrorCode;                            // クレジット決済エラーコード
    private int _moneyKbn;                                      // マネー区分
    private int _msICKbn;                                       // 磁気・IC区分取得
    private String _posEntryMode = "000";                       // POSエントリモード
    private int _onOffKbn;                                      // オンオフ区分
    private String _chipCC;                                     // チップコンディションコード
    private int _signatureFlag;                                 // サイン欄印字フラグ
    private String[] _msTrackInfo;                              // 磁気カードトラック情報
    private String _trackData;                                  // トラックデータ
    private String _rsaDataForPayment;                          // 売上用暗号化データ
    private static boolean _pinSkipFBFlg;                       // フォールバック有効フラグ
    private static boolean _pinLessFlg;                         // PINレス取扱可否フラグ
    private static int _pinLessLimit;                           // PINレス限度額
    private static boolean _msAvailableFlg;                     // MS移行フラグ
    private static boolean _brandEnableFlg;                     // ブランド有効/無効フラグ
    private static int _acquirerOnlinePayType;                  // アクワイアラ指定オンライン要求支払種別
    private static String _txnTypeCd;                           // 取引分類コード
    private static String _chargeTypeCd;                        // チャージタイプコード
    private static String _appPersonalInfo;                     // AP個別情報
    private static boolean _icCardExist;                        // スロットのカード有無
    private Date _procStartDate;                                // クレジット処理開始時間
    private Date _procEndDate;                                  // クレジット処理終了時間
    private Date _pinStartDate;                                 // PIN入力開始時間
    private Date _pinEndDate;                                   // PIN入力終了時間

    private static CreditSettlement _creSettle = null;
    private Handler _handler = null;
    private IccStateMachine _iccStateMachine;
    private DemoStateMachine _demoStateMachine;
    private MsOnlineAuthStateMachine _msOnlineAuthStateMachine;

    public CreditCardScanFragment _creFragment;
    public FragmentActivity _fragmentActivity;

    private SlipData _slipData;
    public SlipData getSlipData() {
        return _slipData;
    }

    private ActivateIF _activateIF;
    public ActivateIF getActivateIF() {
        return _activateIF;
    }

    public CreditSettlementListener _listener = null;
    public DemoEventListener _demoListener = null;

    /* 売上・印刷用データ */
    public CreditResult.Result _creditResult = null;

    /* EMV処理クラス */
    final public EmvProcessing _emvProc = EmvProcessing.newInstance();

    final private EmvCLProcess _emvCLProc = new EmvCLProcess(this);
    /* MCセンター通信クラス */
    final public McCenterCommManager _mcCenterCommManager = new McCenterCommManager(this);
    /* AID情報 */
    final public TerminalAidInfo _aidInfo = new TerminalAidInfo();

    final private BehaviorSubject<CLState> _clState = BehaviorSubject.create();
    public BehaviorSubject<CLState> getCLState() {
        return _clState;
    }

    private String DemoTransType = null;

    //ADD-S BMT S.Oyama 2024/11/15 フタバ双方向向け改修
    private int _creditProcStatusDemoBackup = k_CREDIT_STAT_IDLE;         // クレジット決済処理状態(デモモード向けバックアップ)
    //ADD-E BMT S.Oyama 2024/11/15 フタバ双方向向け改修

    private CreditSettlement() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                _handler = new creditSettlementHandle(Looper.myLooper());
                Looper.loop();
            }
        }).start();
    }

    public static CreditSettlement getInstance(){
        return _creSettle == null ? _creSettle = new CreditSettlement() : _creSettle;
    }

    public void setListener(CreditSettlementListener listener) {
        _listener = listener;
    }

    public boolean isSameListener(CreditSettlementListener listener) {
        return listener.equals(_listener);
    }

    public EmvProcessing getEmvProcInstance() {
        return _emvProc;
    }

    public EmvCLProcess getEmvCLProcInstance() {
        return _emvCLProc;
    }

    /**
     * クレジット決済処理状態設定・取得
     */
    public void setCreditProcStatus(int stat) {
        _creditProcStatus = stat;
    }
    public int getCreditProcStatus() {
        return _creditProcStatus;
    }

    /**
     * クレジット決済エラー設定・取得
     */
    public void setCreditError(String code) {
        _creditErrorCode = code;
        setCreditProcStatus(k_CREDIT_STAT_ERROR);
    }
    public void setWaitCardTimeout() {
        _creditErrorCode = CreditErrorCodes.T01;
        setCreditProcStatus(k_CREDIT_STAT_TIMEOUT_WAIT_CARD);
    }
    public void setCancelPin() {
        setCreditProcStatus(k_CREDIT_STAT_CANCEL_PIN);
    }
    public String getCreditErrorCode() {
        return CreditErrorMap.get(_creditErrorCode);
    }
    public String getCreditErrorCodeDetail() {
        return _creditErrorCode;
    }

    /**
     * メッセージハンドラ
     */
    private class creditSettlementHandle extends Handler {
        creditSettlementHandle(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            CreditHandlMessage creMsg = (CreditHandlMessage)msg.obj;
            switch (creMsg) {
                case START_CREDIT:              // クレジット決済開始（IC、または、磁気）
                case START_IC_PROC:             // ICカード処理開始
                case START_CL_PROC:             // ICカード処理開始
                case START_MS_AUTH:             // オンラインオーソリ取消開始
                case START_CREDIT_CANCEL:       // クレジット取消開始
                case RETRY_WAIT_CARD:           // 再度カード読み込み待ちへ移行
                case RETRY_WAIT_CARD_AND_SEEPHONE:           // 再度カード読み込み待ちへ移行
                case START_DEMO_IC_PROC:        // デモICカード処理開始
                case START_DEMO_CL_PROC:        // デモICカード処理開始
                case START_DEMO_AUTH:           // デモオーソリ開始
                    procCredit(creMsg);
                    break;
                case START_INPUT_PIN:           // PIN入力開始
                    if (_listener != null) _listener.OnSound(R.raw.credit_input_pin);
                    break;
                case START_ANNOUNCE_SIGNATURE:  // サイン音声案内開始
                    if (_listener != null) _listener.OnSound(R.raw.credit_signature);
                    break;
                case START_SELECT_APP:
                    selectApplication();
                    break;
                case END_CREDIT:                // クレジット決済終了
                    procCreditEnd();
                    break;
                default:
                    Timber.tag(LOGTAG).e("Recv unknown message in handleMessage");
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    /**
     * メッセージ送信
     */
    public void sendMessage(CreditHandlMessage msg) {
        while ( null == _handler ) {
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Message message = _handler.obtainMessage();
        message.obj = msg;
        _handler.sendMessage(message);
    }

    /**
     * クレジット決済開始のメッセージ送信
     */
    public void startCredit(CreditCardScanFragment fragment, FragmentActivity activity, ActivateIF activateIF) {
        _creFragment = fragment;
        _fragmentActivity = activity;
        _activateIF = activateIF;
        setCreditError(CreditErrorCodes.T99); // エラーの初期設定
        sendMessage(CreditHandlMessage.START_CREDIT);
    }

    /**
     * クレジット取消開始のメッセージ送信
     */
    public void startCreditCancel(CreditCardScanFragment fragment, FragmentActivity activity, SlipData slipData, ActivateIF activateIF) {
        _creFragment = fragment;
        _fragmentActivity = activity;
        _slipData = slipData;
        _activateIF = activateIF;
        setCreditError(CreditErrorCodes.T99); // エラーの初期設定
        sendMessage(CreditHandlMessage.START_CREDIT_CANCEL);
    }

    /**
     * PIN入力開始のメッセージ送信
     */
    public void startInputPin() {
        // PIN入力の音声案内
        Timber.tag(LOGTAG).i("PIN入力音声案内鳴動");
        sendMessage(CreditHandlMessage.START_INPUT_PIN);
    }

    /**
     * サイン音声案内のメッセージ送信
     */
    public void startAnnounceSignature() {
        // サインが必要な場合の音声案内
        Timber.tag(LOGTAG).i("サイン音声案内鳴動");
        sendMessage(CreditHandlMessage.START_ANNOUNCE_SIGNATURE);
    }

    /**
     * アプリケーション選択のメッセージ送信
     */
    public void startSelectApplication(String[] apps) {
        setAppList(apps);
        sendMessage(CreditHandlMessage.START_SELECT_APP);
    }

    /**
     * デモオーソリ開始のメッセージ送信
     */
    public void startDemoAuth() {
        sendMessage(CreditHandlMessage.START_DEMO_AUTH);
    }

    /**
     * アプリケーション選択ダイアログ表示
     */
    public void selectApplication() {
        Timber.tag(LOGTAG).i("アプリケーション選択ダイアログ表示");
        String[] apps = getAppList();
        if (_listener != null && apps != null) {
            _listener.selectApplication(apps);
        } else {
            Timber.tag(LOGTAG).e("リスナーがありません in selectApplication");
        }
    }

    /**
     * クレジット決済処理
     */
    private void procCredit(CreditHandlMessage msg){
        new Thread(() -> {
                int ret = 0;
                switch (msg) {
                    // クレジット決済開始（IC、または、磁気）
                    case START_CREDIT:
                    // クレジット取消開始（IC、または、磁気）
                    case START_CREDIT_CANCEL:
                        // クレジット開始時間記録
                        if (AppPreference.isMoneyContactless()) {
                            _clState.onNext(CLState.None);
                        }

                        resetCreditProcTime();
                        resetPinProcTime();
                        setCreditStartTime();
                        initCreditVariable();
                        // クレジット情報保存開始
                        _moneyKbn = k_MONEY_KBN_CREDIT;
                        startSaveCreditResult();

                        if (CreditHandlMessage.START_CREDIT == msg) {
                            Timber.tag(LOGTAG).i("クレジット決済開始");
                            setCreditProcStatus(k_CREDIT_STAT_PROCESSING);
                            //ADD-S BMT S.Oyama 2024/11/15 フタバ双方向向け改修
                            _creditProcStatusDemoBackup = k_CREDIT_STAT_PROCESSING;
                            //ADD-E BMT S.Oyama 2024/11/15 フタバ双方向向け改修
                        } else {
                            Timber.tag(LOGTAG).i("クレジット取消開始");
                            setCreditProcStatus(k_CREDIT_STAT_CANCEL_PROCESSING);
                            //ADD-S BMT S.Oyama 2024/11/15 フタバ双方向向け改修
                            _creditProcStatusDemoBackup = k_CREDIT_STAT_CANCEL_PROCESSING;
                            //ADD-E BMT S.Oyama 2024/11/15 フタバ双方向向け改修
                        }

                        if (AppPreference.isDemoMode()) {
                            // デモモード
                            DemoTransType = "支払";
                            if (CreditHandlMessage.START_CREDIT_CANCEL == msg) DemoTransType = "取消";

                            // カード検出
                            _demoStateMachine = new DemoStateMachine(_creSettle, cardListener, _emvProc, _emvCLProc);
                            _demoStateMachine.changeStatus(DemoStateMachine.Status.STATUS_DETECT_CARD);
                            try {
                                ret = _demoStateMachine.start();
                            } catch (SDKException e) {
                                e.printStackTrace();
                            }
                        } else {
                            // カードデータ保護用公開鍵取得、カード検出
                            _iccStateMachine = new IccStateMachine(_creSettle, cardListener, _emvProc, _emvCLProc);
                            _iccStateMachine.changeStatus(IccStateMachine.Status.STATUS_DETECT_CARD);
                            try {
                                ret = _iccStateMachine.start();
                            } catch (SDKException e) {
                                e.printStackTrace();
                            }
                        }

                        if (CreditSettlement.k_OK != ret) {
                            setCreditProcStatus(k_CREDIT_STAT_ERROR);
                            sendMessage(CreditHandlMessage.END_CREDIT);
                            break;
                        }
                        if (_listener != null) _listener.OnSound(R.raw.credit_start);

                        /* カード検出／キャンセル／タイムアウト時に CardListener.callback() が呼び出される */

                        break;
                    // ICカード処理開始
                    case START_IC_PROC:
                        Timber.tag(LOGTAG).i("接触ICカード処理開始");
                        if (_listener == null) {
                            Timber.tag(LOGTAG).e("リスナーがありません in START_IC_PROC");
                            return;
                        }
                        _listener.OnProcStart();
                        _iccStateMachine.changeStatus(IccStateMachine.Status.STATUS_IC_PROC);
                        try {
                            ret = _iccStateMachine.start();
                        } catch (SDKException e) {
                            e.printStackTrace();
                        }
                        // クレジット決済終了
                        sendMessage(CreditHandlMessage.END_CREDIT);
                        break;
                    // 非接触ICカード処理開始
                    case START_CL_PROC:
                        Timber.tag(LOGTAG).i("非接触ICカード処理開始");
                        if (_listener == null) {
                            Timber.tag(LOGTAG).e("リスナーがありません in START_IC_PROC");
                            return;
                        }
                        _listener.OnProcStart();
                        _iccStateMachine.changeStatus(IccStateMachine.Status.STATUS_CL_PROC);
                        try {
                            ret = _iccStateMachine.start();
                        } catch (SDKException e) {
                            e.printStackTrace();
                        }
                        if (ret == k_ERROR_READ_AGAIN) {
                            Timber.tag(LOGTAG).e("Retry wait card");
                            sendMessage(CreditHandlMessage.RETRY_WAIT_CARD);
                        } else if (ret == k_ERROR_READ_AGAIN_AND_SEEPHONE) {
                            Timber.tag(LOGTAG).e("Retry wait phone");
                            sendMessage(CreditHandlMessage.RETRY_WAIT_CARD_AND_SEEPHONE);
                        } else {
                            // クレジット決済終了
                            sendMessage(CreditHandlMessage.END_CREDIT);
                        }
                        break;
                    // 磁気カード処理（決済・取消）開始　※ICカード（接続・非接続）取消も含む
                    case START_MS_AUTH:
                        Timber.tag(LOGTAG).i("磁気カード処理（決済・取消）開始　※ICカード（接続・非接続）取消も含む");
                        if (_listener == null) {
                            Timber.tag(LOGTAG).e("リスナーがありません in START_MS_AUTH");
                            return;
                        }
                        _listener.OnProcStart();
                        _msOnlineAuthStateMachine = new MsOnlineAuthStateMachine(_creSettle, cardListener, _emvProc, _emvCLProc);
                        _msOnlineAuthStateMachine.changeStatus(MsOnlineAuthStateMachine.Status.STATUS_MS_ONLINE_AUTH);
                        try {
                            ret = _msOnlineAuthStateMachine.start();
                        } catch (SDKException e) {
                            e.printStackTrace();
                        }
                        // 決済or取消終了
                        sendMessage(CreditHandlMessage.END_CREDIT);
                        break;
                    // 再度カード読み込み待ちへ移行
                    case RETRY_WAIT_CARD:
                        Timber.tag(LOGTAG).i("再度カード読み込み待ち開始");
                        setMSICKbn(k_MSIC_KBN_UNKNOWN);
                        //ADD-S BMT S.Oyama 2024/11/15 フタバ双方向向け改修
                        if (AppPreference.isDemoMode()) {
                            _demoStateMachine.changeStatus(DemoStateMachine.Status.STATUS_NONE);
                            try {
                                ret = _demoStateMachine.start();
                            } catch (SDKException e) {
                                e.printStackTrace();
                            }
                            _demoStateMachine.changeStatus(DemoStateMachine.Status.STATUS_DETECT_CARD);
                            try {
                                ret = _demoStateMachine.start();
                            } catch (SDKException e) {
                                e.printStackTrace();
                            }
                            setCreditProcStatus(_creditProcStatusDemoBackup);
                        } else {
                            _iccStateMachine.changeStatus(IccStateMachine.Status.STATUS_NONE);
                            try {
                                ret = _iccStateMachine.start();
                            } catch (SDKException e) {
                                e.printStackTrace();
                            }
                            _iccStateMachine.changeStatus(IccStateMachine.Status.STATUS_DETECT_CARD);
                            try {
                                ret = _iccStateMachine.start();
                            } catch (SDKException e) {
                                e.printStackTrace();
                            }
                        }
                        //ADD-E BMT S.Oyama 2024/11/15 フタバ双方向向け改修
                        if (_listener != null) _listener.OnSound(R.raw.credit_read_error);
                        break;
                    case RETRY_WAIT_CARD_AND_SEEPHONE:
                        Timber.tag(LOGTAG).i("「携帯電話の指示に従ってください」案内");
                        setMSICKbn(k_MSIC_KBN_UNKNOWN);
                        _iccStateMachine.changeStatus(IccStateMachine.Status.STATUS_NONE);
                        try {
                            ret = _iccStateMachine.start();
                        } catch (SDKException e) {
                            e.printStackTrace();
                        }
                        _iccStateMachine.changeStatus(IccStateMachine.Status.STATUS_DETECT_CARD);
                        try {
                            ret = _iccStateMachine.start();
                        } catch (SDKException e) {
                            e.printStackTrace();
                        }
                        break;
                    // デモICカード処理開始
                    case START_DEMO_IC_PROC:
                        if(k_CREDIT_STAT_PROCESSING == getCreditProcStatus()) {
                            // 支払
                            Timber.tag(LOGTAG).i("デモ：接触ICカード支払処理開始");
                            _demoStateMachine.changeStatus(DemoStateMachine.Status.STATUS_DEMO_IC_PROC);
                            try {
                                ret = _demoStateMachine.start();
                            } catch (SDKException e) {
                                e.printStackTrace();
                            }
                        } else {
                            // 取消
                            Timber.tag(LOGTAG).i("デモ：接触ICカード取消処理開始");
                            sendMessage(CreditHandlMessage.START_DEMO_AUTH);
                        }
                        break;
                    case START_DEMO_CL_PROC:
                        // 支払
                        Timber.tag(LOGTAG).i("デモ：非接触ICカード処理開始");
                        _demoStateMachine.changeStatus(DemoStateMachine.Status.STATUS_DEMO_CL_PROC);
                        try {
                            ret = _demoStateMachine.start();
                        } catch (SDKException e) {
                            e.printStackTrace();
                        }
                        break;
                    // デモオーソリ開始
                    case START_DEMO_AUTH:
                        Timber.tag(LOGTAG).i("デモ：オーソリ開始");
                        if (_listener == null) {
                            Timber.tag(LOGTAG).e("リスナーがありません in START_IC_PROC");
                            return;
                        }
                        _listener.OnProcStart();
                        _demoStateMachine.changeStatus(DemoStateMachine.Status.STATUS_DEMO_AUTH);
                        try {
                            ret = _demoStateMachine.start();
                        } catch (SDKException e) {
                            e.printStackTrace();
                        }
                        // クレジット決済終了
                        sendMessage(CreditHandlMessage.END_CREDIT);
                        break;
                    default:
                        Timber.tag(LOGTAG).e("Recv unknown message in procCredit");
                        break;
                }
        }).start();
    }

    /**
     * クレジット決済処理終了
     */
    private void procCreditEnd() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // クレジット終了時間記録
                setCreditEndTime();

                int stat = getCreditProcStatus();
                switch (stat) {
                    // オンライン処理成功
                    case k_CREDIT_STAT_ONLINE_OK:
                    // オンライン取消処理成功
                    case k_CREDIT_STAT_ONLINE_CANCEL_OK:
                        // オンオフ区分：オンライン
                        if (stat == k_CREDIT_STAT_ONLINE_OK) {
                            Timber.tag(LOGTAG).i("クレジット処理終了：オンライン処理成功");
                        } else {
                            Timber.tag(LOGTAG).i("クレジット処理終了：オンライン取消処理成功");
                        }

                        setOnOffKbn(k_ONOFF_KBN_ONLINE);
                        if (AppPreference.isDemoMode()) {
                            // デモモード
                            saveCreditResultForDemo();
                        } else {
                            // オーソリ応答検証後のクレジット情報保存
                            saveCreditResultAfterAuthVerification();
                        }
                        if (_listener != null) {
                            _listener.OnSound(R.raw.credit_auth_ok);
                            _listener.OnProcEnd();
                        } else {
                            Timber.tag(LOGTAG).e("リスナーがありません in Online OK");
                        }
                        break;
                    // オンライン処理失敗
                    case k_CREDIT_STAT_ONLINE_NG:
                        // オンオフ区分：オンライン不能
                        Timber.tag(LOGTAG).e("クレジット処理終了：オンライン処理失敗");
                        setOnOffKbn(k_ONOFF_KBN_ONLINE_IMPOSSIBLE);

                        if (k_CHIPCC_IC == _chipCC) {
                            // ICカード時のみ拒否売上を生成
                            // オーソリ応答検証後のクレジット情報保存
                            saveCreditResultAfterAuthVerification();
                        }

                        if (_listener != null) {
                            _listener.OnSound(R.raw.credit_auth_ng);
                            _listener.OnError(getCreditErrorCode());
                        } else {
                            Timber.tag(LOGTAG).e("リスナーがありません in Online NG");
                        }
                        break;
                    // オフライン判定
                    case k_CREDIT_STAT_OFFLINE:
                        Timber.tag(LOGTAG).e("クレジット処理終了：オフライン判定");
                        // オンオフ区分：オフライン
                        setOnOffKbn(k_ONOFF_KBN_OFFLINE);
                        if (_listener != null) {
                            _listener.OnError(getCreditErrorCode());
                        } else {
                            Timber.tag(LOGTAG).e("リスナーがありません in Offline");
                        }
                        break;
                    // オンライン取消処理失敗
                    case k_CREDIT_STAT_ONLINE_CANCEL_NG:
                        Timber.tag(LOGTAG).e("クレジット処理終了：オンライン取消処理失敗");
                        if (_listener != null) {
                            _listener.OnSound(R.raw.credit_auth_ng);
                            _listener.OnError(getCreditErrorCode());
                        } else {
                            Timber.tag(LOGTAG).e("リスナーがありません in Online cancel NG");
                        }
                        break;
                    // エラー
                    case k_CREDIT_STAT_ERROR:
                        Timber.tag(LOGTAG).e("クレジット処理終了：エラー発生");
                        if (_listener != null) {
                            _listener.OnSound(R.raw.credit_auth_ng);
                            _listener.OnError(getCreditErrorCode());
                        } else {
                            Timber.tag(LOGTAG).e("リスナーがありません in Other ERROR");
                        }
                        break;
                    // カード読み込み待ちタイムアウト
                    case k_CREDIT_STAT_TIMEOUT_WAIT_CARD:
                        Timber.tag(LOGTAG).e("クレジット処理終了：カード読み込み待ちタイムアウト発生");
                        if (_listener != null) {
                            _listener.timeoutWaitCard(getCreditErrorCode());
                        } else {
                            Timber.tag(LOGTAG).e("リスナーがありません in Tout wait card");
                        }
                        break;
                    case k_CREDIT_STAT_CANCEL_PIN:
                        Timber.tag(LOGTAG).e("クレジット処理終了：PIN入力キャンセル");
                        if (_listener != null) {
                            _listener.cancelPin();
                        } else {
                            Timber.tag(LOGTAG).e("リスナーがありません in Cancel pin");
                        }
                        break;
                    default:
                        Timber.tag(LOGTAG).e("Unknown status in procCreditEnd");
                        if (_listener != null) {
                            _listener.OnError(getCreditErrorCode());
                        } else {
                            Timber.tag(LOGTAG).e("リスナーがありません in Unknown status");
                        }
                        break;
                }
                setCreditProcStatus(k_CREDIT_STAT_IDLE);
            }
        }).start();
    }

    /**
     * カード検出リスナー（IC、または、磁気）
     */
    CardListener cardListener = new CardListener() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void callback(CardInfo cardInfo) {
            int ret = -1;
            /* IC、または、磁気カードを検出したらここに来る */
            if (INMODE_MAG == cardInfo.getCardType()) {
                /* 磁気カードを検出 */
                setMSICKbn(k_MSIC_KBN_MS);
                _chipCC = k_CHIPCC_MS;
                setPosEntryMode(k_POSEMODE_MS, k_POSEMODE_PIN_NOTEXISTS);
                setSignatureFlag(k_SIGNATURE_NECESSARY); // サイン欄あり
                setMsTrackInfo(cardInfo.getTrackNo());
                ret = 0;
            } else if (INMODE_IC == cardInfo.getCardType()) {
                /* ICカードを検出 */
                if (true == cardInfo.isResultFalg()) {
                    setMSICKbn(k_MSIC_KBN_IC);
                    _chipCC = k_CHIPCC_IC;
                    setPosEntryMode(k_POSEMODE_IC, k_POSEMODE_PIN_NOTEXISTS);
                    setSignatureFlag(k_SIGNATURE_NECESSARY); // サイン欄ありで初期設定
                    ret = 0;
                } else {
                    Timber.tag(LOGTAG).e("False isResultFalg");
                    setCreditError(CreditErrorCodes.T99);
                    ret = -1;
                }
            } else if (INMODE_NFC == cardInfo.getCardType()) {
                /* ICカードを検出 */
                if (true == cardInfo.isResultFalg()) {
                    setMSICKbn(k_MSIC_KBN_CONTACTLESS_IC);
                    _chipCC = k_CHIPCC_IC;
                    setPosEntryMode(k_POSEMODE_CL);
                    setSignatureFlag(k_SIGNATURE_NECESSARY); // サイン欄ありで初期設定
                    ret = 0;
                } else {
//                    Timber.tag(LOGTAG).e("False isResultFalg");
//                    setCreditError(CreditErrorCodes.T99);
                    // 再読み込みに回す
                    ret = -1;
                }
            } else {
                // エラーコードは getCard() や onSearchResult() で設定
                ret = -1;
            }

            if (ret == 0) {
                initCreditResult();
                int stat = getCreditProcStatus();
                if (AppPreference.isDemoMode()) {
                    if (_msICKbn == k_MSIC_KBN_IC) {
                        // デモICカード処理開始
                        sendMessage(CreditHandlMessage.START_DEMO_IC_PROC);
                    } else if (_msICKbn == k_MSIC_KBN_CONTACTLESS_IC) {
                        // プラスチックカード・モバイル版関係無く15000円まではサイン欄不要にする
                        if (DemoTransType != null && DemoTransType.equals("取消")) {
                            // 取消時
                            setSignatureFlag(_slipData.transAmount > 15_000 ? k_SIGNATURE_NECESSARY : k_SIGNATURE_SPACE_NONE);
                        } else {
                            // 支払時
                            setSignatureFlag(Amount.getFixedAmount() > 15_000 ? k_SIGNATURE_NECESSARY : k_SIGNATURE_SPACE_NONE);
                        }
                        sendMessage(CreditHandlMessage.START_DEMO_CL_PROC);
                    } else {
                        if (true == checkServiceCode()) {
                            // ICチップ付カードをスワイプした場合、ICカード利用へ誘導
                            setCreditError(CreditErrorCodes.T04);
                            sendMessage(CreditHandlMessage.END_CREDIT);
                        } else {
                            // MSオンラインオーソリ開始
                            sendMessage(CreditHandlMessage.START_DEMO_AUTH);
                        }
                    }
                } else if (k_CREDIT_STAT_PROCESSING == stat) {
                    if (_msICKbn == k_MSIC_KBN_IC) {
                        // ICカード処理開始
                        sendMessage(CreditHandlMessage.START_IC_PROC);
                    } else if (_msICKbn == k_MSIC_KBN_CONTACTLESS_IC) {
                        sendMessage(CreditHandlMessage.START_CL_PROC);
                    } else {
                        if (true == checkServiceCode()) {
                            // ICチップ付カードをスワイプした場合、ICカード利用へ誘導
                            setCreditError(CreditErrorCodes.T04);
                            sendMessage(CreditHandlMessage.END_CREDIT);
                        } else {
                            // MSオンラインオーソリ開始
                            sendMessage(CreditHandlMessage.START_MS_AUTH);
                        }
                    }
                } else if (k_CREDIT_STAT_CANCEL_PROCESSING == stat) {
                    // オンラインオーソリ取消開始（ICクレジット取消もここで処理する）
                    sendMessage(CreditHandlMessage.START_MS_AUTH);
                } else {
                    Timber.tag(LOGTAG).e("Unknown CreditProcStatus");
                    setCreditError(CreditErrorCodes.T99);
                    sendMessage(CreditHandlMessage.END_CREDIT);
                }
            } else {
                if (CreditErrorCodes.T01 == getCreditErrorCodeDetail()) {
                    // カード読み込み待ちタイムアウト
                    Timber.tag(LOGTAG).i("Wait card timeout");
                    setCreditProcStatus(k_CREDIT_STAT_TIMEOUT_WAIT_CARD);
                    sendMessage(CreditHandlMessage.END_CREDIT);
                } else if (k_MSIC_KBN_MS == getMSICKbn()) {
                    // 磁気カードの読み込みが不十分だった場合、再度カード読み込み待ちへ移行
                    Timber.tag(LOGTAG).e("Retry wait card");
                    // エラー履歴に残す
                    setCreditError(CreditErrorCodes.T02);
                    CommonErrorDialog commonErrorDialog = new CommonErrorDialog();
                    commonErrorDialog.ShowErrorMessage(_fragmentActivity, getCreditErrorCode());
                    sendMessage(CreditHandlMessage.RETRY_WAIT_CARD);
                } else {
                    // 処理を中断
                    Timber.tag(LOGTAG).e("Other ERROR");
                    sendMessage(CreditHandlMessage.END_CREDIT);
                }
            }
        }
    };

    /**
     * クレジットカードのトラック情報取得
     */
    public String getTrackInfo() {
        String jis1Track1 = "";
        String jis1Track2 = "";
        String jis2 = "";
        String trackData = "";

        if (getMSICKbn() == k_MSIC_KBN_IC) {
            Timber.tag(LOGTAG).i("接触ICリーダーからカード情報取得成功");

            // JIS1トラック1、JIS2はデータなし

            // JIS1トラック2相当データを取得
            byte[] track2 = getEmvProcInstance().emvGetTrack2Info();
            // JIS1トラック2相当データは圧縮数値になっているためASCIIに変換する
            for (int i = 0; i < track2.length; i++) {
                byte[] tmp = new byte[1];
                tmp[0] = (byte) (((track2[i] >> 4) & 0x0F) + 0x30);
                jis1Track2 += ISOUtil.hexString(tmp);
                tmp[0] = (byte) ((track2[i] & 0x0F) + 0x30);
                jis1Track2 += ISOUtil.hexString(tmp);
            }
        }
        else if (getMSICKbn() == k_MSIC_KBN_CONTACTLESS_IC) {
            Timber.tag(LOGTAG).i("非接触ICリーダーからカード情報取得成功");

            // Mag-Stripeモード非対応のためJIS1トラック1、JIS2はデータなし

            // JIS1トラック2相当データを取得
            byte[] track2 = getEmvCLProcInstance().getTrack2();
            // JIS1トラック2相当データは圧縮数値になっているためASCIIに変換する
            for (int i = 0; i < track2.length; i++) {
                byte[] tmp = new byte[1];
                tmp[0] = (byte) (((track2[i] >> 4) & 0x0F) + 0x30);
                jis1Track2 += ISOUtil.hexString(tmp);
                tmp[0] = (byte) ((track2[i] & 0x0F) + 0x30);
                jis1Track2 += ISOUtil.hexString(tmp);
            }
        }
        else {
            Timber.tag(LOGTAG).i("磁気リーダーからカード情報取得成功");
            String logmsg = "getTrackInfo MAG";
            String sData[] = getMsTrackInfo();
            byte[] bData;

            // JIS1トラック1
            if (0 < sData[0].length()) {
                int idx = sData[0].indexOf("B");
                if (idx == 0) {
                    // 1バイト目がFC（"B"）であればJIS1トラック1
                    // FC（"B"）は省いて保存
                    logmsg += ", JIS 1 Track 1";
                    jis1Track1 = sData[0].substring(idx + 1);
                    bData = jis1Track1.getBytes();
                    jis1Track1 = ISOUtil.hexString(bData);

                } else {
                    // JIS2と判定
                    logmsg += ", JIS 2";
                    jis2 = sData[0];
                    bData = jis2.getBytes();
                    jis2 = ISOUtil.hexString(bData);
                }
            }

            if (0 < sData[1].length()) {
                int idx = sData[1].indexOf("=");
                if (idx >= 0) {
                    // セパレータ（"="）が含まれていればJIS1トラック2
                    logmsg += ", JIS 1 Track 2";
                    jis1Track2 = sData[1];
                    bData = jis1Track2.getBytes();
                    jis1Track2 = ISOUtil.hexString(bData);
                } else {
                    // JIS2と判定
                    logmsg += ", JIS 2";
                    jis2 = sData[1];
                    bData = jis2.getBytes();
                    jis2 = ISOUtil.hexString(bData);
                }
            }
            Timber.tag(LOGTAG).i(logmsg);
        }

        // 最大サイズに満たない分は "0" で埋める
        while (jis1Track1.length() < (CreditSettlement.k_JIS1_TRACK1_SIZE * 2)) {
            jis1Track1 += "0";
        }
        while (jis1Track2.length() < (CreditSettlement.k_JIS1_TRACK2_SIZE * 2)) {
            jis1Track2 += "0";
        }
        while (jis2.length() < (CreditSettlement.k_JIS2_SIZE * 2)) {
            jis2 += "0";
        }

        trackData = jis1Track1 + jis1Track2 + jis2;

        return trackData;
    }

    /**
     * クレジットカードのトラック情報保存・読出
     */
    public void saveTrackData(String data) {
        _trackData = data;
    }
    public String loadTrackData() {
        return _trackData;
    }

    /**
     * マネー区分取得
     */
    public int getMoneyKbn() {
        return _moneyKbn;
    }

    /**
     * 磁気・IC区分設定・取得
     */
    public void setMSICKbn(int kbn) {
        _msICKbn = kbn;
    }
    public int getMSICKbn() {
        return _msICKbn;
    }

    /**
     * 支払方法取得
     */
    public String getPayMethod() {
        // "10" 固定
        return "10";
    }

    /**
     * 商品コード取得
     */
    public String getProductCd() {
        String productCd;

        productCd = AppPreference.getProductCode();

        return productCd;
    }

    /**
     * POSエントリモード設定・取得
     */
    public void setPosEntryMode(String posEntryMode1, String posEntryMode2) {
        if (null != posEntryMode1) {
            // 1バイト目の設定
            _posEntryMode = posEntryMode1.substring(0, 2) + _posEntryMode.substring(2);
        }
        if (null != posEntryMode2) {
            // 2バイト目の設定
            _posEntryMode = _posEntryMode.substring(0, 2) + posEntryMode2.charAt(0);
        }
        _emvProc.emvSetPosEntryMode(_posEntryMode.substring(0, 2));
    }
    public void setPosEntryMode(String posEntryMode) {
        _posEntryMode = posEntryMode;
        _emvProc.emvSetPosEntryMode(_posEntryMode.substring(0, 2));
    }
    public String getPosEntryMode() {
        return _posEntryMode;
    }

    /**
     * PANシーケンスナンバー設定・取得
     */
    public String getPanSeqNo() {
        String panSeqNo = "FFFFFFFF";
        if (_msICKbn == k_MSIC_KBN_IC) {
            byte[] bPanSeqNo = _emvProc.emvGetPanSeqNo();
            if (-1 != bPanSeqNo[0]) {
                panSeqNo = String.format("%08x", bPanSeqNo[0]);
            }
        }
        else if (_msICKbn == k_MSIC_KBN_CONTACTLESS_IC) {
            if (_emvCLProc.getPanSeqNo() != null) {
                panSeqNo = String.format("00000%s", _emvCLProc.getPanSeqNo());
            }
        }
        return panSeqNo;
    }

    /**
     * IC端末対応フラグ取得
     */
    public int getIcTerminalFlg() {
        int icTerminalFlg;

        icTerminalFlg = AppPreference.isMoneyContactless() ? 3 : 1;

        return icTerminalFlg;
    }

    /**
     * オンオフ区分設定・取得
     */
    public void setOnOffKbn(int onOffKbn) {
        _onOffKbn = onOffKbn;
    }
    public int getOnOffKbn() {
        return _onOffKbn;
    }

    /**
     * チップコンディションコード取得
     */
    public String getChipCC() {
        return _chipCC;
    }

    /**
     * 強制オンライン取得
     */
    public int getForcedOnline() {
        // 0固定
        return 0;
    }

    /**
     * 強制承認取得
     */
    public int getForcedApproval() {
        // 0固定
        return 0;
    }

    /**
     * カード有効期限取得
     */
    public String getCardExpDate() {
        // "xx/xx"固定
        return "XX/XX";
    }

    /**
     * アプリケーションラベル設定・取得
     */
    public String get_ApplicationLabel() {
        String applicationLabel = "";
        if (isIC()) {
            byte[] appLabel = _emvProc.emvGetApplicationLabel();
            applicationLabel = new String(appLabel);
        } else if (isCL()) {
            byte[] appLabel = _emvCLProc.getApplicationLabel();
            applicationLabel = new String(appLabel);
        }

        return applicationLabel;
    }

    /**
     * AID取得（カードから取得したフルサイズのAID）
     */
    public String get_fullAID() {
        String sAid = " ";
        if (isIC()) {
            byte[] bAid = _emvProc.emvGetAID();
            sAid = ISOUtil.hexString(bAid);
        } else if (isCL()) {
            byte[] bAid = _emvCLProc.getAID();
            sAid = ISOUtil.hexString(bAid);
        }
        return sAid;
    }
    //ADD-S BMT S.Oyama 2024/10/03 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  ATC取得
     * @note   ATCを取得する IC CL
     * @param [in] なし
     * @retval なし
     * @return String ATC情報 0~65535までの数値を5桁0詰めで返す 取得失敗時は空白5個
     * @private
     */
    /******************************************************************************/
    public String get_ATC() {
        String result = "     ";
        if (isIC()) {
            result = _emvProc.emvGetATC();
        } else if (isCL()) {
            result = _emvCLProc.getATC();
        }
        return result;
    }
    //ADD-S BMT S.Oyama 2024/10/03 フタバ双方向向け改修

    /**
     * AID取得（カード判定で取得した値）
     * ※カード判定応答受信前は不定値が返ります
     */
    public String get_AID() {
        String sAid = _creditResult.aid;
        return sAid;
    }

    /**
     * サイン欄印字フラグ設定・取得
     */
    public void setSignatureFlag(int flg) {
        _signatureFlag = flg;
    }
    public int getSignatureFlag() {
        return _signatureFlag;
    }

    /**
     * フォールバック有効フラグ設定・取得
     */
    public static void setPinSkipFBFlg(boolean flg) {
        _pinSkipFBFlg = flg;
    }
    public static boolean getPinSkipFBFlg() {
        return _pinSkipFBFlg;
    }

    /**
     * PINレス取扱可否フラグ設定・取得
     */
    public static void setPinLessFlg(boolean flg) {
        _pinLessFlg = flg;
    }
    public static boolean getPinLessFlg() {
        return _pinLessFlg;
    }

    /**
     * PINレス限度額設定・取得
     */
    public static void setPinLessLimit(int pinLessLimit) {
        _pinLessLimit = pinLessLimit;
    }
    public static int getPinLessLimit() {
        return _pinLessLimit;
    }

    /**
     * MS移行フラグ設定・取得
     */
    public static void setMsAvailableFlg(boolean flg) {
        _msAvailableFlg = flg;
    }
    public static boolean getMsAvailableFlg() {
        return _msAvailableFlg;
    }

    /**
     * ブランド有効/無効フラグ設定・取得
     */
    public static void setBrandEnableFlg(boolean flg) {
        _brandEnableFlg = flg;
    }
    public static boolean getBrandEnableFlg() {
        return _brandEnableFlg;
    }

    /**
     * アクワイアラ指定オンライン要求支払種別設定・取得
     */
    public static void setAcquirerOnlinePayType(int type) {
        _acquirerOnlinePayType = type;
    }
    public static int getAcquirerOnlinePayType() {
        return _acquirerOnlinePayType;
    }

    /**
     * 取引分類コード設定・取得
     */
    public static void setTxnTypeCd(String code) {
        _txnTypeCd = code;
    }
    public static String getTxnTypeCd() {
        return _txnTypeCd;
    }

    /**
     * チャージタイプコード設定・取得
     */
    public static void setChargeTypeCd(String code) {
        _chargeTypeCd = code;
    }
    public static String getChargeTypeCd() {
        return _chargeTypeCd;
    }

    /**
     * AP個別情報設定・取得
     */
    public static void setAppPersonalInfo(String info) {
        _appPersonalInfo = info;
    }
    public static String getAppPersonalInfo() {
        return _appPersonalInfo;
    }

    /**
     * 磁気カードトラック情報設定・取得
     */
    public void setMsTrackInfo(String[] trackInfo) {
        _msTrackInfo = trackInfo;
    }
    public String[] getMsTrackInfo() {
        return _msTrackInfo;
    }

    /**
     * 売上用暗号化データ設定・取得
     */
    public void setRsaDataForPayment(String rsaData) {
        _rsaDataForPayment = rsaData;
    }
    public String getRsaDataForPayment() {
        return _rsaDataForPayment;
    }

    /**
     * JIS1トラック2のサービスコードチェック
     */
    public boolean checkServiceCode() {
        boolean ret = false;

        String[] trackInfo = getMsTrackInfo();

        int idx = trackInfo[1].indexOf("="); // セパレータを検索
        String str = trackInfo[1].substring(idx + 1);
        if (idx >= 0) {
            if (str.charAt(4) == '2' || str.charAt(4) == '6') {
                ret = true;
            } else {
                ret = false;
            }
        }
        return ret;
    }

    /**
     * 暗証番号（固定値）取得
     * （オフラインPIN認証実行時は、オーソリ要求で送信する暗証番号として固定値を使用する）
     */
    public String getFixedPinNo(String aid) {
        String val = "";
//セゾンより2020でとの回答をもらっていたが本番環境でG42（暗証番号エラー）が返却されたため修正
//        if (aid.contains("A0000000031010") || aid.contains("A0000000041010")) {
//            // VISA or MASTER
//            val = "2020";
//        } else {
            val = "0000";
//        }
        return val;
    }

    /**
     * スロットのカード有無設定・取得
     */
    public void setIcCardExistFlg(boolean flg) {
        _icCardExist = flg;
    }
    public boolean getIcCardExistFlg() {
        return _icCardExist;
    }

    /**
     * クレジット開始時間記録
     */
    public void setCreditStartTime() {
        String date;
        Date dateObj = new Date();
        SimpleDateFormat format = new SimpleDateFormat( "yyMMddHHmmssSSS", Locale.JAPAN );
        date = format.format( dateObj );
        Timber.tag(LOGTAG).i("Credit start time %s", date);
        try {
            _procStartDate = format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * クレジット終了時間記録
     */
    public void setCreditEndTime() {
        String date;
        Date dateObj = new Date();
        SimpleDateFormat format = new SimpleDateFormat( "yyMMddHHmmssSSS", Locale.JAPAN );
        date = format.format( dateObj );
        Timber.tag(LOGTAG).i("Credit end time %s", date);
        try {
            _procEndDate = format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * クレジット処理時間取得（ms）
     */
    public int GetCreditProcTime() {
        long diff = 0;

        if(_procStartDate != null && _procEndDate != null) {
            diff = _procEndDate.getTime() - _procStartDate.getTime();
        }
        Timber.tag(LOGTAG).i("Credit proc time %d ミリ秒", diff);

        return (int)diff;
    }

    /**
     * クレジット処理時間リセット
     */
    public void resetCreditProcTime() {
        _procStartDate = null;
        _procEndDate = null;
    }

    /**
     * PIN入力開始時間記録
     */
    public void setPinStartTime() {
        String date;
        Date dateObj = new Date();
        SimpleDateFormat format = new SimpleDateFormat( "yyMMddHHmmssSSS", Locale.JAPAN );
        date = format.format( dateObj );
        Timber.tag(LOGTAG).i("Pin start time %s", date);
        try {
            _pinStartDate = format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * PIN入力終了時間記録
     */
    public void setPinEndTime() {
        String date;
        Date dateObj = new Date();
        SimpleDateFormat format = new SimpleDateFormat( "yyMMddHHmmssSSS", Locale.JAPAN );
        date = format.format( dateObj );
        Timber.tag(LOGTAG).i("Pin end time %s", date);
        try {
            _pinEndDate = format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * PIN入力時間取得（ms）
     */
    public int getPinProcTime() {
        long diff = 0;

        if(_pinStartDate != null && _pinEndDate != null) {
            diff = _pinEndDate.getTime() - _pinStartDate.getTime();
        }
        Timber.tag(LOGTAG).i("Pin proc time %d ミリ秒", diff);

        return (int)diff;
    }

    /**
     * PIN入力時間リセット
     */
    public void resetPinProcTime() {
        _pinStartDate = null;
        _pinEndDate = null;
    }

    /**
     * ICカード処理のエラー設定
     */
    public void setIcProcError(int error) {
        switch (error) {
            // オフラインデータ認証NG
            case EmvProcessing.k_ICPROC_OFFLINEDATAAUTH_NG:
                setCreditError(CreditErrorCodes.T19);
                break;
            // 本人確認NG
            case EmvProcessing.k_ICPROC_CHOLDER_VERIFY_NG:
            case EmvProcessing.k_ICPROC_REMOVE_CARD:
                setCreditError(CreditErrorCodes.T21);
                break;
            // 端末リスク管理NG
            case EmvProcessing.k_ICPROC_TERM_RISK_MANAGE_NG:
                setCreditError(CreditErrorCodes.T22);
                break;
            // オフライン判定
            case EmvProcessing.k_ICPROC_OFFLINE:
                setCreditError(CreditErrorCodes.T23);
                break;
            // PIN入力キャンセル
            case EmvProcessing.k_ICPROC_CANCEL:
                setCancelPin();
                break;
            // PIN入力タイムアウト
            case EmvProcessing.k_ICPROC_TIMEOUT:
                setCreditError(CreditErrorCodes.T30);
                break;
            // PINバイパス不可
            case EmvProcessing.k_ICPROC_PINBYPASS_DISABLE:
                setCreditError(CreditErrorCodes.T35);
                break;
            default:
                Timber.tag(LOGTAG).e("Unknown error in setIcProcError");
                setCreditError(CreditErrorCodes.T99);
                break;
        }
    }

    /**
     * オンラインオーソリ要求用のTAGテーブル取得
     */
    public int [] getTagsOnlineAuthTable() {
        int [] tbl = ParamConfig.TagsOnlineAuth_Default;
        String aid = get_AID();
        switch(aid) {
            case "A0000000651010":  // JCB
                tbl = ParamConfig.TagsOnlineAuth_JCB;
                break;
            case "A00000002501":    // AMEX
                tbl = ParamConfig.TagsOnlineAuth_AMEX;
                break;
            case "A0000001523010":  // DINERS
                tbl = ParamConfig.TagsOnlineAuth_DINERS;
                break;
            case "A0000000031010":  // VISA
                tbl = ParamConfig.TagsOnlineAuth_VISA;
                break;
            case "A0000000041010":  // MASTER
                tbl = ParamConfig.TagsOnlineAuth_MASTER;
                break;
            default:
                Timber.tag(LOGTAG).e("Unknown AID in getTagsOnlineAuthTable");
                tbl = ParamConfig.TagsOnlineAuth_Default;
                break;
        }
        return tbl;
    }

    /**
     * オンラインオーソリ結果設定
     */
    public void setAuthResCode() {
        byte[] resCode = null;
        byte[] authCode = null;
        byte[] authData = null;
        byte[] scriptData = null;
        byte[] iccData;
        byte[] value;
        int len;
        String cafisErrorCd = "";

        // response code
        if (CreditSettlement.AuthErrorReason.WAITRES_TOUT == getOnlineAuthErrorReason()) {
            // レスポンス待ちタイムアウト
            resCode = new byte[2];
            resCode[0] = 0x30;
            resCode[1] = 0x35;
        } else {
            if ((null != _creditResult.resCd) && (0 < _creditResult.resCd.length())) {
                resCode = _creditResult.resCd.getBytes();
            }
        }

        // オーソリエラーコード
        if ((null != _creditResult.cafisErrorCd) && (0 < _creditResult.cafisErrorCd.length())) {
            cafisErrorCd = _creditResult.cafisErrorCd;
        }

        if ((null != _creditResult.iccData) && (0 < _creditResult.iccData.length())) {
            iccData = new byte[_creditResult.iccData.length() / 2];
            value = new byte[_creditResult.iccData.length() / 2];

            // ICC関連データ(String)をbyte配列に変換
            for (int index = 0; index < iccData.length; index++) {
                iccData[index] = (byte) Integer.parseInt(_creditResult.iccData.substring(index * 2, (index + 1) * 2), 16);
            }

            // authentication code(TAG8A)
            len = TLVUtil.getTlvData(iccData, iccData.length, 0x8A, value, false);
            if (0 < len) {
                authCode = new byte[len];
                System.arraycopy(value, 0, authCode, 0, len);
            }

            // authentication data(TAG91)
            len = TLVUtil.getTlvData(iccData, iccData.length, 0x91, value, false);
            if (0 < len) {
                authData = new byte[len];
                System.arraycopy(value, 0, authData, 0, len);
            }

            // script data(TAG71)
            len = TLVUtil.getTlvData(iccData, iccData.length, 0x71, value, true);
            if (0 < len) {
                scriptData = new byte[len];
                System.arraycopy(value, 0, scriptData, 0, len);
            } else {
                // script data(TAG72)
                len = TLVUtil.getTlvData(iccData, iccData.length, 0x72, value, true);
                if (0 < len) {
                    scriptData = new byte[len];
                    System.arraycopy(value, 0, scriptData, 0, len);
                }
            }
        }

        if (_msICKbn == k_MSIC_KBN_IC) {
            _emvProc.emvSetAuthResCode(resCode, authCode, authData, scriptData, cafisErrorCd);
        } else if (_msICKbn == k_MSIC_KBN_CONTACTLESS_IC) {
            _emvCLProc.setAuthCode(resCode);
//            _emvCLProc.emvSetAuthResCode(resCode, authCode, authData, scriptData, cafisErrorCd);
        }
    }

    /**
     * クレジット情報保存（売上、印刷用）
     */
    // クレジット情報初期設定
    public void initCreditResult() {
        // アプリケーションリスト
        _creditResult.appList = null;
        // 承認番号
        _creditResult.cafisApprovalNo = "0";
        // アクワイアラID
        _creditResult.acquireId = 0;
        // 磁気・IC区分
        _creditResult.msICKbn = getMSICKbn();
        // オンオフ区分（オンライン固定）
        _creditResult.onOffKbn = k_ONOFF_KBN_ONLINE;
        // チップコンディションコード
        _creditResult.chipCC = getChipCC();
        // 強制オンライン
        _creditResult.forcedOnline = getForcedOnline();;
        // 強制承認
        _creditResult.forcedApproval = getForcedApproval();;
        // 商品コード
        _creditResult.productCd = getProductCd();
        // AID
        _creditResult.aid = "";
        // POSエントリモード
        _creditResult.posEntryMode = getPosEntryMode();
        // PANシーケンスナンバー
        _creditResult.panSeqNo =  0xFFFFFFFF;
        // IC端末対応フラグ
        _creditResult.icTerminalFlg = getIcTerminalFlg();
        // ブランド識別
        _creditResult.brandSign = "";
        // 鍵種別
        _creditResult.keyType = _mcCenterCommManager.getKeyTypeCredit();
        // 鍵バージョン
        _creditResult.keyVersion = _mcCenterCommManager.getKeyVerCredit();
        // 暗号化データ
        _creditResult.rsaData = "";
    }
    // カード判定応答のクレジット情報保存
    public void saveCreditResult(CardAnalyze.Response res) {
        // アクワイアラID
        _creditResult.acquireId = res.acquirerId;
        // イシュア名
        _creditResult.issuerName = res.issuerName;
        // 印字名
        _creditResult.printText = res.printText;
        // 仕向け先コード
        _creditResult.deliveryCode = res.deliveryCode;
        // 仕向け先名
        _creditResult.deliveryName = res.deliveryName;
        // クレジットアクワイアラID
        _creditResult.acquirerId = res.acquirerId;
        // ブランド識別
        _creditResult.brandSign = res.brandSign;
        // AID
        _creditResult.aid = res.aid;
        // カード区分
        _creditResult.cardKbn = res.cardKbn;
    }
    // オンラインオーソリ要求のクレジット情報保存
    public void saveCreditResult(OnlineAuth.Request req) {
        // マネー区分
        _creditResult.moneyKbn = req.moneyKbn;
        // 磁気・IC区分
        _creditResult.msICKbn = req.msICKbn;
        // 伝票番号（オーソリ応答正常の場合、上書きする）
        _creditResult.terminalProcNo = req.terminalProcNo;
        // 売上金額（オーソリ応答正常の場合、上書きする）
        _creditResult.fare = req.fare;
        // 支払方法（オーソリ応答正常の場合、上書きする）
        _creditResult.payMethod = req.payMethod;
        // 商品コード（オーソリ応答正常の場合、上書きする）
        _creditResult.productCd = req.productCd;
        // 乗務員コード
        _creditResult.driverCd = req.driverCd;
        // POSエントリモード
        _creditResult.posEntryMode = req.posEntryMode;
        // PANシーケンスナンバー
        _creditResult.panSeqNo = (int)Long.parseLong(req.panSeqNo, 16);
        // IC端末対応フラグ
        _creditResult.icTerminalFlg = req.icTerminalFlg;
        // 鍵種別
        _creditResult.keyType = req.keyType;
        // 鍵バージョン
        _creditResult.keyVersion = req.keyVersion;
        // 暗号化データ
        _creditResult.rsaData = getRsaDataForPayment();
    }
    // オンラインオーソリ応答のクレジット情報保存
    public void saveCreditResult(OnlineAuth.Response res) {
        // オーソリエラーコード（売上・印刷では未使用）
        _creditResult.cafisErrorCd = res.cafisErrorCd;
        // 処理成否
        _creditResult.result = res.result;
        // 端末番号
        _creditResult.terminalNo = res.terminalNo;
        // 売上日時
        _creditResult.authDateTime = res.authDateTime;
        // レスポンスコード
        _creditResult.resCd = res.resCd;
        // ICC関連データ
        _creditResult.iccData = res.iccData;
        // マスクされたカード番号
        _creditResult.maskedMemberNo = res.maskedMemberNo;

        if (true == res.result) {
            // オーソリ正常時
            // 承認番号
            _creditResult.cafisApprovalNo = res.cafisApprovalNo;
            // 伝票番号
            _creditResult.terminalProcNo = res.terminalProcNo;
            // 売上金額
            _creditResult.fare = res.fare;
            // 取引内容
            _creditResult.transactionType = res.transactionType;
            // 支払方法
            _creditResult.payMethod = res.payMethod;
            // 商品コード
            _creditResult.productCd = res.productCd;
            // KID
            _creditResult.kid = res.kid;
        } else {
            // オーソリ異常時
            // 承認番号（0）
            _creditResult.cafisApprovalNo = "0";
            // 伝票番号（オーソリ要求時の値を使用）
            // 売上金額（オーソリ要求時の値を使用）
            // 取引内容（売上）
            _creditResult.transactionType = 1;
            // 支払方法（オーソリ要求時の値を使用）
            // 商品コード（オーソリ要求時の値を使用）
            // KID（空）
            _creditResult.kid = "";
        }
    }
    // オンラインオーソリ取消要求のクレジット情報保存
    public void saveCreditResult(OnlineAuthCancel.Request req) {
        // マネー区分
        _creditResult.moneyKbn = req.moneyKbn;
        // 磁気・IC区分
        _creditResult.msICKbn = req.msICKbn;
        // 乗務員コード
        _creditResult.driverCd = req.driverCd;
        // POSエントリモード
        _creditResult.posEntryMode = req.posEntryMode;
        // PANシーケンスナンバー
        _creditResult.panSeqNo = (int)Long.parseLong(req.panSeqNo, 16);
        //_creditResult.panSeqNo = Integer.parseInt(req.panSeqNo);
        // IC端末対応フラグ
        _creditResult.icTerminalFlg = req.icTerminalFlg;
        // 鍵種別
        _creditResult.keyType = req.keyType;
        // 鍵バージョン
        _creditResult.keyVersion = req.keyVersion;
        // 暗号化データ
        _creditResult.rsaData = getRsaDataForPayment();
    }
    // オンラインオーソリ取消応答のクレジット情報保存
    public void saveCreditResult(OnlineAuthCancel.Response res) {
        // オーソリエラーコード（売上・印刷では未使用）
        _creditResult.cafisErrorCd = res.cafisErrorCd;
        // 処理成否
        _creditResult.result = res.result;
        // 端末番号
        _creditResult.terminalNo = res.terminalNo;
        // 取消日時
        _creditResult.authDateTime = res.authDateTime;
        // レスポンスコード
        _creditResult.resCd = res.resCd;
        // ICC関連データ
        _creditResult.iccData = res.iccData;
        // マスクされたカード番号
        _creditResult.maskedMemberNo = res.maskedMemberNo;
        // 承認番号
        _creditResult.cafisApprovalNo = res.cafisApprovalNo;
        // 伝票番号
        _creditResult.terminalProcNo = res.terminalProcNo;
        // 売上金額
        _creditResult.fare = res.fare;
        // 取引内容
        _creditResult.transactionType = res.transactionType;
        // 支払方法
        _creditResult.payMethod = res.payMethod;
        // 商品コード
        _creditResult.productCd = res.productCd;
        // KID
        _creditResult.kid = res.kid;
    }
    // オーソリ応答検証後のクレジット情報保存
    public void saveCreditResultAfterAuthVerification() {
        // オンオフ区分（オンライン固定）
        _creditResult.onOffKbn = k_ONOFF_KBN_ONLINE;
        // チップコンディションコード
        _creditResult.chipCC = getChipCC();
        // 強制オンライン
        _creditResult.forcedOnline = getForcedOnline();
        // 強制承認
        _creditResult.forcedApproval = getForcedApproval();
        // カード有効期限
        _creditResult.cardExpDate = getCardExpDate();
        // アプリケーションラベル
        _creditResult.applicationLabel = get_ApplicationLabel();
        // AID
        _creditResult.aid = get_fullAID();
        // サイン欄印字フラグ
        _creditResult.signatureFlag = getSignatureFlag();
        // 暗号化データ
        _creditResult.rsaData = getRsaDataForPayment();

        //ADD-S BMT S.Oyama 2024/10/03 フタバ双方向向け改修
        //ATC
        _creditResult.atc = get_ATC();

        //PIN存在フラグ Pin有り時はtrue
        _creditResult.pinExistFL = !getPinLessFlg();            //getPinLessFlg()はPin無し時にtrueを返すので、逆にする
        //ADD-E BMT S.Oyama 2024/10/03 フタバ双方向向け改修
    }

    /**
     * クレジット情報保存開始
     */
    public void startSaveCreditResult()
    {
        _creditResult = null;
        _creditResult = new CreditResult.Result();
        _creditResult.validFlg = false;
        _creditResult.errorReason = AuthErrorReason.NONE;
    }
    /**
     * クレジット情報有効フラグセット
     */
    public void setCreditResultValidFlg(boolean flag)
    {
        _creditResult.validFlg = flag;
    }
    /**
     * クレジット情報有効フラグ取得
     */
    public boolean getCreditResultValidFlg()
    {
        return _creditResult.validFlg;
    }

    /**
     * アプリケーションリスト保存
     */
    public void setAppList(String[] apps) {
        _creditResult.appList = apps;
    }

    /**
     * アプリケーションリスト取得
     */
    public String[] getAppList() {
        return _creditResult.appList;
    }

    // オーソリNG要因保存
    public void setOnlineAuthErrorReason(AuthErrorReason reason) {
        _creditResult.errorReason = reason;
    }
    // オーソリNG要因取得
    public AuthErrorReason getOnlineAuthErrorReason() {
        return _creditResult.errorReason;
    }

    public boolean isIC() {
        return k_MSIC_KBN_IC == getMSICKbn();
    }

    public boolean isCL() {
        return k_MSIC_KBN_CONTACTLESS_IC == getMSICKbn();
    }
    // クレジット関連変数の初期化
    public void initCreditVariable() {
        _creditProcStatus = k_CREDIT_STAT_IDLE;
        _creditErrorCode = null;
        _moneyKbn = 0;
        _msICKbn = k_MSIC_KBN_UNKNOWN;
        _posEntryMode = "000";
        _onOffKbn = 0;
        _chipCC = null;
        _signatureFlag = k_SIGNATURE_NECESSARY;
        _msTrackInfo = null;
        _trackData = null;
        _rsaDataForPayment = null;
        _pinSkipFBFlg = true;
        _pinLessFlg = false;
        _pinLessLimit = 0;
        _msAvailableFlg= true;
        _brandEnableFlg = true;
        _acquirerOnlinePayType = 0;
        _txnTypeCd = "R";
        _chargeTypeCd = "99";
        _appPersonalInfo = null;
        _icCardExist = true;
    }

    // デモ用のクレジット情報保存
    public void saveCreditResultForDemo() {
        int stat = getCreditProcStatus();

        // 有効フラグ（売上・印刷では未使用）
        // 本フラグがtrueであれば、売上データとして保存する
        _creditResult.validFlg = true;
        // 印字名
        _creditResult.printText = "JCB GROUP";
        // AID
        _creditResult.aid = "A0000000651010";
        // サイン欄印字フラグ
        _creditResult.signatureFlag = getSignatureFlag();
        // 乗務員コード
        _creditResult.driverCd = AppPreference.getMcDriverId();
        // PANシーケンスナンバー
        _creditResult.panSeqNo = 1;
        // 端末番号
        _creditResult.terminalNo = AppPreference.getMcTermId();
        // 日時
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMddHHmmss", Locale.JAPANESE);
        _creditResult.authDateTime = dateFmt.format(new Date());
        // レスポンスコード
        _creditResult.resCd = "00";
        // 支払方法
        _creditResult.payMethod = getPayMethod();
        // 商品コード
        _creditResult.productCd = getProductCd();
        // カード有効期限
        _creditResult.cardExpDate = getCardExpDate();;
        // アプリケーションラベル
        _creditResult.applicationLabel = "JCB Credit";
        // マスクされたカード番号
        _creditResult.maskedMemberNo = "123456******1234";

        if (k_CREDIT_STAT_ONLINE_OK == stat) {
            // 決済

            // 売上金額
            _creditResult.fare = Amount.getTotalAmount();;
            // 承認番号
            _creditResult.cafisApprovalNo = "00111";
            // 伝票番号
            _creditResult.terminalProcNo = AppPreference.getTermSequence();
            // 取引内容
            _creditResult.transactionType = 1;
        } else {
            // 取消

            // 取消金額
            _creditResult.fare = _slipData.transAmount;
            // 承認番号
            _creditResult.cafisApprovalNo = "00112";
            // 伝票番号
            _creditResult.terminalProcNo = AppPreference.getTermSequence();
            // 取引内容
            _creditResult.transactionType = 4;
        }

        //ADD-S BMT S.Oyama 2024/10/03 フタバ双方向向け改修
        //ATC
        _creditResult.atc = "     ";
        //ADD-E BMT S.Oyama 2024/10/03 フタバ双方向向け改修
    }

    public void setDemoEventListener(DemoEventListener listener) {
        _demoListener = listener;
    }

    public void onPinInputRequired() {
        if (_demoListener != null) {
            _demoListener.onPinInputRequired();
            startInputPin();
        }
    }

    public void playSound(@RawRes int id) {
        if (_listener != null) _listener.OnSound(id);
    }
}
