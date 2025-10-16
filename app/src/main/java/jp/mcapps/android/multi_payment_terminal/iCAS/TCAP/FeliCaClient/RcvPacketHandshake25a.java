package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

import java.util.List;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgAccept;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgWarning;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPMessage;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPPacket;

public class RcvPacketHandshake25a extends RcvPacketBase {

    enum state
    {
        ORDER_SERVER_HELLO_WAIT(0),             // SERVER_HELLO待ち
        ORDER_ACCEPT_WAIT(1),                   // ACCEPT待ち
        ORDER_SERVER_HELLO_DONE_WAIT(2),        // SERVER_HELLO_DONE待ち
        ORDER_COMPLETE(3),                      // 正常
        ORDER_ERROR(4),                         // メッセージ順エラー
        ;

        private final int _val;

        state(final int val) {
            _val = val;
        }

        public int getInt() {
            return _val;
        }
    }

    public RcvPacketHandshake25a(TCAPPacket handShakePacket) {
        super(handShakePacket);
    }

    @Override
    public boolean IsSupportMessage(TCAPMessage message, List<DeviceElement> deviceList) {
        boolean bRetVal = false;

        // デバイスIDが規定の値かどうかチェック
        if(message.GetDeviceID() != TCAP_MSG_DEVICEID_INVALID) {
            // デバイスID不正。エラー確定。
        } else {
            int mid = (message.GetExtension() << 8) | (message.GetMessageType());

            switch(mid) {
                case RcvPacketBase.MID_WARNING:
                case RcvPacketBase.MID_SERVER_HELLO:
                case RcvPacketBase.MID_SERVER_HELLO_DONE:
                case RcvPacketBase.MID_ACCEPT:
                    bRetVal = true;
                    break;
                default:
                    break;
            }
        }

        return bRetVal;
    }

    @Override
    public TCAPMessage GetDetailMessage(TCAPMessage inMessage) {
        TCAPMessage tcapMessage = null;
        int mid = (inMessage.GetExtension() << 8) | (inMessage.GetMessageType());

        switch (mid) {
            case RcvPacketBase.MID_WARNING:
                tcapMessage = new MsgWarning(inMessage);
                break;
            case RcvPacketBase.MID_ACCEPT:
                tcapMessage = new MsgAccept(inMessage);
                break;
            case RcvPacketBase.MID_SERVER_HELLO:
            case RcvPacketBase.MID_SERVER_HELLO_DONE:
            default:
                break;
        }

        return tcapMessage;
    }

    @Override
    public long ValidateMessageOrder() {
        state eState = state.ORDER_SERVER_HELLO_WAIT;
        int messageNum = GetMessageNum();
        TCAPMessage tcapMessage;
        long lRetVal = FeliCaClient.FC_ERR_SUCCESS;     // 正常終了で初期化

        // メッセージ順のチェック処理
        for(int i = 0; i < messageNum; i++) {
            tcapMessage = GetMessage(i);
            if(tcapMessage == null) {
                // 予期せぬエラー（内部エラー）
                return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
            }
            int mid = (tcapMessage.GetExtension() << 8) | (tcapMessage.GetMessageType());

            switch(eState) {
                // SERVER_HELLO待ち
                case ORDER_SERVER_HELLO_WAIT:
                    switch (mid) {
                        case MID_SERVER_HELLO:
                            eState = state.ORDER_ACCEPT_WAIT;
                            break;
                        case MID_WARNING:
                            // 無視
                            break;
                        default:
                            eState = state.ORDER_ERROR;
                            break;
                    }
                    break;
                // ACCEPT待ち
                case ORDER_ACCEPT_WAIT:
                    switch (mid) {
                        case MID_ACCEPT:
                            eState = state.ORDER_SERVER_HELLO_DONE_WAIT;
                            break;
                        case MID_WARNING:
                            // 無視
                            break;
                        default:
                            eState = state.ORDER_ERROR;
                            break;
                    }
                    break;
                // SERVER_HELLO_DONE待ち
                case ORDER_SERVER_HELLO_DONE_WAIT:
                    switch (mid) {
                        case MID_SERVER_HELLO_DONE:
                            eState = state.ORDER_COMPLETE;
                            break;
                        case MID_WARNING:
                            // 無視
                            break;
                        default:
                            eState = state.ORDER_ERROR;
                            break;
                    }
                    break;
                // 正常
                case ORDER_COMPLETE:
                    switch (mid) {
                        case MID_WARNING:
                            // 無視
                            break;
                        default:
                            eState = state.ORDER_ERROR;
                            break;
                    }
                    break;
                // メッセージ順エラー
                case ORDER_ERROR:
                default:
                    eState = state.ORDER_ERROR;
                    break;
            }

            if(eState == state.ORDER_ERROR) {
                break;
            }
        }

        if(eState != state.ORDER_COMPLETE) {
            lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
        }

        return lRetVal;
    }
}
