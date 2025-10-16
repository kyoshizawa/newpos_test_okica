package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule;

public class MsgSetRetryCount extends TCAPMessage {
    public static final int MESSAGE_LENGTH = 4;     // メッセージデータの最小長

    public MsgSetRetryCount(TCAPMessage setRetryCountMessage) {
        super(setRetryCountMessage);
    }

    /**
     * リトライ回数取得
     * リトライ回数を取得します。
     *
     * @return リトライ回数
     */
    public long GetRetryCount() {
        long retVal = 0;
        byte[] message = GetMessageData();

        if(message == null) {
            // メモリ確保失敗
        } else if(GetLength() < 4) {
            // データ長不正
        } else {
            retVal = ((message[0] << 24) & 0x00000000FF000000)
                   | ((message[1] << 16) & 0x0000000000FF0000)
                   | ((message[2] << 8)  & 0x000000000000FF00)
                   |  (message[3]        & 0x00000000000000FF);
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
