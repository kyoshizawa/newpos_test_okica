package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

import java.util.ArrayList;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPPacket;
import timber.log.Timber;

public class TCAPContext {
    private final List<TCAPPacket> _sendPacketList;           // 送信パケットリスト
    private final List<TCAPPacket> _recvPacketList;           // 受信パケットリスト
    private int _returnCode;                                  // TCAP通信で取得する終了コード
    private boolean _bIsStop;                                 // 終了フラグ
    private char _version;                                    // TCAPバージョン
    private byte _Reserved1;
    private byte _Reserved2;
    private byte[] _errorMessage;                             // エラーメッセージ
    private final List<DeviceElement> _deviceList;            // デバイスリスト

    final private int MAX_MESSAGE_SIZE = TCAPPacket.TCAP_PACKETS_MAX_LENGTH;   // パケット最大長
    final private int MAX_PACKET_NUM   = MAX_MESSAGE_SIZE/(5+6);    // 最大パケット数

    final static public int MAX_LIST_NUM = 64;      // デバイスリスト最大数

    public TCAPContext() {
        _sendPacketList = new ArrayList<>(MAX_PACKET_NUM);
        _recvPacketList = new ArrayList<>(MAX_PACKET_NUM);
        _deviceList = new ArrayList<>(MAX_LIST_NUM);
        _returnCode = IFeliCaClient.errorCode.ERR_NONE.getInt();
        _bIsStop = false;
        _version = 0;
        _errorMessage = null;
    }

    /**
     * 送信パケットリスト取得
     * 送信パケットリストを取得します。
     *
     * @return 送信パケットリスト
     */
    public List<TCAPPacket> GetSendPacketList() {
        return _sendPacketList;
    }

    /**
     * 受信パケットリスト取得
     * 受信パケットリストを取得します。
     *
     * @return 受信パケットリスト
     */
    public List<TCAPPacket> GetRecvPacketList() {
        return _recvPacketList;
    }

    /**
     * デバイスリスト取得
     * デバイスリストを取得します。
     *
     * @return デバイスリスト
     */
    public List<DeviceElement> GetDeviceList() {
        return _deviceList;
    }

    /**
     * TCAP通信バージョン設定
     * TCAP通信バージョンを設定します。
     *
     * @param version TCAP通信バージョン
     */
    public void SetVersion(char version) {
        _version = version;
    }

    /**
     * TCAP通信バージョン取得
     * TCAP通信バージョンを取得します。
     *
     * @return TCAP通信バージョン
     */
    public int GetVersion() {
        return _version;
    }

    /**
     * TCAP通信のリターンコード設定
     * TCAP通信のリターンコードを設定します。
     *
     * @param returnCode TCAP通信のリターンコード
     */
    public void SetReturnCode(int returnCode) {
        _returnCode = returnCode;
    }

    /**
     * TCAP通信のリターンコード取得
     * TCAP通信のリターンコードを取得します。
     *
     * @return TCAP通信のリターンコード
     */
    public int GetReturnCode() {
        return _returnCode;
    }

    /**
     * エラーメッセージの設定
     * エラーメッセージを設定します。
     *
     * @param errorMessage エラーメッセージ
     */
    public void SetErrorMessage(byte[] errorMessage) {
        if(errorMessage != null && errorMessage.length > 0) {
            Timber.tag("iCAS").d("TCAPContext SetErrorMessage %s", new String(errorMessage));
            _errorMessage = new byte[errorMessage.length];
            System.arraycopy(errorMessage, 0, _errorMessage, 0, errorMessage.length);
        }
    }

    /**
     * エラーメッセージの取得
     * エラーメッセージを取得します。
     *
     * @return エラーメッセージ
     */
    public byte[] GetErrorMessage() {
        return _errorMessage;
    }

    /**
     * 通信中断フラグ取得
     * 通信中断フラグを取得します。
     *
     * @return 通信中断フラグ
     */
    public boolean IsStop() {
        return _bIsStop;
    }

    /**
     * 通信中断フラグをONにする
     * 通信中断フラグをONに設定します。
     */
    public void Stop() {
        // 中止
        _bIsStop = true;

        // デバイスリストのデバイスにキャンセル送信
        for(DeviceElement deviceElement : _deviceList) {
            if(deviceElement != null) {
                deviceElement.Cancel();
            }
        }
    }

    /**
     * コンテキストの初期化します。
     * コンテキストの内容を初期化します。
     */
    public void Clear() {
        _returnCode = IFeliCaClient.errorCode.ERR_NONE.getInt();
        _bIsStop = false;
        _version = 0;
        _errorMessage = null;
    }
}
