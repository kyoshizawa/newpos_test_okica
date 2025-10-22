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
import jp.mcapps.android.multi_payment_terminal.database.history.uriOkica.ISOUtil;
import jp.mcapps.android.multi_payment_terminal.error.CreditErrorCodes;
import jp.mcapps.android.multi_payment_terminal.error.CreditErrorMap;

import jp.mcapps.android.multi_payment_terminal.ui.error.CommonErrorDialog;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.CardAnalyze;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.OnlineAuth;
import jp.mcapps.android.multi_payment_terminal.webapi.mc_pos_center.data.OnlineAuthCancel;
import timber.log.Timber;



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
    /* MCセンター通信クラス */
    final public McCenterCommManager _mcCenterCommManager = new McCenterCommManager(this);
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

    public FragmentActivity _fragmentActivity;

    private SlipData _slipData;
    public SlipData getSlipData() {
        return _slipData;
    }


    public CreditSettlementListener _listener = null;
    public DemoEventListener _demoListener = null;

    /* 売上・印刷用データ */
    /* AID情報 */
    final public TerminalAidInfo _aidInfo = new TerminalAidInfo();


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
                    //procCredit(creMsg);
                    break;
                case START_INPUT_PIN:           // PIN入力開始
                    if (_listener != null) _listener.OnSound(R.raw.credit_input_pin);
                    break;
                case START_ANNOUNCE_SIGNATURE:  // サイン音声案内開始
                    if (_listener != null) _listener.OnSound(R.raw.credit_signature);
                    break;
                case START_SELECT_APP:
                    //selectApplication();
                    break;
                case END_CREDIT:                // クレジット決済終了
                    //procCreditEnd();
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
     * PIN入力開始のメッセージ送信
     */
    public void startInputPin() {
        // PIN入力の音声案内
        Timber.tag(LOGTAG).i("PIN入力音声案内鳴動");
        sendMessage(CreditHandlMessage.START_INPUT_PIN);
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
        //_emvProc.emvSetPosEntryMode(_posEntryMode.substring(0, 2));
    }
    public void setPosEntryMode(String posEntryMode) {
        _posEntryMode = posEntryMode;
        //_emvProc.emvSetPosEntryMode(_posEntryMode.substring(0, 2));
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
//            byte[] bPanSeqNo = _emvProc.emvGetPanSeqNo();
//            if (-1 != bPanSeqNo[0]) {
//                panSeqNo = String.format("%08x", bPanSeqNo[0]);
//            }
        }
        else if (_msICKbn == k_MSIC_KBN_CONTACTLESS_IC) {
//            if (_emvCLProc.getPanSeqNo() != null) {
//                panSeqNo = String.format("00000%s", _emvCLProc.getPanSeqNo());
//            }
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
