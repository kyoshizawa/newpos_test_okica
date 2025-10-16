package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaChip.FeliCaChip;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAPTest.iCASClientTest;
import okhttp3.CookieJar;

public class FeliCaClient implements IFeliCaClient, Runnable {

    // FcErrorCodeBaseより移植した定数群
    ////////////////////////////////////////////////////////////////////////
    public static final int FC_ERR_SUCCESS               = 0;                   // エラーなし

    public static final int FC_ERR_CLNT_MODULE_BASE      = 0x80010100;          // FeliCaClientモジュールエラー開始位置
//    public static final int FC_ERR_THRD_MODULE_BASE      = 0x80010200;          // Threadモジュールエラー開始位置
    public static final int FC_ERR_TCAP_MODULE_BASE      = 0x80010400;          // TCAPメッセージモジュールエラー開始位置
//    public static final int FC_ERR_HTTP_MODULE_BASE      = 0x80010800;          // HTTPモジュールエラー開始位置
    ////////////////////////////////////////////////////////////////////////
    // FeliCaClientErrorCodeより移植した定数群
    public static final int FC_ERR_CLIENT_UNKNOWN        = FC_ERR_CLNT_MODULE_BASE + 0x01;          // 不明なエラー
    public static final int FC_ERR_CLIENT_BADPARAM       = FC_ERR_CLNT_MODULE_BASE + 0x02;          // パラメータ不正
    public static final int FC_ERR_CLIENT_NO_MEMORY      = FC_ERR_CLNT_MODULE_BASE + 0x03;          // メモリ不足
    public static final int FC_ERR_CLIENT_FATAL          = FC_ERR_CLNT_MODULE_BASE + 0x04;          // 致命的内部エラー
//    public static final int FC_ERR_CLIENT_PACKET         = FC_ERR_CLNT_MODULE_BASE + 0x05;          // パケットエラー
    public static final int FC_ERR_CLIENT_FAILURE        = FC_ERR_CLNT_MODULE_BASE + 0x06;          // 処理失敗
    public static final int FC_ERR_CLIENT_HTTP           = FC_ERR_CLNT_MODULE_BASE + 0x07;          // HTTP通信エラー
    public static final int FC_ERR_CLIENT_CANCEL         = FC_ERR_CLNT_MODULE_BASE + 0x08;          // キャンセルにより終了
    public static final int FC_ERR_CLIENT_PROTOCOL       = FC_ERR_CLNT_MODULE_BASE + 0x09;          // プロトコルエラー
//    public static final int FC_ERR_CLIENT_SERVER         = FC_ERR_CLNT_MODULE_BASE + 0x0A;          // サーバーエラーを検出
    public static final int FC_ERR_CLIENT_SEQUENCE       = FC_ERR_CLNT_MODULE_BASE + 0x0B;          // シーケンスエラー

