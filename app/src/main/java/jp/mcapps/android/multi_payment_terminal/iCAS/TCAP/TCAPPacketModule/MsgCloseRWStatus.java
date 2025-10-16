package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule;

public class MsgCloseRWStatus extends TCAPMessage {
    public static final int STATUS_SUCCESS = 0x01;      // 成功
    public static final int STATUS_FAILED  = 0x00;      // 失敗

    private final char _status;           // ステータス

    public MsgCloseRWStatus(final TCAPMessage tcapMessage) {
        super(tcapMessage);

        _status = 0;
    }

    public MsgCloseRWStatus(char deviceID, char status) {
        super((char)TCAPPacket.TCAP_MSG_EXT_FELICA, deviceID, (char)TCAPPacket.TCAP_MSG_MT_CLOSE_RW_STATUS);

        byte[] data = new byte[1];

        _status = status;
        data[0] = (byte)_status;
        SetMessageData(data, data.length);
    }

    @Override
    public boolean ValidateFormat() {
        boolean bRetVal = false;

        if(0 < GetLength()) {
            if(GetMessageData() != null) {
                bRetVal = true;
            }
        }

        return bRetVal;
    }

    @Override
    public boolean ValidateData() {
        return true;
    }
}
