package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule;

public class MsgIllegalStateError extends TCAPMessage {
    public static final int ILLEGAL_STATE = 80;     // 状態不正

    public MsgIllegalStateError(TCAPMessage illegalStateErrorMessage) {
        super(illegalStateErrorMessage);
    }

    public MsgIllegalStateError(byte[] error, int length) {
        super((char) TCAPPacket.TCAP_MSG_EXT_STANDARD, (char) TCAPPacket.TCAP_MSG_DEVICEID_INVALID, (char) TCAPPacket.TCAP_MSG_MT_ILLEGAL_STATE_ERROR);

        if(error != null) {
            SetMessageData(error, length);
        }
    }

    public MsgIllegalStateError(int errorType) {
        super((char) TCAPPacket.TCAP_MSG_EXT_STANDARD, (char) TCAPPacket.TCAP_MSG_DEVICEID_INVALID, (char) TCAPPacket.TCAP_MSG_MT_ILLEGAL_STATE_ERROR);

        byte[] tempData = new byte[2];

        // 検証
        if(errorType == ILLEGAL_STATE) {
            // エラーメッセージタイプ格納
            tempData[0] = 0x38;
            tempData[1] = 0x30;

            SetMessageData(tempData, tempData.length);
        }
    }

    @Override
    public boolean ValidateFormat() {
        boolean bRetVal = true;

        if(0 < GetLength()) {
            if(GetMessageData() == null) {
                bRetVal = false;
            }
        }

        return bRetVal;
    }

    @Override
    public boolean ValidateData() {
        boolean bRetVal;

        byte[] message = GetMessageData();
        long length = GetLength();

        if(message == null && 0 < length) {
            bRetVal = false;
        } else {
            // メッセージ部は0x20～0x7Eのコードしか許容しない
            bRetVal = IsAsciiCode(message, (int)length);
        }

        return bRetVal;
    }
}