    public static final int FC_ERR_CLIENT_MSG_UNEXPECTED = FC_ERR_CLNT_MODULE_BASE + 0x10;          // UnexpectedError発生
    public static final int FC_ERR_CLIENT_MSG_FORMAT     = FC_ERR_CLNT_MODULE_BASE + 0x11;          // PacketFormatError発生
    public static final int FC_ERR_CLIENT_MSG_ILLEGAL    = FC_ERR_CLNT_MODULE_BASE + 0x12;          // IllegalStateError発生
    public static final int FC_ERR_CLIENT_MSG_SERVER     = FC_ERR_CLNT_MODULE_BASE + 0x13;          // サーバからエラーパケットが来た
    public static final int FC_ERR_CLIENT_MSG_TCAP_VER   = FC_ERR_CLNT_MODULE_BASE + 0x14;          // サーバがTCAP2.5未対応

//    private Handler _handlerToMainThread;                           // メインスレッドへのハンドラ
    private static FeliCaClient _instance;                          // インスタンス
    private Thread _thread;                                         // スレッド
    private String _url;                                            // サーバURL
//    private CookieJar _cookiejar;                                   // cookie
    private final TCAPContext _tcapContext;                         // TCAP通信コンテキスト
    private IFeliCaClientEventListener _iFeliCaClientEventListener; // 通信終了コールバックI/F
    private ClientStateMachine _stateMachine;                       // 通信状態管理
    private final TCAPCommunicationAgent _tlamCommAgent;            // TLAM通信エージェント
    private final TCAPCommunicationAgent _tcapCommAgent;            // TCAP通信エージェント
    private boolean _bUseParam;                                     // TLAM通信をするかどうか
    private byte[] _tlamPostData;                                   // TLAM通信POSTデータ
    private JSONObject _params;                                     // 業務パラメータ
    private File _clientCertificateFile;                            // クライアント証明書
    private char[] _jremPassword;                                   // JREM Password
    /*
        private long _tlamPostDataLength;                               // TLAM通信POSTデータ長
    */
    private boolean _bIsStarted;                                    // サーバとの通信処理中かどうか
    private boolean _bNoneChipAccess;                               // チップアクセスしない業務
/*
    private static final HashMap<Integer, Integer> _errorMap = new HashMap<>();   // FeliCaクライアント内部エラーとアプリケーションに返却するエラーの対応テーブル

    static {
        _errorMap.put(FC_ERR_CLIENT_HTTP,           IFeliCaClientEventListener.errorType.TYPE_HTTP.getInt());
        _errorMap.put(FC_ERR_CLIENT_NO_MEMORY,      IFeliCaClientEventListener.errorType.TYPE_MEMORY.getInt());
        _errorMap.put(FC_ERR_CLIENT_CANCEL,         IFeliCaClientEventListener.errorType.TYPE_CANCEL.getInt());
        _errorMap.put(FC_ERR_CLIENT_MSG_SERVER,     IFeliCaClientEventListener.errorType.TYPE_PROTOCOL.getInt());
        _errorMap.put(FC_ERR_CLIENT_MSG_UNEXPECTED, IFeliCaClientEventListener.errorType.TYPE_PROTOCOL.getInt());
        _errorMap.put(FC_ERR_CLIENT_MSG_FORMAT,     IFeliCaClientEventListener.errorType.TYPE_PROTOCOL.getInt());
        _errorMap.put(FC_ERR_CLIENT_MSG_ILLEGAL,    IFeliCaClientEventListener.errorType.TYPE_PROTOCOL.getInt());
        _errorMap.put(FC_ERR_CLIENT_PROTOCOL,       IFeliCaClientEventListener.errorType.TYPE_PROTOCOL.getInt());
        _errorMap.put(FC_ERR_CLIENT_MSG_TCAP_VER,   IFeliCaClientEventListener.errorType.TYPE_PROTOCOL.getInt());
    }
*/

    private FeliCaClient() {
        _thread = null;
        _iFeliCaClientEventListener = null;
        _stateMachine = null;
        _bUseParam = false;
        _tlamPostData = null;
/*
        _tlamPostDataLength = 0;
*/
        _bIsStarted = false;
        _tcapContext = new TCAPContext();
        _tlamCommAgent = new TCAPCommunicationAgent();
        _tcapCommAgent = new TCAPCommunicationAgent();
        _url = "";
        _bNoneChipAccess = false;
    }

    public static FeliCaClient getInstance() {
        if(_instance == null) {
            _instance = new FeliCaClient();
        }
        return _instance;
    }

    @Override
    public void SetUrl(final String url) {
        if(url == null) {
            // 指定URLがNULL
        } else {
            _url = url;
        }
    }

    @Override
    public void SetCookie(final CookieJar cookiejar)
    {
//        _cookiejar = cookiejar;
    }

    @Override
    public String GetUrl() {
        return _url;
    }

