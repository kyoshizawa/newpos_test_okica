package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule;

public class MsgWarning extends TCAPMessage {

    public MsgWarning(TCAPMessage warningMessage) {
        super(warningMessage);
    }

    public MsgWarning(byte[] warningMessage) {
        super((char)TCAPPacket.TCAP_MSG_EXT_STANDARD,
              (char)TCAPPacket.TCAP_MSG_DEVICEID_INVALID,
              (char)TCAPPacket.TCAP_MSG_MT_WARNING
             );

        if(warningMessage != null) {
            SetMessageData(warningMessage, warningMessage.length);
        }
    }

    @Override
    public boolean ValidateFormat() {
        return true;
    }

    @Override
    public boolean ValidateData() {
        return true;
    }
}
