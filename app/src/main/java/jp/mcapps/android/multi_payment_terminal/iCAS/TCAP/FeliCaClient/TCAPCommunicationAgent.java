package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.HttpModule.FeliCaHttpClient;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgPacketFormatError;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgUnexpectedError;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPMessage;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPPacket;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAPTest.iCASClientTest;
import okhttp3.Headers;
import okhttp3.Response;
import timber.log.Timber;

public class TCAPCommunicationAgent {
    // 定数
    private final int RECEIVE_BUFFER_SIZE     = 65535+5;	// 受信バッファサイズ
    private final int NETWORK_TIMEOUT_DEFAULT = 60000;		// ネットワークタイムアウト初期値(ミリ秒)
    private final int NETWORK_TIMEOUT_MIN     = 60000;		// ネットワークタイムアウト最小値(ミリ秒)
    private final int NETWORK_TIMEOUT_MAX     = 120000;		// ネットワークタイムアウト最大値(ミリ秒)
//    private final int MAX_URL_LENGTH          = 4096;		// URL最大長
    // HTTPエラー文字列
//    private final String HTTP_ERR_STR_TIMEOUT_CONNECT   =	"HTTP connect timeout.";
//    private final String HTTP_ERR_STR_TIMEOUT           =	"HTTP timeout.";
    private final String HTTP_ERR_STR_COMMUNICATION     =	"HTTP communication error.";
    private final String HTTP_ERR_STR_ABORTED           =	"HTTP Aborted.";
    private final String HTTP_ERR_STR_RESPONSE_CODE     =	"Invalid response code:";
    private final String HTTP_ERR_STR_CONTENT_TYPE      =	"Invalid content-type:";
    private final String HTTP_ERR_STR_NO_CONTENT_LENGTH =	"No Content-Length header.";
    private final String HTTP_ERR_STR_NO_MEMORY         =	"No memory.";
    private final String HTTP_ERR_STR_SEQUENCE          =	"Error Sequence.";
    private final String HTTP_ERR_STR_BADPARAM          =	"Error bad parameter.";

    private FeliCaHttpClient _httpClient;         // HTTP通信モジュール
    private String _url;                          // URL文字列
    private String _cookie;                       // Cookie
    private String _errorMessage;                 // 通信エラー文字列
    private boolean _bIsAborted;                  // 通信中断フラグ
    private int _connectionTimeout;               // 通信タイムアウト時間（単位：ミリ秒）
    private TCAPMessage _tcapMessage;             // エラーメッセージ
    private IFeliCaClientEventListener _iFeliCaClientEventListener; // 通信終了コールバックI/F
    private JSONObject _params;                   // 業務パラメータ

    public TCAPCommunicationAgent() {
        _httpClient = null;
        _url = null;
        _bIsAborted = false;

        _errorMessage = "";
        _tcapMessage = null;
    }

    /**
     * 初期化
     * 内部メンバの初期化を行い、URLを記憶します。
     *
     * @param url    URL文字列
     *
     * @return エラーコード
     */
    public long Initialize(String url) {
        long lRetVal;

        // エラーメッセージを初期化
        _errorMessage = "";

        _params = null;

        _connectionTimeout = NETWORK_TIMEOUT_DEFAULT;

        if(url != null) {
            _url = url;
            _httpClient = FeliCaHttpClient.get_instance();
            lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        } else {
            _errorMessage = HTTP_ERR_STR_NO_MEMORY;
            lRetVal = FeliCaClient.FC_ERR_CLIENT_NO_MEMORY;
        }

        _cookie = "";

        _bIsAborted = false;

        return lRetVal;
    }

    /**
     * サーバと接続
     * 指定URLのサーバと接続します。
     *
     * @return エラーコード
     */
    public long Connect() {
        long lRetVal;

        // 接続方法を変更しているため、常に成功で返す
        lRetVal = FeliCaClient.FC_ERR_SUCCESS;

        return lRetVal;
    }

