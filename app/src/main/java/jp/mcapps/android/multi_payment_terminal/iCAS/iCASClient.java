package jp.mcapps.android.multi_payment_terminal.iCAS;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.data.EmoneyOpeningInfo;
import jp.mcapps.android.multi_payment_terminal.error.JremRasErrorCodes;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.FeliCaClient;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.IDevice;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.IFeliCaClientEventListener;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPPacket;
import jp.mcapps.android.multi_payment_terminal.iCAS.data.DeviceClient;
import timber.log.Timber;

public class iCASClient implements IFeliCaClientEventListener {
//    public static int transCount = 0;
//    public static boolean transFlag = false;

    // マネー種別
    public enum moneyType
    {
        MONEY_UNKNOWN(0),           // 不明
        MONEY_SUICA(1),             // 交通系
        MONEY_ID(2),                // iD
        MONEY_WAON(3),              // WAON
        MONEY_QUICPAY(4),           // QUICPay
        MONEY_EDY(5),               // 楽天Edy
        MONEY_NANACO(6),            // nanaco
        ;

        private final int _val;

        moneyType(final int val) {
            _val = val;
        }

        public int getInt() {
            return _val;
        }
    }
//    public static int noEnd_test = 0;
    // 業務識別子（サポートしているもののみ定義）
    // 交通系
    public static final int BUSINESS_ID_SUICA_PAY               = 68;           // 引去
//    public static final int BUSINESS_ID_SUICA_CHARGE            = 70;           // チャージ
    public static final int BUSINESS_ID_SUICA_REFUND            = 71;           // 直近控除
//    public static final int BUSINESS_ID_SUICA_CHARGE_CANCEL     = 72;           // チャージ取消
    public static final int BUSINESS_ID_SUICA_REMAIN            = 73;           // 残額照会
    public static final int BUSINESS_ID_SUICA_IDI_PAY           = 74;           // IDi指定引去
//    public static final int BUSINESS_ID_SUICA_IDI_CHARGE        = 76;           // IDi指定チャージ
    public static final int BUSINESS_ID_SUICA_STATUS_REPLY      = 82;           // 業務処理状態応答
    // iD
    public static final int BUSINESS_ID_ID_PAY                  = 201;          // 引去
    public static final int BUSINESS_ID_ID_REFUND               = 202;          // 取消
//    public static final int BUSINESS_ID_ID_MANUAL               = 203;          // マニュアル返品
    public static final int BUSINESS_ID_ID_REMAIN               = 204;          // 残額照会
    public static final int BUSINESS_ID_ID_STATUS_REPLY         = 205;          // 業務処理状態応答
    public static final int BUSINESS_ID_ID_JOURNAL              = 206;          // 日計
//    public static final int BUSINESS_ID_ID_INTERIM_JOURNAL      = 207;          // 中間計
    // QUICPay
    public static final int BUSINESS_ID_QUICPAY_PAY             = 301;          // 引去
    public static final int BUSINESS_ID_QUICPAY_REFUND          = 302;          // 取消
    //    public static final int BUSINESS_ID_QUICPAY_MANUAL          = 303;          // マニュアル返品
    public static final int BUSINESS_ID_QUICPAY_HISTORY         = 304;          // 履歴出力
    public static final int BUSINESS_ID_QUICPAY_STATUS_REPLY    = 305;          // 業務処理状態応答
    public static final int BUSINESS_ID_QUICPAY_JOURNAL         = 306;          // 日計
    //    public static final int BUSINESS_ID_QUICPAY_INTERIM_JOURNAL = 307;          // 中間計
    // WAON
//    public static final int BUSINESS_ID_WAON_CHARGE             = 401;          // チャージ
//    public static final int BUSINESS_ID_WAON_CHARGE_CANCEL      = 402;          // チャージ取消
    public static final int BUSINESS_ID_WAON_PAY                = 403;          // 引去
    public static final int BUSINESS_ID_WAON_REFUND             = 404;          // 取消
    public static final int BUSINESS_ID_WAON_REMAIN             = 406;          // 残高履歴照会
//    public static final int BUSINESS_ID_WAON_P_CHARGE           = 407;          // ポイントチャージ
    public static final int BUSINESS_ID_WAON_STATUS_REPLY       = 408;          // 業務処理状態応答
//    public static final int BUSINESS_ID_WAON_JOURNAL            = 409;          // 日計
//    public static final int BUSINESS_ID_WAON_INTERIM_JOURNAL    = 410;          // 中間計
//    public static final int BUSINESS_ID_WAON_PRE_REMAIN         = 411;          // 残高履歴照会（事前）
    public static final int BUSINESS_ID_WAON_PRE_STATUS_REPLY   = 412;          // 業務処理状態応答（事前）
    // Edy
    public static final int BUSINESS_ID_EDY_PAY                 = 501;          // 引去
    public static final int BUSINESS_ID_EDY_CHARGE              = 502;          // チャージ
    public static final int BUSINESS_ID_EDY_REMAIN              = 503;          // 残高照会
    public static final int BUSINESS_ID_EDY_FORCE_REMAIN        = 504;          // 強制残高照会
    public static final int BUSINESS_ID_EDY_HISTORY             = 505;          // 取引履歴
    public static final int BUSINESS_ID_EDY_STATUS_REPLY        = 506;          // 業務処理状態応答
    public static final int BUSINESS_ID_EDY_JOURNAL             = 507;          // 日計
//    public static final int BUSINESS_ID_EDY_INTERIM_JOURNAL     = 508;          // 中間計
    public static final int BUSINESS_ID_EDY_FIRST_COMM          = 509;          // 初回通信
    public static final int BUSINESS_ID_EDY_CHARGE_AUTH         = 510;          // チャージ認証
    public static final int BUSINESS_ID_EDY_REMOVAL             = 511;          // 撤去
    // nanaco
    public static final int BUSINESS_ID_NANACO_PAY              = 601;          // 引去
    public static final int BUSINESS_ID_NANACO_REMAIN           = 602;          // 残高確認
//    public static final int BUSINESS_ID_NANACO_CHARGE           = 603;          // チャージ
    public static final int BUSINESS_ID_NANACO_PREV_TRAN        = 604;          // 前回取引確認
    public static final int BUSINESS_ID_NANACO_JOURNAL          = 605;          // 日計
//    public static final int BUSINESS_ID_NANACO_INTERIM_JOURNAL  = 606;          // 中間計

    // スレッド間メッセージ
    public static final int FC_MSG_TO_MAIN_ON_FINISH            = 0x0001;             // 正常終了
    public static final int FC_MSG_TO_MAIN_ON_ERROR             = 0x0002;             // エラー終了
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_CANCEL_FINAL = 0x1001;          // デバイス操作要求（CANCEL最終確認）
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_STATUS    = 0x1002;             // デバイス操作要求（STATUS）
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_RW_PARAM  = 0x1003;             // デバイス操作要求（R/W_PARAM）
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_OPERATION = 0x1004;             // デバイス操作要求（OPERATION）
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_CONFIRM   = 0x1005;             // デバイス操作要求（CONFIRM）入力完了時
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_DISPLAY   = 0x1006;             // デバイス操作要求（DISPLAY）
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_RETRY_NOEND = 0x1007;           // デバイス操作要求（RETRY）処理未了時
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_RESULT    = 0x1008;             // デバイス操作要求（RESULT 交通系）
    public static final int FC_MSG_TO_MAIN_ON_JOURNAL           = 0x1009;             // 日計
    public static final int FC_MSG_TO_MAIN_ON_RECOVERY          = 0x100A;             // 決済未完了時の業務処理状態応答によるリカバリ
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_CANCEL    = 0x100B;             // デバイス操作要求（CANCEL）
    public static final int FC_MSG_TO_MAIN_ON_WAON_PRE_STATUS   = 0x100C;             // WAON業務処理状態応答（事前）

    // エラーコード
    public static final int ERROR_CLIENT_CANCEL             = 1;            // キャンセル終了
    public static final int ERROR_CLIENT_UNKNOWN            = -1;           // 不明なエラー
    public static final int ERROR_CLIENT_BAD_PARAM          = -2;           // パラメータ不正
    public static final int ERROR_CLIENT_NO_MEMORY          = -3;           // メモリ不足
    public static final int ERROR_CLIENT_FATAL              = -4;           // 致命的内部エラー
    public static final int ERROR_CLIENT_FAILURE            = -6;           // 処理失敗
    public static final int ERROR_CLIENT_HTTP               = -7;           // ＨＴＴＰ通信エラー
    public static final int ERROR_CLIENT_PROTOCOL           = -9;           // プロトコルエラー
    public static final int ERROR_CLIENT_SEQUENCE           = -11;          // シーケンスエラー
    public static final int ERROR_CLIENT_MSG_UNEXPECTED     = -12;          // unexpected error
    public static final int ERROR_CLIENT_MSG_FORMAT         = -13;          // packet format error
    public static final int ERROR_CLIENT_MSG_ILLEGAL        = -14;          // iLLegal state error
    public static final int ERROR_CLIENT_MSG_SERVER         = -15;          // サーバからエラーパケット受信
    public static final int ERROR_CLIENT_MSG_TCAP_VERSION   = -16;          // サーバがTCAP2.5未対応
    public static final int ERROR_STATUS_REPLY_NO_END       = -17;          // 業務処理状態応答 未了応答（処理自体は成功だがOnErrorOccurredで返さないと伝票が印刷されないためエラーで返す）

    // セッションタイムアウト時間
    public static final int SESSION_TIMEOUT_MSTIME_SUICA   = 10000;
    public static final int SESSION_TIMEOUT_MSTIME_ID      = 15000;
    public static final int SESSION_TIMEOUT_MSTIME_WAON    = 15000;
    public static final int SESSION_TIMEOUT_MSTIME_QUICPAY = 10000;    // 仕様書に記載がないので交通系と同じ時間とする
    public static final int SESSION_TIMEOUT_MSTIME_EDY     = 15000;
    public static final int SESSION_TIMEOUT_MSTIME_NANACO  = 15000;

    private static iCASClient _instance;            // インスタンス
    private static int _tuuban;                     // 処理通番（全電子マネー共通） ※現状はRAMでの管理
    private static final HashMap<Integer, Integer> _errorMap = new HashMap<>();     // エラーコード変換
    private static byte[] _pinData;                 // 暗号化された暗証番号
    private static int _pinLength;                  // 暗号化された暗証番号レングス
    private static boolean _pinCancel;              // 暗証番号入力キャンセル