    @Override
    public long Start(boolean bUseParam, final byte[] data, int dataLength) {
        long lRetVal = errorCode.ERR_UNKNOWN.getInt();

/*
        // ワーカースレッドからの通知受信処理（メインスレッドへ通知が必要な場合、ここで受信する）
        _handlerToMainThread = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what) {
                    case FC_MSG_TO_MAIN_ON_FINISH:
                        // 処理成功 正常終了コールバック実行
                        _iFeliCaClientEventListener.OnFinished(_tcapContext.GetReturnCode());
                        break;
                    case FC_MSG_TO_MAIN_ON_ERROR:
                        // 処理失敗 エラーコールバック実行
                        String errorMessage = new String(_tcapContext.GetErrorMessage());
                        _iFeliCaClientEventListener.OnErrorOccurred(GetErrorType(msg.arg1), errorMessage);
                        break;
                    case FC_MSG_TO_MAIN_ON_OPERATE:     // デバイス操作要求
                        if(msg.obj instanceof IDevice.DeviceOperate) {
                            IDevice.DeviceOperate deviceOperate = (IDevice.DeviceOperate)msg.obj;
                            _iFeliCaClientEventListener.OnDeviceOperate(deviceOperate);
                        }
                        break;
                    default:
                        break;
                }
            }
        };
*/

        if(_bIsStarted) {
            // 既に起動中
            return errorCode.ERR_ALREADY_STARTED.getInt();
        }

        // スレッド生成
        _thread = new Thread(this);

        // TLAMPOSTデータ用バッファクリア
        _tlamPostData = null;
/*
            _tlamPostDataLength = 0;
*/

        _bUseParam = bUseParam;

        // TLAM通信を行う場合
        if (bUseParam && data != null) {
            // TLAMPOSTデータ用のバッファを確保
            _tlamPostData = new byte[data.length];

            System.arraycopy(data, 0, _tlamPostData, 0, data.length);
        }

        _thread.start();
        lRetVal = errorCode.ERR_NONE.getInt();

        return lRetVal;
    }

    @Override
    public long Stop(boolean bForced) {
        if(!IsStarted()) {
            return FC_ERR_SUCCESS;
        }

        // 強制中断の場合、HTTPConnectionを切断
        if(bForced) {
            _tlamCommAgent.Cancel();
            _tcapCommAgent.Cancel();
        }

        // 中断フラグを立てて中断処理へ移行させる
        _tcapContext.Stop();

        return FC_ERR_SUCCESS;
    }

    @Override
    public boolean IsStarted() {
        return _bIsStarted;
    }

    @Override
    public void run() {
        // スレッドメイン処理
        // 動作中であることを記録
        _bIsStarted = true;

        // FeliCaチップの初期化
        InitFeliCaChips();

        // 通信メイン処理実行
        int error = (int)DoCommunication();

/*
        // FeliCaチップの終了処理
        ResetFeliCaChips();
*/

        // 動作停止
        _bIsStarted = false;

        if(error == FC_ERR_SUCCESS) {
//            _handlerToMainThread.sendEmptyMessage(FC_MSG_TO_MAIN_ON_FINISH);
            // チップアクセスなしの業務は順番制御のためOnNoneChipAccessResponse()からOnFinishedを通知
            if(!_bNoneChipAccess) {
                _iFeliCaClientEventListener.OnFinished(_tcapContext.GetReturnCode());
            }
        } else {
            // FormatとIllegalエラーはエラー文字列を上書きする
            switch(error) {
                case FC_ERR_CLIENT_MSG_FORMAT:
                    _tcapContext.SetErrorMessage("Packet format error.".getBytes());
                    break;
                case FC_ERR_CLIENT_MSG_ILLEGAL:
                    _tcapContext.SetErrorMessage("Illegal state error.".getBytes());
                    break;
                case FC_ERR_CLIENT_CANCEL:
                    _tcapContext.SetErrorMessage("Canceled.".getBytes());
                    break;
                case FC_ERR_CLIENT_NO_MEMORY:
                    _tcapContext.SetErrorMessage("No memory.".getBytes());
                    break;
                default:
                    break;
            }

            String errorMessage = null;
            if(_tcapContext.GetErrorMessage() != null) {
                errorMessage = new String(_tcapContext.GetErrorMessage());
            }
            // エラー詳細がわからなくなるので、変換せずにそのままエラーコードを上げる
//            _iFeliCaClientEventListener.OnErrorOccurred(GetErrorType(error), errorMessage);
            _iFeliCaClientEventListener.OnErrorOccurred(error, errorMessage);
        }
    }

