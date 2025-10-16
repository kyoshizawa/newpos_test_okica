package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.FeliCaClient;

public class TCAPPacket {
    private final int OFFSET_TO_MAJOR_VERSION     = 0;      // メジャーバージョンオフセット
    private final int OFFSET_TO_MINOR_VERSION     = 1;      // マイナーバージョンオブセット
    private final int OFFSET_TO_SUB_PROTOCOL_TYPE = 2;      // SPTオフセット
    private final int OFFSET_TO_LENGTH_HIGH       = 3;      // パケットデータ長上位バイトオフセット
    private final int OFFSET_TO_LENGTH_LOW        = 4;      // パケットデータ長下位バイトオフセット
    private final int HEADER_SIZE                 = 5;      // パケットヘッダサイズ

    // TCAPPacketErrorCodeより移植した定数群
    ////////////////////////////////////////////////////////////////////////
    public static final long FC_ERR_TCAP_UNKNOWN      = FeliCaClient.FC_ERR_TCAP_MODULE_BASE + 0x01;          // 不明なエラー
    public static final long FC_ERR_TCAP_BAD_PARAM    = FeliCaClient.FC_ERR_TCAP_MODULE_BASE + 0x02;          // パラメータ不正
    public static final long FC_ERR_TCAP_NO_MEMORY    = FeliCaClient.FC_ERR_TCAP_MODULE_BASE + 0x03;          // メモリ足らず
    public static final long FC_ERR_TCAP_PACKET       = FeliCaClient.FC_ERR_TCAP_MODULE_BASE + 0x04;          // パケット不正
    public static final long FC_ERR_TCAP_DUMP         = FeliCaClient.FC_ERR_TCAP_MODULE_BASE + 0x05;          // パケットデータのダンプに失敗
    public static final long FC_ERR_TCAP_HEAD_LENGTH  = FeliCaClient.FC_ERR_TCAP_MODULE_BASE + 0x06;          // パケットヘッダのレングス不正
    public static final long FC_ERR_TCAP_MESSAGE      = FeliCaClient.FC_ERR_TCAP_MODULE_BASE + 0x07;          // メッセージ不正
    public static final long FC_ERR_TCAP_PACKETS_SIZE = FeliCaClient.FC_ERR_TCAP_MODULE_BASE + 0x08;          // パケット列レングス不正

    // TCAPPacketDefより移植した定数群
    ////////////////////////////////////////////////////////////////////////
    // TCAPパケットのメッセージ最大長
    public static final int TCAP_MSG_MAX_LENGTH                  = 65535;
    ////////////////////////////////////////////////////////////////////////
    // TCAPパケットの最大長
    public static final int TCAP_PACKETS_MAX_LENGTH              = TCAP_MSG_MAX_LENGTH + 5;     // メッセージ最大サイズ + パケットヘッダサイズ
    ////////////////////////////////////////////////////////////////////////
    // TCAPパケットのVersion値定義
    public static final int TCAP_VERSION_25                      = 0x0205;
    ////////////////////////////////////////////////////////////////////////
    // TCAPパケットのSubProtocolType値定義
    public static final int TCAP_SPT_HAND_SHAKE                  = 0x01;
    public static final int TCAP_SPT_FAREWELL                    = 0x02;
    public static final int TCAP_SPT_ERROR                       = 0x03;
    public static final int TCAP_SPT_APPLICATION_DATA_TRANSFER   = 0x04;
    public static final int TCAP_SPT_UPDATE_ENTITY               = 0x05;
    public static final int TCAP_SPT_OPERATE_ENTITY              = 0x06;
    ////////////////////////////////////////////////////////////////////////
    // TCAPメッセージのExt値定義
    public static final int TCAP_MSG_EXT_STANDARD                = 0x00;
    public static final int TCAP_MSG_EXT_FELICA                  = 0x01;
    ////////////////////////////////////////////////////////////////////////
    // TCAPメッセージのDeviceID値定義
    public static final int TCAP_MSG_DEVICEID_INVALID            = 0x00;
    ////////////////////////////////////////////////////////////////////////
    // TCAPメッセージのMT値定義
    // 共通
//    public static final int TCAP_MSG_MT_FINISHED                 = 0x00;
    public static final int TCAP_MSG_MT_WARNING                  = 0x01;

    // HP
    // Client -> Server
    public static final int TCAP_MSG_MT_CLIENT_HELLO             = 0x21;
    public static final int TCAP_MSG_MT_CLIENT_HELLO_DONE        = 0x22;
    public static final int TCAP_MSG_MT_DEVICES                  = 0x25;
    public static final int TCAP_MSG_MT_FEATURES                 = 0x26;

    // Server -> Client
    public static final int TCAP_MSG_MT_SERVER_HELLO             = 0x23;
    public static final int TCAP_MSG_MT_SERVER_HELLO_DONE        = 0x24;
    public static final int TCAP_MSG_MT_ACCEPT                   = 0x81;