    private Handler _handlerToMainThread;           // メインスレッドへのハンドラ
    private final FeliCaClient _feliCaClient;       // FeliCaクライアント
    private boolean _bIsAborted;                    // 中断フラグ
    private boolean _bIsCancelDisable;              // キャンセル無効フラグ
    private BusinessParameter _parameter;           // 業務パラメータ
    private moneyType _money;                       // 処理中のマネー種別
    private IiCASClient _rwUIEventListener;         // R/W UIイベントリスナ
    private int _status;                            // 業務ステータス（1:処理開始 2:未確定 3:処理完了）
    private Date _prepareStart;                     // 処理時間計測（開始）
    private Date _prepareEnd;                       // 処理時間計測（state1変化時）
    private Date _procStart;                        // 処理時間計測（state2変化時）
    private Date _procEnd;                          // 処理時間計測（state3変化時）
    private Date _pinStart;                         // 処理時間計測（pin入力開始時）
    private Date _pinEnd;                           // 処理時間計測（pin入力終了時）
    private boolean _retry;                         // リトライフラグ
    private int _retryCode;                         // リトライ要因となるコード（353になった場合は上書き禁止）
    private Object _noEndResult;                    // 処理未了(353)になった時点でのRESULTデータ
    private boolean _failure;                       // 決済未完了フラグ（交通系のみ使用。交通系だけは業務処理状態応答を実施する前に同一決済ＩＤ、同一業務、同一金額でRASへアクセスし復旧処理を行う）
    private String _prevSid;                        // 前回決済ID
    private moneyType _prevMoney;                   // 前回選択マネー種別
    private Object _prevResult;                     // 前回Resultデータ
    private BusinessParameter _parameterBk;         // 業務パラメータ（WAONの事前業務実施時の保存用）
    private String _waonValue;                      // 金額（WAONの事前業務実施時の保存用）
    private Object _waonResult;                     // WAONチャージ用テストコード
    private boolean _noEndFlag;                     // 処理未了発生フラグ（処理未了中に業務処理状態応答を実施するために使用）
    private iCASClientDemo _demo;
    private boolean _isPolling;                     // カードかざしフラグ

    static {
        _errorMap.put(FeliCaClient.FC_ERR_CLIENT_UNKNOWN,           ERROR_CLIENT_UNKNOWN);
        _errorMap.put(FeliCaClient.FC_ERR_CLIENT_BADPARAM,          ERROR_CLIENT_BAD_PARAM);
        _errorMap.put(FeliCaClient.FC_ERR_CLIENT_NO_MEMORY,         ERROR_CLIENT_NO_MEMORY);
        _errorMap.put(FeliCaClient.FC_ERR_CLIENT_FATAL,             ERROR_CLIENT_FATAL);
        _errorMap.put(FeliCaClient.FC_ERR_CLIENT_FAILURE,           ERROR_CLIENT_FAILURE);
        _errorMap.put(FeliCaClient.FC_ERR_CLIENT_HTTP,              ERROR_CLIENT_HTTP);
        _errorMap.put(FeliCaClient.FC_ERR_CLIENT_CANCEL,            ERROR_CLIENT_CANCEL);
        _errorMap.put(FeliCaClient.FC_ERR_CLIENT_PROTOCOL,          ERROR_CLIENT_PROTOCOL);
        _errorMap.put(FeliCaClient.FC_ERR_CLIENT_SEQUENCE,          ERROR_CLIENT_SEQUENCE);
        _errorMap.put(FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED,    ERROR_CLIENT_MSG_UNEXPECTED);
        _errorMap.put(FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT,        ERROR_CLIENT_MSG_FORMAT);
        _errorMap.put(FeliCaClient.FC_ERR_CLIENT_MSG_ILLEGAL,       ERROR_CLIENT_MSG_ILLEGAL);
        _errorMap.put(FeliCaClient.FC_ERR_CLIENT_MSG_SERVER,        ERROR_CLIENT_MSG_SERVER);
        _errorMap.put(FeliCaClient.FC_ERR_CLIENT_MSG_TCAP_VER,      ERROR_CLIENT_MSG_TCAP_VERSION);
    }

    private iCASClient() {
        _bIsAborted = false;
        _bIsCancelDisable = false;
        _feliCaClient = FeliCaClient.getInstance();
        _feliCaClient.SetEventListener(this);
        _tuuban = 0;
        _pinData = new byte[65535];
        _pinLength = 0;
        _pinCancel = false;
        _parameterBk = new BusinessParameter();

        // AddDeviceする前にイベントリスナの設定をすること
        iCASDevice.AddDevice(_feliCaClient);
    }

    public static iCASClient getInstance() {
        if(_instance == null) {
            _instance = new iCASClient();
        }
        return _instance;
    }

    public long OnStart(BusinessParameter parameter) throws JSONException, IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, InterruptedException {
        long lRet;

//        noEnd_test = 0;

        if(AppPreference.isDemoMode()
        && !parameter.businessId.equals(String.valueOf(BUSINESS_ID_EDY_FIRST_COMM))
        && !parameter.businessId.equals(String.valueOf(BUSINESS_ID_EDY_REMOVAL))
        && !parameter.businessId.equals(String.valueOf(BUSINESS_ID_QUICPAY_JOURNAL))
        && !parameter.businessId.equals(String.valueOf(BUSINESS_ID_NANACO_JOURNAL))) {
            iCASClient.moneyType money;
            // 業務パラメータ固定値のセット
            // バージョン番号
            parameter.vr = "0001";
            parameter.sid = MakeSid();
            // 処理日時
            Date dateObj = new Date();
            SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmmss", Locale.JAPAN);
            parameter.time = format.format(dateObj);
            _parameter = parameter;

            money = checkBusinessParameter(_parameter);
            if (money == moneyType.MONEY_UNKNOWN) {
                return FeliCaClient.FC_ERR_CLIENT_BADPARAM;
            }
            _demo = new iCASClientDemo(_rwUIEventListener, money);
            lRet = _demo.OnStart(parameter);
        } else {
            _prevResult = null;
            _prepareStart = null;
            PrepareStart();
            lRet = OnStart(parameter, false, true);
        }
        // やり直しに備えて決済IDを保持
        _prevSid = _parameter.sid;

        return lRet;
    }

    public long OnStart(BusinessParameter parameter, String recoverSid) throws JSONException, IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, InterruptedException {
        long lRet;

        parameter.sid = recoverSid;

        lRet = OnStart(parameter, false, false);

        return lRet;
    }

    public long OnStart(BusinessParameter parameter, boolean bRetry, boolean bMakeSid) throws JSONException, IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, InterruptedException {
        long lRetVal = FeliCaClient.FC_ERR_SUCCESS;

        Initialize(bRetry, parameter);
        Timber.tag("iCAS").i("iCASClient OnStart retry = %s", bRetry);

        if(!bRetry) {
            _parameter = parameter;
            if(Integer.parseInt(_parameter.businessId) == BUSINESS_ID_WAON_REFUND) {
                // WAON取消の場合取消前に事前確認を実施
                _waonValue = ((BusinessParameter.Waon)_parameter.money).value;
                _parameterBk.money = _parameter.money;
                _parameterBk.businessId = _parameter.businessId;
                _parameterBk.sid = _parameter.sid;
                _parameterBk.organizationalUnit = _parameter.organizationalUnit;
                _parameterBk.commonName = _parameter.commonName;
                _parameterBk.message = _parameter.message;
                _parameterBk.time = _parameter.time;
                _parameterBk.vr = _parameter.vr;

                _parameter.businessId = String.valueOf(BUSINESS_ID_WAON_PRE_STATUS_REPLY);
                ((BusinessParameter.Waon)(_parameter.money)).value = null;
                ((BusinessParameter.Waon)(_parameter.money)).uiGuideline = null;
                ((BusinessParameter.Waon)(_parameter.money)).inProgressUI = null;
            } else if(Integer.parseInt(_parameter.businessId) == BUSINESS_ID_WAON_PRE_STATUS_REPLY) {
                // WAON事前確認の場合はWAON取消を実施
                _parameter = _parameterBk;
                ((BusinessParameter.Waon)_parameter.money).value = _waonValue;
                ((BusinessParameter.Waon)_parameter.money).uiGuideline = "ON";
                ((BusinessParameter.Waon)_parameter.money).inProgressUI = "ON";
            }
/*
            else if(Integer.parseInt(_parameter.businessId) == BUSINESS_ID_WAON_CHARGE) {
                // WAONチャージの場合取消前に残高照会を実施
                _waonValue = ((BusinessParameter.Waon)_parameter.money).value;
                _parameterBk.money = _parameter.money;
                _parameterBk.businessId = _parameter.businessId;
                _parameterBk.sid = _parameter.sid;
                _parameterBk.organizationalUnit = _parameter.organizationalUnit;
                _parameterBk.commonName = _parameter.commonName;
                _parameterBk.message = _parameter.message;
                _parameterBk.time = _parameter.time;
                _parameterBk.vr = _parameter.vr;

                _parameter.businessId = String.valueOf(BUSINESS_ID_WAON_PRE_REMAIN);
                ((BusinessParameter.Waon)(_parameter.money)).value = null;
            } else if(Integer.parseInt(_parameter.businessId) == BUSINESS_ID_WAON_PRE_REMAIN) {
                // WAON事前残高照会の場合はWAONチャージを実施
                _parameter = _parameterBk;
                ((BusinessParameter.Waon)_parameter.money).value = _waonValue;
                ((BusinessParameter.Waon)_parameter.money).idm = ((DeviceClient.ResultWAON)_waonResult).idm;
                ((BusinessParameter.Waon)_parameter.money).waonNum = ((DeviceClient.ResultWAON)_waonResult).waonNum;
                ((BusinessParameter.Waon)_parameter.money).cardThroughNum = ((DeviceClient.ResultWAON)_waonResult).cardThroughNum;
                _waonResult = null;
            }
*/
            Thread thread = new Thread(() -> {
                // クライアント証明書取得
                final File file = new File(
                        MainApplication.getInstance().getFilesDir(), BuildConfig.JREM_CLIENT_CERTIFICATE);

                // クライアント証明書がダウンロードされていない
                if (!file.exists()) {
                    throw new IllegalStateException("jrem client certificate file not exists!!");
                }
                _feliCaClient.SetClientCertificateFile(file);

                // Jrem Password
                _feliCaClient.SetJremPassword(AppPreference.getJremPassword().toCharArray());

                // クライアント証明書の共通名、部門名を設定
                try {
                    SetParameterKeys(file, _parameter);
                } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException exception) {
                    exception.printStackTrace();
                }
            });
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 業務パラメータ固定値のセット
            // バージョン番号
            _parameter.vr = "0001";

            if(_parameter.businessId.equals(String.valueOf(BUSINESS_ID_EDY_FIRST_COMM))
            || _parameter.businessId.equals(String.valueOf(BUSINESS_ID_EDY_REMOVAL))) {
                _parameter.sid = null;
            } else if(bMakeSid) {
                _parameter.sid = MakeSid();
            }

            // 処理日時
            Date dateObj = new Date();
            SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmmss", Locale.JAPAN);
            _parameter.time = format.format(dateObj);

            // 業務パラメータチェック
            _money = checkBusinessParameter(_parameter);
            if (_money == moneyType.MONEY_UNKNOWN) {
                return FeliCaClient.FC_ERR_CLIENT_BADPARAM;
            } else if(_money == moneyType.MONEY_WAON && _money == _prevMoney) {
                if(((BusinessParameter.Waon)(_parameter.money)).together != null && ((BusinessParameter.Waon)(_parameter.money)).together.equals("ON")) {
                    DeviceClient.ResultWAON resultWAON = (DeviceClient.ResultWAON)_prevResult;
                    _parameter.sid = _prevSid;
//                    ((BusinessParameter.Waon)(_parameter.money)).idm = resultWAON.idm;
//                    ((BusinessParameter.Waon)(_parameter.money)).waonNum = resultWAON.waonNum;
//                    ((BusinessParameter.Waon)(_parameter.money)).cardThroughNum = resultWAON.cardThroughNum;
                    Timber.tag("iCAS").i("checkBusinessParameter update sid=%s", _parameter.sid);
                }
            } else if(_money == moneyType.MONEY_EDY) {
                if(((BusinessParameter.Edy)(_parameter.money)).totalAmountDischargeFlg != null && ((BusinessParameter.Edy)(_parameter.money)).totalAmountDischargeFlg.equals("ON")) {
/*
                    // カード残額を10円丸めた値を支払金額にセット
                    String val = String.valueOf((Integer.parseInt(((DeviceClient.ResultEdy)(_prevResult)).saleHistories[0].afterBalance) / 10) * 10);
                    ((BusinessParameter.Edy)(_parameter.money)).value = val;
*/
                    _parameter.sid = _prevSid;
                    Timber.tag("iCAS").i("checkBusinessParameter update sid=%s", _parameter.sid);
                }
            } else if(_money == moneyType.MONEY_NANACO) {
                if (((BusinessParameter.nanaco) (_parameter.money)).totalAmountDischargeFlg != null && ((BusinessParameter.nanaco) (_parameter.money)).totalAmountDischargeFlg.equals("ON")) {
                    _parameter.sid = _prevSid;
                    Timber.tag("iCAS").i("checkBusinessParameter update sid=%s", _parameter.sid);
                }
            }

            // メッセージ認証コードの生成
            _parameter.message = generateHmac(_parameter);

            // チップアクセス業務設定
            switch(Integer.parseInt(_parameter.businessId)) {
                case BUSINESS_ID_SUICA_STATUS_REPLY:
                case BUSINESS_ID_ID_STATUS_REPLY:
                case BUSINESS_ID_WAON_STATUS_REPLY:
                case BUSINESS_ID_QUICPAY_STATUS_REPLY:
                case BUSINESS_ID_EDY_STATUS_REPLY:
                case BUSINESS_ID_NANACO_PREV_TRAN:
                case BUSINESS_ID_EDY_JOURNAL:
                case BUSINESS_ID_EDY_CHARGE_AUTH:
                case BUSINESS_ID_QUICPAY_JOURNAL:
                case BUSINESS_ID_NANACO_JOURNAL:
                case BUSINESS_ID_EDY_FIRST_COMM:
                case BUSINESS_ID_EDY_REMOVAL:
                case BUSINESS_ID_WAON_PRE_STATUS_REPLY:
                    _feliCaClient.SetNoneChipAccess(true);
                    break;
                default:
                    _feliCaClient.SetNoneChipAccess(false);
                    break;
            }
        }

