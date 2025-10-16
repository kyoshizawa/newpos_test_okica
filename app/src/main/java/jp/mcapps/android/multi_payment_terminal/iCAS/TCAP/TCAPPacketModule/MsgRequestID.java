package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule;

public class MsgRequestID extends TCAPMessage {
    public static final int MESSAGE_LENGTH = 2;     // メッセージデータの最小長

    public MsgRequestID(TCAPMessage requestIDMessage) {
        super(requestIDMessage);
    }

    public MsgRequestID(char requestID) {
        super((char)TCAPPacket.TCAP_MSG_EXT_STANDARD, (char)TCAPPacket.TCAP_MSG_DEVICEID_INVALID, (char)TCAPPacket.TCAP_MSG_MT_REQUEST_ID);

        byte[] reqID = new byte[2];
        reqID[0] = (byte)(requestID >> 8);
        reqID[1] = (byte)(requestID & 0x00FF);

        SetMessageData(reqID, reqID.length);
    }
    /**
     * リクエスト番号取得
     *
     * @return リクエスト番号
     */
    public char GetID() {
        char retVal = 0;
        byte[] message = GetMessageData();

        if(message == null) {
            // メモリ確保失敗
        } else if(GetLength() < 2) {
            // データ長不正
        } else {
            retVal = (char)((message[0] << 8)
                          | (message[1]));
        }

        return retVal;
    }

    @Override
    public boolean ValidateFormat() {
        boolean bRetVal = false;

        if(GetLength() == MESSAGE_LENGTH) {
            bRetVal = true;
        }

        return bRetVal;
    }

    @Override
    public boolean ValidateData() {
        return true;
    }
}
