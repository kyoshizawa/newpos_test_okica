package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule;

import java.nio.ByteBuffer;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.FeliCaClient;

public class TCAPMessage {
    private final int OFFSET_TO_EXTENSION = 0;              // エクステンションオフセット
    private final int OFFSET_TO_DEVICE_ID_HIGH = 1;         // デバイスID上位バイトオフセット
    private final int OFFSET_TO_DEVICE_ID_LOW = 2;          // デバイスID下位バイトオフセット
    private final int OFFSET_TO_MESSAGE_TYPE = 3;           // メッセージタイプオフセット
    private final int OFFSET_TO_LENGTH_HIGH = 4;            // メッセージデータ長上位バイトオフセット
    private final int OFFSET_TO_LENGTH_LOW = 5;             // メッセージデータ長下位バイトオフセット
    private final int HEADER_SIZE = 6;                      // メッセージヘッダサイズ

    private char _extension;           // エクステンション
    private char _messageType;         // メッセージタイプ
    private char _deviceID;            // デバイスID
    private char _length;               // データ長
    private byte[] _messageData;       // メッセージデータ

    public TCAPMessage(TCAPMessage tcapMessage) {
        _extension = 0;
        _deviceID = 0;
        _messageType = 0;
        _length = 0;
        _messageData = null;

        if(this != tcapMessage) {
            // 初期化
            Clear();

            // エクステンション
            _extension = tcapMessage._extension;
            // デバイスID
            _deviceID = tcapMessage._deviceID;
            // メッセージタイプ
            _messageType = tcapMessage._messageType;
            // レングス
            _length = tcapMessage._length;

            if(0 < tcapMessage._length && tcapMessage._messageData != null) {
                _messageData = new byte[_length];
                System.arraycopy(tcapMessage._messageData, 0, _messageData, 0, tcapMessage._length);
            } else {
                _messageData = null;
            }
        }
    }

    public TCAPMessage(char ext, char devID, char mt) {
        _extension = ext;
        _deviceID = devID;
        _messageType = mt;
        _length = 0;
        _messageData = null;
    }

    /**
     * データ解析
     * データを解析します。
     *
     * @param tcapMessage Parseするデータ
     * @param length      Parseするデータのサイズ
     *
     * @return 解析したデータのサイズ
     *         マイナス値の場合はエラー
     */
    public long Parse(final byte[] tcapMessage, int length) {
        long lRetVal;

        Clear();

        if(tcapMessage != null && length >= HEADER_SIZE) {
            // エクステンション
            _extension = (char)tcapMessage[OFFSET_TO_EXTENSION];
            // デバイスID
            _deviceID = (char)(tcapMessage[OFFSET_TO_DEVICE_ID_HIGH] << 8 | tcapMessage[OFFSET_TO_DEVICE_ID_LOW]);
            // MT値
            _messageType = (char)(tcapMessage[OFFSET_TO_MESSAGE_TYPE] & 0x000000FF);
            // データのサイズ
            _length = (char)(((tcapMessage[OFFSET_TO_LENGTH_HIGH] << 8) & 0x0000FF00) | (tcapMessage[OFFSET_TO_LENGTH_LOW] & 0x000000FF));

            // データ部
            if(_length == 0) {
                lRetVal = HEADER_SIZE;
            } else if(length - HEADER_SIZE < _length) {
                lRetVal = TCAPPacket.FC_ERR_TCAP_PACKETS_SIZE;
            } else {
                _messageData = new byte[_length];
                System.arraycopy(tcapMessage, HEADER_SIZE, _messageData, 0, _length);
                lRetVal = _length + HEADER_SIZE;
            }
        } else {
            // パラメータ不正
            lRetVal = TCAPPacket.FC_ERR_TCAP_BAD_PARAM;
        }

        return lRetVal;
    }

    /**
     * TCAPメッセージのバイナリ化
     * TCAPメッセージのバイナリ化します。
     *
     * @param outBuffer 文字列バッファ
     *
     * @return 解析したデータのサイズ
     *         マイナス値の場合はエラー
     */
    public long Dump(final ByteBuffer outBuffer) {
        long lRetVal;
        byte[] messageHead = new byte[HEADER_SIZE];

        messageHead[OFFSET_TO_EXTENSION]      = (byte)_extension;
        messageHead[OFFSET_TO_DEVICE_ID_HIGH] = (byte)(_deviceID >> 8);
        messageHead[OFFSET_TO_DEVICE_ID_LOW]  = (byte)_deviceID;
        messageHead[OFFSET_TO_MESSAGE_TYPE]   = (byte)_messageType;
        messageHead[OFFSET_TO_LENGTH_HIGH]    = (byte)(_length >> 8);
        messageHead[OFFSET_TO_LENGTH_LOW]     = (byte)_length;

        // メッセージヘッダ
        outBuffer.put(messageHead);

        // データ部
        if(_length > 0) {
            outBuffer.put(_messageData);
        }

        lRetVal = _length + HEADER_SIZE;

        return lRetVal;
    }