    // FP
    // Client -> Server
//    public static final int TCAP_MSG_MT_CLIENT_GOOD_BYE          = 0x21;
//    public static final int TCAP_MSG_MT_CLIENT_GOOD_BYE_DONE     = 0x22;

    // Server -> Client
    public static final int TCAP_MSG_MT_SERVER_GOOD_BYE          = 0x23;
    public static final int TCAP_MSG_MT_SERVER_GOOD_BYE_DONE     = 0x24;
    public static final int TCAP_MSG_MT_RETURN_CODE              = 0x25;

    // EP
    public static final int TCAP_MSG_MT_PACKET_FORMAT_ERROR      = 0x21;
    public static final int TCAP_MSG_MT_ILLEGAL_STATE_ERROR      = 0x22;
    public static final int TCAP_MSG_MT_UNEXPECTED_ERROR         = 0x23;

    // UEP
    // Server -> Client
    public static final int TCAP_MSG_MT_SET_NETWORK_TIMEOUT      = 0x81;
    public static final int TCAP_MSG_MT_REQUEST_ID               = 0x30;

    // 拡張
    // Server -> Client
//    public static final int TCAP_MSG_MT_SELECT_INTERNAL_FELICA   = 0x01;
    public static final int TCAP_MSG_MT_SET_FELICA_TIMEOUT       = 0x81;
    public static final int TCAP_MSG_MT_SET_RETRY_COUNT          = 0x82;

    // 拡張
    // Client -> Server
//    public static final int TCAP_MSG_MT_SELECTED_FELICA_DEVICE   = 0x03;

    // OEP
    // Server -> Client
    public static final int TCAP_MSG_MT_OPERATE_DEVICE           = 0x25;
//    public static final int TCAP_MSG_MT_PLAY_SOUND               = 0x81;

    // 拡張
    // Server -> Client
    public static final int TCAP_MSG_MT_OPEN_RW_REQUEST          = 0x01;
    public static final int TCAP_MSG_MT_CLOSE_RW_REQUEST         = 0x05;

    // Client -> Server
    public static final int TCAP_MSG_MT_DEVICE_RESPONSE          = 0x26;

    // 拡張
    // Client -> Server
    public static final int TCAP_MSG_MT_OPEN_RW_STATUS           = 0x02;
    public static final int TCAP_MSG_MT_CLOSE_RW_STATUS          = 0x06;

    // ADTP
    // Server -> Client
    public static final int TCAP_MSG_MT_FELICA_COMMAND           = 0x01;
    public static final int TCAP_MSG_MT_FELICA_PRECOMMAND        = 0x04;
    public static final int TCAP_MSG_MT_FELICA_EXCOMMAND         = 0x05;
    public static final int TCAP_MSG_MT_FELICA_COMMAND_THRURW    = 0x06;
    public static final int TCAP_MSG_MT_FELICA_PRECOMMAND_THRURW = 0x09;
    public static final int TCAP_MSG_MT_FELICA_EXCOMMAND_THRURW  = 0x0A;

    // Client -> Server
    public static final int TCAP_MSG_MT_FELICA_RESPONSE          = 0x02;
    public static final int TCAP_MSG_MT_FELICA_ERROR             = 0x03;
    public static final int TCAP_MSG_MT_FELICA_RESPONSE_THRURW   = 0x07;
    public static final int TCAP_MSG_MT_FELICA_ERROR_THRURW      = 0x08;
    ////////////////////////////////////////////////////////////////////////

    private char _version;                  // TCAPプロトコルバージョン
    private char _subProtocolType;          // サブプロトコルタイプ
    private byte _reserved1;
    private int _length;                    // TCAPメッセージ列のデータ長
    private final List<TCAPMessage> _messageList; // メッセージリスト

    public TCAPPacket() {
        _subProtocolType = 0;
        _length = 0;
        _messageList = new ArrayList<>(TCAP_MSG_MAX_LENGTH / 6);     // ( TCAPパケットのメッセージ最大長 / メッセージ最小長 )
    }

    public TCAPPacket(final TCAPPacket tcapPacket) {
        _subProtocolType = 0;
        _length = 0;
        _messageList = new ArrayList<>(TCAP_MSG_MAX_LENGTH / 6);     // ( TCAPパケットのメッセージ最大長 / メッセージ最小長 )

        Clear();

        if(this != tcapPacket) {
            // 初期化
            Clear();

            // バージョン
            _version = tcapPacket.GetVersion();

            // サブプロトコルタイプ
            _subProtocolType = tcapPacket.GetSubProtocolType();

            // レングス
            _length = tcapPacket.GetLength();

            // メッセージリスト
            int listNum = tcapPacket.GetMessageNum();

            for(int i = 0; i < listNum; i++) {
                TCAPMessage message = new TCAPMessage(tcapPacket.GetMessage(i));

                _messageList.add(message);
            }
        }
    }

