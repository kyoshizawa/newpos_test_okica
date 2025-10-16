package jp.mcapps.android.multi_payment_terminal.iCAS.TCAPTest;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.FeliCaClient;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.IDevice;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.IFeliCaClientEventListener;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPPacket;
import jp.mcapps.android.multi_payment_terminal.iCAS.iCASDevice;
import timber.log.Timber;

public class iCASClientTest implements IFeliCaClientEventListener {
    // TCAPテストモード定義
    public static final int TCAP_TEST_NONE     = 0;
    public static final int TCAP_TEST_HP_A_005 = 1;
    public static final int TCAP_TEST_HP_A_008 = 4;
    public static final int TCAP_TEST_HP_A_009 = 5;
    public static final int TCAP_TEST_HP_E_062 = 6;
    public static final int TCAP_TEST_HP_E_065 = 9;
    public static final int TCAP_TEST_HP_E_066 = 10;
    public static final int TCAP_TEST_HP_E_069 = 13;
    public static final int TCAP_TEST_HP_E_071 = 14;
    public static final int TCAP_TEST_HP_E_075 = 15;

    public static final int TCAP_TEST_TLAM_A_001 = 0x1001;
    public static final int TCAP_TEST_TLAM_A_005 = 0x1005;
    public static final int TCAP_TEST_TLAM_A_006 = 0x1006;
    public static final int TCAP_TEST_TLAM_E_023 = 0x1007;
    public static final int TCAP_TEST_TLAM_E_024 = 0x1008;
    public static final int TCAP_TEST_TLAM_N_016 = 0x1009;
    public static final int TCAP_TEST_TLAM_E_013 = 0x100A;
    public static final int TCAP_TEST_TLAM_E_016 = 0x100B;
    public static final int TCAP_TEST_TLAM_E_019 = 0x100C;
    public static final int TCAP_TEST_TLAM_E_020 = 0x100D;

    public static final int TCAP_TEST_FP_A_006 = 0x2001;    // ユーザーキャンセル
    public static final int TCAP_TEST_FP_A_007 = 0x2002;    // ユーザーキャンセル
    public static final int TCAP_TEST_FP_E_053 = 0x2003;    // 予期しないエラー

    public static final int TCAP_TEST_EP_A_007 = 0x3001;    // ユーザーキャンセル スクリプトでError発生させる
    public static final int TCAP_TEST_EP_A_008 = 0x3002;    // ユーザーキャンセル
    public static final int TCAP_TEST_EP_A_009 = 0x3003;    // ユーザーキャンセル
    public static final int TCAP_TEST_EP_A_012 = 0x3004;    // ユーザーキャンセル
    public static final int TCAP_TEST_EP_E_049 = 0x3005;    // 下位レイヤーによる切断
    public static final int TCAP_TEST_EP_E_052 = 0x3006;    // HTTPタイムアウト
    public static final int TCAP_TEST_EP_E_055 = 0x3007;    // 予期しないエラー

    public static final int TCAP_TEST_UEP_E_046 = 0x4001;   // 予期しないエラー

    public static final int TCAP_TEST_OEP_E_048 = 0x5001;   // 予期しないエラー

    public static final int TCAP_TEST_ADTP_A_008 = 0x6001;  // ユーザーキャンセル
    public static final int TCAP_TEST_ADTP_A_011 = 0x6002;  // ユーザーキャンセル
    public static final int TCAP_TEST_ADTP_E_066 = 0x6003;  // 下位レイヤーによる切断
    public static final int TCAP_TEST_ADTP_E_067 = 0x6004;  // 下位レイヤーによる切断
    public static final int TCAP_TEST_ADTP_E_069 = 0x6005;  // HTTPタイムアウト
    public static final int TCAP_TEST_ADTP_E_070 = 0x6006;  // HTTPタイムアウト
    public static final int TCAP_TEST_ADTP_E_073 = 0x6007;  // 予期しないエラー



