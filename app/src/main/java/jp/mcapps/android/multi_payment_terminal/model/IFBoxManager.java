package jp.mcapps.android.multi_payment_terminal.model;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.data.Amount;
import jp.mcapps.android.multi_payment_terminal.data.DeviceServiceInfo;
import jp.mcapps.android.multi_payment_terminal.data.FirmWareInfo;
import jp.mcapps.android.multi_payment_terminal.data.IFBoxAppModels;
import jp.mcapps.android.multi_payment_terminal.database.Converters;
import jp.mcapps.android.multi_payment_terminal.error.ErrorManage;
import jp.mcapps.android.multi_payment_terminal.model.device_network_manager.DeviceNetworkManager;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterConst;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterManager;
import jp.mcapps.android.multi_payment_terminal.thread.printer.PrinterProc;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.IFBoxApi;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.IFBoxApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.IFBoxWebSocketClient;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Meter;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Version;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.WebSocketPayload;
import timber.log.Timber;

public class IFBoxManager {
    //ADD-S BMT S.Oyama 2024/09/26 フタバ双方向向け改修
    private final String IFBOXMANAGER_VERSION = "2025/04/02 (1)";

    public static class SendMeterDataStatus_FutabaD {
        public static final int ERROR_TIMEOUT           = -1;   // タイムアウト
        public static final int ERROR_NOTCONNECTED      = -2;   // 未接続
        public static final int ERROR_SELECTMODE        = -3;   // 選択モードエラー
        public static final int ERROR_SENDNG            = -4;   // 送信NG
        public static final int ERROR_820NACK           = -5;   // 820側エラー
        public static final int ERROR_PAPERLACKING      = -6;   // 紙切れ
        public static final int ERROR_PROCESSCODE_ERROR = -8;   // 処理コード表示要求起因エラー FREE/ER**
        public static final int ERROR_PROCESSCODE_ERRRETURN = -9;   // 処理コード表示要求起因エラー FREE/ER** 異常処理コード画面戻り要求キーを必要とする場合

        public static final int NONE                    = 0;    // 未定義　未使用　初期値
        public static final int SENDING                 = 1;    // 送信中
        public static final int SENDOK                  = 2;    // 送信OK

        public static final int COLDSTART_START         = 10;   // 冷起動開始
        public static final int COLDSTART_RESET         = 11;   // 冷起動リセット
        public static final int COLDSTART_AUTHID        = 12;   // 冷起動認証ID

        public static final int ADVANCEPAY_ADVANCE      = 20;   // 立替定額・立替
        public static final int ADVANCEPAY_FLATRATE     = 21;   // 立替定額・定額
        public static final int ADVANCEPAY_CLEAR        = 22;   // 立替定額取消

        public static final int SETTLEMENTSELECT_CASH     = 30;   // 決済選択・現金
        public static final int SETTLEMENTSELECT_CREDIT   = 31;   // 決済選択・クレジット
        public static final int SETTLEMENTSELECT_EDY      = 32;   // 決済選択・電子マネー：Edy
        public static final int SETTLEMENTSELECT_ID       = 33;   // 決済選択・電子マネー：iD
        public static final int SETTLEMENTSELECT_NANACO   = 34;   // 決済選択・電子マネー：nanaco
        public static final int SETTLEMENTSELECT_OKICA    = 35;   // 決済選択・電子マネー：okica
        public static final int SETTLEMENTSELECT_QUICPAY  = 36;   // 決済選択・電子マネー：QUICPay
        public static final int SETTLEMENTSELECT_SUICA    = 37;   // 決済選択・電子マネー：SUICA
        public static final int SETTLEMENTSELECT_WAON     = 38;   // 決済選択・電子マネー：WAON
        public static final int SETTLEMENTSELECT_QR       = 39;   // 決済選択・QR


        public static final int SEPARATION_TIKECT               = 50;   // 分別：チケット
        public static final int SEPARATION_TIKECT_FIX           = 51;   // 分別：チケットの送信完了後のセットキー送信処理用
        public static final int SEPARATION_TIKECT_CANCEL        = 52;   // 分別：チケットのキャンセル

        public static final int SEPARATION_CREDITCASH_FIRST     = 60;   // 分別：クレジットカード・現金(初期電文送出)
        public static final int SEPARATION_SUICACASH_FIRST      = 61;   // 分別：スイカ・現金(初期電文送出)
        public static final int SEPARATION_QRCASH_FIRST         = 62;   // 分別：QR・現金(初期電文送出)

        public static final int SEPARATION_CREDITCASH_SECCOND   = 70;   // 分別：クレジットカード・現金(本体電文送出)
        public static final int SEPARATION_SUICACASH_SECCOND    = 71;   // 分別：スイカ・現金(本体電文送出)
        public static final int SEPARATION_QRCASH_SECCOND       = 72;   // 分別：QR・現金(本体電文送出)

        public static final int BALANCEINQUIRY_EDY              = 90;   // 残高照会：Edy
        public static final int BALANCEINQUIRY_NANACO           = 92;   // 残高照会：nanaco
        public static final int BALANCEINQUIRY_SUICA            = 95;   // 残高照会：SUICA
        public static final int BALANCEINQUIRY_WAON             = 96;   // 残高照会：WAON

        public static final int DISCOUNTTYPE_CONFIRMATION       = 100;  // 割引額カード使用での取得モード
        public static final int DISCOUNTTYPE_JOB1               = 101;  // 割引種１（フタバD時はゆうゆう）
        public static final int DISCOUNTTYPE_JOB2               = 102;  // 割引種２（フタバD時はすこやか）
        public static final int DISCOUNTTYPE_JOB3               = 103;  // 割引種３
        public static final int DISCOUNTTYPE_JOB4               = 104;  // 割引種４
        public static final int DISCOUNTTYPE_JOB5               = 105;  // 割引種５

        public static final int RECEIPT_PRINT                   = 110;  // 領収書発行　　　分別チケット後現金時も本フェーズを使用
        public static final int TICKET_PRINT                    = 111;  // チケット伝票発行
        public static final int WAON_HISTORY                    = 112;  // WAON取引履歴(開始要求)
        public static final int WAON_HISTORYJOBCORE             = 113;  // WAON取引履歴(履歴通知本体)
        public static final int REPRINT_PRINT                   = 114;  // 伝票再印刷
        public static final int AGGREGATE_PRINT                 = 115;  // 集計印刷

        public static final int DISCOUNTTYPE_NOTIFY_CONFIRMATION       = 120;  // 割引額カード使用での取得モード(通知)
        public static final int DISCOUNTTYPE_NOTIFY_JOB1               = 121;  // 割引種１（フタバD時はゆうゆう）(通知)
        public static final int DISCOUNTTYPE_NOTIFY_JOB2               = 122;  // 割引種２（フタバD時はすこやか）(通知)
        public static final int DISCOUNTTYPE_NOTIFY_JOB3               = 123;  // 割引種３(通知)
        public static final int DISCOUNTTYPE_NOTIFY_JOB4               = 124;  // 割引種４(通知)
        public static final int DISCOUNTTYPE_NOTIFY_JOB5               = 125;  // 割引種５(通知)

        public static final int PREPAID_PAY                             = 200;  // プリペイド支払い                     <-- trans_type : 0
        public static final int PREPAID_PAYREFUND                       = 201;  // プリペイド支払い取り消し              <-- trans_type : 1
        public static final int PREPAID_POINTADD                        = 202;  // プリペイドポイント付与                <-- trans_type : 2
        public static final int PREPAID_POINTREFUND                     = 203;  // プリペイドポイント付与取り消し          <-- trans_type : 3
        public static final int PREPAID_CACHECHARGE                     = 204;  // プリペイド現金チャージ                 <-- trans_type : 4
        public static final int PREPAID_CHCHECHARGEREFUND               = 205;  // プリペイド現金チャージ取り消し          <-- trans_type : 5
        public static final int PREPAID_CARDBUY                         = 206;  // プリペイドカード発売                   <-- trans_type : 7
        public static final int PREPAID_POINTCHARGE                     = 207;  // プリペイドポイントチャージ              <-- trans_type : 6
        public static final int PREPAID_BALANCE                         = 208;  // プリペイドカード照会                   <-- trans_typeに定義なし
        public static final int PREPAID_EOC                             = 219;  // プリペイド系のコード終端(End of Code)

        public static final int SETTLEMENTSELECT_PREPAID_PAY                 = 230;  // プリペイド支払い                     <-- trans_type : 0
        public static final int SETTLEMENTSELECT_PREPAID_PAYREFUND           = 231;  // プリペイド支払い取り消し              <-- trans_type : 1
        public static final int SETTLEMENTSELECT_PREPAID_POINTADD            = 232;  // プリペイドポイント付与                <-- trans_type : 2
        public static final int SETTLEMENTSELECT_PREPAID_POINTREFUND         = 233;  // プリペイドポイント付与取り消し          <-- trans_type : 3
        public static final int SETTLEMENTSELECT_PREPAID_CACHECHARGE         = 234;  // プリペイド現金チャージ                 <-- trans_type : 4
        public static final int SETTLEMENTSELECT_PREPAID_CHCHECHARGEREFUND   = 235;  // プリペイド現金チャージ取り消し          <-- trans_type : 5
        public static final int SETTLEMENTSELECT_PREPAID_CARDBUY             = 236;  // プリペイドカード発売                   <-- trans_type : 7
        public static final int SETTLEMENTSELECT_PREPAID_POINTCHARGE         = 237;  // プリペイドポイントチャージ              <-- trans_type : 6

        public static final int METER_RECOVERY_CODE999                   = 290;  // メータ復旧動作（コード999送付）
        public static final int METER_RECOVERY_ERRORCLEAR                = 291;  // メータ復旧動作（エラー解除）


        public static final int AUTODAILYREPORT_IN                      = 300;  // 自動日報（メニューin）
        public static final int AUTODAILYREPORT_OUT                     = 301;  // 自動日報（メニューout）

        public static final int AUTODAILYREPORT_FUEL_IN                 = 310;  // 自動日報（燃料in）
        public static final int AUTODAILYREPORT_FUEL_OUT                = 311;  // 自動日報（燃料out [入力解除]）
        public static final int AUTODAILYREPORT_FUEL_INPUT              = 312;  // 自動日報（燃料入力終了）
        public static final int AUTODAILYREPORT_FUEL_CLEAR              = 313;  // 自動日報（燃料クリア）

        public static final int AUTODAILYREPORT_EOC                     = 399;  // 自動日報系のコード終端(End of Code)

        public static final int GENERICABORTCODE_NONACK                 = 950;  // 汎用の中止コード(ACKなし)
        public static final int GENERICABORTCODE_ACK                    = 951;  // 汎用の中止コード(ACKあり)
        public static final int GENERICTEISEIKEY_NONACK                 = 952;  // 汎用の訂正キー送付処理(ACKなし)
        public static final int GENERICTEISEIKEY_ACK                    = 953;  // 汎用の訂正キー送付処理(ACKあり)
        public static final int GENERICPROCESSCODEERR_RETURN            = 954;  // 異常処理コード画面戻り

        public static final int ACK_NYUUKO                              = 1100;  // 入庫の返信ACK　750->820
        public static final int ACK_SYUKKO                              = 1101;  // 出庫の返信ACK　750->820
        public static final int ACK_CARNO                               = 1102;  // 車番通知の返信ACK　750->820


        public static final int ACKERR_STATUS_SETTLEMENTABORT_CREDIT    = 1501;     // 決済画面での中止要求クレジット
        public static final int ACKERR_STATUS_SETTLEMENTABORT_EMONEY    = 1502;     // 決済画面での中止要求EMoney
        public static final int ACKERR_STATUS_SETTLEMENTABORT_QR        = 1503;     // 決済画面での中止要求QR
    }

    //ADD-S BMT S.Oyama 2025/01/28 フタバ双方向向け改修
    public static class Send820Status_Error_FutabaD {                   //820エラー時のステータス
        public static final int ERROR_STATUS820_NONE                    =   0;
        public static final int ERROR_STATUS820_RESETREQ                =   1;      // 820が起動信号を送信
        public static final int ERROR_STATUS820_SENDNONE                = 100;      // データ送信不可（アイドル以外の状態）
        public static final int ERROR_STATUS820_SENDSTOP                = 101;      // データ送信中止（上位レベルで再送信）
        public static final int ERROR_STATUS820_DATABROKEN              = 200;      // 受信データ異常
        public static final int ERROR_STATUS820_MATER_CONNECTERR        = 900;      // メーターとの通信不可（メーターとの初回通信失敗状態）
        public static final int ERROR_STATUS820_MATER_CONNECTTIMEOUT    = 901;      // メーターとの通信タイムアウト（要求に対して無応答またはNAK応答）
        public static final int ERROR_STATUS820_MATER_RESETCOUNT        = 902;      // リセット要求待ち3秒のリトライオーバー
        public static final int ERROR_STATUS820_PAPERLACKING            = 903;      // キーコード送付～ファンクション実行要求待ち時に用紙切れとして音声ガイダンス９を送付してきた時に７５０へ通知するエラーコード
    }
    //ADD-E BMT S.Oyama 2024/05/01/28 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2025/02/10 フタバ双方向向け改修
    public static class Send820Status_JobReq_FutabaD {                   //820meter_sub_cmd:5 処理コード要求の詳細コード
        public static final int JOBREQ_NONE                             =   0;
        public static final int JOBREQ_SD_NONSET                        =   -3;     // メモリーカード未挿入時の「挿入してください」時の処理
        public static final int JOBREQ_SD_SET                           =   3;      // メモリーカード未挿入時の「挿入してください」表示中止要求時
        public static final int JOBREQ_OUTOFPAPER                       =   9;      // 用紙切れの処理
        public static final int JOBREQ_PRINTEND_RECIEPTTICKET           =   1;      // 領収書　チケット伝票時の印刷完了信号
        public static final int JOBREQ_V3_PRINTSTART_RECV               = 9000;     // V3印刷処理関連命令受信(print_start, print_end)
    }
    //ADD-E BMT S.Oyama 2025/02/10 フタバ双方向向け改修
    //ADD-S BMT S.Oyama 2025/02/26 フタバ双方向向け改修
    public static class Send820Status_ProcessCode_KeyREQ {                   //820meter_sub_cmd:5 処理コード要求に乗ってくるキー要求コード
        public static final String KEYREQ_TORIKESHI     = "TORIKESHI";       // 取消キー要求
        public static final String KEYREQ_SETKEY        = "SETKEY";          // セットキー要求
        public static final String KEYREQ_TEISEI        = "TEISEI";          // 訂正キー要求
        public static final String KEYREQ_NASHI         = "NASHI";           // キーなし
    }
    //ADD-E BMT S.Oyama 2025/02/26 フタバ双方向向け改修

    public static class Send820Status_ProcessCode_FuncNAME {                 //820meter_sub_cmd:5 処理コード要求に乗ってくる機能名
        public static final String FUNCNAME_HANEAGARI   = "HANEAGARI";       // 跳ね上がり
        public static final String FUNCNAME_SYUUKEI     = "SYUUKEI";         // 集計印字
        public static final String FUNCNAME_KAMIGIRE    = "KAMIGIRE";        // 紙切れ
    }

    public static class SendMeterData_FutabaD{
        public Integer meter_sub_cmd;
        public String status;
        public String term_ver;
        public String status_code;              //応答識別コード
        public Integer car_id;
        public Integer input_kingaku;
        public Integer key_code;
        public String trans_date;
        public Integer discount_way;
        public Integer discount_type;
        public String exp_date;
        public Integer adr_l2;
        public Integer adr_l3;
        public Integer adr_input1;
        public Integer adr_input2;
        public Integer if_ver;
    }

    //送信時のエラーに関する，詳細情報を乗せるクラス
    public static class SendMeterDataInfo_FutabaD {
        public int      StatusCode;
        public boolean  IsLoopBreakOut;
        public int      ErrorCode820;
        public int      ErrorCodeExt1;
        public String   FREEMessage1;
        public String   FREEMessage2;
        public String   FREEMessage3;
    }

    //送信時のエラーに関する，詳細情報を乗せるクラス
    public static class SendMeterDataError_FutabaD {
        public int      ErrorCode;
        public String   ErrorMessage;
        public int      ErrorCode820;
    }
    //ADD-E BMT S.Oyama 2024/09/26 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2025/01/22 フタバ双方向向け改修
    //print_start以外の印刷処理モード（領収書，チケット伝票等）
    public static class ExtPrintJobMode {
        public static final int NONE                    = 0;    // 未定義　未使用　初期値
        public static final int RECEIPT_PRINT           = 34;   // 領収書
        public static final int TICKET_PRINT            = 43;   // チケット伝票
    }