    @Override
    public void SetEventListener(IFeliCaClientEventListener iFeliCaClientEventListener) {
        _iFeliCaClientEventListener = iFeliCaClientEventListener;
    }

    @Override
    public IFeliCaClientEventListener GetEventListener() {
        return _iFeliCaClientEventListener;
    }

    @Override
    public long AddDevice(IDevice iDevice) {
        long lRetVal;

        if(iDevice == null) {
            lRetVal = errorCode.ERR_BAD_PARAM.getInt();     // パラメータエラー
        } else {
            List<DeviceElement> deviceList = _tcapContext.GetDeviceList();
            if(deviceList.size() >= TCAPContext.MAX_LIST_NUM) {
                // デバイスリストに空きが無い
                lRetVal = errorCode.ERR_ADD_DEVICE.getInt();
            } else {
                // 追加処理
                ((Device)iDevice).SetListener(_iFeliCaClientEventListener);
                DeviceWrapper deviceWrapper = new DeviceWrapper(((Device)iDevice).GetDeviceID(), iDevice);

                if(deviceList.add(deviceWrapper)) {
                    lRetVal = errorCode.ERR_NONE.getInt();
                } else {
                    lRetVal = errorCode.ERR_ADD_DEVICE.getInt();
                }
            }
        }

        return lRetVal;
    }

    @Override
    public long AddFeliCaChip(IFeliCaChip iFeliCaChip) {
        long lRetVal;

        if(iFeliCaChip == null) {
            lRetVal = errorCode.ERR_BAD_PARAM.getInt();     // パラメータエラー
        } else {
            List<DeviceElement> deviceList = _tcapContext.GetDeviceList();
            if(deviceList.size() >= TCAPContext.MAX_LIST_NUM) {
                // デバイスリストに空きが無い
                lRetVal = errorCode.ERR_ADD_DEVICE.getInt();
            } else {
                // 追加処理
//                ((FeliCaChip)iFeliCaChip).SetHandler(_handlerToMainThread);
                ((FeliCaChip)iFeliCaChip).SetListener(_iFeliCaClientEventListener);
                FeliCaChipWrapper feliCaChipWrapper = new FeliCaChipWrapper(((FeliCaChip)iFeliCaChip).GetDeviceID(), iFeliCaChip);

                if(deviceList.add(feliCaChipWrapper)) {
                    lRetVal = errorCode.ERR_NONE.getInt();
                } else {
                    lRetVal = errorCode.ERR_ADD_DEVICE.getInt();
                }
            }
        }

        return lRetVal;
    }

    @Override
    public IDevice GetDevice(final String deviceType, final String deviceName) {
        DeviceElement deviceElement = null;

        for(DeviceElement element : _tcapContext.GetDeviceList()) {
            String type = new String(element.GetType());
            String name = new String(element.GetName());
            if(deviceType.equals(type) && deviceName.equals(name)) {
                deviceElement = element;
                break;
            }
        }

        if(deviceElement == null) {
            // デバイスが見つからなかった
        } else if(deviceElement.IsFeliCaChip()) {
            // デバイスは見つかったがFeliCaチップデバイスだった
            deviceElement = null;
        }

        return (IDevice)deviceElement;
    }

    @Override
    public IFeliCaChip GetFeliCaDevice(final String deviceType, final String deviceName) {
        DeviceElement deviceElement = null;

        for(DeviceElement element : _tcapContext.GetDeviceList()) {
            String type = new String(element.GetType());
            String name = new String(element.GetName());
            if(deviceType.equals(type) && deviceName.equals(name)) {
                deviceElement = element;
                break;
            }
        }

        if(deviceElement == null) {
            // デバイスが見つからなかった
        } else if(!deviceElement.IsFeliCaChip()) {
            // デバイスは見つかったがFeliCaチップデバイスではなかった
            deviceElement = null;
        }

        return (IFeliCaChip)deviceElement;
    }

/*
    public long GetErrorType(int errorCode) {
        long errorType = IFeliCaClientEventListener.errorType.TYPE_UNKNOWN.getInt();

        if(_errorMap.containsKey(errorCode)) {
            errorType = _errorMap.get(errorCode);
        }

        return errorType;
    }
*/

