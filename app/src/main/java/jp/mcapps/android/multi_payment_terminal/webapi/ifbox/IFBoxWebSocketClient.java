package jp.mcapps.android.multi_payment_terminal.webapi.ifbox;

import android.os.Handler;
import android.os.HandlerThread;

import com.google.gson.Gson;
import com.neovisionaries.ws.client.ThreadType;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketListener;
import com.neovisionaries.ws.client.WebSocketOpcode;
import com.neovisionaries.ws.client.WebSocketState;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.net.SocketFactory;

import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;
import timber.log.Timber;

public class IFBoxWebSocketClient {
    private static final int CONNECTION_TIMEOUT_MS = 10000;
    private static final int SOCKET_TIMEOUT_MS = 10000;
    private static final int PING_INTERVAL_MS = 5000;
    private static final int RECONNECTION_INTERVAL_MS = 5000;
    private static final int ALIVE_INTERVAL = 10000;
    public static int PORT = 81;

    private final Handler _handler;
    private boolean isAccOn_IMA820 = true;

    private WebSocket _ws;
    private String _endpoint;
    private Gson _gson = new Gson();
    private SocketFactory _socketFactory = null;
    public enum ConnectionStatus {
        Disconnected("未接続"),
        Connecting("接続中"),
        Connected("接続完了"),
        Reconnecting("再接続"),
        ;

        private String _status;
        ConnectionStatus(String status) {
            _status = status;
        }

        @Override
        public String toString() {
            return "WebSocket接続状態: " + this._status;
        }
    }

    public void setSocketFactory(SocketFactory socketFactory) {
        _socketFactory = socketFactory;
    }

    /* init */ {
        final HandlerThread ht = new HandlerThread(getClass().getSimpleName());
        ht.start();
        _handler = new Handler(ht.getLooper());
    }

    private PublishSubject<String> _receiveText = PublishSubject.create();
    public PublishSubject<String> getReceiveText() {
        return _receiveText;
    }

    private BehaviorSubject<ConnectionStatus> _connectionStatus = BehaviorSubject.createDefault(ConnectionStatus.Disconnected);
    public BehaviorSubject<ConnectionStatus> getConnectionStatus() {
        return _connectionStatus;
    }
    public void setConnectionStatus(ConnectionStatus status) {
        if (_connectionStatus.getValue() != status) {

            if (status == ConnectionStatus.Connected) {
                isAccOn_IMA820 = true;
            }

            Timber.i("%s", status);
            _connectionStatus.onNext(status);
        }
    }

    public void connect(String endpoint) {
        _handler.post(() ->  connectSync(endpoint));
    }

    public void connectSync(String endpoint) {
        _handler.removeCallbacksAndMessages(null);
        _endpoint = endpoint;

        try {
            final WebSocket ws = _socketFactory != null
                    ? new WebSocketFactory()
                    .setSocketFactory(_socketFactory)
                    .setConnectionTimeout(CONNECTION_TIMEOUT_MS)
                    .setSocketTimeout(SOCKET_TIMEOUT_MS)
                    .createSocket(URI.create(endpoint))
                    : new WebSocketFactory()
                    .setSocketTimeout(SOCKET_TIMEOUT_MS)
                    .setConnectionTimeout(CONNECTION_TIMEOUT_MS)
                    .setSocketTimeout(SOCKET_TIMEOUT_MS)
                    .createSocket(URI.create(endpoint));

            ws.addListener(new WebSocketListenerImpl());
            ws.setPingInterval(PING_INTERVAL_MS);
            ws.connect();

            disconnect();
            _ws = ws;
            setConnectionStatus(ConnectionStatus.Connecting);
        } catch (Exception e) {
            Timber.e(e);
            if (_endpoint != null) {
                reconnect();
            } else {
                Timber.e("_endpoint = null");
                setConnectionStatus(ConnectionStatus.Disconnected);
            }
        }
    }

    public void reconnect() {
        setConnectionStatus(ConnectionStatus.Reconnecting);
        //Timber.i("reconnect WebSocket");
        Timber.i("reconnect WebSocket endpoint:%s", _endpoint);
        _handler.postDelayed(() -> connectSync(_endpoint), RECONNECTION_INTERVAL_MS);
    }

    public void disconnect() {
        if (_ws != null) {
            Timber.i("disconnect WebSocket");
            _ws.disconnect();
            _ws = null;
        } else {
            Timber.e("disconnect->_ws = null");
        }
    }

    public boolean broadcastTXT(String sendMsg) {
        if (_ws != null) {
            _ws.sendText(sendMsg);
            return true;
        } else {
            Timber.e("broadcastTXT->_ws = null");
            return false;
        }
    }

    public void setAccOff_IMA820() {
        isAccOn_IMA820 = false;
    }

    public void setAccOn_IMA820() {
        isAccOn_IMA820 = true;
    }

    public boolean isAccOn_IMA820() {
        return isAccOn_IMA820;
    }

    public int getAliveInterval() { return ALIVE_INTERVAL; }

    private class WebSocketListenerImpl implements WebSocketListener {
        private boolean _receivedPongFrame = true;