    public MutableLiveData<Integer> _extPrintJobMode = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getExtPrintJobMode(){
        return _extPrintJobMode;
    }
    public void setExtPrintJobMode(Integer mode){
        _extPrintJobMode.setValue(mode);
    }
    //ADD-E BMT S.Oyama 2025/01/22 フタバ双方向向け改修


    private final MainApplication _app = MainApplication.getInstance();
    private final IFBoxApi _apiClient = new IFBoxApiImpl();
    private final IFBoxWebSocketClient _wsClient = new IFBoxWebSocketClient();
    private final DeviceNetworkManager _deviceNetworkManager;
    private final Disposer _disposer = new Disposer();
    private final Gson _gson = new GsonBuilder().disableHtmlEscaping().create();

    private boolean isStarted;
    private Timer wsRetryTimer = null;
    private boolean _connectCheckWorking = false;
    private boolean isPrintResult = false;

    private final PublishSubject<Meter.Response> _meterInfo = PublishSubject.create();
    public final PublishSubject<Meter.Response> getMeterInfo() {
        return _meterInfo;
    }
    private final PublishSubject<Meter.ResponseStatus> _meterStatNotice = PublishSubject.create();
    public final PublishSubject<Meter.ResponseStatus> getMeterStatNotice() {
        return _meterStatNotice;
    }
    public static Disposable meterStatNoticeDisposable = null;

    private final PublishSubject<Boolean> _exitManualMode = PublishSubject.create();
    public final PublishSubject<Boolean> getExitManualMode() {
        return _exitManualMode;
    }
    public static Disposable exitManualModeDisposable = null;

    // フタバの手動決済モード時に印刷終了確認
    private final PublishSubject<Boolean> _printEndManual = PublishSubject.create();
    public final PublishSubject<Boolean> getPrintEndManual() {
        return _printEndManual;
    }
    public static Disposable printEndManualDisposable = null;

    //ADD-S BMT S.Oyama 2024/09/03 フタバ双方向向け改修
    private final PublishSubject<Meter.ResponseFutabaD> _meterDataV4 = PublishSubject.create();
    public final PublishSubject<Meter.ResponseFutabaD> getMeterDataV4() {
        Timber.i("[FUTABA-D]IFBoxManager getMeterDataV4() *******");
        return _meterDataV4;
    }
    public static Disposable meterDataV4Disposable_MenuHome = null;
    public static Disposable meterDataV4ErrorDisposable_MenuHome = null;
    public static Disposable meterDataV4Disposable_DailyReportTop = null;
    public static Disposable meterDataV4Disposable_DailyReportJob = null;
    public static Disposable meterDataV4InfoDisposable_DailyReportFuel = null;
    //ADD-S BMT S.Oyama 2025/03/14 フタバ双方向向け改修
    public static Disposable meterDataV4Disposable_HistoryMenu = null;
    //ADD-E BMT S.Oyama 2025/03/14 フタバ双方向向け改修
    private final PublishSubject<SendMeterDataError_FutabaD> _meterDataV4Error = PublishSubject.create();
    public final PublishSubject<SendMeterDataError_FutabaD> getMeterDataV4Error() {
        return _meterDataV4Error;
    }

    public static Disposable meterDataV4Disposable_ScanEmoneyQR = null;

    private boolean _isConnected820 = false;                              //起動時初期状態，あるいは820通信が切断された場合はfalse 820通信が確立された場合はtrue
    public boolean getIsConnected820() {return _isConnected820;}
    private SendMeterData_FutabaD _sendMeterData_FutabaD = new SendMeterData_FutabaD();     //メータデータクラス /printdata/v3::meter_data
    public int _sendMeterDataStatus_ColdStart = SendMeterDataStatus_FutabaD.NONE;           //メータデータ送信ステータスコールドスタート用
    public int _sendMeterDataPhase_ColdStart = SendMeterDataStatus_FutabaD.NONE;            //メータデータ送信フェーズコールドスタート用
    public int _sendMeterDataStatus_General = SendMeterDataStatus_FutabaD.NONE;           //メータデータ送信ステータス一般用
    public int _sendMeterDataPhase_General  = SendMeterDataStatus_FutabaD.NONE;            //メータデータ送信フェーズ一般用
    private Timer _wsRetryTimerFutabaD = null;

    public void setSendMeterDataStatus_General(int status) {
        _sendMeterDataStatus_General = status;
    }
    public boolean isSendMeterDataStatus_General_Sending() {                          //ACKが必要な750->820送信処理中はTRUE
        return _sendMeterDataStatus_General == SendMeterDataStatus_FutabaD.SENDING;
    }

    public void setSendMeterDataPhase_General(int phase) {
        _sendMeterDataPhase_General = phase;
        //ADD-S BMT S.Oyama 2025/03/11 フタバ双方向向け改修
        if (phase == SendMeterDataStatus_FutabaD.WAON_HISTORYJOBCORE) {
            _isPaperlackingWithSetkey = true;
        }
        //ADD-E BMT S.Oyama 2025/03/11 フタバ双方向向け改修
    }

    private boolean _isCarNoSendREQ = false;                  //双方向時，車番送信要求フラグ

    //ADD-E BMT S.Oyama 2024/09/03 フタバ双方向向け改修
    //ADD-S BMT S.Oyama 2024/12/16 フタバ双方向向け改修
    private boolean _is820FirmUpdatingFL  = false;           //820のファームウエア更新中の場合はTRUE
    //ADD-E BMT S.Oyama 2024/12/16 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2025/02/26 フタバ双方向向け改修
    private HashMap<String, Integer> _futabaDMeterProcessCodeDic = new HashMap<>();  //処理コード表示要求時　メータがセットしてくる処理コード
    //ADD-E BMT S.Oyama 2025/02/26 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2025/03/11 フタバ双方向向け改修
    private boolean _isPaperlackingWithSetkey = false;             //紙切れが発生した　かつ セットキーを返却する必要がある場合だけTRUE
    public boolean getIsPaperlackingWithSetkey() {return _isPaperlackingWithSetkey;}
    public void setIsPaperlackingWithSetkey(boolean isPaperlackingWithSetkey) {
        _isPaperlackingWithSetkey = isPaperlackingWithSetkey;
    }
    //ADD-E BMT S.Oyama 2025/03/11 フタバ双方向向け改修

    private  String _meterStatus = "KUUSYA" ;
    public String getMeterStatus() {return _meterStatus;}
    private String _meterVersion = null;
    public String getMeterVersion() {return _meterVersion;}

    public IFBoxManager(DeviceNetworkManager deviceNetworkManager) {
        _deviceNetworkManager = deviceNetworkManager;
        initialize();
    }
    private Timer wsAliveTimer = null;

