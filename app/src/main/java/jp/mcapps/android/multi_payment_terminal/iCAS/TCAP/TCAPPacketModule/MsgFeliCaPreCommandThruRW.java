package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule;

public class MsgFeliCaPreCommandThruRW extends TCAPMessage {
    private static final int MESSAGE_MIN_LENGTH = 4;        // メッセージ最小長
    private static final int MESSAGE_MAX_LENGTH = 253;      // メッセージ最大長

    public MsgFeliCaPreCommandThruRW(TCAPMessage feliCaCommandThruRWMessage) {
        super(feliCaCommandThruRWMessage);
    }

    /**
     * パラメータID取得
     * パラメータIDを取得します。
     *
     * @return パラメータID
     */
    public char GetParameterID() {
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
     * パラメータ取得開始位置取得
     * パラメータ取得開始位置を取得します。
     *
     * @return パラメータ取得開始位置
     */
    public char GetStartPosition() {
        char retVal = 0;
        byte[] data = GetMessageData();

        if(data == null) {
            // データなし
        } else if(GetLength() < 4) {
            // データ長不正
        } else {
            retVal = (char)data[1];
        }

        return retVal;
    }

    /**
     * パラメータ長取得
     * パラメータ長を取得します。
     *
     * @return パラメータ長
     */
    public char GetParameterLength() {
        char retVal = 0;
        byte[] data = GetMessageData();

        if(data == null) {
            // データなし
        } else if(GetLength() < data[3] + 3) {  // +3はパラメータID、パラメータ埋め込み位置、コマンドの長さの3byte分
            // データ長不正
        } else {
            retVal = (char)data[2];
        }

        return retVal;
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
        } else if(GetLength() < 4) {
            // データ長不正
        } else {
            retVal = (char)data[3];
        }

        return retVal;
    }

    /**
     * FeliCaチップに通知するFeliCaコマンド取得
     * FeliCaチップに通知するFeliCaコマンドを取得します。
     *
     * @return FeliCaコマンドが存在する場合FeliCaコマンド、それ以外はnull
     */
    public byte[] GetCommand() {
        byte[] retVal = null;
        byte[] data = GetMessageData();

        if(data == null) {
            // データなし
        } else if(GetLength() < 5) {
            // データ長不正
        } else {
            retVal = new byte[data.length-3];
            System.arraycopy(data, 3, retVal, 0, retVal.length);
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
