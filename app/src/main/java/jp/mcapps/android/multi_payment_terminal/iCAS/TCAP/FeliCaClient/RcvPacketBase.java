package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

import java.util.List;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgPacketFormatError;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgUnexpectedError;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPMessage;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPPacket;

public abstract class RcvPacketBase extends TCAPPacket {
    // 共通
//    public static final int MID_FINISHED                  = (TCAPPacket.TCAP_MSG_EXT_STANDARD << 8) | (TCAPPacket.TCAP_MSG_MT_FINISHED);
    public static final int MID_WARNING                   = (TCAPPacket.TCAP_MSG_EXT_STANDARD << 8) | (TCAPPacket.TCAP_MSG_MT_WARNING);
    // HP
    public static final int MID_SERVER_HELLO              = (TCAPPacket.TCAP_MSG_EXT_STANDARD << 8) | (TCAPPacket.TCAP_MSG_MT_SERVER_HELLO);
    public static final int MID_SERVER_HELLO_DONE         = (TCAPPacket.TCAP_MSG_EXT_STANDARD << 8) | (TCAPPacket.TCAP_MSG_MT_SERVER_HELLO_DONE);
    public static final int MID_ACCEPT                    = (TCAPPacket.TCAP_MSG_EXT_STANDARD << 8) | (TCAPPacket.TCAP_MSG_MT_ACCEPT);
    public static final int MID_SERVER_GOOD_BYE           = (TCAPPacket.TCAP_MSG_EXT_STANDARD << 8) | (TCAPPacket.TCAP_MSG_MT_SERVER_GOOD_BYE);
    public static final int MID_SERVER_GOOD_BYE_DONE      = (TCAPPacket.TCAP_MSG_EXT_STANDARD << 8) | (TCAPPacket.TCAP_MSG_MT_SERVER_GOOD_BYE_DONE);
    public static final int MID_RETURN_CODE               = (TCAPPacket.TCAP_MSG_EXT_STANDARD << 8) | (TCAPPacket.TCAP_MSG_MT_RETURN_CODE);
    // ADTP
    public static final int MID_FELICA_COMMAND            = (TCAPPacket.TCAP_MSG_EXT_FELICA   << 8) | (TCAPPacket.TCAP_MSG_MT_FELICA_COMMAND);
    public static final int MID_FELICA_COMMAND_THRURW     = (TCAPPacket.TCAP_MSG_EXT_FELICA   << 8) | (TCAPPacket.TCAP_MSG_MT_FELICA_COMMAND_THRURW);
    public static final int MID_FELICA_PRECOMMAND         = (TCAPPacket.TCAP_MSG_EXT_FELICA   << 8) | (TCAPPacket.TCAP_MSG_MT_FELICA_PRECOMMAND);
    public static final int MID_FELICA_PRECOMMAND_THRURW  = (TCAPPacket.TCAP_MSG_EXT_FELICA   << 8) | (TCAPPacket.TCAP_MSG_MT_FELICA_PRECOMMAND_THRURW);
    public static final int MID_FELICA_EXCOMMAND          = (TCAPPacket.TCAP_MSG_EXT_FELICA   << 8) | (TCAPPacket.TCAP_MSG_MT_FELICA_EXCOMMAND);
    public static final int MID_FELICA_EXCOMMAND_THRURW   = (TCAPPacket.TCAP_MSG_EXT_FELICA   << 8) | (TCAPPacket.TCAP_MSG_MT_FELICA_EXCOMMAND_THRURW);
    // UEP
    public static final int MID_SET_NETWORK_TIMEOUT       = (TCAPPacket.TCAP_MSG_EXT_STANDARD << 8) | (TCAPPacket.TCAP_MSG_MT_SET_NETWORK_TIMEOUT);
    public static final int MID_REQUEST_ID                = (TCAPPacket.TCAP_MSG_EXT_STANDARD << 8) | (TCAPPacket.TCAP_MSG_MT_REQUEST_ID);
//    public static final int MID_SELECT_INTERNAL_FELICA    = (TCAPPacket.TCAP_MSG_EXT_FELICA   << 8) | (TCAPPacket.TCAP_MSG_MT_SELECT_INTERNAL_FELICA);
    public static final int MID_SET_FELICA_TIMEOUT        = (TCAPPacket.TCAP_MSG_EXT_FELICA   << 8) | (TCAPPacket.TCAP_MSG_MT_SET_FELICA_TIMEOUT);
    public static final int MID_SET_RETRY_COUNT           = (TCAPPacket.TCAP_MSG_EXT_FELICA   << 8) | (TCAPPacket.TCAP_MSG_MT_SET_RETRY_COUNT);
    // OEP
    public static final int MID_OPERATE_DEVIDE            = (TCAPPacket.TCAP_MSG_EXT_STANDARD << 8) | (TCAPPacket.TCAP_MSG_MT_OPERATE_DEVICE);
//    public static final int MID_PLAY_SOUND                = (TCAPPacket.TCAP_MSG_EXT_STANDARD << 8) | (TCAPPacket.TCAP_MSG_MT_PLAY_SOUND);
    public static final int MID_OPEN_RW_REQUEST           = (TCAPPacket.TCAP_MSG_EXT_FELICA   << 8) | (TCAPPacket.TCAP_MSG_MT_OPEN_RW_REQUEST);
    public static final int MID_CLOSE_RW_REQUEST          = (TCAPPacket.TCAP_MSG_EXT_FELICA   << 8) | (TCAPPacket.TCAP_MSG_MT_CLOSE_RW_REQUEST);
    // EP
    public static final int MID_PACKET_FORMAT_ERROR       = (TCAPPacket.TCAP_MSG_EXT_STANDARD << 8) | (TCAPPacket.TCAP_MSG_MT_PACKET_FORMAT_ERROR);
    public static final int MID_ILLEGAL_STATE_ERROR       = (TCAPPacket.TCAP_MSG_EXT_STANDARD << 8) | (TCAPPacket.TCAP_MSG_MT_ILLEGAL_STATE_ERROR);
    public static final int MID_UNEXPECTED_ERROR          = (TCAPPacket.TCAP_MSG_EXT_STANDARD << 8) | (TCAPPacket.TCAP_MSG_MT_UNEXPECTED_ERROR);

