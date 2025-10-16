package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule;

public class MsgFeliCaCommandThruRW extends TCAPMessage {
    private static final int MESSAGE_MIN_LENGTH = 1;        // メッセージ最小長
    private static final int MESSAGE_MAX_LENGTH = 250;      // メッセージ最大長

    public MsgFeliCaCommandThruRW(TCAPMessage feliCaCommandThruRWMessage) {
        super(feliCaCommandThruRWMessage);
    }

    /**
     * コマンドの長さ取得
     * コマンドの長さを取得します。
     *
     * @return コマンドの長さ
     */
    public char GetCommandLength() {
        char retVal = 0;
        byte[] data = GetMessageData();

        if(data == null) {
            // データなし
        } else if(GetLength() < 1) {
            // データ長不正
        } else {
            retVal = (char)data[0];
        }

        return retVal;
    }

    /**
     * コマンドコード取得
     * コマンドコードを取得します。
     *
     * @return コマンドコード
     */
    public char GetCommandCode() {
        char retVal = 0;
        byte[] data = GetMessageData();

        if(data == null) {
            // データなし
        } else if(GetLength() < 2) {
            // データ長不正
        } else {
            retVal = (char)data[1];
        }

        return retVal;
    }

    /**
     * コマンドパラメータ取得
     * コマンドパラメータを取得します。
     *
     * @return コマンドパラメータが存在する場合コマンドパラメータ、それ以外はnull
     */
    public byte[] GetCommandParameter() {
        byte[] retVal = null;
        byte[] data = GetMessageData();

        if(data == null) {
            // データなし
        } else if(GetLength() < 3) {
            // データ長不正
        } else {
            retVal = new byte[data.length-2];
            System.arraycopy(data, 2, retVal, 0, retVal.length);
        }

        return retVal;
    }

    @Override
    public boolean ValidateFormat() {
        boolean bRetVal = false;

        if((MESSAGE_MIN_LENGTH <= GetLength()) && (GetLength() <= MESSAGE_MAX_LENGTH)) {
            bRetVal = true;
        }

        return bRetVal;
    }

    @Override
    public boolean ValidateData() {
        return true;
    }
}