    public TCAPPacket(char version, char subProtocolType) {
        _subProtocolType = subProtocolType;
        _version = version;
        _length = 0;
        _messageList = new ArrayList<>(TCAP_MSG_MAX_LENGTH / 6);     // ( TCAPパケットのメッセージ最大長 / メッセージ最小長 )
    }

    /**
     * データ解析
     * データを解析します。
     *
     * @param tcapBody Parseするデータ
     * @param tcapSize Parseするデータのサイズ
     *
     * @return 解析したデータのサイズ
     *         マイナス値の場合はエラー
     */
    public long Parse(final byte[] tcapBody, int tcapSize) {
        long lRetVal;

        if(tcapBody != null && tcapSize >= HEADER_SIZE) {
            // バージョン情報
            _version = (char)(tcapBody[OFFSET_TO_MAJOR_VERSION] << 8 | tcapBody[OFFSET_TO_MINOR_VERSION]);

            // サブプロトコルタイプ
            _subProtocolType = (char)tcapBody[OFFSET_TO_SUB_PROTOCOL_TYPE];

            // レングス
            _length = ((tcapBody[OFFSET_TO_LENGTH_HIGH] << 8) & 0x0000FF00) | (tcapBody[OFFSET_TO_LENGTH_LOW] & 0x000000FF);

            // レングスチェック
            if(tcapSize >= _length + HEADER_SIZE) {
                byte[] data = new byte[_length];
                System.arraycopy(tcapBody, 5, data, 0, _length);
                // メッセージ取得
                if(MessageListParse(data, _length) == FeliCaClient.FC_ERR_SUCCESS) {
                    lRetVal = _length + HEADER_SIZE;
                } else {
                    // メッセージ不正
                    lRetVal = TCAPPacket.FC_ERR_TCAP_MESSAGE;
                }
            } else {
                // レングス不正
                lRetVal = TCAPPacket.FC_ERR_TCAP_HEAD_LENGTH;
            }
        } else {
            // パラメータ不正
            lRetVal = TCAPPacket.FC_ERR_TCAP_BAD_PARAM;
        }

        return lRetVal;
    }

    /**
     * データ解析
     * データを解析します。
     *
     * @param tcapBody Parseするデータ
     * @param length   Parseするデータのサイズ
     *
     * @return 解析したデータのサイズ
     *         マイナス値の場合はエラー
     */
    private long MessageListParse(final byte[] tcapBody, int length) {
        long lRetVal;

        // 初期化
        _messageList.clear();

        if(length < 1) {
            // bodyなし。正常終了。
            lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        } else if(tcapBody == null && length > 1) {
            // パラメータエラー
            lRetVal = TCAPPacket.FC_ERR_TCAP_BAD_PARAM;
        } else {
            lRetVal = 0;

            int dataCount = 0;

            while(dataCount < length) {
                TCAPMessage message = new TCAPMessage((char)0, (char)0, (char)0);

                byte[] data = new byte[length - dataCount];
                System.arraycopy(tcapBody, dataCount, data, 0, length - dataCount);
                int messageDataSize = (int)message.Parse(data, length - dataCount);

                if(messageDataSize < 1) {
                    // パケットデータ不正
                    lRetVal = TCAPPacket.FC_ERR_TCAP_PACKET;
                    break;
                } else {
                    dataCount += messageDataSize;
                    _messageList.add(message);
                }
            }
        }
        return lRetVal;
    }

    /**
     * TCAPパケットをバイナリ化
     * TCAPパケットをバイナリ化します。
     *
     * @param outBuffer 文字列格納先
     *
     * @return 格納したデータサイズ
     *         マイナス値の場合はエラー
     */
    public long Dump(ByteBuffer outBuffer) {
        long lRetVal;

        // ヘッダ部ダンプ
        byte[] packetHead = new byte[HEADER_SIZE];

        packetHead[OFFSET_TO_MAJOR_VERSION]      = (byte)GetMajorVersion();
        packetHead[OFFSET_TO_MINOR_VERSION]      = (byte)GetMinorVersion();
        packetHead[OFFSET_TO_SUB_PROTOCOL_TYPE]  = (byte)GetSubProtocolType();
        packetHead[OFFSET_TO_LENGTH_HIGH]    = (byte)(GetLength() >> 8);
        packetHead[OFFSET_TO_LENGTH_LOW]     = (byte)GetLength();

        // ヘッダ部分格納
        outBuffer.put(packetHead);

        // 各メッセージのダンプ
        lRetVal = MessageListDump(outBuffer);
        if(lRetVal >= 0) {
            lRetVal = lRetVal + HEADER_SIZE;
        }

        return lRetVal;
    }