        // 業務パラメータをセット
        Gson gson = new Gson();
        String str = gson.toJson(_parameter);
        String strMoney = gson.toJson(_parameter.money);
        JSONObject json = new JSONObject(str);
        JSONObject money = new JSONObject(strMoney);
        json.remove("commonName");
        json.remove("organizationalUnit");
        json.remove("money");
        for(Iterator<String> it = money.keys(); it.hasNext();) {
            String key = it.next();
            json.put(key, money.getString(key));
        }

        _feliCaClient.SetParams(json);

        // ワーカースレッドからの通知受信処理（メインスレッドへ通知が必要な場合、ここで受信する）
        _handlerToMainThread = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (_rwUIEventListener == null) {
                    Timber.tag("iCAS").i("handleMessage _rwUIEventListener is null !!!");
                    return;
                }

                switch(msg.what) {
                    case FC_MSG_TO_MAIN_ON_FINISH:
                        if(!_retry) {
                            /* 復旧不要（交通系のE353, E354以外） */
                            // 処理成功 正常終了コールバック実行
//                            Timber.tag("iCAS").d("FC_MSG_TO_MAIN_ON_FINISH OnFinished");
//                            _rwUIEventListener.OnFinished(msg.arg1);
                            if(_parameter.businessId.equals(String.valueOf(BUSINESS_ID_EDY_CHARGE_AUTH))) {
                                _parameter.businessId = String.valueOf(BUSINESS_ID_EDY_CHARGE);
                                ((BusinessParameter.Edy)(_parameter.money)).value = "10000";
                                ((BusinessParameter.Edy)(_parameter.money)).retryFlg = "OFF";
                                ((BusinessParameter.Edy)(_parameter.money)).depositMeans = "1";
                                ((BusinessParameter.Edy)(_parameter.money)).training = "OFF";
                                ((BusinessParameter.Edy)(_parameter.money)).uiGuideline = "OFF";
                                ((BusinessParameter.Edy)(_parameter.money)).inProgressUI = "OFF";
                                try {
                                    OnStart(_parameter);
                                } catch (JSONException | IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Timber.tag("iCAS").i("FC_MSG_TO_MAIN_ON_FINISH OnFinished");
                                _rwUIEventListener.OnFinished(msg.arg1);
                            }

                        } else {
                            /* 復旧必要（交通系のE353, E354） */
                            try {
                                OnStart(null, true, false);
                                _retry = false;
                            } catch (JSONException | IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case FC_MSG_TO_MAIN_ON_ERROR:
                        int businessId = Integer.parseInt(_parameter.businessId);

                        if(GetResult(_money) != null && GetResult(_money).equals("true") && _status == 3) {
                            // ステータス３かつOnResultがtrueの場合はすでに取引が成立しているのでエラーを返さない
                            Timber.tag("iCAS").i("FC_MSG_TO_MAIN_ON_ERROR OnFinished");
                            _rwUIEventListener.OnFinished(0);
                        } else if(businessId == BUSINESS_ID_SUICA_STATUS_REPLY
                               || businessId == BUSINESS_ID_ID_STATUS_REPLY
                               || businessId == BUSINESS_ID_WAON_STATUS_REPLY
                               || businessId == BUSINESS_ID_QUICPAY_STATUS_REPLY
                               || businessId == BUSINESS_ID_EDY_STATUS_REPLY
                               || businessId == BUSINESS_ID_NANACO_PREV_TRAN) {

                            if (msg.arg1 == ERROR_STATUS_REPLY_NO_END) {
                                /* 業務処理状態応答 未了応答　→ 処理未了タイムアウト扱い（処理未了の情報があるので処理未了RASタイムアウト） */
                                Timber.tag("iCAS").i("FC_MSG_TO_MAIN_ON_ERROR OnErrorOccurred1 arg1=%d string=%s", msg.arg1, (String) msg.obj);
//                                _rwUIEventListener.OnErrorOccurred(msg.arg1, (String) msg.obj);   //OnRecoveryを呼んでいるので不要なため削除
                                _rwUIEventListener.OnFinished(0);   // 処理時間を売上データに入れるためonFinishedを呼び出す
                            } else if(msg.arg1 == ERROR_CLIENT_FAILURE) {
                                /* 業務処理状態応答 不正立 → 通常エラーとして通知 */
                                Timber.tag("iCAS").i("FC_MSG_TO_MAIN_ON_ERROR OnErrorOccurred2 arg1=%d string=%s", msg.arg1, (String) msg.obj);
                                _rwUIEventListener.OnErrorOccurred(msg.arg1, (String) msg.obj);
                            } else {
                                /* 業務処理状態応答 結果未取得　→ 処理未了タイムアウト扱い（処理未了の情報がないので端末タイムアウト） */
                                StatusReplyFailure();
//                                _rwUIEventListener.OnErrorOccurred(msg.arg1, (String) msg.obj);   //OnRecoveryを呼んでいるので不要なため削除
                                _rwUIEventListener.OnFinished(0);   // 処理時間を売上データに入れるためonFinishedを呼び出す
                            }
                        } else if(businessId == BUSINESS_ID_EDY_FIRST_COMM || businessId == BUSINESS_ID_EDY_CHARGE_AUTH){
                            if (msg.arg1 == ERROR_CLIENT_FAILURE) {
                                /* Edy初回通信処理失敗 */
                                Timber.tag("iCAS").i("FC_MSG_TO_MAIN_ON_ERROR OnErrorOccurred3 arg1=%d string=%s", msg.arg1, (String) msg.obj);
                                _rwUIEventListener.OnErrorOccurred(msg.arg1, (String) msg.obj);
                            }
                        } else if(_failure) {
                            _failure = false;
                            // リカバリでも復旧しない場合は業務処理状態応答を実施
/*
                            // 決済不成立 ケース４、５、６確認用
                            Timber.tag("iCAS").d("★★★★★ ジャマー停止 ★★★★★");
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
*/
                            StartStatusReply();
                        } else if(!_retry) {
                            /* 復旧不要（交通系のE353, E354以外） */
                            Timber.tag("iCAS").i("FC_MSG_TO_MAIN_ON_ERROR status=%s", _status);
                            if (_status == 2 || _noEndFlag) { // && msg.arg1 == ERROR_CLIENT_HTTP) {  // ※通信エラー以外のstatus == 2も復旧処理を行う
/*
                                _bIsCancelDisable = false;
                                Timber.tag("iCAS").i("FC_MSG_TO_MAIN_ON_ERROR OnCancelDisable false");
                                _rwUIEventListener.OnCancelDisable(_bIsCancelDisable);
*/
                                // 決済未完了のため復旧処理を実施
                                if (_money == moneyType.MONEY_SUICA) {
                                    // 交通系
                                    // 同一決済ＩＤ、同一業務、同一金額でRASへアクセスし復旧処理を行う それでもダメな場合は業務処理状態応答を行う
                                    _failure = true;
                                    Timber.tag("iCAS").i("Suica recovery start");

                                    TimerTask task = new TimerTask() {
                                        public void run() {
                                            try {
                                                OnStart(null, true, false);
                                            } catch (JSONException | IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException | InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    };
/*
                                    // 決済不成立 ケース３確認用
                                    Timber.tag("iCAS").d("★★★★★ ジャマー停止 ★★★★★");
*/
                                    Timer timer = new Timer();
                                    timer.schedule(task, SESSION_TIMEOUT_MSTIME_SUICA);
                                } else {
                                    // その他マネー 業務処理状態応答を実施
/*
                                    // 決済不成立 ケース４、５、６確認用
                                    Timber.tag("iCAS").d("★★★★★ ジャマー停止 ★★★★★");
                                    try {
                                        Thread.sleep(10000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
*/
                                    StartStatusReply();
                                }
                            } else if(_money == moneyType.MONEY_SUICA
                                   && _retryCode == JremRasErrorCodes.E353) {
                                // 処理未了発生中にエラーが発生した場合、決済未完了のシーケンスを実施する
                                // 交通系
                                // 同一決済ＩＤ、同一業務、同一金額でRASへアクセスし復旧処理を行う それでもダメな場合は業務処理状態応答を行う
                                _failure = true;
                                Timber.tag("iCAS").i("Suica recovery start");

                                TimerTask task = new TimerTask() {
                                    public void run() {
                                        try {
                                            OnStart(null, true, false);
                                        } catch (JSONException | IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException | InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                };
                                Timer timer = new Timer();
                                timer.schedule(task, SESSION_TIMEOUT_MSTIME_SUICA);
                            } else if(_money == moneyType.MONEY_EDY
                                    && businessId == BUSINESS_ID_EDY_FORCE_REMAIN
                                    && IsNoEnd() ) {
                                // 通信エラーは他マネーと合わせるようにカード番号をハイフン表示させる
                                ((DeviceClient.ResultEdy)(_noEndResult)).saleHistories = null;
                                _rwUIEventListener.OnRecovery((DeviceClient.ResultEdy)(_noEndResult));
//                                    _rwUIEventListener.OnResultEdy((DeviceClient.ResultEdy)(_noEndResult));
                                _rwUIEventListener.OnFinished(0);
                            } else {
                                // 処理失敗 エラーコールバック実行
                                Timber.tag("iCAS").i("FC_MSG_TO_MAIN_ON_ERROR OnErrorOccurred3 arg1=%d string=%s", msg.arg1, (String)msg.obj);
                                _rwUIEventListener.OnErrorOccurred(msg.arg1, (String) msg.obj);
                            }
                        } else {
                            /* 復旧必要（交通系のE353, E354） */
                            try {
                                OnStart(null, true, false);
                                _retry = false;
                            } catch (JSONException | IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        _prevMoney = moneyType.MONEY_UNKNOWN;
                        _prevResult = null; // エラー発生時はリザルトデータクリア
                        break;

                    case FC_MSG_TO_MAIN_ON_OPERATE_CANCEL:     // デバイス操作要求（キャンセル）
                        if(!_bIsAborted && _bIsCancelDisable && (!IsNoEnd() || msg.arg1 == 1)) {
                            Timber.tag("iCAS").i("FC_MSG_TO_MAIN_ON_OPERATE_CANCEL OnCancelDisable false");
                            _bIsCancelDisable = false;
                            _rwUIEventListener.OnCancelDisable(_bIsCancelDisable);
                        }
                        break;

                    case FC_MSG_TO_MAIN_ON_OPERATE_CANCEL_FINAL:     // デバイス操作要求（キャンセル最終確認）
                        if(!_bIsAborted) {
                            Timber.tag("iCAS").i("FC_MSG_TO_MAIN_ON_OPERATE_CANCEL_FINAL OnCancelDisable true");
                            _bIsCancelDisable = true;
                            _rwUIEventListener.OnCancelDisable(_bIsCancelDisable);
                            if(_money == moneyType.MONEY_EDY && msg.arg1 == 1) {
                                Timber.tag("iCAS").d("★★★ noEndFlag true ★★★");
                                _noEndFlag = true;
                            }
                        }
                        break;
                    case FC_MSG_TO_MAIN_ON_OPERATE_STATUS:
                        DeviceClient.Status status;
                        status = (DeviceClient.Status)msg.obj;
                        _status = Integer.parseInt(status.status);

                        if(_status == 1) {
                            PrepareEnd();
                            GetPrepareTimeMillseconds();
                        }
/*
                        // ステータスが変化した段階で処理時間計測用のメソッドを呼ぶ
                        if(_status == 2) {
//                            ProcStart();      // ポーリングで応答があったところからに変更
                        } else if(_status == 3) {
//                            ProcEnd();        // RWCloseに変更
                        }
*/
                        _rwUIEventListener.OnStatusChanged(status);
                        break;
                    case FC_MSG_TO_MAIN_ON_OPERATE_RW_PARAM:
                        _rwUIEventListener.OnUIUpdate((DeviceClient.RWParam)(msg.obj));
                        break;
                    case FC_MSG_TO_MAIN_ON_OPERATE_DISPLAY:
                        _rwUIEventListener.OnDisplay((DeviceClient.Display)(msg.obj));
                        break;
                    case FC_MSG_TO_MAIN_ON_OPERATE_RETRY_NOEND:
                        Timber.tag("iCAS").d("★★★ noEndFlag true ★★★");
                        _noEndFlag = true;
                        if(_money == moneyType.MONEY_NANACO) {
                            // nanacoはキャンセルボタン表示制御のみ
                            _bIsCancelDisable = (boolean)msg.obj;
                            Timber.tag("iCAS").i("FC_MSG_TO_MAIN_ON_OPERATE_RETRY_NOEND OnCancelDisable %S", String.valueOf(_bIsCancelDisable));
                            _rwUIEventListener.OnCancelDisable(_bIsCancelDisable);
                        } else {
                            _noEndResult = msg.obj;
                            _retryCode = JremRasErrorCodes.E353;    // 処理未了によるリトライのコードをセット
                        }
                        break;
                    case FC_MSG_TO_MAIN_ON_OPERATE_OPERATION:
                        // 計測開始
                        PinStart();
                        _rwUIEventListener.OnOperation((DeviceClient.Operation)(msg.obj));
                        break;
                    case FC_MSG_TO_MAIN_ON_OPERATE_CONFIRM:
                        // 計測終了
                        PinEnd();
                        break;
                    case FC_MSG_TO_MAIN_ON_RECOVERY:
                        Timber.tag("iCAS").i("FC_MSG_TO_MAIN_ON_RECOVERY");
                        ProcEnd();
                        _rwUIEventListener.OnRecovery(msg.obj);
                        _prevMoney = moneyType.MONEY_UNKNOWN;
                        _prevResult = null; // リカバリー時はリザルトデータクリア
                        break;
                    case FC_MSG_TO_MAIN_ON_OPERATE_RESULT:
                        if((msg.arg1 == JremRasErrorCodes.E353 || msg.arg1 == JremRasErrorCodes.E355)
                        && (_money == moneyType.MONEY_SUICA)
                        && (Integer.parseInt(_parameter.businessId) != BUSINESS_ID_SUICA_STATUS_REPLY)) {
                            Timber.tag("iCAS").i("FC_MSG_TO_MAIN_ON_OPERATE_RESULT arg1=%d IsNoEnd=%s", msg.arg1, IsNoEnd());
                            // リトライを内部でするためOnResultxxは呼ばない。他マネーはRETRYにより自動でリトライするため353, 355は発生しない。（QUICPayは発生するが復旧不要のため通さない）
                            _retry = true;
                            // 353に1度でもなった場合は要因を変更しない
                            if (_retryCode != JremRasErrorCodes.E353) {
                                _retryCode = msg.arg1;
                            }
                            // 353の場合はResultを保持
                            if (msg.arg1 == JremRasErrorCodes.E353) {
                                _noEndResult = msg.obj;
                                Timber.tag("iCAS").d("★★★ Suica noEndFlag true ★★★");
                                _noEndFlag = true;
                            }
                        } else if(msg.arg1 == JremRasErrorCodes.E95
                               && _money == moneyType.MONEY_WAON
                               && ((DeviceClient.ResultWAON)msg.obj).unFinFlg != null
                               && ((DeviceClient.ResultWAON)msg.obj).unFinFlg.equals("true")
                               && Integer.parseInt(_parameter.businessId) != BUSINESS_ID_WAON_STATUS_REPLY) {
                            Timber.tag("iCAS").i("FC_MSG_TO_MAIN_ON_OPERATE_RESULT WAON Auto Retry");
                            // WAON処理未了タイムアウト時は自動でリトライを実施（WAONは手動で中断されないかぎりリトライを続ける仕様）
                            _retry = true;
                            _bIsCancelDisable = false;
                            Timber.tag("iCAS").i("WAON Auto Retry OnCancelDisable false");
                            _rwUIEventListener.OnCancelDisable(_bIsCancelDisable);
                        } else if(_money == moneyType.MONEY_EDY
                               && ((DeviceClient.ResultEdy)(msg.obj)).autoRetryFlg != null
                               && ((DeviceClient.ResultEdy)(msg.obj)).autoRetryFlg.equals("true")) {
                            Timber.tag("iCAS").i("FC_MSG_TO_MAIN_ON_OPERATE_RESULT Edy Auto Retry");
                            // Edy autoRetryFlgがtrueの場合は自動でリトライを実施
                            _retry = true;
                            _bIsCancelDisable = false;
                            Timber.tag("iCAS").i("Edy Auto Retry OnCancelDisable false");
                            _rwUIEventListener.OnCancelDisable(_bIsCancelDisable);
                            _retryCode = JremRasErrorCodes.E353;
                            _noEndResult = msg.obj;
                            // 処理未了のコードを353に書き換え
                            ((DeviceClient.ResultEdy)_noEndResult).code = String.valueOf(_retryCode);
                            // Edyの自動リトライはリトライフラグを立てる
                            ((BusinessParameter.Edy)(_parameter.money)).retryFlg = "ON";
                            // リトライフラグを変えたのでメッセージ認証コードの生成
                            _parameter.message = generateHmac(_parameter);
                        } else {
                            if(msg.arg1 == 0) {
                                // 正常終了時はリトライ要因をクリア
                                _retryCode = 0;
//                            } else if(msg.arg1 == JremRasErrorCodes.E353 && Integer.parseInt(_parameter.businessId) == BUSINESS_ID_SUICA_STATUS_REPLY) {
                            } else if(_money == moneyType.MONEY_EDY && msg.arg1 == 1042) {
                                // 強残NGは処理未了データクリア
                                _retryCode = 0;
                                _noEndResult = null;
                            } else if(msg.arg1 == JremRasErrorCodes.E353) {
                                _retryCode = JremRasErrorCodes.E353;
                                _noEndResult = msg.obj;
                            }
                            Timber.tag("iCAS").i("FC_MSG_TO_MAIN_ON_OPERATE_RESULT arg1=%d IsNoEnd=%s", msg.arg1, IsNoEnd());

                            switch (_money) {
                                case MONEY_SUICA:
                                    if(IsNoEnd()) {
                                        if(_procEnd != null) {
                                            ProcEnd();
                                        }
                                        _rwUIEventListener.OnResultSuica((DeviceClient.Result)(_noEndResult));
                                    } else {
                                        _rwUIEventListener.OnResultSuica((DeviceClient.Result)(msg.obj));
                                    }
                                    break;
                                case MONEY_ID:
                                    if(IsNoEnd()) {
                                        _rwUIEventListener.OnResultID((DeviceClient.ResultID) (_noEndResult));
                                    } else {
                                        _rwUIEventListener.OnResultID((DeviceClient.ResultID) (msg.obj));
                                    }
                                    break;
                                case MONEY_WAON:
/*
                                    if(Integer.parseInt(_parameter.businessId) == BUSINESS_ID_WAON_PRE_REMAIN) {
                                        try {
                                            _waonResult = msg.obj;
                                            OnStart(_parameter);
                                        } catch (JSONException | IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException | InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        if (IsNoEnd()) {
                                            _rwUIEventListener.OnResultWAON((DeviceClient.ResultWAON) (_noEndResult));
                                        } else {
                                            _rwUIEventListener.OnResultWAON((DeviceClient.ResultWAON) (msg.obj));
                                        }
                                    }
*/
                                    if (IsNoEnd()) {
                                        _rwUIEventListener.OnResultWAON((DeviceClient.ResultWAON) (_noEndResult));
                                    } else {
                                        _rwUIEventListener.OnResultWAON((DeviceClient.ResultWAON) (msg.obj));
                                    }
                                    break;
                                case MONEY_QUICPAY:
                                    if(IsNoEnd()) {
                                        _rwUIEventListener.OnResultQUICPay((DeviceClient.ResultQUICPay)(_noEndResult));
                                    } else {
                                        _rwUIEventListener.OnResultQUICPay((DeviceClient.ResultQUICPay)(msg.obj));
                                    }
                                    break;
                                case MONEY_EDY:
                                    if(IsNoEnd() && _parameter.businessId.equals(String.valueOf(BUSINESS_ID_EDY_FORCE_REMAIN))) {
                                        _rwUIEventListener.OnResultEdy((DeviceClient.ResultEdy)(_noEndResult));
                                    } else {
                                        _rwUIEventListener.OnResultEdy((DeviceClient.ResultEdy)(msg.obj));
                                    }
                                    break;
                                case MONEY_NANACO:
                                    if(IsNoEnd()) {
                                        _rwUIEventListener.OnResultnanaco((DeviceClient.Resultnanaco)(_noEndResult));
                                    } else {
                                        _rwUIEventListener.OnResultnanaco((DeviceClient.Resultnanaco)(msg.obj));
                                    }
                                    break;
                                default:
                                    break;
                            }
                            _retry = false;
                        }
                        _prevMoney = _money;
                        _prevResult = msg.obj;
                        break;
                    case FC_MSG_TO_MAIN_ON_JOURNAL:
                        switch(_money) {
                            case MONEY_EDY:
                                _rwUIEventListener.OnJournalEdy((String)(msg.obj));
                                break;
                            case MONEY_NANACO:
                                _rwUIEventListener.OnJournalnanaco((String)(msg.obj));
                                break;
                            case MONEY_QUICPAY:
                                _rwUIEventListener.OnJournalQUICPay((String)(msg.obj));
                                break;
                        }
                        break;
                    case FC_MSG_TO_MAIN_ON_WAON_PRE_STATUS:     // WAON業務処理状態応答（事前）
                        // 取消可能なのでWAONの取消業務を実施
                        try {
                            OnStart(_parameter);
                        } catch (JSONException | IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException | InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
            }
        };

        String baseURL = BuildConfig.JREM_RAS_BASE_URL + "/";
        switch(_money) {
            case MONEY_SUICA:
                _feliCaClient.SetUrl(baseURL + "pos/start.do");           // 交通系
                break;
            case MONEY_ID:
                _feliCaClient.SetUrl(baseURL +  EmoneyOpeningInfo.getId().url);
                break;
            case MONEY_WAON:
                _feliCaClient.SetUrl(baseURL + EmoneyOpeningInfo.getWaon().url);
                break;
            case MONEY_QUICPAY:
                _feliCaClient.SetUrl(baseURL + EmoneyOpeningInfo.getQuicpay().url);
                break;
            case MONEY_EDY:
                _feliCaClient.SetUrl(baseURL + EmoneyOpeningInfo.getEdy().url);
                break;
            case MONEY_NANACO:
                _feliCaClient.SetUrl(baseURL + EmoneyOpeningInfo.getNanaco().url);
                break;
        }

        // useParamはtrue固定。起動時はTLAM通信しない想定のためdataはnull指定。
        lRetVal = _feliCaClient.Start(true, null, 0);

        return lRetVal;
    }

    public long OnStop(boolean bForced) {
        long lRetVal = FeliCaClient.FC_ERR_CLIENT_FAILURE;

        Timber.tag("iCAS").i("iCASClient OnStop bForced=%s", bForced);

        if(AppPreference.isDemoMode() && _demo != null) {
            lRetVal = _demo.OnStop(bForced);
        } else if(!_bIsCancelDisable) {
            _bIsAborted = true;
//            _feliCaClient.Stop(bForced);
            lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        }

        return lRetVal;
    }

    @Override
    public void OnFinished(int statusCode) {
        Timber.tag("iCAS").i("iCASClient OnFinished returnCode=%04x", statusCode);

        // ログに残すために処理時間計測メソッド呼出
        GetProcTimeMillseconds();
        GetPrepareTimeMillseconds();

        Message message = new Message();
        message.what = FC_MSG_TO_MAIN_ON_FINISH;
        message.arg1 = statusCode;
        _handlerToMainThread.sendMessage(message);
    }

    @Override
    public void OnErrorOccurred(int errorCode, final String errorMessage) {
        Timber.tag("iCAS").i("iCASClient OnErrorOccurred errorCode=%d erroCodOrg=%d errorMessage=%s", getErrorCode(errorCode), errorCode, errorMessage);

        Message message = new Message();
        message.what = FC_MSG_TO_MAIN_ON_ERROR;
        message.arg1 = getErrorCode(errorCode);
        message.obj = errorMessage;
        _handlerToMainThread.sendMessage(message);
    }

    @Override
    public long OnTransmitRW(byte[] command, long timeout, byte[] response) {
        long lRetVal;
        StringBuilder str = new StringBuilder();

        if(command[1] == 0x10 || command[1] == 0x40) {
            ProcStart();      // 相互認証からに変更
        }

        if(_rwUIEventListener == null) {
            Timber.tag("iCAS").i("OnTransmitRW  _rwUIEventListener is null !!!");
            OnStop(false);
            return 0;
        }

        lRetVal = _rwUIEventListener.OnTransmitRW(command, timeout, response);

        for(int idx=0; idx<20; idx++) {
            if(lRetVal < idx+1) {
                break;
            }
            str.append(String.format("%02X", response[idx]));
        }
        Timber.tag("iCAS").i("OnTransmitRW result %d respons %s", lRetVal, str.toString());

        if(lRetVal == 2 && str.toString().equals("6300")) {
//            _isPolling = false;
        } else if(lRetVal == 2 && str.toString().equals("9000")) {
//            _isPolling = false;
        } else if(lRetVal >= 2) {
            _isPolling = true;
        } else {
//            _isPolling = false;
        }
/*
        // WAON処理未了（未書き込み）
        if(noEnd_test == 0 && lRetVal == 26) {
            noEnd_test = 1;
        } else if(noEnd_test == 1 && lRetVal == 26) {
            Timber.tag("iCAS").d("★★★★★ カードを離す ★★★★★");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            response[0] = 0x63;
            response[1] = 0x00;
            lRetVal = 2;
            noEnd_test = 2;
        }
*/
/*
        // WAON処理未了（書き込み済み）
        if(noEnd_test == 0 && lRetVal == 26) {
            noEnd_test = 1;
        } else if(noEnd_test == 1 && lRetVal == 26) {
            noEnd_test = 2;
        } else if(noEnd_test == 2 && lRetVal == 26) {
            Timber.tag("iCAS").d("★★★★★ カードを離す ★★★★★");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            response[0] = 0x63;
            response[1] = 0x00;
            lRetVal = 2;
            noEnd_test = 2;
        }
*/
/*
        // Edy処理未了中の処理未了用コード
        if(lRetVal == 11 && noEnd_test == 1) {
            // ここでカードを離すと未完了
            Timber.tag("iCAS").d("★★★★★ カードを離す ★★★★★");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            noEnd_test = 2;
        }
*/
/*
        // Edy処理未了確認用コード（強残NG）
        if(lRetVal == 11) {
            // ここでカードを離すと未完了
            if(noEnd_test == 0) {
                Timber.tag("iCAS").d("★★★★★ カードを離す ★★★★★");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                noEnd_test = 1;
            }
        }
*/
/*
        // Edy処理未了確認用コード（強残OK）
        if(lRetVal == 11) {
            noEnd_test = 1;
        }
        if(noEnd_test == 1 && lRetVal == 26) {
            Timber.tag("iCAS").d("★★★★★ カードを離す ★★★★★");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            response[0] = 0x63;
            response[1] = 0x00;
            lRetVal = 2;
            noEnd_test = 2;
        }
*/
/*
        if(lRetVal > 1) {
            if(response[1] == 0x01 && _status == 1) {
                ProcStart();      // ポーリングで応答があったところからに変更
            }
        }
*/
/*
        if(lRetVal > 1) {
            if(response[1] == 0x01 && _status == 1) {
                transFlag = true;
            }
        }

        if(transFlag) {
            transCount++;
        }

        if(transCount == 6) {
            int a = 1;
            a = a+2;
        }
*/
        return lRetVal;
    }

    @Override
    public void OnRWClose() {
        ProcEnd();
    }

    @Override
    public long OnDeviceOperate(IDevice.DeviceOperate deviceOperate, TCAPPacket replayPacket) {
        long lRetVal = FeliCaClient.FC_ERR_SUCCESS;

        try {
            iCASDevice icasDevice = new iCASDevice(deviceOperate, _money, Integer.parseInt(_parameter.businessId));
            icasDevice.Operate(_bIsAborted, _handlerToMainThread, replayPacket, _isPolling);
        } catch (IOException | InterruptedException exception) {
            lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
        }

        return lRetVal;
    }

    @Override
    public long OnNoneChipAccessResponse(byte[] data, int dataLength) {
        long lRetVal = FeliCaClient.FC_ERR_SUCCESS;

        try {
            iCASDevice icasDevice = new iCASDevice(null, _money, Integer.parseInt(_parameter.businessId));
            icasDevice.NoneChipAccessResponse(data, dataLength, _handlerToMainThread);
        } catch (IOException exception) {
            lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
        }

        return lRetVal;
    }

    /**
     * 業務パラメータチェック
     * 業務パラメータが正しいかチェックします。
     *
     * @param parameter 業務パラメータ
     *
     * @return マネー種別（チェックＮＧの場合はマネー種別を不明で返却）
     */
    public moneyType checkBusinessParameter(BusinessParameter parameter) {
        moneyType lRetVal = moneyType.MONEY_UNKNOWN;

        // 業務識別子
        if(parameter.businessId != null && !parameter.businessId.equals("")) {
            int id = Integer.parseInt(parameter.businessId);
            Timber.tag("iCAS").i("checkBusinessParameter businessId=%d", id);
            lRetVal = GetMoneyTypeFromBusinessID(id);
        }

        if(lRetVal == moneyType.MONEY_UNKNOWN) {
            return lRetVal;
        }

        if((parameter.sid == null || parameter.sid.equals("")) && (Integer.parseInt(parameter.businessId) != BUSINESS_ID_EDY_FIRST_COMM) && (Integer.parseInt(parameter.businessId) != BUSINESS_ID_EDY_REMOVAL)) {
            return moneyType.MONEY_UNKNOWN;
        } else {
            if((Integer.parseInt(parameter.businessId) != BUSINESS_ID_EDY_FIRST_COMM) && (Integer.parseInt(parameter.businessId) != BUSINESS_ID_EDY_REMOVAL)) {
                int sid = Integer.parseInt(parameter.sid);
                Timber.tag("iCAS").i("checkBusinessParameter sid=%d", sid);
            }
        }

        // マネー毎のチェック
        switch(lRetVal) {
            case MONEY_SUICA:
                lRetVal = checkSuica(parameter);
                break;
            case MONEY_ID:
                break;
            case MONEY_WAON:
                lRetVal = checkWAON(parameter);
                break;
            case MONEY_QUICPAY:
                break;
            case MONEY_EDY:
                break;
            case MONEY_NANACO:
                lRetVal = checknanaco(parameter);
                break;
            default:
                return moneyType.MONEY_UNKNOWN;
        }

        return lRetVal;
    }

    public moneyType GetMoneyTypeFromBusinessID(int id) {
        moneyType lRetVal = moneyType.MONEY_UNKNOWN;

        if(id == BUSINESS_ID_SUICA_PAY
                || id == BUSINESS_ID_SUICA_REFUND
                || id == BUSINESS_ID_SUICA_REMAIN
//            || id == BUSINESS_ID_SUICA_CHARGE     // ★チャージをするときはここを有効にする
                || id == BUSINESS_ID_SUICA_IDI_PAY
                || id == BUSINESS_ID_SUICA_STATUS_REPLY) {
            // 交通系
            lRetVal = moneyType.MONEY_SUICA;
        } else if(id == BUSINESS_ID_ID_PAY
                || id == BUSINESS_ID_ID_REFUND
                || id == BUSINESS_ID_ID_REMAIN
                || id == BUSINESS_ID_ID_JOURNAL
                || id == BUSINESS_ID_ID_STATUS_REPLY) {
            // iD
            lRetVal = moneyType.MONEY_ID;
        } else if(id == BUSINESS_ID_WAON_PAY
                || id == BUSINESS_ID_WAON_REMAIN
                || id == BUSINESS_ID_WAON_REFUND
//                || id == BUSINESS_ID_WAON_CHARGE
//                || id == BUSINESS_ID_WAON_PRE_REMAIN
                || id == BUSINESS_ID_WAON_STATUS_REPLY
                || id == BUSINESS_ID_WAON_PRE_STATUS_REPLY) {
            // WAON
            lRetVal = moneyType.MONEY_WAON;
        } else if(id == BUSINESS_ID_QUICPAY_PAY
                || id == BUSINESS_ID_QUICPAY_REFUND
                || id == BUSINESS_ID_QUICPAY_HISTORY
                || id == BUSINESS_ID_QUICPAY_STATUS_REPLY
                || id == BUSINESS_ID_QUICPAY_JOURNAL) {
            // QUICPay
            lRetVal = moneyType.MONEY_QUICPAY;
        } else if(id == BUSINESS_ID_EDY_PAY
                || id == BUSINESS_ID_EDY_CHARGE
                || id == BUSINESS_ID_EDY_REMAIN
                || id == BUSINESS_ID_EDY_FORCE_REMAIN
                || id == BUSINESS_ID_EDY_HISTORY
                || id == BUSINESS_ID_EDY_STATUS_REPLY
                || id == BUSINESS_ID_EDY_FIRST_COMM
                || id == BUSINESS_ID_EDY_CHARGE_AUTH
                || id == BUSINESS_ID_EDY_REMOVAL
                || id == BUSINESS_ID_EDY_JOURNAL) {
            // Edy
            lRetVal = moneyType.MONEY_EDY;
        } else if(id == BUSINESS_ID_NANACO_PAY
                || id == BUSINESS_ID_NANACO_REMAIN
//                || id == BUSINESS_ID_NANACO_CHARGE
                || id == BUSINESS_ID_NANACO_PREV_TRAN
                || id == BUSINESS_ID_NANACO_JOURNAL) {
            // nanaco
            lRetVal = moneyType.MONEY_NANACO;
        } else {
            // 不明な業務識別子
            Timber.tag("iCAS").i("unknown businessId");
        }

        return lRetVal;
    }

    public moneyType checkSuica(BusinessParameter parameter) {
        int id = Integer.parseInt(parameter.businessId);
        moneyType lRetVal = moneyType.MONEY_SUICA;
        BusinessParameter.Suica suica = (BusinessParameter.Suica)parameter.money;

        // 処理金額
        if(suica.value != null) {
            int value = Integer.parseInt(suica.value);
            Timber.tag("iCAS").i("checkBusinessParameter value=%d", value);
            // 交通系は1～99,999円まで
            if((0 >= value || value > 99999) && (id == BUSINESS_ID_SUICA_PAY)) {
                return moneyType.MONEY_UNKNOWN;
            }
        }

        // 現金併用
        if(id == BUSINESS_ID_SUICA_PAY || id == BUSINESS_ID_SUICA_IDI_PAY) {
            if(suica.together != null && !suica.together.equals("") && !suica.together.equals("ON") && !suica.together.equals("OFF")) {
                Timber.tag("iCAS").i("checkSuica unknown together=%s", suica.together);
                return moneyType.MONEY_UNKNOWN;
            } else {
                Timber.tag("iCAS").i("checkSuica together=%s", suica.together);
            }
        } else {
            // 引去以外はnullに設定しなおす
            suica.together = null;
        }

        // 旧決済ID
        if(id == BUSINESS_ID_SUICA_STATUS_REPLY) {
            if(suica.oldSid != null && !suica.oldSid.equals("")) {
                int oldSid = Integer.parseInt(suica.oldSid);
                Timber.tag("iCAS").i("checkSuica oldSid=%d", oldSid);
            } else {
                return moneyType.MONEY_UNKNOWN;
            }

        } else {
            // 業務処理状態応答以外はnullに設定しなおす
            suica.oldSid = null;
        }

        // UIガイドライン対応フラグ
        if(id == BUSINESS_ID_SUICA_STATUS_REPLY) {
            if (suica.uiGuideline != null && !suica.uiGuideline.equals("")) {
                // 業務処理状態応答はnullに設定しなおす
                suica.uiGuideline = null;
            }
        } else {
            if(suica.uiGuideline != null && !suica.uiGuideline.equals("") && !suica.uiGuideline.equals("ON") && !suica.uiGuideline.equals("OFF")) {
                Timber.tag("iCAS").i("checkSuica unknown uiGuideline=%s", suica.uiGuideline);
                return moneyType.MONEY_UNKNOWN;
            } else {
                Timber.tag("iCAS").i("checkSuica uiGuideline=%s", suica.uiGuideline);
            }
        }

        // 処理中ＵＩフラグ
        if(id == BUSINESS_ID_SUICA_STATUS_REPLY) {
            if (suica.inProgressUI != null && !suica.inProgressUI.equals("")) {
                // 業務処理状態応答はnullに設定しなおす
                suica.inProgressUI = null;
            }
        } else {
            if(suica.inProgressUI != null && !suica.inProgressUI.equals("") && !suica.inProgressUI.equals("ON") && !suica.inProgressUI.equals("OFF")) {
                Timber.tag("iCAS").i("checkSuica unknown inProgressUI=%s", suica.inProgressUI);
                return moneyType.MONEY_UNKNOWN;
            } else {
                Timber.tag("iCAS").i("checkSuica inProgressUI=%s", suica.inProgressUI);
            }
        }

        return lRetVal;
    }

    public moneyType checkWAON(BusinessParameter parameter) {
        int id = Integer.parseInt(parameter.businessId);
        moneyType lRetVal = moneyType.MONEY_WAON;
        BusinessParameter.Waon waon = (BusinessParameter.Waon)parameter.money;

        // WAONのチェック処理はパラメータの項目が多いのと、ＮＧはそもそも実装エラーなので重要な項目以外はチェックしない
        // 処理金額
        if(waon.value != null) {
            int val = Integer.parseInt(waon.value);
            Timber.tag("iCAS").i("checkBusinessParameter value=%d", val);
            // その他マネーは1～999,999円まで
            if((0 >= val || val > 999999) && (id == BUSINESS_ID_WAON_PAY || id == BUSINESS_ID_WAON_REFUND)) {
                return moneyType.MONEY_UNKNOWN;
            }
        }

        // 現金併用
        if(id == BUSINESS_ID_WAON_PAY) {
            if(waon.together == null || waon.together.equals("") || (!waon.together.equals("ON") && !waon.together.equals("OFF"))) {
                Timber.tag("iCAS").i("checkWAON unknown together=%s", waon.together);
                return moneyType.MONEY_UNKNOWN;
            } else {
                Timber.tag("iCAS").i("checkWAON together=%s", waon.together);
            }
        } else {
            // 引去以外はnullに設定しなおす
            waon.together = null;
        }

        // 合計取引金額
        if(id == BUSINESS_ID_WAON_PAY) {
            if(waon.totalValue == null || waon.totalValue.equals("")) {
                Timber.tag("iCAS").i("checkWAON unknown totalValue=%s", waon.totalValue);
                return moneyType.MONEY_UNKNOWN;
            } else {
                int value = Integer.parseInt(waon.totalValue);
                Timber.tag("iCAS").i("checkWAON totalValue=%d", value);
                if(1 > value || value > 100000) {
                    return moneyType.MONEY_UNKNOWN;
                }
            }
        } else {
            // 引去以外はnullに設定しなおす
            waon.totalValue = null;
        }

        // ポイント対象金額
        if(id == BUSINESS_ID_WAON_PAY) {
            if(waon.pointValue == null || waon.pointValue.equals("")) {
                Timber.tag("iCAS").i("checkWAON unknown pointValue=%s", waon.pointValue);
                return moneyType.MONEY_UNKNOWN;
            } else {
                int value = Integer.parseInt(waon.pointValue);
                Timber.tag("iCAS").i("checkWAON pointValue=%d", value);
                if(0 > value || value > 100000) {
                    return moneyType.MONEY_UNKNOWN;
                }
            }
        } else {
            // 引去以外はnullに設定しなおす
            waon.pointValue = null;
        }

        return lRetVal;
    }

    public moneyType checknanaco(BusinessParameter parameter) {
        int id = Integer.parseInt(parameter.businessId);
        moneyType lRetVal = moneyType.MONEY_NANACO;
        BusinessParameter.nanaco nanaco = (BusinessParameter.nanaco)parameter.money;

        // nanacoのチェック処理はＮＧはそもそも実装エラーなので重要な項目以外はチェックしない
        // 処理金額
        if(nanaco.value != null) {
            int val = Integer.parseInt(nanaco.value);
            Timber.tag("iCAS").i("checkBusinessParameter value=%d", val);
            // その他マネーは1～999,999円まで
            if((0 >= val || val > 999999) && (id == BUSINESS_ID_NANACO_PAY)) {
                return moneyType.MONEY_UNKNOWN;
            }
        }

        return lRetVal;
    }

    public String generateHmac(BusinessParameter parameter) {
        StringBuilder retVal = new StringBuilder();
        String key, val;
//        int id = Integer.parseInt(parameter.businessId);

        key = parameter.commonName + parameter.organizationalUnit;
        val = parameter.businessId + parameter.vr + parameter.time;// + parameter.sid;

        // Edyの初回通信業務はsidがnull
        if(parameter.sid != null) {
            val += parameter.sid;
        }

        // マネー毎の生成（businessparameterからjsonを生成した場合、項目が順不同になってしまうため、各マネー毎に仕様書の定義順にvalを算出する)
        switch(_money) {
            case MONEY_SUICA:
                BusinessParameter.Suica suica = (BusinessParameter.Suica)parameter.money;

                if(suica.value != null) {
                    val += suica.value;
                }
                if(suica.idi != null) {
                    val += suica.idi;
                }
                if(suica.together != null) {
                    val += suica.together;
                }
                if(suica.oldSid != null) {
                    val += suica.oldSid;
                }
                if(suica.uiGuideline != null) {
                    val += suica.uiGuideline;
                }
                if(suica.inProgressUI != null) {
                    val += suica.inProgressUI;
                }
                break;
            case MONEY_ID:
                BusinessParameter.iD iD = (BusinessParameter.iD)parameter.money;

                if(iD.value != null) {
                    val += iD.value;
                }
                if(iD.slipNo != null) {
                    val += iD.slipNo;
                }
                if(iD.oldTermIdentId != null) {
                    val += iD.oldTermIdentId;
                }
                if(iD.oldSid != null) {
                    val += iD.oldSid;
                }
                if(iD.payment != null) {
                    val += iD.payment;
                }
                if(iD.training != null) {
                    val += iD.training;
                }
                if(iD.uiGuideline != null) {
                    val += iD.uiGuideline;
                }
                if(iD.inProgressUI != null) {
                    val += iD.inProgressUI;
                }
                break;
            case MONEY_WAON:
                BusinessParameter.Waon waon = (BusinessParameter.Waon)parameter.money;
                if(waon.value != null) {
                    val += waon.value;
                }
                if(waon.together != null) {
                    val += waon.together;
                }
                if(waon.totalValue != null) {
                    val += waon.totalValue;
                }
                if(waon.pointValue != null) {
                    val += waon.pointValue;
                }
                if(waon.slipNo != null) {
                    val += waon.slipNo;
                }
                if(waon.idm != null) {
                    val += waon.idm;
                }
                if(waon.waonNum != null) {
                    val += waon.waonNum;
                }
                if(waon.cardThroughNum != null) {
                    val += waon.cardThroughNum;
                }
                if(waon.oldTermIdentId != null) {
                    val += waon.oldTermIdentId;
                }
                if(waon.oldSid != null) {
                    val += waon.oldSid;
                }
                if(waon.training != null) {
                    val += waon.training;
                }
                if(waon.uiGuideline != null) {
                    val += waon.uiGuideline;
                }
                if(waon.inProgressUI != null) {
                    val += waon.inProgressUI;
                }
                break;
            case MONEY_QUICPAY:
                BusinessParameter.QUICPay quicPay = (BusinessParameter.QUICPay)parameter.money;
                if(quicPay.value != null) {
                    val += quicPay.value;
                }
                if(quicPay.slipNo != null) {
                    val += quicPay.slipNo;
                }
                if(quicPay.oldTermIdentId != null) {
                    val += quicPay.oldTermIdentId;
                }
                if(quicPay.oldSid != null) {
                    val += quicPay.oldSid;
                }
                if(quicPay.training != null) {
                    val += quicPay.training;
                }
                if(quicPay.uiGuideline != null) {
                    val += quicPay.uiGuideline;
                }
                if(quicPay.inProgressUI != null) {
                    val += quicPay.inProgressUI;
                }
                break;
            case MONEY_EDY:
                BusinessParameter.Edy edy = (BusinessParameter.Edy)parameter.money;
                if(edy.value != null) {
                    val += edy.value;
                }
                if(edy.depositMeans != null) {
                    val += edy.depositMeans;
                }
                if(edy.retryFlg != null) {
                    val += edy.retryFlg;
                }
                if(edy.totalAmountDischargeFlg != null) {
                    val += edy.totalAmountDischargeFlg;
                }
                if(edy.otherCardUseFlg != null) {
                    val += edy.otherCardUseFlg;
                }
                if(edy.oldSid != null) {
                    val += edy.oldSid;
                }
                if(edy.daTermFrom != null) {
                    val += edy.daTermFrom;
                }
                if(edy.training != null) {
                    val += edy.training;
                }
                if(edy.uiGuideline != null) {
                    val += edy.uiGuideline;
                }
                if(edy.inProgressUI != null) {
                    val += edy.inProgressUI;
                }
                break;
            case MONEY_NANACO:
                BusinessParameter.nanaco nanaco = (BusinessParameter.nanaco)parameter.money;
                if(nanaco.oldSid != null) {
                    val += nanaco.oldSid;
                }
                if(nanaco.value != null) {
                    val += nanaco.value;
                }
                if(nanaco.totalAmountDischargeFlg != null) {
                    val += nanaco.totalAmountDischargeFlg;
                }
                if(nanaco.otherCardUseFlg != null) {
                    val += nanaco.otherCardUseFlg;
                }
                if(nanaco.daTermFrom != null) {
                    val += nanaco.daTermFrom;
                }
                if(nanaco.training != null) {
                    val += nanaco.training;
                }
                if(nanaco.cancelStopFlg != null) {
                    val += nanaco.cancelStopFlg;
                }
                if(nanaco.uiGuideline != null) {
                    val += nanaco.uiGuideline;
                }
                if(nanaco.inProgressUI != null) {
                    val += nanaco.inProgressUI;
                }
                break;
            default:
                return "";
        }

        try {
            // HMACの計算を実施する
            byte[] hmacByte = getHmacSha1(key, val);

            // 計算後の値を文字列に変換する
            for(byte data : hmacByte) {
                retVal.append(String.format("%02X", data));
            }

        } catch (NoSuchAlgorithmException e) {
            Timber.tag("iCAS").i("generateHmac NoSuchAlgorithmException %s", e.getMessage());
            return "";
        } catch (InvalidKeyException e) {
            Timber.tag("iCAS").i("generateHmac InvalidKeyException %s", e.getMessage());
            return "";
        }

        Timber.tag("iCAS").i("★HMAC key = %s", key);
        Timber.tag("iCAS").i("★HMAC val = %s", val);
        Timber.tag("iCAS").i("★HMAC = %s", retVal.toString());

        return retVal.toString();
    }

    /**
     * HMAC-SHA1 方式を使って、HMAC 値を計算する
     * @param key キーとなる値
     * @param value 計算用の値
     * @return 計算後のHMAC 値
     * @throws NoSuchAlgorithmException, InvalidKeyException
     */
    public static byte[] getHmacSha1(String key, String value) throws NoSuchAlgorithmException, InvalidKeyException {
        return getHmac(key, value, "HmacSHA1");
    }

    /**
     * HMAC-SHA1 方式を使って、HMAC 値を計算する
     * @param key キーのなる値
     * @param value 計算用の値
     * @param macMethodName 計算方式(MAC_NAME_HMACSHA1、又は、
     * MAC_NAME_HMACMD5)
     *
     * @return 計算後のHMAC 値
     * @throws NoSuchAlgorithmException, InvalidKeyException
     */
    public static byte[] getHmac(String key, String value, String macMethodName)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac m;
        m = Mac.getInstance(macMethodName);
        SecretKey secKey = new SecretKeySpec(key.getBytes(), macMethodName);
        m.init(secKey);
        byte[] valueByte = value.getBytes();
        m.update(valueByte);
        return m.doFinal();
    }

    @Override
    public void OnCancelAvailable() {
        // iCASClientTestでのみ利用するため処理なし
    }

    @Override
    public void OnCommunicationOff() {
        // iCASClientTestでのみ利用するため処理なし
    }

    public void SetParameterKeys(File file, BusinessParameter parameter) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        final InputStream inputStream = new FileInputStream(file);
        KeyStore p12 = KeyStore.getInstance("PKCS12");
        p12.load(inputStream, AppPreference.getJremPassword().toCharArray());

        inputStream.close();

        Enumeration<String> e = p12.aliases();

        parameter.commonName = null;
        parameter.organizationalUnit = null;

        while(e.hasMoreElements()) {
            String alias = e.nextElement();
            X509Certificate c = (X509Certificate) p12.getCertificate(alias);
            Principal subject = c.getSubjectDN();
            String[] subjectArray = subject.toString().split(",");
            for(String s : subjectArray) {
                String[] str = s.trim().split("=");
                if(str[0].equals("CN") && parameter.commonName == null) {
                    parameter.commonName = str[1];
                    Timber.tag("iCAS").i("%s=%s", str[0], str[1]);
                } else if(str[0].equals("OU") && parameter.organizationalUnit == null) {
                    parameter.organizationalUnit = str[1];
                    Timber.tag("iCAS").i("%s=%s", str[0], str[1]);
                }

                if(parameter.commonName != null && parameter.organizationalUnit != null) {
                    break;
                }
            }
        }
    }

    static public String MakeSid() {
        String retVal;
        Date dateObj = new Date();
        SimpleDateFormat format = new SimpleDateFormat( "MddHHmm", Locale.JAPAN );
        retVal = format.format( dateObj );
        retVal += String.format(Locale.JAPAN, "%02d", _tuuban);

        _tuuban++;
        if(_tuuban >= 100) {
            _tuuban = 0;
        }

        return retVal;
    }

    public void SetRWUIEventListener(IiCASClient listener) {
        _rwUIEventListener = listener;
    }

    public static int getErrorCode(long errorCode) {
        Integer value = _errorMap.get((int)errorCode);
        return value != null ? value : 0;
    }

    public void PrepareStart() {
        String date;
        Date dateObj = new Date();
        SimpleDateFormat format = new SimpleDateFormat( "yyMMddHHmmssSSS", Locale.JAPAN );
        date = format.format( dateObj );
        Timber.tag("iCAS").i("PrepareStart %s", date);
        try {
            _prepareStart = format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    public void PrepareEnd() {
        String date;
        Date dateObj = new Date();
        SimpleDateFormat format = new SimpleDateFormat( "yyMMddHHmmssSSS", Locale.JAPAN );
        date = format.format( dateObj );
        Timber.tag("iCAS").i("PrepareEnd %s", date);
        try {
            _prepareEnd = format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    public void ProcStart() {
        String date;
        Date dateObj = new Date();
        SimpleDateFormat format = new SimpleDateFormat( "yyMMddHHmmssSSS", Locale.JAPAN );
        date = format.format( dateObj );
        Timber.tag("iCAS").i("ProcStart %s", date);
        try {
            _procStart = format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    public void ProcEnd() {
        String date;
        Date dateObj = new Date();
        SimpleDateFormat format = new SimpleDateFormat( "yyMMddHHmmssSSS", Locale.JAPAN );
        date = format.format( dateObj );
        Timber.tag("iCAS").i("ProcEnd %s", date);
        try {
            _procEnd = format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    public void PinStart() {
        String date;
        Date dateObj = new Date();
        SimpleDateFormat format = new SimpleDateFormat( "yyMMddHHmmssSSS", Locale.JAPAN );
        date = format.format( dateObj );
        Timber.tag("iCAS").i("PinStart %s", date);
        try {
            _pinStart = format.parse(date);
            _pinEnd = null;
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    public void PinEnd() {
        String date;
        Date dateObj = new Date();
        SimpleDateFormat format = new SimpleDateFormat( "yyMMddHHmmssSSS", Locale.JAPAN );
        date = format.format( dateObj );
        Timber.tag("iCAS").i("PinEnd %s", date);
        try {
            _pinEnd = format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    public int GetPrepareTimeMillseconds() {
        long diff = 0;

        if(_prepareStart != null && _prepareEnd != null) {
            diff = _prepareEnd.getTime() - _prepareStart.getTime();
        }
        Timber.tag("iCAS").i("preparetime %d ミリ秒", diff);

        return (int)diff;
    }
    public int GetProcTimeMillseconds() {
        long diff = 0;

        if(_procStart != null && _procEnd != null) {
//            diff = _procEnd.getTime() - _procStart.getTime() - GetPinTimeMillseconds();
            // ログに残すためにピン入力時間取得
            GetPinTimeMillseconds();
            diff = _procEnd.getTime() - _procStart.getTime();
        }
        Timber.tag("iCAS").i("proctime %d ミリ秒", diff);

        return (int)diff;
    }
    public int GetPinTimeMillseconds() {
        long diff = 0;

        if(_pinStart != null && _pinEnd != null) {
            diff = _pinEnd.getTime() - _pinStart.getTime();
        }

        Timber.tag("iCAS").i("pintime %d ミリ秒", diff);

        return (int)diff;
    }
    public void Initialize(boolean bRetry, BusinessParameter parameter) {
//        transCount = 0;
//        transFlag = false;

        // キャンセル禁止フラグクリア
        _bIsCancelDisable = false;
        // 中断フラグクリア
        _bIsAborted = false;
        // ステータスクリア
        _status = 1;

        // 計測時間クリア
//        _prepareStart = null;
        _prepareEnd = null;
        if(!bRetry) {
            _procStart = null;
            _pinStart = null;
            _pinEnd = null;
        }
        _procEnd = null;

        if(!bRetry) {
            iCASDevice.Initialize();
            _isPolling = false;
            // リトライフラグクリア
            _retry = false;
            _noEndFlag = false;
            // 強制残高照会時のタイムアウトで使用するため処理未了のデータは残しておく
            if(Integer.parseInt(parameter.businessId) != BUSINESS_ID_EDY_FORCE_REMAIN) {
                _retryCode = 0;
                _noEndResult = null;
                _failure = false;
            }
        }
    }

    // 処理未了判定
    public boolean IsNoEnd() {
        boolean bRet = false;

        if(_retryCode == JremRasErrorCodes.E353 && _noEndResult != null) {
            // 処理未了
            bRet = true;
        }

        return bRet;
    }

    // OnError時はIsNoEndを呼んで処理未了だったらこれを呼んでもらう
    public Object GetNoEndResult() {
        return _noEndResult;
    }

    public static void SetPinData(byte[] pinData, int pinLength) {
        _pinLength = pinLength;
        if(_pinLength > 0) {
            System.arraycopy(pinData, 0, _pinData, 0, _pinLength);
        }
    }

    public static byte[] GetPinData() {
        if(_pinLength > 0) {
            return _pinData;
        } else {
            return null;
        }
    }

    public static int GetPinLength() { return _pinLength; }

    public static void SetPinCancel(boolean cancel) { _pinCancel = cancel; }

    public static boolean GetPinCancel() { return _pinCancel; }

    public void StartStatusReply() {
        int timeout = 0;
        String oldSid = _parameter.sid;

        Timber.tag("iCAS").i("StartStatusReply");
        _parameter = new BusinessParameter();

        switch(_money) {
            case MONEY_SUICA:
                timeout = SESSION_TIMEOUT_MSTIME_SUICA;
                BusinessParameter.Suica suica = new BusinessParameter.Suica();
                _parameter.businessId = String.valueOf(BUSINESS_ID_SUICA_STATUS_REPLY);
                _parameter.money = suica;
                suica.value = null;
                suica.idi = null;
                suica.together = null;
                suica.oldSid = oldSid;
                suica.uiGuideline = null;
                suica.inProgressUI = null;
                break;
            case MONEY_ID:
                timeout = SESSION_TIMEOUT_MSTIME_ID;
                BusinessParameter.iD id = new BusinessParameter.iD();
                _parameter.businessId = String.valueOf(BUSINESS_ID_ID_STATUS_REPLY);
                _parameter.money = id;
                id.oldSid = oldSid;
                id.training = "OFF";
                break;
            case MONEY_WAON:
                timeout = SESSION_TIMEOUT_MSTIME_WAON;
                BusinessParameter.Waon waon = new BusinessParameter.Waon();
                _parameter.businessId = String.valueOf(BUSINESS_ID_WAON_STATUS_REPLY);
                _parameter.money = waon;
                waon.oldSid = oldSid;
                waon.training = "OFF";
                break;
            case MONEY_QUICPAY:
                timeout = SESSION_TIMEOUT_MSTIME_QUICPAY;
                BusinessParameter.QUICPay quicPay = new BusinessParameter.QUICPay();
                _parameter.businessId = String.valueOf(BUSINESS_ID_QUICPAY_STATUS_REPLY);
                _parameter.money = quicPay;
                quicPay.oldSid = oldSid;
                quicPay.training = "OFF";
                break;
            case MONEY_EDY:
                timeout = SESSION_TIMEOUT_MSTIME_EDY;
                BusinessParameter.Edy edy = new BusinessParameter.Edy();
                _parameter.businessId = String.valueOf(BUSINESS_ID_EDY_STATUS_REPLY);
                _parameter.money = edy;
                edy.oldSid = oldSid;
                edy.training = "OFF";
                break;
            case MONEY_NANACO:
                timeout = SESSION_TIMEOUT_MSTIME_NANACO;
                BusinessParameter.nanaco nanaco = new BusinessParameter.nanaco();
                _parameter.businessId = String.valueOf(BUSINESS_ID_NANACO_PREV_TRAN);
                _parameter.money = nanaco;
                nanaco.oldSid = oldSid;
                nanaco.training = "OFF";
                break;
            default:
                break;
        }

        TimerTask task = new TimerTask() {
            public void run() {
                try {

                    Date procStart = null;
                    if(_procStart != null) {
                        procStart = new Date();
                        procStart = (Date) _procStart.clone();
                    }
                    OnStart(_parameter);
                    _procStart = procStart;
                } catch (JSONException | IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Timer timer = new Timer();
        timer.schedule(task, timeout);
    }

    public void StatusReplyFailure() {
        Timber.tag("iCAS").i("StatusReplyFailure");

        ProcEnd();

        switch(_money) {
            case MONEY_SUICA:
                DeviceClient.Result result = new DeviceClient.Result();
                result.result = "false";
                result.code = String.valueOf(JremRasErrorCodes.E353);
                result.sid = ((BusinessParameter.Suica)(_parameter.money)).oldSid;
                _rwUIEventListener.OnRecovery(result);
//                _rwUIEventListener.OnResultSuica(result);
                break;
            case MONEY_ID:
                DeviceClient.ResultID resultID = new DeviceClient.ResultID();
                resultID.result = "false";
                resultID.code = String.valueOf(JremRasErrorCodes.E353);
                resultID.sid = ((BusinessParameter.iD)(_parameter.money)).oldSid;
                _rwUIEventListener.OnRecovery(resultID);
//                _rwUIEventListener.OnResultID(resultID);
                break;
            case MONEY_WAON:
                DeviceClient.ResultWAON resultWAON = new DeviceClient.ResultWAON();
                resultWAON.result = "false";
                resultWAON.code = String.valueOf(JremRasErrorCodes.E353);
                resultWAON.sid = ((BusinessParameter.Waon)(_parameter.money)).oldSid;
                _rwUIEventListener.OnRecovery(resultWAON);
//                _rwUIEventListener.OnResultWAON(resultWAON);
                break;
            case MONEY_QUICPAY:
                DeviceClient.ResultQUICPay resultQUICPay = new DeviceClient.ResultQUICPay();
                resultQUICPay.result = "false";
                resultQUICPay.code = String.valueOf(JremRasErrorCodes.E353);
                resultQUICPay.sid = ((BusinessParameter.QUICPay)(_parameter.money)).oldSid;
                _rwUIEventListener.OnRecovery(resultQUICPay);
//                _rwUIEventListener.OnResultQUICPay(resultQUICPay);
                break;
            case MONEY_EDY:
                DeviceClient.ResultEdy resultEdy = new DeviceClient.ResultEdy();
                resultEdy.result = "false";
                resultEdy.code = String.valueOf(JremRasErrorCodes.E353);
                resultEdy.sid = ((BusinessParameter.Edy)(_parameter.money)).oldSid;
                _rwUIEventListener.OnRecovery(resultEdy);
//                _rwUIEventListener.OnResultEdy(resultEdy);
                break;
            case MONEY_NANACO:
                DeviceClient.Resultnanaco resultnanaco = new DeviceClient.Resultnanaco();
                resultnanaco.result = "false";
                resultnanaco.code = String.valueOf(JremRasErrorCodes.E353);
                resultnanaco.sid = ((BusinessParameter.nanaco)(_parameter.money)).oldSid;
                _rwUIEventListener.OnRecovery(resultnanaco);
//                _rwUIEventListener.OnResultnanaco(resultnanaco);
                break;
            default:
                break;
        }
    }

    public String GetSid() {
        if(_parameter != null) {
            return _parameter.sid;
        } else {
            return null;
        }
    }

    public String GetResult(iCASClient.moneyType money) {
        String ret = null;

        if(_prevResult != null && _money == _prevMoney) {
            switch(money) {
                case MONEY_SUICA:
                    ret = ((DeviceClient.Result)_prevResult).result;
                    break;
                case MONEY_ID:
                    ret = ((DeviceClient.ResultID)_prevResult).result;
                    break;
                case MONEY_WAON:
                    ret = ((DeviceClient.ResultWAON)_prevResult).result;
                    break;
                case MONEY_QUICPAY:
                    ret = ((DeviceClient.ResultQUICPay)_prevResult).result;
                    break;
                case MONEY_EDY:
                    ret = ((DeviceClient.ResultEdy)_prevResult).result;
                    break;
                case MONEY_NANACO:
                    ret = ((DeviceClient.Resultnanaco)_prevResult).result;
                    break;
            }
        }

        return ret;
    }
}