    // TCAPテストモード
    public static int TCAP_TEST_MODE = TCAP_TEST_NONE;

    // スレッド間メッセージ
    public static final int FC_MSG_TO_MAIN_ON_FINISH            = 0x0001;             // 正常終了
    public static final int FC_MSG_TO_MAIN_ON_ERROR             = 0x0002;             // エラー終了
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_CANCEL    = 0x1001;             // デバイス操作要求（CANCEL）
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_STATUS    = 0x1002;             // デバイス操作要求（STATUS）
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_RW_PARAM  = 0x1003;             // デバイス操作要求（R/W_PARAM）
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_OPERATION = 0x1004;             // デバイス操作要求（OPERATION）
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_CONFIRM   = 0x1005;             // デバイス操作要求（CONFIRM）
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_DISPLAY   = 0x1006;             // デバイス操作要求（DISPLAY）
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_RETRY     = 0x1007;             // デバイス操作要求（RETRY）
    public static final int FC_MSG_TO_MAIN_ON_OPERATE_RESULT    = 0x1008;             // デバイス操作要求（RESULT）
    public static final int FC_MSG_TO_MAIN_ON_CANCEL_AVAILABLE  = 0xF001;             // キャンセル許可（TCAPテスト専用）
    public static final int FC_MSG_TO_MAIN_ON_COMMUNICATION_OFF = 0xF002;             // 通信切断指示（TCAPテスト専用）

    private static iCASClientTest _instance;  // インスタンス
    private Handler _handlerToMainThread;     // メインスレッドへのハンドラ
    private FeliCaClient _feliCaClient;       // FeliCaクライアント
    private boolean _bIsAborted;              // 中断フラグ
    private boolean _bIsCancelDisable;        // キャンセル無効フラグ
    private IiCASClientTest _icasClientTestEventListener;
    private JSONObject _params;

    private iCASClientTest() {
        _bIsAborted = false;
        _bIsCancelDisable = false;
        _feliCaClient = FeliCaClient.getInstance();
        _feliCaClient.SetEventListener(this);

//        // AddDeviceする前にイベントリスナの設定をすること
//        iCASDeviceTest.AddDevice(_feliCaClient);
    }

    public static iCASClientTest getInstance() {
        if(_instance == null) {
            _instance = new iCASClientTest();
        }
        return _instance;
    }

    public void setEventListener(IiCASClientTest listener) {
        _icasClientTestEventListener = listener;
    }