    public long DoCommunication() {
        long felicaError = FC_ERR_CLIENT_UNKNOWN;

        // コンテキストをクリア
        _tcapContext.Clear();

        if(_iFeliCaClientEventListener == null) {
            // イベントリスナが設定されていない
        } else if(_url == null || _url.length() < 1) {
            // URLオブジェクト構築失敗
        } else {
            /***********************************************************************************/
            /*** TCAP試験用コード ***/
            if(iCASClientTest.IsStarted()) {
                // TCAP試験用にイベントリスナ設定
                _tlamCommAgent.SetListener(_iFeliCaClientEventListener);
                _tcapCommAgent.SetListener(_iFeliCaClientEventListener);

                if (iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_TLAM_A_001) {
                    // ここでキャンセル可能にしてキャンセルボタンを押す時間を確保するためスリープ
                    _iFeliCaClientEventListener.OnCancelAvailable();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            /***********************************************************************************/
            else {
                _tlamCommAgent.SetListener(_iFeliCaClientEventListener);
            }

            // TLAMパラメータを使用する場合(基本的にこちらしか使用しない想定）
            if(_bUseParam) {
                TLAMResponse tlamResponse = new TLAMResponse();
                String url;

                // 通信エージェントを初期化
                felicaError = (int)_tlamCommAgent.Initialize(_url);
                if(felicaError != FC_ERR_SUCCESS) {
                    _tcapContext.SetErrorMessage(_tcapCommAgent.GetLastErrorStr().getBytes());
                    return felicaError;
                }

                // TLAM通信のためサーバに接続
                _tlamCommAgent.SetParams(_params);
                felicaError = (int)_tlamCommAgent.Connect();
                if(felicaError != FC_ERR_SUCCESS) {
                    _tcapContext.SetErrorMessage(_tcapCommAgent.GetLastErrorStr().getBytes());
                    return felicaError;
                }

                // 中断された場合、処理を継続しない
                if(_tcapContext.IsStop()) {
                    return FC_ERR_CLIENT_CANCEL;
                }

                // TLAM通信実行
//                felicaError = _tlamCommAgent.DoTLAMTransaction(_tlamPostData, _tlamPostDataLength, tlamResponse);
                felicaError = _tlamCommAgent.DoTLAMTransaction(_tlamPostData, tlamResponse, _clientCertificateFile, _jremPassword, _bNoneChipAccess);
                if(felicaError != FC_ERR_SUCCESS) {
                    _tcapContext.SetErrorMessage(_tlamCommAgent.GetLastErrorStr().getBytes());
                    return felicaError;
                } else if(_bNoneChipAccess) {
                    // チップアクセスしない業務はここで終了
                    return FC_ERR_SUCCESS;
                }

                // TLAMレスポンスに「SERV」があればTCAP2.5、それ以外はエラーとする。
                if(tlamResponse.GetValue("SERV".getBytes()) != null) {
                    _stateMachine = ClientStateMachine25a.CreateInstance(_tcapContext, _tcapCommAgent);
                    url = new String(tlamResponse.GetValue("SERV".getBytes()));
                    // URLのチェック
                    String[] str = url.split(":");
                    if(str.length > 0) {
                        if(!str[0].equals("http") && !str[0].equals("https")) {
                            _tcapContext.SetErrorMessage(String.format("unsupported url scheme error %s", str[0]).getBytes());
                            return FC_ERR_CLIENT_HTTP;
                        }
                    } else {
                        _tcapContext.SetErrorMessage("unsupported url scheme error %s".getBytes());
                        return FC_ERR_CLIENT_HTTP;
                    }
                } else {
                    // TCAP2.1は未対応
                    _tcapContext.SetErrorMessage("TCAP version error.".getBytes());
                    return FC_ERR_CLIENT_MSG_TCAP_VER;
                }

                // メモリ確保失敗
                if(_stateMachine == null) {
                    return FC_ERR_CLIENT_NO_MEMORY;
                }

                // TLAMレスポンス内のURL不正
                if(url.length() < 1) {
                    _stateMachine = null;

                    // アプリケーションにプロトコルエラーを通知する
                    _tcapContext.SetErrorMessage("Communication initiation error.".getBytes());
                    return FC_ERR_CLIENT_PROTOCOL;
                } else {
                    // URL設定
                    _stateMachine.SetUrl(url);

                    // 初期ステータスをHANDSHAKEに設定
                    _stateMachine.ChangeState(ClientStateMachine.status.STATUS_HANDSHAKE);

                    // 通信開始
                    felicaError = (int)_stateMachine.Start();

                    // 通信終了
                    _stateMachine = null;
                }
            } else {
                // TLAMパラメータを使用しない場合、TCAP2.5として動作する。
                _stateMachine =  ClientStateMachine25a.CreateInstance(_tcapContext, _tcapCommAgent);

                // URL設定
                _stateMachine.SetUrl(_url);

                // 初期ステータスをHANDSHAKEに設定
                _stateMachine.ChangeState(ClientStateMachine.status.STATUS_HANDSHAKE);

                // 通信開始
                felicaError = (int)_stateMachine.Start();

                // 通信終了
                _stateMachine = null;
            }
        }

        return felicaError;
    }

    /**
     * FeliCaチップの初期化
     * 全てのFeliCaチップの初期化します。
     *
     */
    void InitFeliCaChips() {
        FeliCaChipWrapper feliCaChipWrapper;

        // DeviceリストからFeliCaチップデバイスを探す
        for(DeviceElement deviceElement : _tcapContext.GetDeviceList()) {
            // FeliCa判定
            if(deviceElement.IsFeliCaChip()) {
                feliCaChipWrapper = (FeliCaChipWrapper)deviceElement;
                feliCaChipWrapper.SetTimeout(FeliCaChipWrapper.FELICA_TIMEOUT_DEFAULT);
                feliCaChipWrapper.SetRetryCount(FeliCaChipWrapper.RETRY_COUNT_DEFAULT);
            }
        }
    }

    /**
     * FeliCaチップのリセット
     * 全てのFeliCaチップのリセットを行います。
     *
     * @return エラーコード ( FeliCaClientErrorCode.h 参照 )
     */
    long ResetFeliCaChips() {
        long lRetVal = FC_ERR_CLIENT_UNKNOWN;
        FeliCaChipWrapper feliCaChipWrapper;

        // DeviceリストからFeliCaチップデバイスを探す
        for(DeviceElement deviceElement : _tcapContext.GetDeviceList()) {
            // FeliCa判定
            if(deviceElement.IsFeliCaChip()) {
                // FeliCaチップリセット
                feliCaChipWrapper = (FeliCaChipWrapper)deviceElement;
                feliCaChipWrapper.Close();
                lRetVal = FC_ERR_SUCCESS;
            }
        }

        return lRetVal;
    }

    @Override
    public long RemoveDevice(final String deviceType, final String deviceName) {
        long lRetVal = FC_ERR_CLIENT_UNKNOWN;
        DeviceElement deviceElement = null;

        // Deviceリストからデバイスを探す
        for(DeviceElement element : _tcapContext.GetDeviceList()) {
            String type = new String(element.GetType());
            String name = new String(element.GetName());
            if(deviceType.equals(type) && deviceName.equals(name)) {
                deviceElement = element;
                break;
            }
        }

        if(deviceElement == null) {
            // デバイスが見つからなかった
        } else {
            if(_tcapContext.GetDeviceList().remove(deviceElement)) {
                lRetVal = FC_ERR_SUCCESS;
            }
        }

        return lRetVal;
    }

    @Override
    public long RemoveAllDevices() {
        _tcapContext.GetDeviceList().clear();
        return errorCode.ERR_NONE.getInt();
    }

    public void SetParams(JSONObject params) {
        _params = params;
    }

    public void SetClientCertificateFile(File file) { _clientCertificateFile = file; }

    public void SetJremPassword(char[] password) { _jremPassword = password; }

    public void SetNoneChipAccess(boolean bNoneChipAccess) { _bNoneChipAccess = bNoneChipAccess; }
}
