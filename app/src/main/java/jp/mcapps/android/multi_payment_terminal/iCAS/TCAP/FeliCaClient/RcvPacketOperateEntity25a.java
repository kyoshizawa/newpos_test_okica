package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

import java.util.List;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgOperateDevice;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgWarning;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPMessage;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPPacket;

public class RcvPacketOperateEntity25a extends RcvPacketBase {
    public RcvPacketOperateEntity25a(final TCAPPacket operateEntityPacket) {
        super(operateEntityPacket);
    }

    @Override
    public boolean IsSupportMessage(TCAPMessage message, List<DeviceElement> deviceList) {
        boolean bRetVal = false;
        DeviceElement device = null;
        int mid = (message.GetExtension() << 8) | (message.GetMessageType());


        switch(mid) {
            case MID_WARNING:
                // デバイスIDが規定の値かどうかチェック
                if(message.GetDeviceID() == TCAPPacket.TCAP_MSG_DEVICEID_INVALID) {
                    bRetVal = true;
                }
                break;
            case MID_OPERATE_DEVIDE:
                // デバイスIDがデバイスリストに存在するかどうか
                for(DeviceElement deviceElement : deviceList) {
                    if(deviceElement.GetDeviceID() == message.GetDeviceID()) {
                        device = deviceElement;
                        break;
                    }
                }
                if(device != null) {
                    // デバイスIDがFeliCaチップデバイスでないかどうかチェック
                    if(!device.IsFeliCaChip()) {
                        return true;
                    }
                }
                break;
            case MID_OPEN_RW_REQUEST:
            case MID_CLOSE_RW_REQUEST:
                // デバイスIDがFeliCaチップデバイスかどうかチェック
                if(IsFelicaDevice(deviceList, message.GetDeviceID())) {
                    bRetVal = true;
                }
                break;
            default:
                break;
        }

        return bRetVal;
    }

    @Override
    public TCAPMessage GetDetailMessage(final TCAPMessage inMessage) {
        TCAPMessage tcapMessage = null;
        int mid = (inMessage.GetExtension() << 8) | (inMessage.GetMessageType());

        switch(mid) {
            case MID_WARNING:
                tcapMessage = new MsgWarning(inMessage);
                break;
            case MID_OPERATE_DEVIDE:
                tcapMessage = new MsgOperateDevice(inMessage);
                break;
            case MID_OPEN_RW_REQUEST:
            case MID_CLOSE_RW_REQUEST:
            default:
                break;
        }

        return tcapMessage;
    }

    @Override
    public long ValidateMessageOrder() {
        long lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        int messageNum = GetMessageNum();
        TCAPMessage tcapMessage;
        int count = 0;

        // WARNING意外のメッセージの個数を数える
        for(int i = 0; i < messageNum; i++) {
            tcapMessage = GetMessage(i);
            if(tcapMessage == null) {
                // 予期せぬエラー（内部エラー）
                return FeliCaClient.FC_ERR_CLIENT_MSG_UNEXPECTED;
            }

            int mid = (tcapMessage.GetExtension() << 8) | (tcapMessage.GetMessageType());
            if(mid != MID_WARNING) {
                count++;
            }
        }

        if(count < 1) {
            lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
        }

        return lRetVal;
    }
}
