package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule;

public class MsgOperateDevice extends TCAPMessage {
    public static final int MESSAGE_LENGTH = 5;     // メッセージデータの最小長

    public MsgOperateDevice(final TCAPMessage operateDeviceMessage) {
        super(operateDeviceMessage);
    }

    /**
     * パラメータ名の長さを取得
     * パラメータ名の長さを取得します。
     *
     * @return パラメータ名の長さ
     */
    public char GetParameterLength() {
        char paramLength = 0;

        if(GetLength() < 1) {
            // レングス不正
        } else {
            byte[] data = GetMessageData();
            if(data != null) {
                paramLength = (char)(data[0] & 0x000000FF);
            }
        }

        return paramLength;
    }

    /**
     * パラメータ名を取得
     * パラメータ名を取得します。
     *
     * @return パラメータ名
     */
    public byte[] GetParameterName() {
        int length = GetParameterLength();
        byte[] retVal = null;
        final byte[] work = GetMessageData();

        if(work == null) {          // データが存在するか
        } else if(length < 1) {    // パラメータの長さチェック
        } else {
            retVal = new byte[length];

            // パラメータ名称をセット
            System.arraycopy(work, 1, retVal, 0, length);
        }

        return retVal;
    }

    /**
     * データの長さ取得
     * データの長さを取得します。
     *
     * @return データの長さ
     */
    public char GetDataLength() {
        char retVal = 0;
        final byte[] work = GetMessageData();

        if(work == null) {
        } else {
            int paramLength = (work[0] & 0x000000FF);

            paramLength += 1 + 2;   // パラメータ名称長さ１バッファ＋予約サイズ２バイト
            retVal = (char)(((work[paramLength] << 8) & 0x0000FF00) | (work[paramLength+1] & 0x000000FF));
        }

        return retVal;
    }

    /**
     * データ取得
     * データを取得します。
     *
     * @return データ
     */
    public byte[] GetData() {
        byte[] retVal = null;
        byte[] work = GetMessageData();

        if(work == null) {
        } else {
            char paramLength = GetParameterLength();
            char dataLength = GetDataLength();

            paramLength += 1 + 2;   // パラメータ名称長さ１バッファ＋予約サイズ２バイト

            if(dataLength < 1) {
            } else {
                retVal = new byte[work.length - (paramLength+2)];
                System.arraycopy(work, paramLength+2, retVal, 0, retVal.length);
            }
        }

        return retVal;
    }

    @Override
    public boolean ValidateFormat() {
        boolean bRetVal = true;

        if(GetLength() < MESSAGE_LENGTH) {
            bRetVal = false;
        }

        return bRetVal;
    }

    @Override
    public boolean ValidateData() {
        boolean bRetVal = true;
        int paramNameLength;
        int paramSize;
        int pos = 0;
        byte[] message = GetMessageData();
        byte[] messageData;
        long length = GetLength();

        if(message == null && 0 < length) {
            // OperateDeviceメッセージなし。正常
        } else {
            pos += 1;

            // パラメータ名長
            paramNameLength = GetParameterLength();

            messageData = new byte[message.length-1];
            System.arraycopy(message, 1, messageData, 0, messageData.length);
            // パラメータ名は0x20～0x7Eのコードしか許容しない
            bRetVal = IsAsciiCode(messageData, paramNameLength);

            if(bRetVal) {
                pos += paramNameLength;

                // 予約の2byteは全て0
                if((message[pos] != (byte)0x00) || (message[pos+1] != (byte)0x00)) {
                    bRetVal = false;
                } else {
                    pos += 2;

                    // パラメータサイズ
                    paramSize = ((message[pos] & 0x000000FF) << 8) | (message[pos+1] & 0x000000FF);

                    // 長さの一致チェック
                    if(GetLength() != 1 + paramNameLength + 2 + 2 + paramSize) {
                        bRetVal = false;
                    }
                }
            }
        }

        return bRetVal;
    }
}
