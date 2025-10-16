package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

import java.util.List;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgFeliCaCommand;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgFeliCaCommandThruRW;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgFeliCaExCommand;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgFeliCaExCommandThruRW;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgFeliCaPreCommand;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgFeliCaPreCommandThruRW;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.MsgWarning;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPMessage;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPPacket;

public class RcvPacketApplicationDataTransfer25a extends RcvPacketBase {

    public RcvPacketApplicationDataTransfer25a(final TCAPPacket applicationDataTransferPacket) {
        super(applicationDataTransferPacket);
    }

    @Override
    public boolean IsSupportMessage(TCAPMessage message, List<DeviceElement> deviceList) {
        boolean bRetVal = false;
        int mid = (message.GetExtension() << 8) | (message.GetMessageType());

        switch(mid) {
            case MID_WARNING:
                // デバイスIDが規定の値かどうかチェック
                if(message.GetDeviceID() == TCAPPacket.TCAP_MSG_DEVICEID_INVALID) {
                    bRetVal = true;
                }
                break;
            case MID_FELICA_COMMAND:
            case MID_FELICA_COMMAND_THRURW:
            case MID_FELICA_PRECOMMAND:
            case MID_FELICA_PRECOMMAND_THRURW:
            case MID_FELICA_EXCOMMAND:
            case MID_FELICA_EXCOMMAND_THRURW:
                // デバイスIDがFeliCaチップデバイスかどうか
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
            case MID_FELICA_COMMAND:
                tcapMessage = new MsgFeliCaCommand(inMessage);
                break;
            case MID_FELICA_COMMAND_THRURW:
                tcapMessage = new MsgFeliCaCommandThruRW(inMessage);
                break;
            case MID_FELICA_PRECOMMAND:
                tcapMessage = new MsgFeliCaPreCommand(inMessage);
                break;
            case MID_FELICA_PRECOMMAND_THRURW:
                tcapMessage = new MsgFeliCaPreCommandThruRW(inMessage);
                break;
            case MID_FELICA_EXCOMMAND:
                tcapMessage = new MsgFeliCaExCommand(inMessage);
                break;
            case MID_FELICA_EXCOMMAND_THRURW:
                tcapMessage = new MsgFeliCaExCommandThruRW(inMessage);
                break;
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