    public long OnStart(String testName, JSONObject params) throws JSONException {
        long lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        boolean bUseTlam = true;
        byte[] data = null;
        String script = "";
        String query = "";

        if(params != null) {
            script = params.getString("script");
            query = "";

            if (params.has("query")) {
                query = params.getString("query");
            }

            if (params.has("body")) {
                data = params.getString("body").getBytes();
            }

            // テストモードの設定
            if (testName.equals("HP-A-005")) {
                TCAP_TEST_MODE = TCAP_TEST_HP_A_005;
            } else if (testName.equals("HP-A-008")) {
                TCAP_TEST_MODE = TCAP_TEST_HP_A_008;
            } else if (testName.equals("HP-A-009")) {
                TCAP_TEST_MODE = TCAP_TEST_HP_A_009;
            } else if (testName.equals("HP-E-062")) {
                TCAP_TEST_MODE = TCAP_TEST_HP_E_062;
            } else if (testName.equals("HP-E-065")) {
                TCAP_TEST_MODE = TCAP_TEST_HP_E_065;
            } else if (testName.equals("HP-E-066")) {
                TCAP_TEST_MODE = TCAP_TEST_HP_E_066;
            } else if (testName.equals("HP-E-069")) {
                TCAP_TEST_MODE = TCAP_TEST_HP_E_069;
            } else if (testName.equals("HP-E-071")) {
                TCAP_TEST_MODE = TCAP_TEST_HP_E_071;
            } else if (testName.equals("HP-E-075")) {
                TCAP_TEST_MODE = TCAP_TEST_HP_E_075;
            } else if (testName.equals("TLAM-A-001")) {
                TCAP_TEST_MODE = TCAP_TEST_TLAM_A_001;
            } else if (testName.equals("TLAM-A-005")) {
                TCAP_TEST_MODE = TCAP_TEST_TLAM_A_005;
            } else if (testName.equals("TLAM-A-006")) {
                TCAP_TEST_MODE = TCAP_TEST_TLAM_A_006;
            } else if (testName.equals("TLAM-E-023")) {
                TCAP_TEST_MODE = TCAP_TEST_TLAM_E_023;
            } else if (testName.equals("TLAM-E-024")) {
                TCAP_TEST_MODE = TCAP_TEST_TLAM_E_024;
            } else if (testName.equals("TLAM-N-016")) {
                TCAP_TEST_MODE = TCAP_TEST_TLAM_N_016;
            } else if (testName.equals("TLAM-E-013")) {
                TCAP_TEST_MODE = TCAP_TEST_TLAM_E_013;
            } else if (testName.equals("TLAM-E-016")) {
                TCAP_TEST_MODE = TCAP_TEST_TLAM_E_016;
            } else if (testName.equals("TLAM-E-019")) {
                TCAP_TEST_MODE = TCAP_TEST_TLAM_E_019;
            } else if (testName.equals("TLAM-E-020")) {
                TCAP_TEST_MODE = TCAP_TEST_TLAM_E_020;
            } else if (testName.equals("FP-A-006")) {
                TCAP_TEST_MODE = TCAP_TEST_FP_A_006;
            } else if (testName.equals("FP-A-007")) {
                TCAP_TEST_MODE = TCAP_TEST_FP_A_007;
            } else if (testName.equals("FP-E-053")) {
                TCAP_TEST_MODE = TCAP_TEST_FP_E_053;
            } else if (testName.equals("EP-A-007")) {
                TCAP_TEST_MODE = TCAP_TEST_EP_A_007;
            } else if (testName.equals("EP-A-008")) {
                TCAP_TEST_MODE = TCAP_TEST_EP_A_008;
            } else if (testName.equals("EP-A-009")) {
                TCAP_TEST_MODE = TCAP_TEST_EP_A_009;
            } else if (testName.equals("EP-A-012")) {
                TCAP_TEST_MODE = TCAP_TEST_EP_A_012;
            } else if (testName.equals("EP-E-049")) {
                TCAP_TEST_MODE = TCAP_TEST_EP_E_049;
            } else if (testName.equals("EP-E-052")) {
                TCAP_TEST_MODE = TCAP_TEST_EP_E_052;
            } else if (testName.equals("EP-E-055")) {
                TCAP_TEST_MODE = TCAP_TEST_EP_E_055;
            } else if (testName.equals("UEP-E-046")) {
                TCAP_TEST_MODE = TCAP_TEST_UEP_E_046;
            } else if (testName.equals("OEP-E-048")) {
                TCAP_TEST_MODE = TCAP_TEST_OEP_E_048;
            } else if (testName.equals("ADTP-A-008")) {
                TCAP_TEST_MODE = TCAP_TEST_ADTP_A_008;
            } else if (testName.equals("ADTP-A-011")) {
                TCAP_TEST_MODE = TCAP_TEST_ADTP_A_011;
            } else if (testName.equals("ADTP-E-066")) {
                TCAP_TEST_MODE = TCAP_TEST_ADTP_E_066;
            } else if (testName.equals("ADTP-E-067")) {
                TCAP_TEST_MODE = TCAP_TEST_ADTP_E_067;
            } else if (testName.equals("ADTP-E-069")) {
                TCAP_TEST_MODE = TCAP_TEST_ADTP_E_069;
            } else if (testName.equals("ADTP-E-070")) {
                TCAP_TEST_MODE = TCAP_TEST_ADTP_E_070;
            } else if (testName.equals("ADTP-E-073")) {
                TCAP_TEST_MODE = TCAP_TEST_ADTP_E_073;
            } else {
                TCAP_TEST_MODE = TCAP_TEST_NONE;
            }

            _feliCaClient.RemoveAllDevices();

            _params = params;
            iCASDeviceTest.AddDevice(_feliCaClient, params);

            if (params.has("useTlam")) {
                int val = Integer.parseInt(params.getString("useTlam"));
                if (val == 0) {
                    // TLAMを使用しない
                    bUseTlam = false;
                }
            }
        }

        // ワーカースレッドからの通知受信処理（メインスレッドへ通知が必要な場合、ここで受信する）
        _handlerToMainThread = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case FC_MSG_TO_MAIN_ON_FINISH:
                        // 処理成功 正常終了コールバック実行
                        _icasClientTestEventListener.OnTestFinished(FeliCaClient.FC_ERR_SUCCESS, msg.arg1, null, testName);
                        break;
                    case FC_MSG_TO_MAIN_ON_ERROR:
                        // 処理失敗 エラーコールバック実行
                        _icasClientTestEventListener.OnTestFinished(msg.arg1, 0, (String) msg.obj, testName);
                        break;
                    case FC_MSG_TO_MAIN_ON_OPERATE_CANCEL:     // デバイス操作要求（キャンセル最終確認）
                        _bIsCancelDisable = true;
                        break;
                    case FC_MSG_TO_MAIN_ON_OPERATE_STATUS:
                        break;
                    /***********************************************************************************/
                    /*** TCAP試験用コード ***/
                    case FC_MSG_TO_MAIN_ON_CANCEL_AVAILABLE:
                        _icasClientTestEventListener.OnCancelAvailable();
                        break;
                    case FC_MSG_TO_MAIN_ON_COMMUNICATION_OFF:
                        _icasClientTestEventListener.OnCommunicationOff();
                        break;
                    /***********************************************************************************/
                    default:
                        break;
                }
            }
        };

        if(_feliCaClient != null) {
//            _feliCaClient.SetUrl("http://54.199.253.247/cgi-bin/FSCTestServer.exe/HandShake/HP/TCAP25_HP_N_001_tcap25.xml");
//            _feliCaClient.SetUrl("http://54.199.253.247/cgi-bin/FSCTestServer.exe/HandShake/FP/TCAP25_FP_N_001_tcap25.xml");
//            _feliCaClient.SetUrl("http://54.199.253.247/cgi-bin/FSCTestServer.exe/HandShake/EP/TCAP25_EP_N_001_tcap25.xml");
//            _feliCaClient.SetUrl("http://54.199.253.247/cgi-bin/FSCTestServer.exe/HandShake/OEP/TCAP25_OEP_N_001_tcap25.xml");
//            _feliCaClient.SetUrl("http://54.199.253.247/cgi-bin/FSCTestServer.exe/HandShake/UEP/TCAP25_UEP_N_001_tcap25.xml");
//            _feliCaClient.SetUrl("http://54.199.253.247/cgi-bin/FSCTestServer.exe/HandShake/ADTP/TCAP25_ADTP_N_001_tcap25.xml");

            if (testName.equals("TLAM-N-016")) {
                // httpsスキームの試験
                _feliCaClient.SetUrl("https://54.199.253.247/cgi-bin/FSCTestServer.exe/HandShake/" + script + ";clientid=" + testName + query);
            } else {
                _feliCaClient.SetUrl("http://54.199.253.247/cgi-bin/FSCTestServer.exe/HandShake/" + script + ";clientid=" + testName + query);
            }

            // useParamはtrue固定。起動時はTLAM通信しない想定のためdataはnull指定。
            _feliCaClient.Start(bUseTlam, data, 0);
        } else {
            lRetVal = FeliCaClient.FC_ERR_CLIENT_FATAL;
        }

        return lRetVal;
    }

    public long OnStop(boolean bForced) {
        long lRetVal = FeliCaClient.FC_ERR_CLIENT_FAILURE;

        if(!_bIsCancelDisable) {
            _bIsAborted = true;
            _feliCaClient.Stop(bForced);
            lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        }

        return lRetVal;
    }

    @Override
    public void OnFinished(int statusCode) {
        Timber.tag("iCAS").d("iCASClient OnFinished returnCode=%04x", statusCode);

        Message message = new Message();
        message.what = FC_MSG_TO_MAIN_ON_FINISH;
        message.arg1 = statusCode;
        _handlerToMainThread.sendMessage(message);
    }

    @Override
    public void OnErrorOccurred(int errorCode, final String errorMessage) {
        Message message = new Message();
        message.what = FC_MSG_TO_MAIN_ON_ERROR;
        message.arg1 = errorCode;
        message.obj = errorMessage;
        _handlerToMainThread.sendMessage(message);
    }

    @Override
    public long OnTransmitRW(byte[] command, long commandLength, byte[] response) {
        long lRetVal = errorType.TYPE_UNKNOWN.getInt();
        String feliCaCmd = "";
        int testCase = 1;   // デバッグで書き換え（デバッグ不要の場合は常に1で動作）

        // サブスレッドでの実行
        if(testCase == 1) {
            if(_params.has("feliCaCmdLen")) {
                try {
                    lRetVal = Integer.parseInt(_params.getString("feliCaCmdLen"));
                    feliCaCmd = _params.getString("feliCaCmd");
                } catch (JSONException e) {
                    return FeliCaClient.FC_ERR_CLIENT_BADPARAM;
                }
            }
        } else if(testCase >= 2) {
            if(_params.has("feliCaCmdLen" + testCase)) {
                try {
                    lRetVal = Integer.parseInt(_params.getString("feliCaCmdLen" + testCase));
                    feliCaCmd = _params.getString("feliCaCmd" + testCase);
                } catch (JSONException e) {
                    return FeliCaClient.FC_ERR_CLIENT_BADPARAM;
                }
            }
        }

        for(int i = 0; i < lRetVal; i++) {
            int val = Integer.parseInt(feliCaCmd.substring(i*2, i*2+2));
            response[i] = (byte)val;
        }
//        response[0] = (byte)lRetVal;
//        response[1] = (byte)0x01;

        return lRetVal;
    }

    @Override
    public long OnDeviceOperate(IDevice.DeviceOperate deviceOperate, TCAPPacket replayPacket) {
        long lRetVal = FeliCaClient.FC_ERR_SUCCESS;

        try {
            iCASDeviceTest icasDevice = new iCASDeviceTest(deviceOperate);
            lRetVal = icasDevice.Operate(_params, _bIsAborted, _handlerToMainThread, replayPacket);
        } catch (IOException | JSONException exception) {
            lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
        }

        return lRetVal;
    }

    @Override
    public long OnNoneChipAccessResponse(byte[] data, int dataLength) {
        long lRetVal = FeliCaClient.FC_ERR_SUCCESS;

        return lRetVal;
    }

    @Override
    public void OnCancelAvailable() {
        Message message = new Message();
        message.what = FC_MSG_TO_MAIN_ON_CANCEL_AVAILABLE;
        _handlerToMainThread.sendMessage(message);
    }

    @Override
    public void OnCommunicationOff() {
        Message message = new Message();
        message.what = FC_MSG_TO_MAIN_ON_COMMUNICATION_OFF;
        _handlerToMainThread.sendMessage(message);
    }

    @Override
    public void OnRWClose() {

    }

    public static boolean IsStarted() {
        if(_instance != null) {
            return true;
        } else {
            return false;
        }
    }
}
