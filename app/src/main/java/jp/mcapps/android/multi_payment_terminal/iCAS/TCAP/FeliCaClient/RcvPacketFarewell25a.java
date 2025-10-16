package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

import java.util.List;
import java.util.Objects;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgReturnCode;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgWarning;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPMessage;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPPacket;

public class RcvPacketFarewell25a extends RcvPacketBase {

    enum state
    {
        ORDER_GOODBYE_FINISHED_WAIT(0),             // SERVER_GOOD_BYE待ちもしくはFINISHED待ち
        ORDER_RETURN_CODE_WAIT(1),                  // RETURN_CODE待ち
        ORDER_SERVER_GOOD_BYE_DONE_WAIT(2),         // SERVER_GOOD_BYE_DONE待ち
        ORDER_COMPLETE(3),                          // 正常
        ORDER_ERROR(4),                             // メッセージ順エラー
        ;

        private final int _val;

        state(final int val) {
            _val = val;
        }

        public int getInt() {
            return _val;
        }
    }

    public RcvPacketFarewell25a(TCAPPacket farewellPacket) {
        super(farewellPacket);
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
                case MID_SERVER_GOOD_BYE:
                case MID_SERVER_GOOD_BYE_DONE:
                case MID_RETURN_CODE:
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
            case MID_RETURN_CODE:
                tcapMessage = new MsgReturnCode(inMessage);
                break;
            default:
                break;
        }

        return tcapMessage;
    }

    @Override
    public long ValidateMessageOrder() {
        state eState = state.ORDER_GOODBYE_FINISHED_WAIT;
        int messageNum = GetMessageNum();
        TCAPMessage tcapMessage;
        long lRetVal = FeliCaClient.FC_ERR_SUCCESS; // 正常終了で初期化

        // メッセージ順のチェック処理
        for(int i = 0; i < messageNum; i++) {
            tcapMessage = GetMessage(i);
            if(tcapMessage == null && eState != state.ORDER_COMPLETE) {
                // 予期せぬエラー（内部エラー）
                return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
            }
            int mid = (Objects.requireNonNull(tcapMessage).GetExtension() << 8) | (tcapMessage.GetMessageType());

            switch (eState) {
                // SERVER_GOOD_BYE待ちもしくはFINISHED待ち
                case ORDER_GOODBYE_FINISHED_WAIT:
                    switch (mid) {
                        case MID_SERVER_GOOD_BYE:
                            eState = state.ORDER_RETURN_CODE_WAIT;          // RETURN_CODE待ちへ
                            break;
                        case MID_WARNING:
                            break;  // 無視
                        default:
                            eState = state.ORDER_ERROR;
                            break;
                    }
                    break;
                // RETURN_CODE待ち
                case ORDER_RETURN_CODE_WAIT:
                    switch (mid) {
                        case MID_RETURN_CODE:
                            eState = state.ORDER_SERVER_GOOD_BYE_DONE_WAIT; // SERVER_GOOD_BYE_DONE待ち
                            break;
                        case MID_WARNING:
                            break;  // 無視
                        default:
                            eState = state.ORDER_ERROR;
                            break;
                    }
                    break;
                // SERVER_GOOD_BYE_DONE待ち
                case ORDER_SERVER_GOOD_BYE_DONE_WAIT:
                    switch (mid) {
                        case MID_SERVER_GOOD_BYE_DONE:
                            eState = state.ORDER_COMPLETE; // 正常
                            break;
                        case MID_WARNING:
                            break;  // 無視
                        default:
                            eState = state.ORDER_ERROR;
                            break;
                    }
                    break;
                // 正常
                case ORDER_COMPLETE:
                    switch (mid) {
                        case MID_WARNING:
                            break;  // 無視
                        default:
                            eState = state.ORDER_ERROR;
                            break;
                    }
                    break;
                // メッセージ順エラー
                case ORDER_ERROR:
                default:
                    // ここに来ることは無いが一応
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