        @Override
        public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception {
            //Timber.d("onStateChanged: %s", newState);
            Timber.i("onStateChanged: %s", newState);
        }

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            //Timber.d("onConnected");
            Timber.i("onConnected");
            setConnectionStatus(ConnectionStatus.Connected);
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException cause) throws Exception {
            if (cause != null) {
                Timber.e("onConnectError %s", cause.getMessage());
            } else {
                Timber.e("onConnectError");
            }
        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
            WebSocketFrame frame = closedByServer
                    ? serverCloseFrame
                    : clientCloseFrame;

            try {
                //Timber.d("onDisconnected: %s", frame.getCloseReason());
                if (frame != null) {
                    Timber.i("onDisconnected: %s", frame.getCloseReason());

                    if (frame.getCloseCode() != 1000) { // 通常のclose以外だったら再接続
                        reconnect();
                    }
                } else {
                    Timber.e("onDisconnected: 【現場発生ケース】 WebSocket再接続実施!!!");
                    reconnect();
                }
            } catch (Exception e) {
                Timber.e(e);
                setConnectionStatus(ConnectionStatus.Disconnected);
            }
        }

        @Override
        public void onFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            Timber.d("onFrame");
        }

        @Override
        public void onContinuationFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            Timber.d("onContinuationFrame");
        }

        @Override
        public void onTextFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            try {
                final String payloadText = frame.getPayloadText();

                Timber.d("onTextFrame: %s", payloadText);
                if (payloadText.contains("Setting renew OK")) {
                    Timber.i("パラメータ設定保存成功");
                }
                if (payloadText.contains("Rebooting...")) {
                    Timber.i("IM-A820リブート");
                }
                _receiveText.onNext(payloadText);
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        @Override
        public void onBinaryFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            Timber.d("onBinaryFrame");
        }

        @Override
        public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            Timber.d("onCloseFrame");
        }

        @Override
        public void onPingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            Timber.d("onPingFrame");
        }

        @Override
        public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            _receivedPongFrame = true;
            Timber.d("onPongFrame");
            setConnectionStatus(ConnectionStatus.Connected);
        }

        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            Timber.d("onTextMessage");
        }

        @Override
        public void onTextMessage(WebSocket websocket, byte[] data) throws Exception {
            Timber.d("onTextMessage");
        }

        @Override
        public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {
            Timber.d("onBinaryMessage");
        }

        @Override
        public void onSendingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            try {
                // タブレット経由で接続するとIF-BOXの電源が切れてもDisconnectイベントが発火しないのでPongで切断を判断する
                if (frame.getOpcode() == WebSocketOpcode.PING) {
                    if (!_receivedPongFrame) {
                        Timber.d("Unreceived Pong Frame");
                        reconnect();
                        return;
                    }
                    _receivedPongFrame = false;
                }

                Timber.d("onSendingFrame: %s", frame);
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        @Override
        public void onFrameSent(WebSocket websocket, WebSocketFrame frame) throws Exception {
            Timber.d("onFrameSent");
        }

        @Override
        public void onFrameUnsent(WebSocket websocket, WebSocketFrame frame) throws Exception {
            Timber.d("onFrameUnsent");
        }

        @Override
        public void onThreadCreated(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {
            Timber.d("onThreadCreated ThreadType: %s", threadType);
        }

        @Override
        public void onThreadStarted(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {
            Timber.d("onThreadStarted");
        }

        @Override
        public void onThreadStopping(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {
            Timber.d("onThreadStopping: %s", threadType);
        }

        @Override
        public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
            if (cause != null) {
                Timber.e("onError %s", cause.getMessage());
            } else {
                Timber.e("onError");
            }
        }

        @Override
        public void onFrameError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
            if (cause != null) {
                Timber.e("onFrameError %s", cause.getMessage());
            } else {
                Timber.e("onFrameError");
            }
        }

        @Override
        public void onMessageError(WebSocket websocket, WebSocketException cause, List<WebSocketFrame> frames) throws Exception {
            if (cause != null) {
                Timber.e("onMessageError %s", cause.getMessage());
            } else {
                Timber.e("onMessageError");
            }
        }

        @Override
        public void onMessageDecompressionError(WebSocket websocket, WebSocketException cause, byte[] compressed) throws Exception {
            if (cause != null) {
                Timber.e("onMessageDecompressionError %s", cause.getMessage());
            } else {
                Timber.e("onMessageDecompressionError");
            }
        }

        @Override
        public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data) throws Exception {
            if (cause != null) {
                Timber.e("onTextMessageError %s", cause.getMessage());
            } else {
                Timber.e("onTextMessageError");
            }
        }

        @Override
        public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
            if (cause != null) {
                Timber.e("onSendError %s", cause.getMessage());
            } else {
                Timber.e("onSendError");
            }
        }

        @Override
        public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
            if (cause != null) {
                Timber.e("onUnexpectedError %s", cause.getMessage());
            } else {
                Timber.e("onUnexpectedError");
            }
        }

        @Override
        public void handleCallbackError(WebSocket websocket, Throwable cause) throws Exception {
            if (cause != null) {
                Timber.e("handleCallbackError %s", cause.getMessage());
            } else {
                Timber.e("handleCallbackError");
            }
            setConnectionStatus(ConnectionStatus.Disconnected);
        }

        @Override
        public void onSendingHandshake(WebSocket websocket, String requestLine, List<String[]> headers) throws Exception {
            Timber.d("onSendingHandshake: %s", requestLine);
        }
    }
}