    private TCAPMessage _errorMessage;

    public RcvPacketBase(TCAPPacket tcapPacket) {
            super(tcapPacket);

        _errorMessage = null;
    }

    public boolean IsFelicaDevice(List<DeviceElement> deviceList, char deviceID) {
        boolean bRetVal = false;

        for(DeviceElement device : deviceList) {
            if(device.GetDeviceID() == deviceID) {
                bRetVal = device.IsFeliCaChip();
            }
        }

        return bRetVal;
    }

    /**
     * パケットデータが正常か否か
     * パケットデータを解析し、正常か否かを判定する。
     * 正常ではない場合、第二引数にエラーメッセージを格納し、戻り値にfalseを返却する。
     *
     * @param deviceList   サーバに送付したデバイスリスト
     *
     * @return エラーコード
     */
    public long ValidatePacket(List<DeviceElement> deviceList) {
        long lRetVal;
        int messageNum = GetMessageNum();
        TCAPMessage tcapMessage;

        _errorMessage = null;

        // パケット内部のメッセージを全て検証
        for(int i = 0; i < messageNum; i++) {
            // メッセージの取得
            tcapMessage = GetMessage(i);
            if(tcapMessage == null) {
                // 予期せぬエラー（内部エラー）
                _errorMessage = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
            }

            // デバイスIDチェック
            if(tcapMessage.GetDeviceID() != TCAP_MSG_DEVICEID_INVALID) {
                DeviceElement deviceElement = null;
                for(DeviceElement device : deviceList) {
                    if(device.GetDeviceID() == tcapMessage.GetDeviceID()) {
                        deviceElement = device;
                        break;
                    }
                }
                if(deviceElement == null) {
                    // デバイスID不正
                    byte[] param = new byte[2];
                    param[0] = (byte)(tcapMessage.GetDeviceID() >> 8);
                    param[1] = (byte)(tcapMessage.GetDeviceID() & 0x00FF);
                    _errorMessage = new MsgPacketFormatError(MsgPacketFormatError.errorMessage.INVALID_DEVICE_ID.getInt(), param, param.length);
                    return FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
                }
            }

            // UpdateEntityがサポートしているメッセージか
            if(!IsSupportMessage(tcapMessage, deviceList)) {
                // サポートしていないエラーを設定して終了
                byte[] param = new byte[5];
                param[0] = (byte)tcapMessage.GetExtension();
                param[1] = (byte)GetSubProtocolType();
                param[2] = (byte)tcapMessage.GetMessageType();
                param[3] = (byte)(tcapMessage.GetDeviceID() >> 8);
                param[4] = (byte)(tcapMessage.GetDeviceID() & 0x00FF);
                _errorMessage = new MsgPacketFormatError(MsgPacketFormatError.errorMessage.UNSUPORTED_MESSAGE.getInt(), param, param.length);
                return FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
            }

            // フォーマットチェックを行うため詳細メッセージオブジェクト取得
            TCAPMessage detailMessage = GetDetailMessage(tcapMessage);
            if(detailMessage != null) {
                // メッセージ取得できたら現在のメッセージリストを切り替える
                if(!ChangeMessage(i, detailMessage)) {
                    // 予期せぬエラー（内部エラー）
                    _errorMessage = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                    return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
                }
                tcapMessage = detailMessage;
            }

            // フォーマット確認
            if(!tcapMessage.ValidateFormat()) {
                _errorMessage = new MsgPacketFormatError(MsgPacketFormatError.errorMessage.MESSAGE_FORMAT_ERROR.getInt(), null, 0);
                return FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
            }
        }

        // メッセージ順番のチェック
        lRetVal = ValidateMessageOrder();
        if(lRetVal == FeliCaClient.FC_ERR_SUCCESS) {
            // メッセージ順正しい
        } else if(lRetVal == FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT) {
            // パケットフォーマットエラー
            _errorMessage = new MsgPacketFormatError(MsgPacketFormatError.errorMessage.MESSAGE_ORDER_ERROR.getInt(), null, 0);
            return lRetVal;
        } else {
            // 予期せぬエラー
            _errorMessage = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
            return lRetVal;
        }

        // メッセージデータが有効か
        for(int i = 0; i < messageNum; i++) {
            tcapMessage = GetMessage(i);
            // メッセージ取得
            if(tcapMessage == null) {
                // 予期せぬエラー（内部エラー）
                _errorMessage = new MsgUnexpectedError(MsgUnexpectedError.errorMessage.UNEXPECTED_ERROR.getInt(), null, 0);
                return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
            }

            if(!tcapMessage.ValidateData()) {
                _errorMessage = new MsgPacketFormatError(MsgPacketFormatError.errorMessage.DATA_FORMAT_ERROR.getInt(), null, 0);
                return FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
            }
        }

        // ここまでくれば通常パケット
        return lRetVal;
    }

    /**
     * サポートしているメッセージかどうか
     *
     * @param message    チェックするメッセージ
     * @param deviceList デバイスリスト
     *
     * @return true :サポートしている
     *         false:サポートしていない
     */
    protected abstract boolean IsSupportMessage(TCAPMessage message, List<DeviceElement> deviceList);

    /**
     * 詳細メッセージオブジェクト取得
     * 引数のメッセージオブジェクトのMT値を判定し、詳細なメッセージオブジェクトを取得します。
     *
     * @param inMessage        メッセージオブジェクト
     *
     * @return 詳細メッセージオブジェクト（設定しなかった場合はnull）
     */
//    protected abstract boolean GetDetailMessage(TCAPMessage inMessage, TCAPMessage outDetailMessage);
    protected abstract TCAPMessage GetDetailMessage(TCAPMessage inMessage);

    /**
     * メッセージ順チェック
     *
     * @return エラーコード
     */
    protected abstract long ValidateMessageOrder();

    public TCAPMessage GetErrorTCAPMessage() {
        return _errorMessage;
    }
}