    /**
     * タイムアウトの設定
     * サーバアプリケーション側からのHTTP応答を待つ時間(単位:ミリ秒)を設定します。
     *
     * @param seconds タイムアウト時間（単位：ミリ秒）
     */
    public void SetNetworkTimeout(int seconds) {
        // エラーメッセージを初期化
        _errorMessage = "";

        if(_httpClient == null) {
            // HTTP設定なし
        } else {
            _connectionTimeout = seconds;
            if(seconds < NETWORK_TIMEOUT_MIN) {
                _connectionTimeout = NETWORK_TIMEOUT_MIN;
            } else if(seconds > NETWORK_TIMEOUT_MAX) {
                _connectionTimeout = NETWORK_TIMEOUT_MAX;
            }
        }
    }

    /**
     * TLAMデータ取得処理
     * TLAMリクエストをサーバに送信し、TLAMレスポンスを受信します。
     * pucTLAMPostDataがNULL又はlTLAMPostDataLengthが1未満の場合、送信メソッドは「GET」になります。
     * それ以外の場合、送信メソッドは「POST」になります。
     *
     * @param postData              サーバへ送信するデータ
     * @param tlamResponse          サーバから受信したTLAMレスポンス
     * @param clientCertificateFile クライアント証明書
     * @param jremPassword           jrem Password
     * @param bNoneChipAccess       チップアクセスしない業務かどうか
     *
     * @return エラーコード
     */
    public long DoTLAMTransaction(byte[] postData, TLAMResponse tlamResponse, File clientCertificateFile, char[] jremPassword, boolean bNoneChipAccess) {
        long lRetVal;

        if(_httpClient == null) {
            return FeliCaClient.FC_ERR_CLIENT_SEQUENCE;
        }

        // 受信バッファ確保
        byte[] tempRecvBuff = new byte[RECEIVE_BUFFER_SIZE];
        /***********************************************************************************/
        /*** TCAP試験用コード ***/
        if(iCASClientTest.IsStarted()) {
            if(iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_TLAM_E_023) {
                tempRecvBuff = null;
            }
        }
        /***********************************************************************************/
        if(tempRecvBuff == null) {
            return FeliCaClient.FC_ERR_CLIENT_NO_MEMORY;
        }

        // 強制中断チェック
        if(_bIsAborted) {
            _errorMessage = HTTP_ERR_STR_ABORTED;
            return FeliCaClient.FC_ERR_CLIENT_HTTP;
        }

        Response response;

        // メソッド種別とURLを設定
        // 起動・業務開始時はGETによるリクエストのみ使用
        try {
            if(postData == null) {
                FeliCaHttpClient.Get get = _httpClient.get(_url);

                if (!iCASClientTest.IsStarted()) {
                    try {
                        FileInputStream inputStream = new FileInputStream(clientCertificateFile);
                        KeyStore keyStore = KeyStore.getInstance("PKCS12");
                        keyStore.load(inputStream, jremPassword);
                        inputStream.close();

                        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                        trustManagerFactory.init(keyStore);
                        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

                        if(trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                            // 予期せぬエラー
                            _tcapMessage = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                            return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                        }

//                        X509TrustManager trustManager = (X509TrustManager)trustManagers[0];

                        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
                        keyManagerFactory.init(keyStore, AppPreference.getJremPassword().toCharArray());
                        SSLContext sslContext = SSLContext.getInstance("SSL");
                        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

                        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                        get.sslSocketFactory(sslSocketFactory);
                    } catch (Exception e) {
                        // 予期せぬエラー
                        _tcapMessage = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                        return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                    }

                /***********************************************************************************/
                /*** TCAP試験用コード TLAM-N-016用 https接続確認のため、証明書を無視する ***/
                } else if(iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_TLAM_N_016) {
                    try {
                        final SSLContext sslContext = SSLContext.getInstance("SSL");
                        sslContext.init(null, new TrustManager[]{new FeliCaHttpClient.MyX509TrustManager()}, new java.security.SecureRandom());
                        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                        get.sslSocketFactory(sslSocketFactory);
                        get.setHostnameVerifier(new HostnameVerifier() {
                            @Override
                            public boolean verify(String hostname, SSLSession session) {
                                return true;
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                /***********************************************************************************/

                // 業務パラメータの設定
                if(_params != null) {
                    Iterator<String> paramNames = _params.keys();
                    while (paramNames.hasNext()) {
                        String param = paramNames.next();
                        try {
                            get = get.addQueryParameter(param, _params.getString(param));
                        } catch (JSONException e) {
                            // 予期せぬエラー
                            _errorMessage = HTTP_ERR_STR_BADPARAM;
                            _tcapMessage = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), GetLastErrorStr().getBytes(), GetLastErrorStr().getBytes().length);
                            return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                        }
                    }
                }
                response = get
                        .addHeader("Content-Type", "application/x-tlam; charset=UTF-8")
                        .response();

            } else {
                FeliCaHttpClient.Post post = _httpClient.post(_url);
                // 業務パラメータの設定
                if(_params != null) {
                    while (_params.keys().hasNext()) {
                        String param = _params.keys().next();
                        try {
                            post = post.addQueryParameter(param, _params.getString(param));
                        } catch (JSONException e) {
                            // 予期せぬエラー
                            _errorMessage = HTTP_ERR_STR_NO_MEMORY;
                            _tcapMessage = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), GetLastErrorStr().getBytes(), GetLastErrorStr().getBytes().length);
                            return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                        }
                    }
                }
                response = post
                        .addHeader("Content-Type", "application/x-tlam; charset=UTF-8")
                        .setBody(postData)
                        .response();
            }

            // 受信バッファにレスポンス格納
            tempRecvBuff = Objects.requireNonNull(response.body()).bytes();

        } catch (IOException ex) {
            _errorMessage = String.format(Locale.JAPAN, HTTP_ERR_STR_COMMUNICATION + " \n%s", ex.getMessage());
            lRetVal = FeliCaClient.FC_ERR_CLIENT_HTTP;
            return lRetVal;
        }

        /***********************************************************************************/
        /*** TCAP試験用コード ***/
        if(iCASClientTest.IsStarted()) {
            if(iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_TLAM_A_005
            || iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_TLAM_A_006) {
                // ここでキャンセル可能にしてキャンセルボタンを押す時間を確保するためスリープ
                _iFeliCaClientEventListener.OnCancelAvailable();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if(iCASClientTest.IsStarted()
        && (iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_HP_E_071
         || iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_TLAM_E_019
         || iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_TLAM_E_020)) {
            int code = response.code();     // デバッグで値を変えて試験を実施

            if (code != 200) {
                // ステータスコード異常
                _errorMessage = String.format(Locale.JAPAN, HTTP_ERR_STR_RESPONSE_CODE + "%d", code);
                lRetVal = FeliCaClient.FC_ERR_CLIENT_HTTP;
                return lRetVal;
            }

        /***********************************************************************************/
        } else {
            if (response.code() != 200) {
                // ステータスコード異常
                _errorMessage = String.format(Locale.JAPAN, HTTP_ERR_STR_RESPONSE_CODE + "%d", response.code());
                lRetVal = FeliCaClient.FC_ERR_CLIENT_HTTP;
                return lRetVal;
            }
        }

        if(!bNoneChipAccess) {
            // Content-Type有無チェック
            boolean bFound = false;
            Headers headers = response.headers();
            for(int i = 0; i < headers.size(); i++) {
                if(headers.name(i).equals("Content-Type")) {
                    bFound = true;
                    if(!headers.value(i).contains("application/x-tlam")) {
                        _errorMessage = HTTP_ERR_STR_CONTENT_TYPE;
                        return FeliCaClient.FC_ERR_CLIENT_HTTP;
                    }
                    break;
                }
            }
            if(!bFound) {
                _errorMessage = HTTP_ERR_STR_CONTENT_TYPE;
                return FeliCaClient.FC_ERR_CLIENT_HTTP;
            }

            // TLAMレスポンスをParseする
            lRetVal = tlamResponse.Parse(tempRecvBuff, tempRecvBuff.length);
        } else {
            lRetVal = _iFeliCaClientEventListener.OnNoneChipAccessResponse(tempRecvBuff, tempRecvBuff.length);
        }

        return lRetVal;
    }

    /**
     * TCAPパケット送信
     * Initializeで指定されたURLにTCAPパケットを送信します。
     * 送信すると引数の送信パケットリストをクリアします
     *
     * @param sendPacketList 送信するパケットリスト
     * @param recvPacketList 受信するパケットリスト（空の応答時はnullを指定）
     *
     * @return エラーコード
     */
    public long SendTCAPPacket(List<TCAPPacket> sendPacketList, List<TCAPPacket> recvPacketList) {
        long lRetVal;

        // 強制中断チェック
        if(_bIsAborted) {
            _errorMessage = HTTP_ERR_STR_ABORTED;
            return FeliCaClient.FC_ERR_CLIENT_HTTP;
        }

        // エラーメッセージ初期化
        _errorMessage = "";
        _tcapMessage = null;

        // TCAPパケットをバイナリ化
        ByteBuffer sendData;
        byte[] data;
        int totalSize = 0;

        // バイナリにするサイズ算出
        for(TCAPPacket packet : sendPacketList) {
            totalSize += packet.GetPacketSize();
        }
        // 必要な領域確保
        sendData = ByteBuffer.allocate(totalSize);
        data = new byte[totalSize];

        /***********************************************************************************/
        /*** TCAP試験用コード ***/
        if(iCASClientTest.IsStarted() && sendPacketList.size() > 0 && sendPacketList.get(0).GetSubProtocolType() == TCAPPacket.TCAP_SPT_HAND_SHAKE) {
            if(iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_TLAM_E_024) {
                data = null;
            }
        }
        /***********************************************************************************/

        if(data == null) {
            // 予期せぬエラー
            _errorMessage = HTTP_ERR_STR_NO_MEMORY;
            _tcapMessage = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), GetLastErrorStr().getBytes(), GetLastErrorStr().getBytes().length);
            return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
        }

        lRetVal = TCAPPacketListDump(sendData, sendPacketList);
        if(lRetVal == TCAPPacket.FC_ERR_TCAP_PACKETS_SIZE) {
            _tcapMessage = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), GetLastErrorStr().getBytes(), GetLastErrorStr().getBytes().length);
            return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
        } else if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
            return lRetVal;
        }
        sendData.get(data);

        if(_url == null) {
            // URLが生成されていない
            _errorMessage = HTTP_ERR_STR_SEQUENCE;
            return FeliCaClient.FC_ERR_CLIENT_SEQUENCE;
        }

        byte[] recvData = null;
        Response response = null;

        /***********************************************************************************/
        /*** TCAP試験用コード ***/
        if(iCASClientTest.IsStarted() && sendPacketList.size() > 0 && sendPacketList.get(0).GetSubProtocolType() == TCAPPacket.TCAP_SPT_HAND_SHAKE) {
            if(iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_HP_A_005) {
                // ここでキャンセル可能にしてキャンセルボタンを押す時間を確保するためスリープ
                _iFeliCaClientEventListener.OnCancelAvailable();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if(iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_HP_E_062
                   || iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_HP_E_066
                   || iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_TLAM_E_013
                   || iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_TLAM_E_016) {
                // ここで通信切断メッセージ表示して通信切断する時間を確保するためスリープ
                _iFeliCaClientEventListener.OnCommunicationOff();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if(iCASClientTest.IsStarted() && sendPacketList.size() > 0 && sendPacketList.get(0).GetSubProtocolType() == TCAPPacket.TCAP_SPT_ERROR) {
            if(iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_EP_E_049
            || iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_EP_E_052) {
                // ここで通信切断メッセージ表示して通信切断する時間を確保するためスリープ
                _iFeliCaClientEventListener.OnCommunicationOff();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if(iCASClientTest.IsStarted() && sendPacketList.size() > 0 && sendPacketList.get(0).GetSubProtocolType() == TCAPPacket.TCAP_SPT_OPERATE_ENTITY) {
            if(iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_ADTP_A_008) {
                // ここでキャンセル可能にしてキャンセルボタンを押す時間を確保するためスリープ
                _iFeliCaClientEventListener.OnCancelAvailable();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if(iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_ADTP_E_066
                   || iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_ADTP_E_069) {
                // ここで通信切断メッセージ表示して通信切断する時間を確保するためスリープ
                _iFeliCaClientEventListener.OnCommunicationOff();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if(iCASClientTest.IsStarted() && sendPacketList.size() > 0 && sendPacketList.get(0).GetSubProtocolType() == TCAPPacket.TCAP_SPT_APPLICATION_DATA_TRANSFER) {
            if(iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_ADTP_A_011) {
                // ここでキャンセル可能にしてキャンセルボタンを押す時間を確保するためスリープ
                _iFeliCaClientEventListener.OnCancelAvailable();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if(iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_ADTP_E_067
                   || iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_ADTP_E_070) {
                // ここで通信切断メッセージ表示して通信切断する時間を確保するためスリープ
                _iFeliCaClientEventListener.OnCommunicationOff();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if(iCASClientTest.IsStarted() && sendPacketList.size() == 0) {
            if(iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_FP_A_006) {
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

        // メソッド種別とURLを設定
        try {
            _httpClient = FeliCaHttpClient.get_instance();
            response = _httpClient
                    .post(_url)
                    .addHeader("Content-Type", "application/x-tcap")
                    .addQueryParameter("Cookie", _cookie)
                    .setBody(data)
                    .setConnectTimeioutMilliseconds(_connectionTimeout)
                    .response();
            // 受信バッファにレスポンス格納
            recvData = Objects.requireNonNull(response.body()).bytes();
        } catch (IOException ex) {
            _errorMessage = String.format(Locale.JAPAN, HTTP_ERR_STR_COMMUNICATION + " \n%s", ex.getMessage());
            lRetVal = FeliCaClient.FC_ERR_CLIENT_HTTP;
            return lRetVal;
        }

        // Cookieクリア
        _cookie = "";

        if(response.code() != 200) {
            // ステータスコード異常
            _errorMessage = String.format(Locale.JAPAN, HTTP_ERR_STR_RESPONSE_CODE + "%d", response.code());
            lRetVal = FeliCaClient.FC_ERR_CLIENT_HTTP;
            return lRetVal;
        }

        // Content-Type有無チェック
        boolean bFound = false;
        Headers headers = response.headers();
        for(int i = 0; i < headers.size(); i++) {
            if(headers.name(i).equals("Content-Type")) {
                bFound = true;
                if(!headers.value(i).contains("application/x-tcap")) {
                    _errorMessage = HTTP_ERR_STR_CONTENT_TYPE;
                    return FeliCaClient.FC_ERR_CLIENT_HTTP;
                }
            }
        }
        if(!bFound) {
            _errorMessage = HTTP_ERR_STR_CONTENT_TYPE;
            return FeliCaClient.FC_ERR_CLIENT_HTTP;
        }

        // Content-Lengthチェック
        boolean bEncoding = false;
        boolean bLength = false;

        for(int i = 0; i < headers.size(); i++) {
            if(headers.name(i).equals("Transfer-Encoding")) {
                if(headers.value(i).length() >= 1) {
                    bEncoding = true;
                }
            } else if(headers.name(i).equals("Content-Length")) {
                if(headers.value(i).length() >= 1) {
                    bLength = true;
                }
            } else if(headers.name(i).equals("Set-Cookie")) {
                // Cookie取得
                _cookie = headers.value(i);
            }
        }

        if(!bEncoding && !bLength) {
            _errorMessage = HTTP_ERR_STR_NO_CONTENT_LENGTH;
            return FeliCaClient.FC_ERR_CLIENT_HTTP;
        }

        if(0 < recvData.length && recvPacketList != null) {
            long errParse = TCAPPacketListParse(recvData, recvData.length, recvPacketList);

            if(errParse == FeliCaClient.FC_ERR_SUCCESS) {
                // 成功
                lRetVal = FeliCaClient.FC_ERR_SUCCESS;
            } else if(errParse == TCAPPacket.FC_ERR_TCAP_PACKET) {
                // パケットフォーマットエラー
                _tcapMessage = new MsgPacketFormatError(MsgPacketFormatError.errorMessage.PACKET_FORMAT_ERROR.getInt(), null, 0);
                lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
            } else {
                // 予期せぬエラー
                _tcapMessage = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), GetLastErrorStr().getBytes(), GetLastErrorStr().getBytes().length);
                lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
            }
        }

        // バッファ初期化
        sendData.clear();
        // パケットクリア
        sendPacketList.clear();

        Timber.tag("iCAS").d("★★★ recvPacketNum = %d", recvPacketList.size());

        /***********************************************************************************/
        /*** TCAP試験用コード ***/
        if(iCASClientTest.IsStarted()
        && (recvPacketList != null && recvPacketList.size() > 0 && recvPacketList.get(0).GetSubProtocolType() == TCAPPacket.TCAP_SPT_HAND_SHAKE)) {
            if(iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_HP_A_008
            || iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_FP_A_007) {
                // ここでキャンセル可能にしてキャンセルボタンを押す時間を確保するためスリープ
                _iFeliCaClientEventListener.OnCancelAvailable();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if(iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_HP_E_065
                   || iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_HP_E_069) {
                // ここで通信切断メッセージ表示して通信切断する時間を確保するためスリープ
                _iFeliCaClientEventListener.OnCommunicationOff();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if(iCASClientTest.IsStarted()
        && (recvPacketList != null && recvPacketList.size() > 0 && recvPacketList.get(0).GetSubProtocolType() == TCAPPacket.TCAP_SPT_FAREWELL)) {
            if (iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_HP_A_008
                    || iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_FP_A_007) {
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

        return lRetVal;
    }

/*
    public long ReceiveTCAPPacket(List<TCAPPacket> recvPacketList, TCAPMessage errorMessage) {
        long lRetVal = FeliCaClient.FC_ERR_CLIENT_UNKNOWN;

        // 強制中断チェック
        if(_bIsAborted) {
            _errorMessage = HTTP_ERR_STR_ABORTED;
            return FeliCaClient.FC_ERR_CLIENT_HTTP;
        }

        // エラーメッセージ初期化
        _errorMessage = "";

        // 受信パケットリストクリア
        recvPacketList.clear();

        if(_httpClient == null) {
            // 予期せぬエラー（内部エラー）
            errorMessage = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
            lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
        }

        byte[] recvData = null;
        Response response = null;

        // メソッド種別とURLを設定
        // 起動・業務開始時はGETによるリクエストのみ使用
        try {
            response = _httpClient
                    .get(_url)
                    .addHeader("Content-Type", "application/x-tcap; charset=UTF-8")
//                    .addQueryParameter("oP0619_3", "aaa")
                    .response();

            // 受信バッファにレスポンス格納
            recvData = Objects.requireNonNull(response.body()).bytes();

        } catch (IOException ex) {
            _errorMessage = String.format(Locale.JAPAN, HTTP_ERR_STR_COMMUNICATION + " \n%s", ex.getMessage());
            lRetVal = FeliCaClient.FC_ERR_CLIENT_HTTP;
            return lRetVal;
        }

        if(response.code() != 200) {
            // ステータスコード異常
            _errorMessage = String.format(Locale.JAPAN, HTTP_ERR_STR_RESPONSE_CODE + "%d", response.code());
            lRetVal = FeliCaClient.FC_ERR_CLIENT_HTTP;
            return lRetVal;
        }

        // Content-Type有無チェック
        boolean bFound = false;
        Headers headers = response.headers();
        for(int i = 0; i < headers.size(); i++) {
            if(headers.name(i).equals("Content-Type")) {
                bFound = true;
                if(!headers.value(i).contains("application/x-tcap")) {
                    _errorMessage = HTTP_ERR_STR_CONTENT_TYPE;
                    return FeliCaClient.FC_ERR_CLIENT_HTTP;
                }
            }
        }
        if(!bFound) {
            _errorMessage = HTTP_ERR_STR_CONTENT_TYPE;
            return FeliCaClient.FC_ERR_CLIENT_HTTP;
        }

        if(0 < recvData.length) {
            long errParse = TCAPPacketListParse(recvData, recvData.length, recvPacketList);

            if(errParse == FeliCaClient.FC_ERR_SUCCESS) {
                // 成功
                lRetVal = FeliCaClient.FC_ERR_SUCCESS;
            } else if(errParse == TCAPPacket.FC_ERR_TCAP_PACKET) {
                // パケットフォーマットエラー
                errorMessage = new MsgPacketFormatError(MsgPacketFormatError.errorMessage.PACKET_FORMAT_ERROR.getInt(), null, 0);
                lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
            } else {
                // 予期せぬエラー
                errorMessage = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
            }
        }

        return lRetVal;
    }
*/

    /**
     * データ解析
     * TCAPパケットリストのデータを解析します。
     *
     * @param data     Parseするデータ
     * @param dataSize Parseするデータのサイズ
     * @param tcapPacketList 格納するパケットリスト
     *
     * @return 格納したデータサイズ
     *         マイナス値の場合はエラー
     */
    private long TCAPPacketListParse(final byte[] data, int dataSize, List<TCAPPacket> tcapPacketList) {
        long lRetVal;

        _errorMessage = "";

        tcapPacketList.clear();

        if(data == null || dataSize < 1) {
            lRetVal = TCAPPacket.FC_ERR_TCAP_BAD_PARAM;
        } else {
            lRetVal = FeliCaClient.FC_ERR_SUCCESS;

            int dataCount = 0;

            while(dataCount < dataSize) {
                TCAPPacket tcapPacket = new TCAPPacket();
                /***********************************************************************************/
                /*** TCAP試験用コード ***/
                if(iCASClientTest.TCAP_TEST_MODE == iCASClientTest.TCAP_TEST_HP_E_075) {
                    tcapPacket = null;
                }
                /***********************************************************************************/

                if(tcapPacket == null) {
                    _errorMessage = HTTP_ERR_STR_NO_MEMORY;
                    lRetVal = TCAPPacket.FC_ERR_TCAP_NO_MEMORY;
                    break;
                } else {
                    byte[] packetData = new byte[dataSize-dataCount];
                    System.arraycopy(data, dataCount, packetData, 0, dataSize-dataCount);
                    long packetDataSize = tcapPacket.Parse(packetData, dataSize-dataCount);
                    if(packetDataSize < 1) {
                        // パケットデータ不正
                        lRetVal = TCAPPacket.FC_ERR_TCAP_PACKET;
                        break;
                    } else {
                        dataCount += packetDataSize;

                        // パケットリストにパケット追加
                        tcapPacketList.add(tcapPacket);
                    }
                }
            }
        }

        return lRetVal;
    }

    /**
     * TCAPパケットリストのバイナリ化
     * TCAPパケットリストのバイナリ化します。
     *
     * @param outBuffer 文字列格納先
     * @param tcapPacketList パケットリスト
     *
     * @return エラーコード
     */
    private long TCAPPacketListDump(ByteBuffer outBuffer, List<TCAPPacket> tcapPacketList) {
        long lRetVal = FeliCaClient.FC_ERR_SUCCESS;     // データなしの場合は成功
        long totalSize = 0;

        for(TCAPPacket packet : tcapPacketList) {
            lRetVal = packet.Dump(outBuffer);
            if(lRetVal < 0) {
                lRetVal = TCAPPacket.FC_ERR_TCAP_DUMP;
                break;
            }
            totalSize += lRetVal;
            lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        }

        if(TCAPPacket.TCAP_PACKETS_MAX_LENGTH < totalSize) {
            lRetVal = TCAPPacket.FC_ERR_TCAP_PACKETS_SIZE;
        }

        if(lRetVal >= 0) {
            // 読み込み準備
            outBuffer.flip();
        }

        return lRetVal;
    }

    /**
     * 最後に発生したエラー情報取得
     *
     * @return エラー文字列
     */
    public String GetLastErrorStr() {
        return _errorMessage;
    }

    /**
     * 通信の中断
     * 通信の中断を行います。
     */
    public void Cancel() {
        // 強制中断
        _bIsAborted = true;
    }

    public TCAPMessage GetTCAPMesasge() { return _tcapMessage; }

    public void SetListener(IFeliCaClientEventListener listener) {
        _iFeliCaClientEventListener = listener;
    }

    public IFeliCaClientEventListener GetListener() {
        return _iFeliCaClientEventListener;
    }

    public void SetParams(JSONObject params) { _params = params; }
}