    /**
     * エクステンション設定
     * エクステンションを設定します。
     *
     * @param ext エクステンション
     */
    public void SetExtension(char ext) {
        _extension = ext;
    }

    /**
     * エクステンション取得
     * エクステンションを取得します。
     *
     * @return エクステンション
     */
    public char GetExtension() {
        return _extension;
    }

    /**
     * デバイスID設定
     * デバイスIDを設定します。
     *
     * @param devID デバイスID
     */
    public void SetDeviceID(char devID) {
        _deviceID = devID;
    }

    /**
     * デバイスID取得
     * デバイスIDを取得します。
     *
     * @return デバイスID
     */
    public char GetDeviceID() {
        return _deviceID;
    }

    /**
     * メッセージタイプ設定
     * メッセージタイプを設定します。
     *
     * @param mt メッセージタイプ
     */
    public void SetMessageType(char mt) {
        _messageType = mt;
    }

    /**
     * メッセージタイプ取得
     * メッセージタイプを取得します。
     *
     * @return メッセージタイプ
     */
    public char GetMessageType() {
        return _messageType;
    }

    /**
     * データ取得
     * TCAPメッセージに付随するデータを取得します。
     *
     * @return メッセージデータ
     */
    public byte[] GetMessageData() {
        return _messageData;
    }

    /**
     * データ設定
     * TCAPメッセージに付随するデータを設定します。
     *
     * @param messageData データ
     * @param length      データ長
     *
     * @return エラー
     */
    public long SetMessageData(byte[] messageData, int length) {
        long lRetVal = TCAPPacket.FC_ERR_TCAP_UNKNOWN;

        _messageData = null;

        if(messageData == null) {
            _length = 0;
            lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        } else {
            _messageData = new byte[length];

            System.arraycopy(messageData, 0, _messageData, 0, length);
            _length = (char)length;
            lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        }

        return lRetVal;
    }

    /**
     * データ更新
     * TCAPメッセージに付随するデータを更新します。
     *
     * @param messageData データ
     * @param length      データ長
     * @param destPos     コピー先の開始位置
     *
     * @return エラー
     */
    public long UpdateMessageData(byte[] messageData, int length, int destPos) {
        long lRetVal = TCAPPacket.FC_ERR_TCAP_UNKNOWN;

        if(messageData == null) {
            _length = 0;
            lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        } else {
            if(_messageData.length < destPos + length) {
                // コピー不可
            } else {
                System.arraycopy(messageData, 0, _messageData, destPos, length);
                lRetVal = FeliCaClient.FC_ERR_SUCCESS;
            }
        }

        return lRetVal;
    }

    /**
     * データサイズ
     * TCAPメッセージに付随するデータサイズを取得します。
     *
     * @return データサイズ
     */
    public int GetLength() {
        return _length;
    }

    /**
     * メッセージサイズ取得
     * TCAPメッセージのサイズを取得します。
     *
     * @return TCAPメッセージサイズ
     */
    public long GetSize() {
        return _length + HEADER_SIZE;
    }

    /**
     * フォーマットの正当性確認
     * フォーマットの正当性チェックします。
     *
     * @return true  正常
     *         false 異常
     */
    public boolean ValidateFormat() {
        boolean bRetVal = false;

        if(_length == 0 && _messageData == null) {
            bRetVal = true;
        }

        return bRetVal;
    }

    /**
     * データ部内容の正当性確認
     * データ部内容の正当性チェックします。
     *
     * @return true  正常
     *         false 異常
     */
    public boolean ValidateData() {
        return true;
    }

    /**
     * ASCII文字判定
     * データがASCII文字か否かを判定します。
     *
     * @param checkStr  データ
     * @param checkSize データサイズ
     *
     * @return true  ASCII文字
     *         false ASCII文字でない
     */
    public boolean IsAsciiCode(byte[] checkStr, int checkSize) {
        boolean bRetVal = true;

        if(checkStr == null && checkSize > 0) {
            bRetVal = false;
        } else {
            for(int lCnt = 0; lCnt < checkSize; lCnt++) {
                if((checkStr[lCnt] < 0x20) || (checkStr[lCnt] > 0x7E)) {
                    bRetVal = false;
                    break;
                }
            }
        }

        return bRetVal;
    }

    private void Clear() {
        _extension = 0;
        _deviceID = 0;
        _messageType = 0;
        _length = 0;
        _messageData = null;
    }
}
