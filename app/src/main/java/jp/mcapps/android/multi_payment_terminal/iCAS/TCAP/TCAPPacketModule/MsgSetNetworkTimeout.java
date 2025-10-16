package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule;

public class MsgSetNetworkTimeout extends TCAPMessage {

    private static final int MESSAGE_LENGTH = 4;        // メッセージデータの最小長

    public MsgSetNetworkTimeout(TCAPMessage setNetworkTimeoutMessage) {
        super(setNetworkTimeoutMessage);
    }

    /**
     * タイムアウト値取得
     * タイムアウト値を取得します。
     *
     * @return タイムアウト値
     */
    public long GetTime() {
        long lRetVal = 0;
        final byte[] message = GetMessageData();

        if(message == null) {
            // メモリ確保失敗
        } else if(GetLength() < 4) {
            // データ長不正
        } else {
            lRetVal = ((message[0] << 24) & 0x00000000FF000000)
                    | ((message[1] << 16) & 0x0000000000FF0000)
                    | ((message[2] << 8)  & 0x000000000000FF00)
                    |  (message[3]        & 0x00000000000000FF);
        }

        return lRetVal;
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