    @SuppressLint("CheckResult")
    @RequiresApi(api = Build.VERSION_CODES.N)
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void start() {
        Timber.i("[FUTABA-D]IFBoxManager Started: My File Version is %s", IFBOXMANAGER_VERSION);

        if (isStarted) return;
        else isStarted = true;

        // キッティングが完了してなかったら何もしない
        // キッティング完了後にrestartをコール
        if (AppPreference.isIFBoxHost() && !AppPreference.isIFBoxSetupFinished()) return;

        _wsClient.getReceiveText()
                .doOnSubscribe(_disposer::addTo)
                .subscribe(payloadText -> {
                    try {
                        isPrintResult = false;
                        WebSocketPayload payload = _gson.fromJson(payloadText, WebSocketPayload.class);
                        final String json = _gson.toJson(payload.data);
                        Timber.tag("IM-A820").i("%s, serial:%s", json, AppPreference.getIFBoxVersionInfo().mcSerial);

                        if (json != null && json.contains("ACC-OFF")) {
                            _wsClient.setAccOff_IMA820();
                        } else if (json != null && json.contains("ACC-ON")) {
                            _wsClient.setAccOn_IMA820();
                        }

                        if (payload.type.equals("/meter/v1")) {
                            Meter.Response meter = (Meter.Response) _gson.fromJson(json, Meter.Response.class);
                            updateMeter(meter);
                        } else if (payload.type.equals("/meter/v2")) {
                            Meter.Response meter = (Meter.Response) _gson.fromJson(json, Meter.Response.class);
                            updateMeter(meter);
                        } else if (payload.type.equals("/meter/v3")) {
                            if (payload.cmd.equals("fare")) {
                                Meter.Response meter = (Meter.Response) _gson.fromJson(json, Meter.Response.class);
                                updateMeter(meter);
                            } else if (payload.cmd.equals("status")) {
                                Meter.ResponseStatus meter = (Meter.ResponseStatus) _gson.fromJson(json, Meter.ResponseStatus.class);
                                if (meter.info.equals("CONN_OK")) {_meterVersion = String.valueOf(meter.version);}
                                if (meter.info.equals("NYUUKO")) {meterStatNotice(meter);}
                            // 現段階では、print_req コマンドは未使用
                            //} else if (payload.cmd.equals("print_req")) {
                            //    PrinterManager.getInstance().req_trans_info();
                            }
                        //ADD-S BMT S.Oyama 2024/09/03 フタバ双方向向け改修
                        } else if (payload.type.equals("/meter/v4")) {
                            Timber.i("[FUTABA-D]  ******* Recv : /meter/v4  Json:%s ", payloadText);

                            if (payload.cmd.equals("fare"))
                            {
                                Meter.Response meter = (Meter.Response) _gson.fromJson(json, Meter.Response.class);
                                updateMeter(meter);
                            }
                            else if (payload.cmd.equals("status"))
                            {
                                Meter.ResponseStatusFutabaD meter = (Meter.ResponseStatusFutabaD) _gson.fromJson(json, Meter.ResponseStatusFutabaD.class);
                                if (meter.info.equals("CONN_OK")) {_meterVersion = String.valueOf(meter.version);}
                                //if (meter.info.equals("NYUUKO")) {meterStatNotice(meter);}            //入庫通知はMeter.ResponseFutabaD側にて行う
                                updateMeterStatusFutabaD(meter);
                            }
                            //} else if (payload.cmd.equals("print_req")) {
                            //    PrinterManager.getInstance().req_trans_info();
                            //}
                            else
                            {
                                Meter.ResponseFutabaD meterFutabaD = (Meter.ResponseFutabaD) _gson.fromJson(json, Meter.ResponseFutabaD.class);
                                meterFutabaD.separate_flg = 0;
                                updateMeter(meterFutabaD);
                            }
                        //ADD-E BMT S.Oyama 2024/09/03 フタバ双方向向け改修
                        } else if (payload.type.equals("/printdata/v1")) {
                            Timber.i("[FUTABA-D]  ******* Recv : /printdata/v1  Json:%s ", payloadText);

                            if (payload.cmd.equals("print_end")) {
                                isPrintResult = true;
                                // タイマの停止
                                if (null != wsRetryTimer) {
                                    wsRetryTimer.cancel();
                                    wsRetryTimer = null;
                                }

                                //ADD-S BMT S.Oyama 2024/11/18 フタバ双方向向け改修
                                killRetryTimerFutabaD();            //V4系タイムアウトタイマを殺す
                                //ADD-E BMT S.Oyama 2024/11/18 フタバ双方向向け改修

                                Meter.ResponseYazaki printer = (Meter.ResponseYazaki) _gson.fromJson(json, Meter.ResponseYazaki.class);
                                updatePrintStatus(printer);
                            }
                        //ADD-S BMT S.Oyama 2024/11/01 フタバ双方向向け改修
//                        } else if (payload.type.equals("/printdata/v2")) {
//                            if (payload.cmd.equals("print_end")) {
//                                isPrintResult = true;
//                                // タイマの停止
//                                if (null != wsRetryTimer) {
//                                    wsRetryTimer.cancel();
//                                    wsRetryTimer = null;
//                                }
//
//                                Meter.ResponseFutabaDPrintEnd printer = (Meter.ResponseFutabaDPrintEnd) _gson.fromJson(json, Meter.ResponseFutabaDPrintEnd.class);
//                                updatePrintStatusFutabaDV2(printer);
//                            }
                        //ADD-E BMT S.Oyama 2024/11/01 フタバ双方向向け改修
                        } else if (payload.type.equals("/alive")) {
                            // バージョン表示用にAppPreferenceに情報をセット
                            Meter.AliveData data = (Meter.AliveData) _gson.fromJson(json, Meter.AliveData.class);
                            if (data.name != null && data.name.equals("IM-A820")) {
                                Timber.i("IM-A820 Alive");
/*
                                Version.Response res = AppPreference.getIFBoxVersionInfo();
                                res.appModel = data.model;
                                res.appName = data.name;
                                res.appVersion = data.version;
                                AppPreference.setIFBoxVersionInfo(res);
*/
                                //ADD-S BMT S.Oyama 2024/09/26 フタバ双方向向け改修
                                if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D)) {          //フタバ双方向時 コールドスタートを実施（起動時初回１回　or 接続が切れるたび）
                                    send820_ColdStart_Start();
                                }
                                //ADD-E BMT S.Oyama 2024/09/26 フタバ双方向向け改修

                                // タイマのリスタート
                                if (null != wsAliveTimer) {
                                    wsAliveTimer.cancel();
                                    wsAliveTimer = null;
                                }
                                wsAliveTimer = new Timer();
                                wsAliveTimer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        Timber.e("Ws Alive受信 タイムアウト");
                                        _wsClient.reconnect();
                                    }
                                }, _wsClient.getAliveInterval() * 5L, _wsClient.getAliveInterval() * 5L);
//                            Timber.d("Ws Alive受信 リスタート");
                            }
                        } else if (payload.type.equals("/log")) {
//                            Timber.i("[FUTABA-D] /log");
                        }
                    } catch (Exception e) {
                        if (isPrintResult) printException();
                        Timber.e(e);
                    }
                }, Timber::d);

        _wsClient.getConnectionStatus()
                .doOnSubscribe(_disposer::addTo)
                .subscribe(status -> {
                    if (status == IFBoxWebSocketClient.ConnectionStatus.Reconnecting) {
                        checkConnection();
                    }
                });

        Timber.d("start IFBoxManager");

        if(null == wsAliveTimer) {
            wsAliveTimer = new Timer();
        }
        wsAliveTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Timber.e("Ws Alive受信 タイムアウト");
                _wsClient.reconnect();
            }
        },  _wsClient.getAliveInterval() * 5L,_wsClient.getAliveInterval() * 5L);

        _deviceNetworkManager.getDeviceServiceInfo()
                .doOnSubscribe(_disposer::addTo)
                .subscribeOn(Schedulers.newThread())
                .subscribe(info -> {
                    if (info.isAvailable()) {
                        //Timber.d("change service info");
                        Timber.i("change service info addr:%s", info.getAddress());

                        _apiClient.setBaseUrl("http://" + info.getAddress());
                        checkConnection();
                    }
                });
    }

    public void stop() {

        //ADD-S BMT S.Oyama 2024/09/26 フタバ双方向向け改修
        if (_wsRetryTimerFutabaD != null) {
            _wsRetryTimerFutabaD.cancel();
            _wsRetryTimerFutabaD = null;
        }
        //ADD-E BMT S.Oyama 2024/09/26 フタバ双方向向け改修

        _wsClient.disconnect();
        _wsClient.setConnectionStatus(IFBoxWebSocketClient.ConnectionStatus.Disconnected);
        _disposer.dispose();
        initialize();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void restart() {
        Timber.i("restart IFBoxManager");
        stop();
        start();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void send(String sendText,int timeout) {

        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {
            Timber.i("[FUTABA-D]send() (V2, V3): timeout:%d mes:%s", timeout, sendText);
        }

        if (isConnected() && _wsClient.broadcastTXT(sendText)) {
            Timber.i("WebSocket送信 -> IM-A820");
            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {
                Timber.i("[FUTABA-D]WebSocket送信 -> IM-A820(V2, V3): ");
            }

            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    this.cancel();
                    PrinterProc printerProc = PrinterProc.getInstance();
                    //ADD-S BMT S.Oyama 2024/10/25 フタバ双方向向け改修
                    if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == false)
                    {
                        printerProc.Printing_Duplex("NG",0,PrinterConst.DuplexPrintStatus_IFBOXERROR_TIMEOUT);
                    }
                    else
                    {
                        printerProc.Printing_DuplexFutabaD("10",0,PrinterConst.DuplexPrintStatus_IFBOXERROR_TIMEOUT);
                    }
                    Timber.e("WebSocket送信 タイムアウト : %s", sendText);
                    if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {
                        Timber.i("[FUTABA-D]WebSocket送信 タイムアウト : (V2, V3):%s ", sendText);
                    }
                }
            };

            if(null == wsRetryTimer){
                wsRetryTimer = new Timer() ;
            }

            wsRetryTimer.schedule(task, timeout);
        }else{
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    PrinterProc printerProc = PrinterProc.getInstance();

                    //ADD-S BMT S.Oyama 2024/10/25 フタバ双方向向け改修
                    if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == false)
                    {
                        printerProc.Printing_Duplex("NG",0,PrinterConst.DuplexPrintStatus_IFBOXERROR);
                    }
                    else
                    {
                        printerProc.Printing_DuplexFutabaD("10",0,PrinterConst.DuplexPrintStatus_IFBOXERROR);
                    }
                    //ADD-E BMT S.Oyama 2024/10/25 フタバ双方向向け改修

                    Timber.e("WebSocket未接続 : %s", sendText);
                    if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == true) {
                        Timber.i("[FUTABA-D]WebSocket未接続 : %s(V2, V3): ", sendText);
                    }
                }
            };

            Timer timer = new Timer();
            timer.schedule(timerTask, timeout);
        }
    }

    public Completable fetchMeter() {
        return Completable.create(emitter -> {
            if (!isConnected()) {
                emitter.onComplete();
                return;
            }

            try {
                if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D)) {
                    // フタバ双方向
                    final Meter.ResponseStatusFutabaD meter = _apiClient.getMeterFutabaD();
                    updateMeterStatusFutabaD(meter);
                    if (!emitter.isDisposed()) {
                        emitter.onComplete();
                    }
                } else {
                    final Meter.Response meter = _apiClient.getMeter();
                    updateMeter(meter);
                    if (!emitter.isDisposed()) {
                        emitter.onComplete();
                    }
                }
            } catch (Exception e) {
                if (!emitter.isDisposed()) {
                    emitter.onError(e);
                }
            }
        });
    }

    public Completable firmwareUpdate(FirmWareInfo firmWareInfo) {
        Timber.d("IM-A820 ファームウェアアップデート");

        return Completable.create(emitter -> {
            while (!isConnected()) {
                if (emitter.isDisposed()) return;
                sleep(1000);
            }

            try {
                // ファイルパスはUpdaterから取得できるようにしたい
                final File localDir = _app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                final String targetFilename = localDir.toString() + File.separator + firmWareInfo.modelName;


                _apiClient.postUpdate(targetFilename);
                emitter.onComplete();
            } catch (Exception e) {
                Timber.d("IM-A820 アップデート失敗");
                Timber.e(e);
                emitter.onError(e);
            }
        });
   };

    private void initialize() {
        isStarted = false;
        _connectCheckWorking = false;

        //ADD-S BMT S.Oyama 2024/09/26 フタバ双方向向け改修
        Timber.i("[FUTABA-D]initialize()");
        _isConnected820 = false;                                                     //820接続状態を未接続にする
        _sendMeterDataStatus_ColdStart = SendMeterDataStatus_FutabaD.NONE;           //メータデータ送信ステータスコールドスタート用
        _sendMeterDataPhase_ColdStart = SendMeterDataStatus_FutabaD.NONE;            //メータデータ送信フェーズコールドスタート用
        _sendMeterDataStatus_General = SendMeterDataStatus_FutabaD.NONE;             //メータデータ送信ステータス一般用
        _sendMeterDataPhase_General  = SendMeterDataStatus_FutabaD.NONE;             //メータデータ送信フェーズ一般用
        //ADD-E BMT S.Oyama 2024/09/26 フタバ双方向向け改修

        //ADD-S BMT S.Oyama 2025/02/26 フタバ双方向向け改修
        _futabaDMeterProcessCodeDic.put("ER01", -1);        //処理コード表示要求に載せられえてくる処理コード辞書を作成
        _futabaDMeterProcessCodeDic.put("ER02", -2);
        _futabaDMeterProcessCodeDic.put("ER03", -3);
        _futabaDMeterProcessCodeDic.put("ER04", -4);
        _futabaDMeterProcessCodeDic.put("ER05", -5);
        _futabaDMeterProcessCodeDic.put("ER06", -6);
        _futabaDMeterProcessCodeDic.put("ER07", -7);
        _futabaDMeterProcessCodeDic.put("ER08", -8);
        _futabaDMeterProcessCodeDic.put("ER09", -9);
        _futabaDMeterProcessCodeDic.put("ER10", -10);
        _futabaDMeterProcessCodeDic.put("FREE", -20);
        //ADD-E BMT S.Oyama 2025/02/26 フタバ双方向向け改修

    }


    private void updateMeter(Meter.Response meter) {
        Integer fare = meter.fare;
        Integer split = meter.fare_split;
        Integer farePaid = meter.fare_paid; // 20231127 支払済対応　t.wada

        //OKBの場合、先にメーター金額を入れる
        if (IFBoxAppModels.isMatch(IFBoxAppModels.OKABE_MS70_D)) {
            // 決済対象の金額は「運賃」＋「ETC」＋「料金」－「割引」－「チケット」
            fare = meter.fare + meter.fare_etc + meter.fare_other;

            if (fare >= meter.fare_discount) {
                fare -= meter.fare_discount;
                // チケット金額は getTotalAmount() で差し引く
            } else {
                Timber.e("割引額異常 運賃：%d, ETC：%d, 料金：%d, 割引：%d, 支払済：%d",
                        meter.fare, meter.fare_etc, meter.fare_other, meter.fare_discount, meter.fare_paid);
            }

            //メーター支払以外ならfareを0に修正
            // 2023.10.31 t.wada タリフの判定は、820から来る料金通知 status_serial に変更
            if (!meter.status_serial.equals("SIHARAI")){
                fare = 0;
                split = 0;
                farePaid = 0;
            }
        }

        if(null != split) {
            if(Amount.getMeterCharge() != fare  || Amount.getTicketAmount() != split) {
                Amount.setCashAmount(0); // 現金分割 メータ金額が0になった（＝支払→空車）orチケット金額が変更された場合はクリアする
            }
            Amount.setTicketAmount(split);   // チケット金額
        }
        if(null != meter.eigyo_num) {
            Amount.setEigyoCount(meter.eigyo_num);      // 営業回数
        }

        // 20231127 支払済対応　t.wada
        if(null != farePaid) {
            if(Amount.getMeterCharge() != fare  || Amount.getPaidAmount() != farePaid) {
                Amount.setCashAmount(0); // 現金分割 メータ金額が0になった（＝支払→空車）orチケット金額が変更された場合はクリアする
            }
            Amount.setPaidAmount(farePaid);   // 支払済金額
        }

        //ADD-S BMT S.Oyama 2024/10/22 フタバ双方向向け改修
        Timber.i("[FUTABA-D]updateMeter(V2,V3) fare:%d", fare);

        if (!IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D)) {
            Amount.setMeterCharge(fare);          // Rxで完結したいけど既存で使用しているので
        }
        //ADD-E BMT S.Oyama 2024/10/22 フタバ双方向向け改修

        Intent intent = new Intent("jp.mcapps.pt-prepaid.METER_LINK");
        intent.putExtra("amount", fare);
        _app.sendBroadcast(intent);
        
        _meterStatus = meter.status ;
        _meterInfo.onNext(meter);
    }

    //ADD-S BMT S.Oyama 2024/09/30 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  決済端末に対して情報通知（フタバ双方向用）
     * @note   820からの通知データを処理する /meter/v4::status
     * @param [in] Meter.ResponseStatusFutabaD meter(フタバ双方向用メーター情報 /meter/v4)
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void updateMeterStatusFutabaD(Meter.ResponseStatusFutabaD meter)
    {
        Timber.i("[FUTABA-D]updateMeterStatusFutabaD");

        if (null == meter) {
            Timber.e("[FUTABA-D]updateMeterStatusFutabaD: meter is null");
            return;
        }

        if (null != meter.meter_fare) {             // 支払金額がNULLでない場合
            int preMeterCharge = Amount.getMeterCharge();
            //Amount.setPaidAmount(meter.meter_fare); // 支払済金額
            Amount.setMeterCharge(meter.meter_fare);
            int meterCharge = Amount.getMeterCharge();
            Timber.i("[FUTABA-D]updateMeterStatusFutabaD setMeterCharge:%d", meter.meter_fare);

            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D)) {
                checkFareChange(preMeterCharge, meterCharge);
            }
        }
        else
        {
            Timber.i("[FUTABA-D]updateMeterStatusFutabaD setMeterCharge:NULL");
        }

        if (null != meter.tatekae) {            // 立替金額がNULLでない場合
            Amount.setTatekae(meter.tatekae);   // 立替金額
        }

        if(null != meter.eigyo_num) {                   // 営業回数がNULLでない場合
            Amount.setEigyoCount(meter.eigyo_num);      // 営業回数
        }

        String tmpmeterStatus = "KUUSYA";
        if (meter.meter_status != null) {
            switch (meter.meter_status) {
                case 0:             // 空車
                    tmpmeterStatus = "KUUSYA";
                    Amount.setFlatRateAmount(0);        // 空車信号時 定額金額をクリア
                    Amount.setDiscountAvailable(0);     // 空車信号時 割引実施フラグをクリア
                    //ADD-S BMT S.Oyama 2024/12/25 フタバ双方向向け改修
                    Amount.setPaymented(0);             // 支払実施済みフラグをクリア
                    Amount.setTicketAmount(0);          // チケット金額を初期化
                    Amount.setCashAmount(0);            // 現金分割分を初期化
                    //ADD-E BMT S.Oyama 2024/12/25 フタバ双方向向け改修
                    break;
                case 1:             // 実写
                    tmpmeterStatus = "JISSYA";
                    break;
                case 2:             //支払
                    tmpmeterStatus = "SIHARAI";
                    break;
            }
        }

        Intent intent = new Intent("jp.mcapps.pt-prepaid.METER_LINK");
        intent.putExtra("amount", Amount.getTotalAmount());
        _app.sendBroadcast(intent);

        _meterStatus = tmpmeterStatus ;

        //メーター情報を更新(v1～v3向け)
        Meter.Response tmpMeterV3 = new Meter.Response();
        tmpMeterV3.status = tmpmeterStatus;
        tmpMeterV3.fare = meter.meter_fare;
        tmpMeterV3.eigyo_num = meter.eigyo_num;
        _meterInfo.onNext(tmpMeterV3);

        if (_isCarNoSendREQ == true) {      //車両番号送信要求がある場合
            _isCarNoSendREQ = false;

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    send820_ACK_CarNo();            //車両番号送信要求応答
                }

            });
            thread.start();
        }
    }
    //ADD-E BMT S.Oyama 2024/09/30 フタバ双方向向け改修

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void checkFareChange(int preMeterCharge, int meterCharge) {
        //Timber.i("[FUTABA-D]*** IFBoxManager::checkFareChange preMeterCharge:%d, meterCharge: %d ***", preMeterCharge, meterCharge);
        if ((preMeterCharge != meterCharge) && (Amount.getTicketAmount() > 0 || Amount.getCashAmount() > 0)) {
            // メーター金額に変化があった場合
            if (Amount.getCashAmount() > 0) {
                // 分別現金を入力済の場合はリセットする
                Amount.setCashAmount(0);
            }
            if (Amount.getTicketAmount() > 0) {
                // 分別チケットを入力済の場合はリセットする
                Amount.setTicketAmount(0);
            }
            Amount.setTotalChangeAmount(0);
        }
    }



    //ADD-S BMT S.Oyama 2024/09/03 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  820へのWebsocket送信主処理（フタバ双方向用）
     * @note   820へのWebsocket送信主処理
     * @param [in] String sendText 送信文字列(JSON)
     * @param [in]  int timeout タイムアウト時間
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void sendFutabaDExt(String sendText,int timeout) {
        sendFutabaD(sendText, timeout);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void sendFutabaD(String sendText,int timeout) {

        Timber.i("[FUTABA-D]sendFutabaD() (V4): timeout:%d mes:%s", timeout, sendText);

        if (isConnected() && _wsClient.broadcastTXT(sendText)) {   //接続中，送信処理
            Timber.i("[FUTABA-D]WebSocket送信 -> IM-A820: Phase(Cold):%d Phase(General):%d", _sendMeterDataPhase_ColdStart, _sendMeterDataPhase_General);

            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    this.cancel();
                    _wsRetryTimerFutabaD = null;

                    Timber.e("[FUTABA-D]WebSocket送信(sendFutabaD) タイムアウト : %s Phase(Cold):%d Phase(General):%d",
                            sendText, _sendMeterDataPhase_ColdStart, _sendMeterDataPhase_General);

                    if (_sendMeterDataPhase_ColdStart != SendMeterDataStatus_FutabaD.NONE) {            //コールドスタート処理時
                        _sendMeterDataStatus_ColdStart = SendMeterDataStatus_FutabaD.ERROR_TIMEOUT;     //メータデータ送信ステータス
                        _sendMeterDataPhase_ColdStart = SendMeterDataStatus_FutabaD.NONE;               //メータデータ送信フェーズ
                        _sendMeterDataStatus_General = SendMeterDataStatus_FutabaD.NONE;                //メータデータ送信ステータス一般用
                        _sendMeterDataPhase_General  = SendMeterDataStatus_FutabaD.NONE;                //メータデータ送信フェーズ一般用
                        _isConnected820 = false;                                                        //820接続状態を未接続にする
                        Timber.i("[FUTABA-D]sendFutabaD() ColdStart Timeout");

                    }
                    else {                                                                              //一般処理時
                        _sendMeterDataStatus_General = SendMeterDataStatus_FutabaD.ERROR_TIMEOUT;       //メータデータ送信ステータス一般用
                        _sendMeterDataPhase_General  = SendMeterDataStatus_FutabaD.NONE;                //メータデータ送信フェーズ一般用
                    }

                    SendMeterDataError_FutabaD sendMeterDataError_FutabaD = new SendMeterDataError_FutabaD();       // タイムアウト時の処理(コールバック処理を追加)
                    sendMeterDataError_FutabaD.ErrorCode = SendMeterDataStatus_FutabaD.ERROR_TIMEOUT;
                    sendMeterDataError_FutabaD.ErrorMessage = "タイムアウト";
                    sendMeterDataError_FutabaD.ErrorCode820 = 0;
                    _meterDataV4Error.onNext(sendMeterDataError_FutabaD);
                }
            };

            boolean tmpTimeoutActiveFLColdStart;
            boolean tmpTimeoutActiveFLGeneral;

            switch(_sendMeterDataPhase_ColdStart)                               //コールドスタート処理時のタイムアウト使用可否
            {
                case SendMeterDataStatus_FutabaD.NONE:
                case SendMeterDataStatus_FutabaD.COLDSTART_AUTHID:              //認証ID送信時は，820応答が無いので，タイムアウト処理はなし
                    tmpTimeoutActiveFLColdStart = false;
                    break;
                default:
                    tmpTimeoutActiveFLColdStart = true;
                    break;
            }

            switch(_sendMeterDataPhase_General) {                               //一般処理時のタイムアウト使用可否
                case SendMeterDataStatus_FutabaD.NONE:
                case SendMeterDataStatus_FutabaD.ACK_NYUUKO:                    //入庫要求時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.ACK_SYUKKO:                    //出庫要求時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.ACK_CARNO:                     //車両番号送信時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.ADVANCEPAY_ADVANCE:            // 立替定額・立替送信時は，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.ADVANCEPAY_FLATRATE:           // 立替定額・定額送信時は，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_CREDIT:                   // クレジット決済中断時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_EMONEY:                   // EMoney決済中断時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_QR:                       // QR決済中断時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.DISCOUNTTYPE_CONFIRMATION:                              // 割引種別(カード使用)時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB1:                                      // 割引種別(手動１)時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB2:                                      // 割引種別(手動２)時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB3:                                      // 割引種別(手動３)時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB4:                                      // 割引種別(手動４)時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB5:                                      // 割引種別(手動５)時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.WAON_HISTORYJOBCORE:                                    // WAON履歴通知主処理時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.RECEIPT_PRINT:                                          // レシート印刷時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.TICKET_PRINT:                                           // チケット印刷時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.AUTODAILYREPORT_OUT:                                    // 自動日報メニュー出時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.AUTODAILYREPORT_FUEL_IN:                                // 自動日報燃料入力in時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.SEPARATION_TIKECT_FIX:                                  // 分別チケット処理完了時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.SEPARATION_TIKECT_CANCEL:                               // 分別チケット処理キャンセル時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.GENERICABORTCODE_NONACK:                                // 一般中断コード(非ACK)時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.GENERICTEISEIKEY_NONACK:                                // 一般訂正キー(非ACK)時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.GENERICPROCESSCODEERR_RETURN:                           // 異常処理コード画面戻り時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                case SendMeterDataStatus_FutabaD.METER_RECOVERY_CODE999:                                 // メータ復旧コード999時の返送ACKは，820応答が無いので，タイムアウト処理はなし
                //case SendMeterDataStatus_FutabaD.REPRINT_PRINT:                 //伝票再印刷時は, 820応答が無いので, タイムアウト処理はなし
                    tmpTimeoutActiveFLGeneral = false;
                    break;
                default:
                    tmpTimeoutActiveFLGeneral = true;
                    break;
            }

            if (tmpTimeoutActiveFLColdStart == true || tmpTimeoutActiveFLGeneral == true) {      //タイムアウト処理が必要な場合 タイマ起動
                if (null == _wsRetryTimerFutabaD) {
                    _wsRetryTimerFutabaD = new Timer();
                }

                _wsRetryTimerFutabaD.schedule(task, timeout);
            }
            else                                                                                //タイムアウト処理が不要な場合
            {
                if (_sendMeterDataPhase_ColdStart != SendMeterDataStatus_FutabaD.NONE) {            //コールドスタート処理時
                    _sendMeterDataStatus_ColdStart = SendMeterDataStatus_FutabaD.NONE;              //メータデータ送信ステータス
                    _sendMeterDataPhase_ColdStart = SendMeterDataStatus_FutabaD.NONE;               //メータデータ送信フェーズ
                    _sendMeterDataStatus_General = SendMeterDataStatus_FutabaD.NONE;                //メータデータ送信ステータス一般用
                    _sendMeterDataPhase_General  = SendMeterDataStatus_FutabaD.NONE;                //メータデータ送信フェーズ一般用
                }
                else {                                                                              //一般処理時
                    _sendMeterDataStatus_General = SendMeterDataStatus_FutabaD.NONE;                //メータデータ送信ステータス一般用
                    _sendMeterDataPhase_General  = SendMeterDataStatus_FutabaD.NONE;                //メータデータ送信フェーズ一般用
                }
            }
        }else{                                                          //接続中でない，送信処理失敗
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    Timber.e("WebSocket未接続(sendFutabaD) : %s Phase(Cold):%d Phase(General):%d", sendText,
                            _sendMeterDataPhase_ColdStart, _sendMeterDataPhase_General);
                    _sendMeterDataStatus_ColdStart = SendMeterDataStatus_FutabaD.ERROR_NOTCONNECTED;           //メータデータ送信ステータス
                    _sendMeterDataPhase_ColdStart = SendMeterDataStatus_FutabaD.NONE;                           //メータデータ送信フェーズ
                    _sendMeterDataStatus_General = SendMeterDataStatus_FutabaD.ERROR_NOTCONNECTED;       //メータデータ送信ステータス一般用
                    _sendMeterDataPhase_General  = SendMeterDataStatus_FutabaD.NONE;                //メータデータ送信フェーズ一般用
                    _isConnected820 = false;                                                     //820接続状態を未接続にする
                    Timber.i("[FUTABA-D]sendFutabaD() Disconnect 820.");

                    SendMeterDataError_FutabaD sendMeterDataError_FutabaD = new SendMeterDataError_FutabaD();       // 未接続時の処理(コールバック処理を追加)
                    sendMeterDataError_FutabaD.ErrorCode = SendMeterDataStatus_FutabaD.ERROR_NOTCONNECTED;
                    sendMeterDataError_FutabaD.ErrorMessage = "820切断";
                    sendMeterDataError_FutabaD.ErrorCode820 = 0;
                    _meterDataV4Error.onNext(sendMeterDataError_FutabaD);
                }
            };

            Timer timer = new Timer();
            timer.schedule(timerTask, timeout);
        }
    }

    /******************************************************************************/
    /*!
     * @brief  決済端末に対して情報通知（フタバ双方向用）
     * @note   820からの通知データを処理する /meter/v4::meter_data
     * @param [in] Meter.ResponseFutabaD meter(フタバ双方向用メーター情報 /meter/v4)
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void updateMeter(Meter.ResponseFutabaD meter)
    {
        Timber.i("[FUTABA-D]updateMeter(ResponseFutabaD) Phase(Cold):%d Phase(General):%d meter_sub_cmd:%d",
                _sendMeterDataPhase_ColdStart, _sendMeterDataPhase_General, meter.meter_sub_cmd);

        if (_sendMeterDataStatus_ColdStart < SendMeterDataStatus_FutabaD.NONE)          //通信系エラー発生時（物理接続断かタイムアウト時）
        {
            Timber.e("[FUTABA-D]updateMeter(ResponseFutabaD) 820 Connection Error Status(Cold):%d", _sendMeterDataStatus_ColdStart);
            _isConnected820 = false;                                                     //820接続状態を未接続にする
            _sendMeterDataStatus_ColdStart = SendMeterDataStatus_FutabaD.NONE;           //メータデータ送信ステータス
            _sendMeterDataPhase_ColdStart = SendMeterDataStatus_FutabaD.NONE;                           //メータデータ送信フェーズ
            _sendMeterDataStatus_General = SendMeterDataStatus_FutabaD.NONE;                //メータデータ送信ステータス一般用
            _sendMeterDataPhase_General  = SendMeterDataStatus_FutabaD.NONE;                //メータデータ送信フェーズ一般用
            return;
        }


        //ADD-S BMT S.Oyama 2025/03/27 フタバ双方向向け改修
        String st = meter.line_41;
        meter.line_41 = convertJavaStringAryFrom820SJisHex(st);
        st = meter.line_42;
        meter.line_42 = convertJavaStringAryFrom820SJisHex(st);
        st = meter.line_43;
        meter.line_43 = convertJavaStringAryFrom820SJisHex(st);
        //ADD-E BMT S.Oyama 2025/03/27 フタバ双方向向け改修

        boolean flOKLen3 = false;
        boolean flOKLen4 = false;
        String tmpStatusStr = meter.status;
        if (tmpStatusStr != null) {
            if (tmpStatusStr.length() == 3) {
                flOKLen3 = tmpStatusStr.equals("000");          //820側からエラーを返してきたとき 3文字系(000以外はエラー)
            } else if (tmpStatusStr.length() == 4) {
                flOKLen4 = tmpStatusStr.equals("0000");         //820側からエラーを返してきたとき 4文字系(0000以外はエラー)
            }
        }

        if ((flOKLen3 == true) || (flOKLen4 == true)) {

        }
        else {                                                                                  //820側から001エラーを返してきたとき(820のリブート信号を受信したとき)
            if ((tmpStatusStr.length() == 3) && (tmpStatusStr.equals("001") == true)) {         //820自身のリブート実施信号受信時
                Timber.e("[FUTABA-D]820->750(updateMeter:FutabaD [820 Reboot signal detected]) Status Error:%s", tmpStatusStr);

                killRetryTimer();                                                               // タイムアウト監視タイマを停止（旧版）
                killRetryTimerFutabaD();                                                        // タイムアウト監視タイマを停止（フタバD向けを停止）

                _sendMeterDataStatus_ColdStart = SendMeterDataStatus_FutabaD.NONE;              //メータデータ送信ステータス
                _sendMeterDataPhase_ColdStart = SendMeterDataStatus_FutabaD.NONE;               //メータデータ送信フェーズ
                _sendMeterDataStatus_General = SendMeterDataStatus_FutabaD.NONE;                //メータデータ送信ステータス一般用
                _sendMeterDataPhase_General  = SendMeterDataStatus_FutabaD.NONE;                //メータデータ送信フェーズ一般用
                _isConnected820 = false;                                                        //820接続状態を未接続にする

                Timber.i("[FUTABA-D]updateMeter(Meter.ResponseFutabaD) Disconnect 820.");
            } else {
                if (_sendMeterDataPhase_ColdStart != SendMeterDataStatus_FutabaD.NONE) {            //コールドスタート処理時
                    Timber.e("[FUTABA-D]820->750(updateMeter:FutabaD [Cold start]) Status Error:%s", tmpStatusStr);

                    killRetryTimerFutabaD();                                                        // タイムアウト監視タイマを停止（フタバD向けを停止）

                    _sendMeterDataStatus_ColdStart = SendMeterDataStatus_FutabaD.NONE;              //メータデータ送信ステータス
                    _sendMeterDataPhase_ColdStart = SendMeterDataStatus_FutabaD.NONE;               //メータデータ送信フェーズ
                    _sendMeterDataStatus_General = SendMeterDataStatus_FutabaD.NONE;                //メータデータ送信ステータス一般用
                    _sendMeterDataPhase_General = SendMeterDataStatus_FutabaD.NONE;                //メータデータ送信フェーズ一般用
                    _isConnected820 = false;                                                        //820接続状態を未接続にする
                    Timber.i("[FUTABA-D]updateMeter(Meter.ResponseFutabaD) Disconnect 820.");

                } else {                                                                              //一般処理時
                    Timber.e("[FUTABA-D]820->750(updateMeter:FutabaD [General Job]) Status Error:%s", tmpStatusStr);

                    killRetryTimerFutabaD();                                                        // タイムアウト監視タイマを停止（フタバD向けを停止）

                    _sendMeterDataStatus_General = SendMeterDataStatus_FutabaD.NONE;                //メータデータ送信ステータス一般用
                    _sendMeterDataPhase_General = SendMeterDataStatus_FutabaD.NONE;                //メータデータ送信フェーズ一般用

                    int tmpErrorCode820 = 0;
                    try {
                        tmpErrorCode820 = Converters.stringToInteger(tmpStatusStr);
                    } catch (NumberFormatException e) {
                        Timber.e("[FUTABA-D]updateMeter(Meter.ResponseFutabaD) NumberFormatException:%s", e.getMessage());
                    }

                    SendMeterDataError_FutabaD sendMeterDataError_FutabaD = new SendMeterDataError_FutabaD();       // 未接続時の処理(コールバック処理を追加)
                    sendMeterDataError_FutabaD.ErrorCode = SendMeterDataStatus_FutabaD.ERROR_820NACK;
                    sendMeterDataError_FutabaD.ErrorMessage = "820NACK";
                    sendMeterDataError_FutabaD.ErrorCode820 = tmpErrorCode820;

                    _meterDataV4Error.onNext(sendMeterDataError_FutabaD);
                }
            }
            return;
        }

        int tmpSubCmd = (meter.meter_sub_cmd != null) ? meter.meter_sub_cmd : 0;

        boolean tmpRecvFunctionJobStartFL = false;
        switch(tmpSubCmd)
        {
            case 1:                     //ID認証
                send820_ColdStart_AuthID();
                break;
            case 9:                     //ファンクション実行要求：
                if (_sendMeterDataPhase_General >= SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_CASH &&
                        _sendMeterDataPhase_General <= SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_QR) {           //前回820送信が「決済選択 No30～39」の場合(CreateChecker, EmoneyChecker, QRCheckerで処理)
                    tmpRecvFunctionJobStartFL = true;
                } else if (_sendMeterDataPhase_General == SendMeterDataStatus_FutabaD.SEPARATION_TIKECT) {          //前回820送信が「チケット」の場合(AmountInputSeparationPayFDFragmentで処理)
                    tmpRecvFunctionJobStartFL = true;
                    killRetryTimer();                   // タイムアウト監視タイマを停止   (分別Ticketは，/printdata/v4::print_startで送出するため)
                } else if ((_sendMeterDataPhase_General >= SendMeterDataStatus_FutabaD.BALANCEINQUIRY_EDY) &&
                            (_sendMeterDataPhase_General <= SendMeterDataStatus_FutabaD.BALANCEINQUIRY_WAON)) {     //前回820送信が「残高照会 No90～96」の場合(EmoneyChackで処理)
                    tmpRecvFunctionJobStartFL = true;
                } else if ( _sendMeterDataPhase_General == SendMeterDataStatus_FutabaD.WAON_HISTORY)                //前回820送信が「WAON履歴 No112」の場合(HomeMenuで処理)
                {
                    tmpRecvFunctionJobStartFL = true;
                } else if ((_sendMeterDataPhase_General >= SendMeterDataStatus_FutabaD.SEPARATION_CREDITCASH_FIRST) &&
                        (_sendMeterDataPhase_General <= SendMeterDataStatus_FutabaD.SEPARATION_QRCASH_FIRST)) {     //前回820送信が「分別：***・現金 No60～62」の場合(HomeMenuで処理)
                    tmpRecvFunctionJobStartFL = true;
                    meter.separate_flg = 1;             //分別フラグを立てる
                } else if ((_sendMeterDataPhase_General >= SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_PAY) &&     //前回820送信が「決済選択(プリペイド) No230～237」の場合(PrinterProcで処理)
                        (_sendMeterDataPhase_General <= SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_POINTCHARGE)) {
                    tmpRecvFunctionJobStartFL = true;
                }

                if (tmpRecvFunctionJobStartFL == true)
                {
                    killRetryTimerFutabaD();                // タイムアウト監視タイマを停止（フタバD向けを停止）

                    _sendMeterDataStatus_General = SendMeterDataStatus_FutabaD.NONE;       //メータデータ送信ステータス一般用
                    _sendMeterDataPhase_General  = SendMeterDataStatus_FutabaD.NONE;       //メータデータ送信フェーズ一般用

                    _meterDataV4.onNext(meter);
                }
                break;
            case 5:                     //処理コード通知
                if (meter.sound_no != null) {
                    int tmpSoundNo = meter.sound_no;
                    if (meterDataV4Disposable_MenuHome != null || meterDataV4InfoDisposable_DailyReportFuel != null) {
                        _meterDataV4.onNext(meter);                             //他所のクラスで処理(自動日報系で使用)
//                        // 現在の画面がホーム画面
//                        if ((tmpSoundNo == -3) || (tmpSoundNo == 3) || (tmpSoundNo == 9)) {           //音声通知：3,9
//                            _meterDataV4.onNext(meter);                         //他所のクラスで処理(HomeMenuで処理)
//                        } else if (tmpSoundNo == 1)                               //音声通知１　領収書，チケット伝票時にメータより通知される
//                        {
//                            _meterDataV4.onNext(meter);                         //他所のクラスで処理(MenuEventHandlerImplで処理)
//                        } else if (tmpSoundNo == 0)                               //音声通知0　他の処理コード要求
//                        {
//                            _meterDataV4.onNext(meter);                         //他所のクラスで処理(HomeMenuで処理)
//                        }
                    } else {
                        // 現在の画面がホーム画面ではない
                        stackingProcessCode(meter);
                    }
                }
                break;
            case 12:                    //割引処理
                if (_sendMeterDataPhase_General >= SendMeterDataStatus_FutabaD.DISCOUNTTYPE_NOTIFY_CONFIRMATION &&
                        _sendMeterDataPhase_General <= SendMeterDataStatus_FutabaD.DISCOUNTTYPE_NOTIFY_JOB5) {    //前回820送信が「割引処理(通知) No120～125」の場合(固定割引JOBで処理)
                    tmpRecvFunctionJobStartFL = true;
                }
                if (tmpRecvFunctionJobStartFL == true)
                {
                    killRetryTimerFutabaD();                // タイムアウト監視タイマを停止（フタバD向けを停止）

                    _sendMeterDataStatus_General = SendMeterDataStatus_FutabaD.NONE;       //メータデータ送信ステータス一般用
                    _sendMeterDataPhase_General  = SendMeterDataStatus_FutabaD.NONE;       //メータデータ送信フェーズ一般用

                    _meterDataV4.onNext(meter);
                }
                break;
            case 13:                    //自動日報
                if (_sendMeterDataPhase_General >= SendMeterDataStatus_FutabaD.AUTODAILYREPORT_IN &&
                        _sendMeterDataPhase_General <= SendMeterDataStatus_FutabaD.AUTODAILYREPORT_EOC) {    //前回820送信が「自動日報 No300～399」の場合
                    tmpRecvFunctionJobStartFL = true;
                }
                if (tmpRecvFunctionJobStartFL == true)
                {
                    killRetryTimerFutabaD();                // タイムアウト監視タイマを停止（フタバD向けを停止）

                    _sendMeterDataStatus_General = SendMeterDataStatus_FutabaD.NONE;       //メータデータ送信ステータス一般用
                    _sendMeterDataPhase_General  = SendMeterDataStatus_FutabaD.NONE;       //メータデータ送信フェーズ一般用

                    _meterDataV4.onNext(meter);
                }
                break;
            //ADD-S BMT S.Oyama 2025/02/07 フタバ双方向向け改修
            case 2:                     //リセット
                send820_ColdStart_ResetGeneral();
                _meterDataV4.onNext(meter);
                break;
            //ADD-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修
            case 4:                     //入庫処理(HOME_MENUで処理)
            case 3:                     //出庫処理(HOME_MENUで処理)
            default:                    //それ以外は他所のクラス側で処理
                _meterDataV4.onNext(meter);
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void stackingProcessCode(Meter.ResponseFutabaD meter) {
        // エラーをスタック
        Timber.i("[FUTABA-D]stackingProcessCode soundNo:%d", meter.sound_no);
        switch (meter.sound_no) {
            case 0:
                if ((meter.line_1 != null) && (meter.line_2 != null)) {
                    int tmpProcessCode = send820_IsProcessCode_ErrorCD(meter.line_1);          //処理コード表示要求エラーコードはLine１に乗ってくる　エラーコードを取得
                    if (tmpProcessCode < 0) {           //エラーコードがある場合
                        String tmpProcessCodeStr[] = send820_KeyAndErrDetail(meter.line_2);      //キー要求，エラーコード詳細を取得(配列２要素)
                        if (meter.line_3 != null) {
                            if (meter.line_3.equals(IFBoxManager.Send820Status_ProcessCode_FuncNAME.FUNCNAME_HANEAGARI)) {
                                String kingaku = tmpProcessCodeStr[1].replaceAll(" ", "");
                                ErrorManage errorManage = ErrorManage.getInstance();
                                errorManage.stackingError(_app.getString(R.string.error_type_FutabaD_FareUp_Warning) + "@@@" + getFareUpMessage(kingaku) + "@@@");
                                Timber.i("跳ね上がり（スタックエラー） %s", kingaku);
                            }
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    public static String getFareUpMessage(String kingaku) {
        String title = "残金　"+"￥" + kingaku.substring(0, Math.min(kingaku.length(), 32)); // タイトルに残金＋金額
        String haneagariMsg = "残金があります" + "\n" + "領収書を印刷します";
        return title + "\n" + haneagariMsg;
    }

    /******************************************************************************/
    /*!
     * @brief  旧API向け820送信時のタイムアウト監視タイマを停止（フタバ双方向用）
     * @note   旧API向け820送信時のタイムアウト監視タイマを停止
     * @param なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    private void killRetryTimer()
    {
        Timber.i("[FUTABA-D]killRetryTimer() ");

        if (wsRetryTimer != null) {
            wsRetryTimer.cancel();
            wsRetryTimer = null;
        }
    }

    /******************************************************************************/
    /*!
     * @brief  フタバD向け820送信時のタイムアウト監視タイマを停止（フタバ双方向用）
     * @note   フタバD向け820送信時のタイムアウト監視タイマを停止
     * @param なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void killRetryTimerFutabaD()
    {
        Timber.i("[FUTABA-D]killRetryTimerFutabaD() ");

        if (_wsRetryTimerFutabaD != null) {
            _wsRetryTimerFutabaD.cancel();
            _wsRetryTimerFutabaD = null;
        }
    }

    /******************************************************************************/
    /*!
     * @brief  SendMeterData_FutabaDのメンバをクリア（フタバ双方向用）
     * @note   SendMeterData_FutabaDのメンバをクリア (プロトコル開始)
     * @param なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    private void send820_ClearMeterDataClassMember()
    {
        Timber.i("[FUTABA-D]send820_ClearMeterDataClassMember() ");

        _sendMeterData_FutabaD.meter_sub_cmd = 0;           //メーターサブコマンド
        _sendMeterData_FutabaD.status = "";                 //応答ｽﾃｰﾀｽ
        _sendMeterData_FutabaD.status_code = "";            //応答ｽﾃｰﾀｽｺｰﾄﾞ
        _sendMeterData_FutabaD.term_ver = "";               //端末バージョン
        _sendMeterData_FutabaD.car_id = 0;                  //車番
        _sendMeterData_FutabaD.input_kingaku = 0;           //入力金額
        _sendMeterData_FutabaD.key_code = 0;                //キーコード
        _sendMeterData_FutabaD.trans_date = "";             //処理日時
        _sendMeterData_FutabaD.discount_way = 0;            //手動割引/割引カード
        _sendMeterData_FutabaD.discount_type = 0;           //割引種別
        _sendMeterData_FutabaD.exp_date = "";               //有効期限
        _sendMeterData_FutabaD.adr_l2 = 0;                  //自動日報階層２
        _sendMeterData_FutabaD.adr_l3 = 0;                  //自動日報階層３
        _sendMeterData_FutabaD.adr_input1 = 0;              //自動日報入力値１
        _sendMeterData_FutabaD.adr_input2 = 0;              //自動日報入力値２
        _sendMeterData_FutabaD.if_ver = 0;                  //通信IFバージョン
    }

    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知（フタバ双方向用）
     * @note   820への通知データを処理する：起動時および再接続時 (プロトコル開始)
     * @param なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    private void send820_ColdStart_Start()
    {
        Timber.i("[FUTABA-D]send820_ColdStart_Start() method in[/alive in]");

        if (_isConnected820 == true)            // 820へ一度初期化した場合は，再通信しない
        {
            Timber.i("[FUTABA-D]send820_ColdStart_Start() 820 is Connected.No Coldstart!(no problem:keep alive)");
            return;
        }
        else
        {
            //_isConnected820 = true;             // 820接続状態を接続中にする
            _sendMeterDataStatus_ColdStart = SendMeterDataStatus_FutabaD.NONE;           //メータデータ送信ステータス
            _sendMeterDataPhase_ColdStart = SendMeterDataStatus_FutabaD.NONE;                           //メータデータ送信フェーズ
        }

        if (_is820FirmUpdatingFL == true)       // 820ファームウェア更新中の場合は，通信しない
        {
            Timber.i("[FUTABA-D]send820_ColdStart_Start() 820 is Connected.No Coldstart!(no problem:820 Firmware Updating)");
            updateFirm820_TimerKill();                 // 820ファームウェア更新中はタイムアウトタイマを停止する
            return;
        }

        Timber.i("[FUTABA-D] ****** 750 Cold start initiated ******");

        killRetryTimerFutabaD();                // タイムアウト監視タイマを停止

        send820_ClearMeterDataClassMember();              //メンバ変数のクリア

        _sendMeterData_FutabaD.status =  "000";
        _sendMeterData_FutabaD.meter_sub_cmd = 6;           //メーターサブコマンド        6:キー入力通知
        _sendMeterData_FutabaD.key_code = 999;              //キーコード                 999:起動時

        _sendMeterDataStatus_ColdStart = SendMeterDataStatus_FutabaD.SENDING;           //メータデータ送信ステータス
        _sendMeterDataPhase_ColdStart = SendMeterDataStatus_FutabaD.COLDSTART_START;                           //メータデータ送信フェーズ

        //_820SendTransactionRunning = SendMeterDataStatus_FutabaD.TRANSACTION_RUNNING;                           //820送信トランザクション実行中

        sendWsMeterdata_FutabaD(PrinterConst.DuplexPrintResponseTimer);              // メーター情報を送信
    }

    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知（フタバ双方向用）
     * @note   820への通知データを処理する：起動時および再接続時 (リセット要求)
     * @param なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    //DEL-S BMT S.Oyama 2025/02/07 フタバ双方向向け改修
//    private void send820_ColdStart_Reset()
//    {
//        Timber.i("[FUTABA-D]send820_ColdStart_Reset() ");
//
//        if (_isConnected820 == true)            // 820への通信が切れた場合は実施しない
//        {
//            Timber.e("[FUTABA-D] send820_ColdStart_Reset():Not connected to 820. ");
//            return;
//        }
//
//        killRetryTimerFutabaD();                // タイムアウト監視タイマを停止
//
//        send820_ClearMeterDataClassMember();              //メンバ変数のクリア
//
//        _sendMeterData_FutabaD.status = "000";              //ステータスコード               000:正常
//        _sendMeterData_FutabaD.meter_sub_cmd = 2;           //メーターサブコマンド            2:リセット要求
//
//        _sendMeterDataStatus_ColdStart = SendMeterDataStatus_FutabaD.SENDING;           //メータデータ送信ステータス
//        _sendMeterDataPhase_ColdStart = SendMeterDataStatus_FutabaD.COLDSTART_RESET;                           //メータデータ送信フェーズ
//
//        sendWsMeterdata_FutabaD(PrinterConst.DuplexPrintResponseTimer);              // メーター情報を送信
//    }
    //DEL-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修
    //ADD-S BMT S.Oyama 2025/02/07 フタバ双方向向け改修
    private void send820_ColdStart_ResetGeneral()
    {
        Timber.i("[FUTABA-D]send820_ColdStart_ResetGeneral() ****** Reset Recv ******");
    }
    //ADD-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修

    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知（フタバ双方向用）
     * @note   820への通知データを処理する：起動時および再接続時 (ID認証)
     * @param なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    private void send820_ColdStart_AuthID()
    {
        Timber.i("[FUTABA-D]send820_ColdStart_AuthID() ");

        if (_isConnected820 == true)            // 820への通信が接続中の場合は実施しない
        {
            Timber.e("[FUTABA-D] send820_ColdStart_AuthID():now connected 820. not work job.");
            return;
        }

        killRetryTimerFutabaD();                // タイムアウト監視タイマを停止

        send820_ClearMeterDataClassMember();              //メンバ変数のクリア

        //DEL-S BMT S.Oyama 2025/02/07 フタバ双方向向け改修
//        String tmpTermVer = "";
//        if (AppPreference.isDemoMode() == true) {        //デモモードの場合
//            tmpTermVer = "DEMO";
//        } else {                                         //通常モードの場合
//            tmpTermVer = "0000";
//        }
//
//        _sendMeterData_FutabaD.status = "000";              //ステータスコード               000:正常
//        _sendMeterData_FutabaD.meter_sub_cmd = 1;           //メーターサブコマンド            1:ID認証
//        _sendMeterData_FutabaD.term_ver = tmpTermVer;       //端末バージョン                 DEMO:デモ版
//        _sendMeterData_FutabaD.if_ver = 2103;               //通信IFﾊﾞｰｼﾞｮﾝ
//
//        //// ※以下は仮で固定埋め込みとしています
//        //_sendMeterData_FutabaD.term_ver = "DEMO";           //端末ﾊﾞｰｼﾞｮﾝ
//        //_sendMeterData_FutabaD.status = "000";              //応答ｽﾃｰﾀｽ
//
//        _sendMeterDataStatus_ColdStart = SendMeterDataStatus_FutabaD.SENDING;           //メータデータ送信ステータス
//        _sendMeterDataPhase_ColdStart = SendMeterDataStatus_FutabaD.COLDSTART_AUTHID;                           //メータデータ送信フェーズ
//
//        sendWsMeterdata_FutabaD(PrinterConst.DuplexMeterSendWaitTimerShortFutabaD);              // メーター情報を送信(ACKの返送はないので，タイムアウトなし)
        //DEL-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修

        //ADD-S BMT S.Oyama 2025/02/07 フタバ双方向向け改修
        // AuthID応答でコールドスタートは終了　各状態変化変数をクリアする
        _sendMeterDataStatus_ColdStart = SendMeterDataStatus_FutabaD.NONE;              //メータデータ送信ステータス
        _sendMeterDataPhase_ColdStart = SendMeterDataStatus_FutabaD.NONE;               //メータデータ送信フェーズ
        _sendMeterDataStatus_General = SendMeterDataStatus_FutabaD.NONE;                //メータデータ送信ステータス一般用
        _sendMeterDataPhase_General  = SendMeterDataStatus_FutabaD.NONE;                //メータデータ送信フェーズ一般用
        //ADD-E BMT S.Oyama 2025/02/07 フタバ双方向向け改修

        _isConnected820 = true;              // 初期化完了後820接続状態を接続中にする
        _isCarNoSendREQ = true;              //車両番号送信要求

        PrinterManager tmpPrinterManager = PrinterManager.getInstance();
        tmpPrinterManager.changePrintStatusEx(PrinterConst.PrintStatus_IDLE);           //プリンタステータスをアイドル化する

        Timber.i("[FUTABA-D] ****** 750 Cold start completed ******");
    }

    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知（フタバ双方向用）
     * @note   820への通知データを処理する：入庫処理のACK
     * @param なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_ACK_Nyuuko() {
        Timber.i("[FUTABA-D]send820_ACK_Nyuuko() ");

        if (_isConnected820 == false)            // 820への通信が切れた場合は実施しない
        {
            Timber.e("[FUTABA-D] send820_ACK_Nyuuko():Not connected to 820. ");
            return;
        }

        if (_sendMeterDataPhase_ColdStart != SendMeterDataStatus_FutabaD.NONE) {            //コールドスタート処理時
            Timber.e("[FUTABA-D] send820_ACK_Nyuuko():ColdStart processing is in progress. ");
            return;
        }

        killRetryTimerFutabaD();                // タイムアウト監視タイマを停止

        send820_ClearMeterDataClassMember();              //メンバ変数のクリア

        _sendMeterData_FutabaD.status = "0000";              //ステータスコード               000:正常
        _sendMeterData_FutabaD.meter_sub_cmd = 4;           //メーターサブコマンド            4:入庫

        _sendMeterDataStatus_General = SendMeterDataStatus_FutabaD.SENDING;           //メータデータ送信ステータス
        _sendMeterDataPhase_General = SendMeterDataStatus_FutabaD.ACK_NYUUKO;                           //メータデータ送信フェーズ

        sendWsMeterdata_FutabaD(PrinterConst.DuplexMeterSendWaitTimerShortFutabaD);              // メーター情報を送信(ACKの返送はないので，タイムアウトなし)
    }

    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知（フタバ双方向用）
     * @note   820への通知データを処理する：出庫処理のACK
     * @param String tmpStatus 820へ返すステータスコード　前段処理に問題なければ"0000" 異常時はそれ以外
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_ACK_Syukko(String tmpStatus) {
        Timber.i("[FUTABA-D]send820_ACK_Nyuuko() Status: %s", tmpStatus);

        if (_isConnected820 == false)            // 820への通信が切れた場合は実施しない
        {
            Timber.e("[FUTABA-D] send820_ACK_Syukko():Not connected to 820. ");
            return;
        }

        if (_sendMeterDataPhase_ColdStart != SendMeterDataStatus_FutabaD.NONE) {            //コールドスタート処理時
            Timber.e("[FUTABA-D] send820_ACK_Syukko():ColdStart processing is in progress. ");
            return;
        }

        killRetryTimerFutabaD();                // タイムアウト監視タイマを停止

        send820_ClearMeterDataClassMember();              //メンバ変数のクリア

        _sendMeterData_FutabaD.status = tmpStatus;          //ステータスコード               0000:正常 それ以外:エラー
        _sendMeterData_FutabaD.meter_sub_cmd = 3;           //メーターサブコマンド            3:出庫

        _sendMeterDataStatus_General = SendMeterDataStatus_FutabaD.SENDING;           //メータデータ送信ステータス
        _sendMeterDataPhase_General = SendMeterDataStatus_FutabaD.ACK_SYUKKO;                           //メータデータ送信フェーズ

        sendWsMeterdata_FutabaD(PrinterConst.DuplexMeterSendWaitTimerShortFutabaD);              // メーター情報を送信(ACKの返送はないので，タイムアウトなし)
    }

    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知（フタバ双方向用）
     * @note   820への通知データを処理する：車番設定通知のACK
     * @param String tmpStatus 820へ返すステータスコード　前段処理に問題なければ"0000" 異常時はそれ以外
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_ACK_CarNo() {
        Timber.i("[FUTABA-D]send820_ACK_CarNo() ");
        if (_isConnected820 == false)            // 820への通信が切れた場合は実施しない
        {
            Timber.e("[FUTABA-D] send820_ACK_CarNo():Not connected to 820. ");
            return;
        }

        if (_sendMeterDataPhase_ColdStart != SendMeterDataStatus_FutabaD.NONE) {            //コールドスタート処理時
            Timber.e("[FUTABA-D] send820_ACK_CarNo():ColdStart processing is in progress. ");
            return;
        }

        killRetryTimerFutabaD();                // タイムアウト監視タイマを停止

        send820_ClearMeterDataClassMember();              //メンバ変数のクリア

        int tmpCarNo = AppPreference.getMcCarId();
        if (tmpCarNo <= 0) {                            //車番が取得できない場合はダミーの車番を設定
            tmpCarNo = 999999;
        }

        _sendMeterData_FutabaD.status = "0000";          //ステータスコード               0000:正常 それ以外:エラー
        _sendMeterData_FutabaD.car_id = tmpCarNo;        //車番
        _sendMeterData_FutabaD.meter_sub_cmd = 7;        //メーターサブコマンド            7:車番設定通知

        _sendMeterDataStatus_General = SendMeterDataStatus_FutabaD.SENDING;           //メータデータ送信ステータス
        _sendMeterDataPhase_General = SendMeterDataStatus_FutabaD.ACK_CARNO;                           //メータデータ送信フェーズ

        sendWsMeterdata_FutabaD(PrinterConst.DuplexMeterSendWaitTimerShortFutabaD);              // メーター情報を送信(ACKの返送はないので，タイムアウトなし)

    }



    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知:立替定額処理（フタバ双方向用）
     * @note   820への通知データを処理する：立替定額処理 この処理に関して，820からのACKは無い
     * @param int tmpAdvanceFlatMode   立替・定額別モード
     * @param int tmpAmount            送信する金額
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_AdvancedPay(int tmpAdvanceFlatMode, int tmpAmount )
    {
        Timber.i("[FUTABA-D]send820_AdvancedPay() Mode:%d Amount:%d", tmpAdvanceFlatMode, tmpAmount);
        if (_isConnected820 == false)            // 820への通信が切れた場合は実施しない
        {
            Timber.e("[FUTABA-D]send820_AdvancedPay():Not connected to 820. ");
            return;
        }

        killRetryTimerFutabaD();                // タイムアウト監視タイマを停止

        send820_ClearMeterDataClassMember();              //メンバ変数のクリア

        _sendMeterData_FutabaD.status = "0000";             //ステータスコード               0000:正常

        if (tmpAdvanceFlatMode == SendMeterDataStatus_FutabaD.ADVANCEPAY_ADVANCE) {
            _sendMeterData_FutabaD.meter_sub_cmd = 10;           //メーターサブコマンド            10:立替
        } else if (tmpAdvanceFlatMode == SendMeterDataStatus_FutabaD.ADVANCEPAY_FLATRATE) {
            _sendMeterData_FutabaD.meter_sub_cmd = 11;           //メーターサブコマンド            11:定額
        }
        else
        {
            Timber.e("[FUTABA-D] send820_AdvancedPay():Mode Error");
            return;
        }
        _sendMeterData_FutabaD.input_kingaku = tmpAmount;      //入力金額

        _sendMeterDataStatus_General = SendMeterDataStatus_FutabaD.SENDING;           //メータデータ送信ステータス
        _sendMeterDataPhase_General = tmpAdvanceFlatMode;                           //メータデータ送信フェーズ

        sendWsMeterdata_FutabaD(PrinterConst.DuplexMeterSendWaitTimerShortFutabaD);              // メーター情報を送信(立替定額処理の返送はないので，タイムアウトなし)
    }

    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知:決済選択通知（フタバ双方向用）
     * @note   820への通知データを処理する：決済選択通知
     * @param int tmpSettlementSelect  決済選択モード
     * @param boolean tmpTranCancelFL  決済取り消しフラグ
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_SettlementSelectMode(int tmpSettlementSelect, boolean tmpTranCancelFL ) {
        Timber.i("[FUTABA-D]send820_SettlementSelectMode() SettlementSelectMode:%d ", tmpSettlementSelect);

        int tmpKeyCode = 0;

        if (tmpTranCancelFL == false) {                 //通常決済
            switch (tmpSettlementSelect) {
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_CREDIT:
                    tmpKeyCode = 32;
                    break;
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_EDY:
                    tmpKeyCode = 917;
                    break;
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_ID:
                    tmpKeyCode = 920;
                    break;
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_NANACO:
                    tmpKeyCode = 939;
                    break;
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_QUICPAY:
                    tmpKeyCode = 921;
                    break;
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_SUICA:
                    tmpKeyCode = 923;
                    break;
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_WAON:
                    tmpKeyCode = 928;
                    break;
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_QR:
                    tmpKeyCode = 801;
                    break;
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_PAY               :       //プリペイド支払
                    tmpKeyCode = 501;
                    break;
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_PAYREFUND         :       //プリペイド支払取り消し
                    tmpKeyCode = 502;
                    break;
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_POINTADD          :       //プリペイドポイント追加
                    tmpKeyCode = 506;
                    break;
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_POINTREFUND       :       //プリペイドポイント返金
                    tmpKeyCode = 507;
                    break;
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_CACHECHARGE       :       //プリペイド現金チャージ
                    tmpKeyCode = 503;
                    break;
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_CHCHECHARGEREFUND :       //プリペイド現金チャージ取り消し
                    tmpKeyCode = 504;
                    break;
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_CARDBUY           :       //プリペイドカード販売
                    tmpKeyCode = 500;
                    break;
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_POINTCHARGE       :       //プリペイドポイントチャージ
                    tmpKeyCode = 505;
                    break;

                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_CASH:             //現金はなし
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_OKICA:            //OKICAはなし
                default:
                    break;
            }
        } else {                                        //決済キャンセル
            switch (tmpSettlementSelect) {
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_CREDIT:
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_ID:
                case SendMeterDataStatus_FutabaD.ADVANCEPAY_CLEAR:        //立替定額取消
                    tmpKeyCode = 24;
                    break;
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_EDY:
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_NANACO:
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_QUICPAY:
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_SUICA:
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_WAON:
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_QR:
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_PAY               :       //プリペイド支払
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_PAYREFUND         :       //プリペイド支払取り消し
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_POINTADD          :       //プリペイドポイント追加
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_POINTREFUND       :       //プリペイドポイント返金
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_CACHECHARGE       :       //プリペイド現金チャージ
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_CHCHECHARGEREFUND :       //プリペイド現金チャージ取り消し
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_CARDBUY           :       //プリペイドカード販売
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_PREPAID_POINTCHARGE       :       //プリペイドポイントチャージ
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_CASH:             //現金はなし
                case SendMeterDataStatus_FutabaD.SETTLEMENTSELECT_OKICA:            //OKICAはなし
                default:
                    return;
            }
        }

        send820_KeyCode(tmpSettlementSelect, tmpKeyCode, true);
    }

    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知:残高照会通知（フタバ双方向用）
     * @note   820への通知データを処理する：残高照会通知
     * @param int tmpSettlementSelect  決済選択モード
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_BalanceInquiryMode(int tmpBalanceInquiry ) {
        Timber.i("[FUTABA-D]send820_BalanceInquiryMode() BalanceInquiry:%d ",  tmpBalanceInquiry);

        int tmpKeyCode = 0;

        switch(tmpBalanceInquiry)
        {
            case SendMeterDataStatus_FutabaD.BALANCEINQUIRY_EDY      :
                //tmpKeyCode = 932;
                tmpKeyCode = 960;
                break;
            case SendMeterDataStatus_FutabaD.BALANCEINQUIRY_NANACO   :
                //tmpKeyCode = 952;
                tmpKeyCode = 961;
                break;
            case SendMeterDataStatus_FutabaD.BALANCEINQUIRY_SUICA    :
                tmpKeyCode = 924;
                break;
            case SendMeterDataStatus_FutabaD.BALANCEINQUIRY_WAON     :
                tmpKeyCode = 929;
                break;
            default:
                break;
        }
        send820_KeyCode(tmpBalanceInquiry, tmpKeyCode, true);
    }

    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知:割引開始要求通知（フタバ双方向用）
     * @note   820への通知データを処理する：割引開始要求通知 カード使用か固定１～５
     * @param int tmpDiscountType  割引種モード
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_DiscountType(int tmpDiscountType ) {
        Timber.i("[FUTABA-D]send820_DiscountType() DiscountType:%d ", tmpDiscountType);

        int tmpKeyCode = 511;

        send820_KeyCode(tmpDiscountType, tmpKeyCode, true);
    }

    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知:割引処理実施（フタバ双方向用）
     * @note   820への通知データを処理する：割引処理実施 カード使用か固定１～５
     * @param tmpDiscountType  割引種モード
     * @param String tmpJobDateTime  処理日時
     * @param String tmpExpireDateTime  有効期限(カード使用時のみ有効，それ以外は空文字)
     * @param int tmpCardDiscountType  カードが保持する割引種モード
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_DiscountExecution(int tmpDiscountType, String tmpJobDateTime, String tmpExpireDateTime ,int tmpCardDiscountType) {
        Timber.i("[FUTABA-D]send820_DiscountExecution() DiscountType:%d JobDateTime:%s ExpireDateTime:%s CardDiscountType:%d", tmpDiscountType, tmpJobDateTime, tmpExpireDateTime, tmpCardDiscountType);

        int tmpdiscountway = 0;
        int tmpdiscounttype = 0;

        if (_isConnected820 == false)            // 820への通信が切れた場合は実施しない
        {
            Timber.e("[FUTABA-D]send820_DiscountExecution():Not connected to 820. ");
            return;
        }

        switch(tmpDiscountType)
        {
            case SendMeterDataStatus_FutabaD.DISCOUNTTYPE_CONFIRMATION      :
                tmpdiscountway = 1;
                tmpdiscounttype = tmpCardDiscountType;
                break;
            case SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB1   :
                tmpdiscountway = 0;
                tmpdiscounttype = 1;
                break;
            case SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB2    :
                tmpdiscountway = 0;
                tmpdiscounttype = 2;
                break;
            case SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB3     :
                tmpdiscountway = 0;
                tmpdiscounttype = 3;
                break;
            case SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB4    :
                tmpdiscountway = 0;
                tmpdiscounttype = 4;
                break;
            case SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB5     :
                tmpdiscountway = 0;
                tmpdiscounttype = 5;
                break;
            default:
                break;
        }

        killRetryTimerFutabaD();                // タイムアウト監視タイマを停止

        send820_ClearMeterDataClassMember();              //メンバ変数のクリア

        _sendMeterData_FutabaD.status           = "000";
        _sendMeterData_FutabaD.meter_sub_cmd    = 12;                   //メーターサブコマンド        12:割引
        _sendMeterData_FutabaD.trans_date       = tmpJobDateTime;       //処理日時
        _sendMeterData_FutabaD.discount_way     = tmpdiscountway;       //割引方法
        _sendMeterData_FutabaD.discount_type    = tmpdiscounttype;      //割引種類
        _sendMeterData_FutabaD.exp_date         = tmpExpireDateTime;    //有効期限

        _sendMeterDataStatus_General= SendMeterDataStatus_FutabaD.SENDING;          //メータデータ送信ステータス
        _sendMeterDataPhase_General = tmpDiscountType;                              //メータデータ送信フェーズ

        sendWsMeterdata_FutabaD(PrinterConst.DuplexMeterSendWaitTimerShortFutabaD);              // メーター情報を送信  ACKなし
    }

    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知:領収書，チケット伝票発行要求通知（フタバ双方向用）
     * @note   820への通知データを処理する：チケット伝票発行要求通知　：分別チケット後現金時も使用
     * @param int tmpReceiptTicketType  領収書／チケット伝票別
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_ReceiptTicketPrint(int tmpReceiptTicketType ) {
        Timber.i("[FUTABA-D]send820_ReceiptTicketPtint() ReceiptTicketType:%d ", tmpReceiptTicketType);

        int tmpKeyCode = 0;

        switch(tmpReceiptTicketType)
        {
            case SendMeterDataStatus_FutabaD.RECEIPT_PRINT     :        // 領収書発行
                tmpKeyCode = 34;
                break;
            case SendMeterDataStatus_FutabaD.TICKET_PRINT      :        // チケット伝票発行
                tmpKeyCode = 43;
                break;
            case SendMeterDataStatus_FutabaD.AGGREGATE_PRINT:
                tmpKeyCode = 41;
                break;
            default:
                break;
        }

        send820_KeyCode(tmpReceiptTicketType, tmpKeyCode, false);
    }

    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知:WAON取引歴要求通知（フタバ双方向用）
     * @note   820への通知データを処理する：WAON取引歴要求通知
     * @param なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_WaonHistoryStart( ) {
        /*
            ＜ファンクション実行No.＞    750->820
            #define DFUTABA_FUNCNUM_WAON                   (928)  // WAON売上
            #define DFUTABA_FUNCNUM_WAON_ZANDAKA           (929)  // WAON残高照会
            #define DFUTABA_FUNCNUM_WAON_HISTORY           (930)  // WAON取引履歴照会

            ＜キーコード＞             820->メータ
            #define KEYCODE_WAON                           (936)    // WAON売上　←Vクレで確認済み
            #define KEYCODE_WAON_ZANDAKA                   (937)    // WAON残高照会　←Vクレで未確認
            #define KEYCODE_WAON_HISTORY                   (938)    // WAON取引履歴照会　←Vクレで確認済み
        * */

        Timber.i("[FUTABA-D]send820_WaonHistoryStart() ");
        int tmpKeyCode = 930;

        send820_KeyCode(SendMeterDataStatus_FutabaD.WAON_HISTORY, tmpKeyCode, true);
    }

    //ADD-S BMT S.Oyama 2025/02/26 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知:訂正キー応答通知（フタバ双方向用）
     * @note   820への通知データを処理する：訂正キー応答通知(ACKなし)
     * @param なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_TeiseiKeyNonAck( ) {
        Timber.i("[FUTABA-D]send820_TeiseiKeyNonAck() ");
        int tmpKeyCode = 21;
        send820_KeyCode(SendMeterDataStatus_FutabaD.GENERICTEISEIKEY_NONACK, tmpKeyCode, false);
    }

    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知:異常処理コード画面戻り通知（フタバ双方向用）
     * @note   820への通知データを処理する：異常処理コード画面戻り(ACKなし)
     * @param なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_GenericProcessCodeErrReturn( ) {
        Timber.i("[FUTABA-D]send820_GenericProcessCodeErrReturn( ) ");
        int tmpKeyCode = 940;
        send820_KeyCode(SendMeterDataStatus_FutabaD.GENERICPROCESSCODEERR_RETURN, tmpKeyCode, false);
    }
    //ADD-E BMT S.Oyama 2025/02/26 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2025/03/14 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知（フタバ双方向用）
     * @note   820への通知データを処理する：コード999を送ってメータリセット
     * @param なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_MeterRecovery_Keycode999_NonAck()
    {
        Timber.i("[FUTABA-D]send820_MeterRecovery_999_Keycode() method in");
        int tmpKeyCode = 999;
        send820_KeyCode(SendMeterDataStatus_FutabaD.METER_RECOVERY_CODE999, tmpKeyCode, false);
    }
    //ADD-E BMT S.Oyama 2025/03/14 フタバ双方向向け改修


    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知:キー入力通知：一般処理向け（フタバ双方向用）
     * @note   820に対して情報通知:キー入力通知：一般処理向け[本メソッドを起動時用には使用しないこと]
     * @param　int tmpPhase    作業フェーズ
     * @param　int tmpKeyCode  キーコード
     * @param　boolean isACKResult  ACK結果を返すかどうか
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_KeyCode(int tmpPhase, int tmpKeyCode, boolean isACKResult ) {
        Timber.i("[FUTABA-D]send820_KeyCode()  Phase:%d KeyCode:%d isACKResult:%d", tmpPhase, tmpKeyCode, isACKResult ? 1 : 0);

        if (_isConnected820 == false)            // 820への通信が切れた場合は実施しない
        {
            Timber.e("[FUTABA-D]send820_KeyCode():Not connected to 820. ");
            return;
        }

        killRetryTimerFutabaD();                // タイムアウト監視タイマを停止

        send820_ClearMeterDataClassMember();              //メンバ変数のクリア

        _sendMeterData_FutabaD.status = "0000";              //ステータスコード               000:正常
        _sendMeterData_FutabaD.meter_sub_cmd = 6;           //メーターサブコマンド        6:キー入力通知
        _sendMeterData_FutabaD.key_code = tmpKeyCode;       //キーコード

        _sendMeterDataStatus_General= SendMeterDataStatus_FutabaD.SENDING;          //メータデータ送信ステータス
        _sendMeterDataPhase_General = tmpPhase;                                     //メータデータ送信フェーズ

        if (isACKResult == true){                   //ACK結果がある場合
            sendWsMeterdata_FutabaD(PrinterConst.DuplexPrintResponseTimer);              // メーター情報を送信
        }
        else                                        //ACK結果がない場合
        {
            sendWsMeterdata_FutabaD(PrinterConst.DuplexMeterSendWaitTimerShortFutabaD);              // メーター情報を送信
        }
    }

    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知:ファンクション通知 ステータスエラー：一般処理向け（フタバ双方向用）
     * @note   820に対して情報通知:ファンクション通知 ステータスエラー：一般処理向け[本メソッドを起動時用には使用しないこと]
     * @param int tmpKeyCode  キーコード
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_FunctionCodeErrorResult(int tmpPhase, boolean isACKResult ) {
        Timber.i("[FUTABA-D]send820_FunctionCodeErrorResult()  Phase:%d isACKResult:%d", tmpPhase, isACKResult ? 1 : 0);

        //String tmpStatus = "";
        int tmpKeyCode = 0;

        switch(tmpPhase)
        {
            case SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_CREDIT:          //決済画面での中止要求：クレジット
            case SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_EMONEY:          //決済画面での中止要求：EMoney
            case SendMeterDataStatus_FutabaD.ACKERR_STATUS_SETTLEMENTABORT_QR:              //決済画面での中止要求：QR
            case SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB1:                             //割引種別：割引1の中止要求
            case SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB2:                             //割引種別：割引2の中止要求
            case SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB3:                             //割引種別：割引3の中止要求
            case SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB4:                             //割引種別：割引4の中止要求
            case SendMeterDataStatus_FutabaD.DISCOUNTTYPE_JOB5:                             //割引種別：割引5の中止要求
            case SendMeterDataStatus_FutabaD.SEPARATION_TIKECT_CANCEL:                      //分別チケット取り消し
            case SendMeterDataStatus_FutabaD.GENERICABORTCODE_NONACK:                       //汎用中止コード：ACKなし
            //case SendMeterDataStatus_FutabaD.GENERICABORTCODE_ACK:                          //一般中止コード：ACKあり
                //tmpStatus = "x59";      //エラーコード（取消キーによる中断）
                tmpKeyCode = 24;    //キーコード（取消キーによる中断）
                break;
        }

        //send820_FunctionCodeStatus(tmpPhase, tmpStatus, isACKResult);

        send820_KeyCode(tmpPhase, tmpKeyCode, isACKResult);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知:ファンクション通知：一般処理向け（フタバ双方向用）
     * @note   820に対して情報通知:ファンクション通知：一般処理向け[本メソッドを起動時用には使用しないこと]
     * @param int tmpPhase 作業フェーズ
     * @param String tmpStatus　返却ステータスコード
     * @param boolean isACKResult ACK結果を返すかどうか
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_FunctionCodeStatus(int tmpPhase, String tmpStatus, boolean isACKResult ) {
        Timber.i("[FUTABA-D]send820_FunctionCodeStatus()  Phase:%d Status:%s isACKResult:%d", tmpPhase, tmpStatus , isACKResult ? 1 : 0);

        if (_isConnected820 == false)            // 820への通信が切れた場合は実施しない
        {
            Timber.e("[FUTABA-D]send820_FunctionCodeStatus():Not connected to 820. ");
            return;
        }

        killRetryTimerFutabaD();                // タイムアウト監視タイマを停止

        send820_ClearMeterDataClassMember();              //メンバ変数のクリア

        _sendMeterData_FutabaD.status = tmpStatus;        //ステータス
        _sendMeterData_FutabaD.meter_sub_cmd = 9;         //メーターサブコマンド        9:ファンクション実行

        _sendMeterDataStatus_General= SendMeterDataStatus_FutabaD.SENDING;          //メータデータ送信ステータス
        _sendMeterDataPhase_General = tmpPhase;                                     //メータデータ送信フェーズ

        if (isACKResult == true){                   //ACK結果がある場合
            sendWsMeterdata_FutabaD(PrinterConst.DuplexPrintResponseTimer);              // メーター情報を送信
        }
        else                                        //ACK結果がない場合
        {
            sendWsMeterdata_FutabaD(PrinterConst.DuplexMeterSendWaitTimerShortFutabaD);              // メーター情報を送信
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    /******************************************************************************/
    /*!
     * @brief  メータデータ通知（フタバ双方向用）
     * @note   PT750->IM820へのメータデータ通知 /printdata/v3:meter_data
     * @param [in] なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    private void sendWsMeterdata_FutabaD(int tmpTimeOut) {

        Timber.i("[FUTABA-D]sendWsMeterdata_FutabaD()  TimeOut:%d ", tmpTimeOut);

        if(_sendMeterData_FutabaD == null)      //メータデータ通知データがNULLの場合
        {
            Timber.tag("IFBoxManager").e("[FUTABA-D]%s：sendWsMeterdata_FutabaD::_sendMeterData_FutabaD is NULL", "820通信異常");
            return;
        }

        JSONObject _params = new JSONObject();
        JSONObject _sendData = new JSONObject();
        try {
            _sendData.put("type", "/printdata/v3");
            _sendData.put("cmd","meter_data");
            if(_sendMeterData_FutabaD.key_code == 22) { //再印刷
                _sendData.put("timer",PrinterConst.DuplexPrintResponseTimer);           //25秒
            } else {
                _sendData.put("timer",PrinterConst.DuplexMeterSendWaitTimerFutabaD);    //10秒
            }

            _params.put("meter_sub_cmd",    _sendMeterData_FutabaD.meter_sub_cmd);
            _params.put("status",           _sendMeterData_FutabaD.status);
            _params.put("status_code",      _sendMeterData_FutabaD.status_code);
            _params.put("term_ver",         _sendMeterData_FutabaD.term_ver);
            _params.put("car_id",           _sendMeterData_FutabaD.car_id);
            _params.put("input_kingaku",    _sendMeterData_FutabaD.input_kingaku);
            _params.put("key_code",         _sendMeterData_FutabaD.key_code);
            _params.put("trans_date",       _sendMeterData_FutabaD.trans_date);
            _params.put("discount_way",     _sendMeterData_FutabaD.discount_way);
            _params.put("discount_type",    _sendMeterData_FutabaD.discount_type);
            _params.put("exp_date",         _sendMeterData_FutabaD.exp_date);
            _params.put("adr_l2",           _sendMeterData_FutabaD.adr_l2);
            _params.put("adr_l3",           _sendMeterData_FutabaD.adr_l3);
            _params.put("adr_input1",       _sendMeterData_FutabaD.adr_input1);
            _params.put("adr_input2",       _sendMeterData_FutabaD.adr_input2);
            _params.put("if_ver",           _sendMeterData_FutabaD.if_ver);

            // パラメータの格納
            _sendData.put("data", _params);
            sendFutabaD(_sendData.toString(), tmpTimeOut);
        } catch (Exception e) {
            Timber.tag("IFBoxManager").e("%s：sendWsMeterdata_FutabaD->Exception e <%s>", "820通信異常", e);
            e.printStackTrace();
        }
    }
    //ADD-E BMT S.Oyama 2024/09/03 フタバ双方向向け改修

    private void meterStatNotice(Meter.ResponseStatus meter) {
        if (meterStatNoticeDisposable != null) {
            _meterStatNotice.onNext(meter);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void exitManualMode(boolean flg) {
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL)) {
            // 手動決済モードを終了
            Timber.i("手動決済モード終了");
            Version.Response ifboxVersionInfo = AppPreference.getIFBoxVersionInfo();
            ifboxVersionInfo.appModel = IFBoxAppModels.FUTABA_D;
            AppPreference.setIFBoxVersionInfo(ifboxVersionInfo);
            AppPreference.setIsTemporaryManualMode(false);

            if (exitManualModeDisposable != null) {
                _exitManualMode.onNext(flg);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void printEndManual(boolean flg) {
        if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL)) {
            // 手動決済モードを終了
            Timber.i("手動決済モード終了");
            if (printEndManualDisposable != null) {
                _printEndManual.onNext(flg);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void updatePrintStatus(Meter.ResponseYazaki printer) {
        if(null != printer.status) {
            PrinterProc printerProc = PrinterProc.getInstance();

            //ADD-S BMT S.Oyama 2024/10/25 フタバ双方向向け改修
            if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D) == false)
            {
                printerProc.Printing_Duplex(printer.status,printer.cont,printer.err_cd);
            }
            else
            {
                Meter.ResponseFutabaD meter = new Meter.ResponseFutabaD();                              //print_start系のackが返ってきたら
                meter.meter_sub_cmd = 5;
                meter.sound_no = Send820Status_JobReq_FutabaD.JOBREQ_V3_PRINTSTART_RECV;
                _meterDataV4.onNext(meter);

                printerProc.Printing_DuplexFutabaD(printer.status,printer.cont,printer.err_cd);
                if (printer.status.equals("OK") == true) {
                    Timber.i("[FUTABA-D]updatePrintStatus() Printing_Duplex[/printdata/v1::print_end] :status:%s cont:%d, err_cd:%d",
                            printer.status, printer.cont, printer.err_cd
                    );
                }
                else
                {
                    Timber.e("[FUTABA-D]updatePrintStatus() Printing_Duplex[/printdata/v1::print_end] :status:%s cont:%d, err_cd:%d",
                            printer.status, printer.cont, printer.err_cd
                    );
                }
            }
            //ADD-E BMT S.Oyama 2024/10/25 フタバ双方向向け改修
        }
    }

    //ADD-S BMT S.Oyama 2024/11/01 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  印刷結果通知（フタバ双方向用）
     * @note   PT750<-IM820への印刷結果通知 /printdata/v2:print_end
     * @param [in] Meter.ResponseFutabaDPrintEnd printer
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
//    @RequiresApi(api = Build.VERSION_CODES.N)
//    private void updatePrintStatusFutabaDV2( Meter.ResponseFutabaDPrintEnd printer)
//    {
//        if(null != printer.status) {
//            PrinterProc printerProc = PrinterProc.getInstance();
//            printerProc.Printing_DuplexFutabaD(printer.status,printer.cont,printer.err_cd_str);
//
//            if (printer.status.equals("0") == true) {
//                Timber.i("[FUTABA-D]updatePrintStatus() Printing_Duplex[/printdata/v1::print_end] :status:%s cont:%d, err_cd:%d",
//                        printer.status, printer.cont, printer.err_cd
//                );
//            }
//            else
//            {
//                Timber.e("[FUTABA-D]updatePrintStatus() Printing_Duplex[/printdata/v1::print_end] :status:%s cont:%d, err_cd:%d",
//                        printer.status, printer.cont, printer.err_cd
//                );
//            }
//        }
//    }
    //ADD-E BMT S.Oyama 2024/11/01 フタバ双方向向け改修

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void printException() {
        PrinterProc printerProc = PrinterProc.getInstance();
        printerProc.Printing_Duplex_Exception(PrinterConst.DuplexPrintStatus_IFBOX_PRINTERROR);
    }

    private void sleep(long millis) {
        try { Thread.sleep(millis); } catch (Exception ignore) {}
    }

    public boolean isConnected() {
        return _wsClient.getConnectionStatus().getValue() == IFBoxWebSocketClient.ConnectionStatus.Connected;
    }

    public void checkConnection() {
        if (_connectCheckWorking) return;
        else _connectCheckWorking = true;

        DeviceServiceInfo info = _deviceNetworkManager.getDeviceServiceInfo().getValue();
        _connectCheckWorking = true;
        final Version.Response version = getVersion();

        if (version != null) {
            //Timber.d("getVersion() Success :%s", version);
            Timber.i("getVersion() Success :%s", version);
            if (_wsClient.getConnectionStatus().getValue() == IFBoxWebSocketClient.ConnectionStatus.Disconnected) {
                //Timber.d("exec ws connect");
                Timber.i("exec ws connect");
                _wsClient.connect("ws://" + info.getWebSocketAddress() + "/ws");

                //ADD-S BMT S.Oyama 2024/09/26 フタバ双方向向け改修
                if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D)) {          //フタバ双方向時
                    Timber.i("[FUTABA-D]checkConnection() disconnect 820");
                    _isConnected820 = false;                                                     //820接続状態を未接続にする
                    _sendMeterDataStatus_ColdStart = SendMeterDataStatus_FutabaD.NONE;           //メータデータ送信ステータス
                    _sendMeterDataPhase_ColdStart = SendMeterDataStatus_FutabaD.NONE;                           //メータデータ送信フェーズ
                }
                //ADD-E BMT S.Oyama 2024/09/26 フタバ双方向向け改修
            }
            _connectCheckWorking = false;
        } else {
            Timber.d("getVersion() Failure");
            Observable
                    .timer(10, TimeUnit.SECONDS)
                    .doOnSubscribe(_disposer::addTo)
                    .subscribe(t -> {
                        // 外stopがかかっていた場合は再帰実行はしない
                        if (_connectCheckWorking) {
                            // そのまま呼ぶと即リターンしてしまうので実行前にfalseにする
                            _connectCheckWorking = false;
                            checkConnection();
                        }
                    });
        }
    }

    public Version.Response getVersion() {
        try {
            return (Version.Response) Single.create(emitter -> {
                try {
                    final Version.Response version = _apiClient.getVersion();
// TODO M.Kodama-S 【IM-A820がオカベ対応後に削除】オカベ双方向：試験用に、強制的にオカベ双方向にする
                    //version.appModel = IFBoxAppModels.OKABE_MS70_D;
// TODO M.Kodama-E 【IM-A820がオカベ対応後に削除】オカベ双方向：試験用に、強制的にオカベ双方向にする

//ADD-S BMT S.Oyama 2024/09/10 フタバ双方向向け改修   テスト向けコード．実稼働時は削除
                    //version.appModel = IFBoxAppModels.FUTABA_D;
                    //version.appModel = IFBoxAppModels.FUTABA;
//ADD-E BMT S.Oyama 2024/09/10 フタバ双方向向け改修

                    if (IFBoxAppModels.isMatch(IFBoxAppModels.FUTABA_D_MANUAL) != true)
                    {
                        AppPreference.setIFBoxVersionInfo(version);
                        if (!emitter.isDisposed()) {
                            emitter.onSuccess(version);
                        }
                    }
                } catch (Exception e) {
                    Timber.e(e);
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }
            }).subscribeOn(Schedulers.io()).blockingGet();
        } catch (Exception ignore) {
            return null;
        }
    }

    public boolean isAccOn_IMA820() {
        return _wsClient.isAccOn_IMA820();
    }

    //ADD-S BMT S.Oyama 2024/11/25 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知:自動日報メニュー メニュー入り（フタバ双方向用）
     * @note   820への通知データを処理する：自動日報メニュー メニュー入り
     * @param 無し
     * @param
     * @param
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_AutoDailyRepotIn( ) {
        Timber.i("[FUTABA-D]send820_AutoDailyRepotIn() ");

        if (_isConnected820 == false)            // 820への通信が切れた場合は実施しない
        {
            Timber.e("[FUTABA-D]send820_AutoDailyRepotIn():Not connected to 820. ");
            return;
        }

        killRetryTimerFutabaD();                // タイムアウト監視タイマを停止

        send820_ClearMeterDataClassMember();              //メンバ変数のクリア

        _sendMeterData_FutabaD.status           = "000";
        _sendMeterData_FutabaD.meter_sub_cmd    = 13;                   //メーターサブコマンド        13:自動日報
        _sendMeterData_FutabaD.adr_l2           = 0;                    //自動日報モード移行

        _sendMeterDataStatus_General= SendMeterDataStatus_FutabaD.SENDING;                          //メータデータ送信ステータス
        _sendMeterDataPhase_General = SendMeterDataStatus_FutabaD.AUTODAILYREPORT_IN;               //メータデータ送信フェーズ

        sendWsMeterdata_FutabaD(PrinterConst.DuplexPrintResponseTimer);              // メーター情報を送信  ACKあり
    }

    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知:自動日報メニュー メニュー出（フタバ双方向用）
     * @note   820への通知データを処理する：自動日報メニュー メニュー出
     * @param なし
     * @param
     * @param
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_AutoDailyRepotOut( ) {
        Timber.i("[FUTABA-D]send820_AutoDailyRepotOut() ");

        if (_isConnected820 == false)            // 820への通信が切れた場合は実施しない
        {
            Timber.e("[FUTABA-D]send820_AutoDailyRepotOut():Not connected to 820. ");
            return;
        }

        killRetryTimerFutabaD();                // タイムアウト監視タイマを停止

        send820_ClearMeterDataClassMember();              //メンバ変数のクリア

        _sendMeterData_FutabaD.status           = "000";
        _sendMeterData_FutabaD.meter_sub_cmd    = 13;                   //メーターサブコマンド        13:自動日報
        _sendMeterData_FutabaD.adr_l2           = 8;                    //自動日報モード解除

        _sendMeterDataStatus_General= SendMeterDataStatus_FutabaD.SENDING;                          //メータデータ送信ステータス
        _sendMeterDataPhase_General = SendMeterDataStatus_FutabaD.AUTODAILYREPORT_OUT;              //メータデータ送信フェーズ

        sendWsMeterdata_FutabaD(PrinterConst.DuplexMeterSendWaitTimerShortFutabaD);              // メーター情報を送信  ACKなし
    }

    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知:自動日報 燃料入力入り（フタバ双方向用）
     * @note   820への通知データを処理する：自動日報 燃料入力入り
     * @param 無し
     * @param
     * @param
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_AutoDailyRepotFuelIn( ) {
        Timber.i("[FUTABA-D]send820_AutoDailyRepotFuelIn() ");

        if (_isConnected820 == false)            // 820への通信が切れた場合は実施しない
        {
            Timber.e("[FUTABA-D]send820_AutoDailyRepotFuelIn():Not connected to 820. ");
            return;
        }

        int tmpKeyCode = 31;

        send820_KeyCode(SendMeterDataStatus_FutabaD.AUTODAILYREPORT_FUEL_IN, tmpKeyCode, false);
    }

    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知:自動日報 燃料入力解除：戻るボタン押下（フタバ双方向用）
     * @note   820への通知データを処理する：自動日報 燃料入力解除：戻るボタン押下
     * @param 無し
     * @param
     * @param
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_AutoDailyRepotFuelOut( ) {
        Timber.i("[FUTABA-D]send820_AutoDailyRepotFuelOut() ");

        if (_isConnected820 == false)            // 820への通信が切れた場合は実施しない
        {
            Timber.e("[FUTABA-D]send820_AutoDailyRepotFuelOut():Not connected to 820. ");
            return;
        }

        killRetryTimerFutabaD();                // タイムアウト監視タイマを停止

        send820_ClearMeterDataClassMember();              //メンバ変数のクリア

        _sendMeterData_FutabaD.status           = "000";
        _sendMeterData_FutabaD.meter_sub_cmd    = 13;                   //メーターサブコマンド        13:自動日報
        _sendMeterData_FutabaD.adr_l2           = 2;                    //2燃料
        _sendMeterData_FutabaD.adr_l3           = 3;                    //3入力解除

        _sendMeterDataStatus_General= SendMeterDataStatus_FutabaD.SENDING;                          //メータデータ送信ステータス
        _sendMeterDataPhase_General = SendMeterDataStatus_FutabaD.AUTODAILYREPORT_FUEL_OUT;         //メータデータ送信フェーズ

        sendWsMeterdata_FutabaD(PrinterConst.DuplexPrintResponseTimer);              // メーター情報を送信  ACKあり
    }

    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知:自動日報 燃料入力終了（フタバ双方向用）
     * @note   820への通知データを処理する：自動日報 燃料入力終了
     * @param int tmpFuelValue
     * @param
     * @param
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_AutoDailyRepotFuelInput(int tmpFuelValue ) {
        Timber.i("[FUTABA-D]send820_AutoDailyRepotFuelInput() ");

        if (_isConnected820 == false)            // 820への通信が切れた場合は実施しない
        {
            Timber.e("[FUTABA-D]send820_AutoDailyRepotFuelInput():Not connected to 820. ");
            return;
        }

        killRetryTimerFutabaD();                // タイムアウト監視タイマを停止

        send820_ClearMeterDataClassMember();              //メンバ変数のクリア

        _sendMeterData_FutabaD.status           = "000";
        _sendMeterData_FutabaD.meter_sub_cmd    = 13;                   //メーターサブコマンド        13:自動日報
        _sendMeterData_FutabaD.adr_l2           = 2;                    //2燃料
        _sendMeterData_FutabaD.adr_l3           = 4;                    //4入力終了
        _sendMeterData_FutabaD.adr_input1       = tmpFuelValue;         //入力値

        _sendMeterDataStatus_General= SendMeterDataStatus_FutabaD.SENDING;                          //メータデータ送信ステータス
        _sendMeterDataPhase_General = SendMeterDataStatus_FutabaD.AUTODAILYREPORT_FUEL_INPUT;       //メータデータ送信フェーズ

        sendWsMeterdata_FutabaD(PrinterConst.DuplexPrintResponseTimer);              // メーター情報を送信  ACKあり
    }

    /******************************************************************************/
    /*!
     * @brief  820に対して情報通知:自動日報 燃料クリア（フタバ双方向用）
     * @note   820への通知データを処理する：自動日報 燃料クリア
     * @param int tmpFuelValue
     * @param
     * @param
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_AutoDailyRepotFuelClear() {
        Timber.i("[FUTABA-D]send820_AutoDailyRepotFuelClear() ");

        if (_isConnected820 == false)            // 820への通信が切れた場合は実施しない
        {
            Timber.e("[FUTABA-D]send820_AutoDailyRepotFuelInput():Not connected to 820. ");
            return;
        }

        killRetryTimerFutabaD();                // タイムアウト監視タイマを停止

        send820_ClearMeterDataClassMember();              //メンバ変数のクリア

        _sendMeterData_FutabaD.status           = "000";
        _sendMeterData_FutabaD.meter_sub_cmd    = 13;                   //メーターサブコマンド        13:自動日報
        _sendMeterData_FutabaD.adr_l2           = 2;                    //2燃料
        _sendMeterData_FutabaD.adr_l3           = 2;                    //2訂正

        _sendMeterDataStatus_General= SendMeterDataStatus_FutabaD.SENDING;                          //メータデータ送信ステータス
        _sendMeterDataPhase_General = SendMeterDataStatus_FutabaD.AUTODAILYREPORT_FUEL_CLEAR;       //メータデータ送信フェーズ

        sendWsMeterdata_FutabaD(PrinterConst.DuplexPrintResponseTimer);              // メーター情報を送信  ACKあり
    }
    //ADD-E BMT S.Oyama 2024/11/25 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2024/12/16 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  820ファームアップデート処理開始
     * @note   820ファームアップデート処理開始
     * @param なし
     * @param
     * @param
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void updateFirm820_Start() {
        Timber.i("[FUTABA-D]!! updateFirm820_Start() ");

        _is820FirmUpdatingFL = true;        //ファームアップデートフラグをON
        updateFirm820_TimerKill();
    }

    /******************************************************************************/
    /*!
     * @brief  820ファームアップデート処理終了
     * @note   820ファームアップデート処理終了
     * @param なし
     * @param
     * @param
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void updateFirm820_Ended() {
        Timber.i("[FUTABA-D]!! updateFirm820_Ended() ");

        _is820FirmUpdatingFL = false;           //ファームアップデートフラグをOFF

        _isConnected820 = false;                //接続を切る
    }

    /******************************************************************************/
    /*!
     * @brief  820ファームアップデート：アップデート中の/aliveタイマを殺す
     * @note   820ファームアップデート：アップデート中の/aliveタイマを殺す
     * @param なし
     * @param
     * @param
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void updateFirm820_TimerKill() {
        Timber.i("[FUTABA-D]updateFirm820_TimerKill()");

        if (null != wsAliveTimer) {
            wsAliveTimer.cancel();
            wsAliveTimer = null;
        }
    }

    //ADD-E BMT S.Oyama 2024/12/16 フタバ双方向向け改修

    /******************************************************************************/
    /*!
     * @brief  820に対して再印刷用キーコード通知（フタバ双方向用）
     * @note   820へ再印刷用キーコードを通知
     * @param なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_Reprint_KeyCode()
    {
        Timber.i("[FUTABA-D]send820_Reprint_KeyCode");

        _sendMeterData_FutabaD.status =  "000";
        _sendMeterData_FutabaD.meter_sub_cmd = 6;           //メーターサブコマンド        6:キー入力通知
        _sendMeterData_FutabaD.key_code = 22;               //キーコード                22:セットキー

        _sendMeterDataStatus_General = SendMeterDataStatus_FutabaD.SENDING;           //メータデータ送信ステータス
        _sendMeterDataPhase_General = SendMeterDataStatus_FutabaD.REPRINT_PRINT;                           //伝票再印刷送信フェーズ

        sendWsMeterdata_FutabaD(PrinterConst.DuplexPrintResponseTimer);              // メーター情報を送信
    }

    /******************************************************************************/
    /*!
     * @brief  820に対して分別チケット時処理完了のキーコード通知（フタバ双方向用）
     * @note   820に対して分別チケット時処理完了のキーコード通知
     * @param なし
     * @retval なし
     * @return
     * @private
     */
    /******************************************************************************/
    public void send820_SeparateTicketJobFix_KeyCode()
    {
        Timber.i("[FUTABA-D]send820_SeparateTicketJobFix_KeyCode");

        _sendMeterData_FutabaD.status =  "000";
        _sendMeterData_FutabaD.meter_sub_cmd = 6;           //メーターサブコマンド        6:キー入力通知
        _sendMeterData_FutabaD.key_code = 22;               //キーコード                22:セットキー

        _sendMeterDataStatus_General= SendMeterDataStatus_FutabaD.SENDING;                     //メータデータ送信ステータス
        _sendMeterDataPhase_General = SendMeterDataStatus_FutabaD.SEPARATION_TIKECT_FIX;       //メータデータ送信フェーズ

        sendWsMeterdata_FutabaD(PrinterConst.DuplexMeterSendWaitTimerShortFutabaD);              // メーター情報を送信
    }

    //ADD-S BMT S.Oyama 2025/02/26 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  820から送られてくる処理コード表示要求　コードがエラー系か返す（フタバ双方向用）
     * @note   820から送られてくる処理コード表示要求　コードがエラー系か返す
     * @param String tmpProcessCD 820から送られてくる処理コード（フタバD専用）
     * @retval なし
     * @return int エラーコード系の場合は　<0  ==0の場合はエラー系ではない
     * @private
     */
    /******************************************************************************/
    public int send820_IsProcessCode_ErrorCD(String tmpProcessCD) {
        int result = 0;

        if (_futabaDMeterProcessCodeDic.containsKey(tmpProcessCD) == true) {
            result = _futabaDMeterProcessCodeDic.get(tmpProcessCD);
        }

        return result;
    }

    /******************************************************************************/
    /*!
     * @brief  820から送られてくる処理コード表示要求　要求キーとエラー詳細情報を分割して返す（フタバ双方向用）
     * @note   820から送られてくる処理コード表示要求　要求キーとエラー詳細情報を分割して返す
     * @param String tmpInput　820から送られてくる要求キーコード＆エラー詳細情報（フタバD専用）
     * @retval なし
     * @return String[] [0]:要求キー [1]:エラー詳細情報
     * @private
     */
    /******************************************************************************/
    public String[] send820_KeyAndErrDetail(String tmpInput) {
        // カンマで分割
        String[] parts = tmpInput.split(",", 2);

        // もしカンマが含まれていなかった場合、2つ目の要素を空文字にする
        if (parts.length == 1) {
            return new String[]{parts[0], ""};
        }
        return parts;
    }
    //ADD-E BMT S.Oyama 2025/02/26 フタバ双方向向け改修

    //ADD-S BMT S.Oyama 2025/03/27 フタバ双方向向け改修
    /******************************************************************************/
    /*!
     * @brief  820から送られてくる処理コード表示要求 FREEメッセージ16進文字列(中身はSJIS)をJAVA Stringへ変換（フタバ双方向用）
     * @note   最大192文字 + 区切りの改行コード2文字を想定
     * @param String tmpInput　16進文字列　最大2000文字（フタバD専用）
     * @retval なし
     * @return String 変換後の文字列
     * @private
     */
    /******************************************************************************/
    public String convertJavaStringAryFrom820SJisHex(String tmpInput) {
        String tmpResult = "";

        if (tmpInput.equals("") == true)                //空文字の場合は打ち切る
        {
            return tmpResult;
        }

        byte[] bytes = new byte[256];                  //受取用のバッファを作成

        int tmpDestIdx = 0;
        String tmpst = "";
        for (int i = 0; i < tmpInput.length(); i += 2) {            //
            tmpst =  String.valueOf(tmpInput.charAt(i));        //Hex1文字目
            if ((i + 1) >= tmpInput.length())                   //Hex2文字目が渡した文字列長より長いとき
            {
                tmpst += "0";                                   //0を付加
            }
            else
            {
                tmpst += String.valueOf(tmpInput.charAt(i + 1));    //Hex2文字目
            }

            try {
                bytes[tmpDestIdx] = (byte) Integer.parseInt(tmpst, 16);     //１６進文字列を１バイトへ変換
            }
            catch (NumberFormatException e) {
                return tmpResult;
            }

            tmpDestIdx++;                       //バイト配列のidxを加算
            if (tmpDestIdx >= (bytes.length - 1))     //バッファサイズより大きくなったら抜ける
            {
                break;
            }
        }

        int tmpNullStartIdx = 0;
        for(int i = 0; i < 256; i++)
        {
            if (bytes[i] == 0)
            {
                tmpNullStartIdx = i;
                break;
            }

        }

        if(tmpNullStartIdx == 0)
        {
            return "";
        }

        byte[] trimmedBytes = Arrays.copyOf(bytes, tmpNullStartIdx);

        tmpResult = new String(trimmedBytes, Charset.forName("Shift_JIS"));            //Java Stringへ変換

        return tmpResult;
    }
    //ADD-E BMT S.Oyama 2025/03/27 フタバ双方向向け改修
}

//    public void send820_AutoDailyRepotFuelIn( ) {
//        Timber.i("[FUTABA-D]send820_AutoDailyRepotFuelIn() ");
//
//        if (_isConnected820 == false)            // 820への通信が切れた場合は実施しない
//        {
//            Timber.e("[FUTABA-D]send820_AutoDailyRepotFuelIn():Not connected to 820. ");
//            return;
//        }
//
//        killRetryTimerFutabaD();                // タイムアウト監視タイマを停止
//
//        send820_ClearMeterDataClassMember();              //メンバ変数のクリア
//
//        _sendMeterData_FutabaD.status           = "000";
//        _sendMeterData_FutabaD.meter_sub_cmd    = 13;                   //メーターサブコマンド        13:自動日報
//        _sendMeterData_FutabaD.adr_l2           = 2;                    //2燃料
//        _sendMeterData_FutabaD.adr_l3           = 1;                    //1入力
//
//        _sendMeterDataStatus_General= SendMeterDataStatus_FutabaD.SENDING;                          //メータデータ送信ステータス
//        _sendMeterDataPhase_General = SendMeterDataStatus_FutabaD.AUTODAILYREPORT_FUEL_IN;          //メータデータ送信フェーズ
//
//        sendWsMeterdata_FutabaD(PrinterConst.DuplexPrintResponseTimer);              // メーター情報を送信  ACKあり
//    }
//
//    public void send820_AutoDailyRepotFuelOut( ) {
//        Timber.i("[FUTABA-D]send820_AutoDailyRepotFuelOut() ");
//
//        if (_isConnected820 == false)            // 820への通信が切れた場合は実施しない
//        {
//            Timber.e("[FUTABA-D]send820_AutoDailyRepotFuelOut():Not connected to 820. ");
//            return;
//        }
//
//        killRetryTimerFutabaD();                // タイムアウト監視タイマを停止
//
//        send820_ClearMeterDataClassMember();              //メンバ変数のクリア
//
//        _sendMeterData_FutabaD.status           = "000";
//        _sendMeterData_FutabaD.meter_sub_cmd    = 13;                   //メーターサブコマンド        13:自動日報
//        _sendMeterData_FutabaD.adr_l2           = 2;                    //2燃料
//        _sendMeterData_FutabaD.adr_l3           = 3;                    //3入力解除
//
//        _sendMeterDataStatus_General= SendMeterDataStatus_FutabaD.SENDING;                          //メータデータ送信ステータス
//        _sendMeterDataPhase_General = SendMeterDataStatus_FutabaD.AUTODAILYREPORT_FUEL_OUT;         //メータデータ送信フェーズ
//
//        sendWsMeterdata_FutabaD(PrinterConst.DuplexPrintResponseTimer);              // メーター情報を送信  ACKあり
//    }