    /**
     * TCAPメッセージのバイナリ化
     * TCAPメッセージのバイナリ化します。
     *
     * @param outBuffer 文字列格納先
     *
     * @return 格納したデータサイズ
     *         マイナス値の場合はエラー
     */
    private long MessageListDump(ByteBuffer outBuffer) {
        long lRetVal = FeliCaClient.FC_ERR_SUCCESS;     // データがない（空の応答パケットの場合）は成功とする
        long lTotalSize = 0;

        for(TCAPMessage message : _messageList) {
            lRetVal = message.Dump(outBuffer);
            if(lRetVal < 0) {
                lRetVal = TCAPPacket.FC_ERR_TCAP_DUMP;
                break;
            }
            lTotalSize += lRetVal;
        }

        if(lRetVal >= 0) {
            lRetVal = lTotalSize;
        }

        return lRetVal;
    }

    /**
     * メッセージ追加
     * メッセージを追加します。
     *
     * @param tcapMessage 追加メッセージ
     *
     * @return エラーコード
     */
    public long AddMessage(TCAPMessage tcapMessage) {
        long lRetVal = TCAPPacket.FC_ERR_TCAP_UNKNOWN;

        if(tcapMessage != null) {
            if(_messageList.add(tcapMessage)) {
                _length += tcapMessage.GetSize();
                lRetVal = FeliCaClient.FC_ERR_SUCCESS;
            }
        } else {
            // パラメータ不正
            lRetVal = TCAPPacket.FC_ERR_TCAP_BAD_PARAM;
        }

        return lRetVal;
    }

    /**
     * TCAPプロトコルバージョン取得
     * TCAPプロトコルバージョンを取得します。
     *
     * @return TCAPプロトコルバージョン
     */
    public char GetVersion() {
        return _version;
    }

    /**
     * TCAPプロトコルメジャーバージョン取得
     * TCAPプロトコルメジャーバージョンを取得します。
     *
     * @return TCAPプロトコルメジャーバージョン
     */
    public char GetMajorVersion() {
        return (char)(_version >> 8);
    }

    /**
     * TCAPプロトコルマイナーバージョン取得
     * TCAPプロトコルマイナーバージョンを取得します。
     *
     * @return TCAPプロトコルマイナーバージョン
     */
    public char GetMinorVersion() {
        return (char)(_version & 0x000000FF);
    }

    /**
     * TCAPプロトコルバージョン設定
     * TCAPプロトコルバージョンを設定します。
     *
     * @param version TCAPプロトコルバージョン
     */
    public void SetVersion(char version) {
        _version = version;
    }

    /**
     * サブプロトコルタイプ取得
     * サブプロトコルタイプを取得します。
     *
     * @return サブプロトコルタイプ
     */
    public char GetSubProtocolType() {
        return _subProtocolType;
    }

    /**
     * サブプロトコルタイプ設定
     * サブプロトコルタイプを設定します。
     *
     * @param subProtocolType サブプロトコルタイプ
     */
    public void SetSubProtocolType(char subProtocolType) {
        _subProtocolType = subProtocolType;
    }

    /**
     * メッセージ列のデータサイズ取得
     * データサイズを取得します。
     *
     * @return データサイズ
     */
    public int GetLength() {
        return _length;
    }

    /**
     * パケットサイズ取得
     * パケットサイズを取得します。
     *
     * @return パケットサイズ
     */
    public long GetPacketSize() {
        return _length + HEADER_SIZE;
    }

    /**
     * メッセージ数取得
     * メッセージ数を取得します。
     *
     * @return メッセージ数
     */
    public int GetMessageNum() {
        return _messageList.size();
    }

    /**
     * メッセージ取得
     * 指定したメッセージを取得します。
     *
     * @param index メッセージのインデックス
     *
     * @return 指定したメッセージ　存在しない場合はnull
     */
    public TCAPMessage GetMessage(int index) {
        TCAPMessage message;

        message = _messageList.get(index);

        return message;
    }

    /**
     * メッセージ変更
     * 指定したメッセージを変更します。
     *
     * @param index   メッセージのインデックス
     * @param message TCAPメッセージ
     *
     * @return 変更結果
     */
    public boolean ChangeMessage(int index, TCAPMessage message) {
        boolean bRetVal = false;

        if(message != null) {
            _messageList.set(index, message);
            bRetVal = true;
        }

        return bRetVal;
    }

    /**
     * メッセージリスト取得
     * TCAPパケットのメッセージリストを取得します。
     *
     * @return TCAPメッセージリスト
     */
    public List<TCAPMessage> MessageList() {
        return _messageList;
    }

    private void Clear() {
        _version = 0;
        _subProtocolType = 0;
        _length = 0;
    }
}
