package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

import java.util.List;
import java.util.Objects;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgIllegalStateError;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgPacketFormatError;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgUnexpectedError;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgWarning;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPMessage;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPPacket;

public class RcvPacketError25a extends RcvPacketBase {

    enum state
    {
        ORDER_ERROR_WAIT(0),             // 初期状態(XXX_ERROR待ち)
        ORDER_COMPLETE(1),               // 正常
        ORDER_ERROR(2),                  // メッセージ順エラー
        ;

        private final int _val;

        state(final int val) {
            _val = val;
        }

        public int getInt() {
            return _val;
        }
    }

    public RcvPacketError25a(TCAPPacket tcapPacket) {
        super(tcapPacket);
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
                case MID_WARNING:
                case MID_PACKET_FORMAT_ERROR:
                case MID_ILLEGAL_STATE_ERROR:
                case MID_UNEXPECTED_ERROR:
                    bRetVal = true;
                    break;
                default:
                    // 非サポートメッセージ
                    break;
            }
        }

        return bRetVal;
    }

    @Override
    public TCAPMessage GetDetailMessage(final TCAPMessage inMessage) {
        TCAPMessage tcapMessage = null;
        int mid = (inMessage.GetExtension() << 8) | (inMessage.GetMessageType());

        switch (mid) {
            case MID_WARNING:
                tcapMessage = new MsgWarning(inMessage);
                break;
            case MID_PACKET_FORMAT_ERROR:
                tcapMessage = new MsgPacketFormatError(inMessage);
                break;
            case MID_ILLEGAL_STATE_ERROR:
                tcapMessage = new MsgIllegalStateError(inMessage);
                break;
            case MID_UNEXPECTED_ERROR:
                tcapMessage = new MsgUnexpectedError(inMessage);
                break;
            default:
                break;
        }

        return tcapMessage;
    }

    @Override
    public long ValidateMessageOrder() {
        state eState = state.ORDER_ERROR_WAIT;
        int messageNum = GetMessageNum();
        TCAPMessage tcapMessage;
        long lRetVal = FeliCaClient.FC_ERR_SUCCESS;

        // メッセージ順のチェック処理
        for(int i = 0; i < messageNum; i++) {
            tcapMessage = GetMessage(i);
            if(tcapMessage == null && eState != state.ORDER_COMPLETE) {
                // 予期せぬエラー（内部エラー）
                return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
            }
            int mid = (Objects.requireNonNull(tcapMessage).GetExtension() << 8) | (tcapMessage.GetMessageType());

            switch (eState) {
                // 初期状態(XXX_ERROR待ち)
                case ORDER_ERROR_WAIT:
                    switch(mid) {
                        case RcvPacketBase.MID_PACKET_FORMAT_ERROR:
                        case RcvPacketBase.MID_ILLEGAL_STATE_ERROR:
                        case RcvPacketBase.MID_UNEXPECTED_ERROR:
                            eState = state.ORDER_COMPLETE;      // 正常
                            break;
                        case RcvPacketBase.MID_WARNING:
                            break;      // 無視
                        default:
                            eState = state.ORDER_ERROR;
                            break;
                    }
                    break;
                // 正常
                case ORDER_COMPLETE:
                    switch(mid) {
                        case MID_WARNING:
                            break;      // 無視
                        default:
                            eState = state.ORDER_ERROR;
                            break;
                    }
                    break;
                // メッセージ順エラー
                case ORDER_ERROR:
                default:
                    // ここに来ることはないが一応
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
