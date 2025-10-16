package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule;

import java.util.Arrays;
import java.util.Locale;

public class MsgPacketFormatError extends TCAPMessage {
    public enum errorMessage
    {
        UNSUPORTED_VERSION(40),                // バージョン不正
        UNSUPORTED_SUBPROTOCOL(41),            // サブプロトコルタイプ不正
        PACKET_FORMAT_ERROR(42),               // パケットフォーマット不正
        INVALID_DEVICE_ID(43),                 // デバイスID不正
        UNSUPORTED_MESSAGE(44),                // 未サポートメッセージ
        MESSAGE_FORMAT_ERROR(45),              // メッセージフォーマット不正
        MESSAGE_ORDER_ERROR(46),               // メッセージ順不正
        DATA_FORMAT_ERROR(47),                 // メッセージデータ不正
        ;

        private final int _val;

        errorMessage(final int val) {
            _val = val;
        }

        public int getInt() {
            return _val;
        }
    }

    public MsgPacketFormatError(TCAPMessage tcapMessage) {
        super(tcapMessage);
    }

    public MsgPacketFormatError(String errorMessage) {
        super((char) TCAPPacket.TCAP_MSG_EXT_STANDARD,
              (char) TCAPPacket.TCAP_MSG_DEVICEID_INVALID,
              (char) TCAPPacket.TCAP_MSG_MT_PACKET_FORMAT_ERROR);

        if(errorMessage != null) {
            SetMessageData(errorMessage.getBytes(), errorMessage.length());
        }
    }

    public MsgPacketFormatError(int errorType, byte[] errorData, int size) {
        super((char) TCAPPacket.TCAP_MSG_EXT_STANDARD,
              (char) TCAPPacket.TCAP_MSG_DEVICEID_INVALID,
              (char) TCAPPacket.TCAP_MSG_MT_PACKET_FORMAT_ERROR);

        byte[] tempData = new byte[2 + size * 3];
        byte[] tempErrorType = new byte[2];

        Arrays.fill(tempData, (byte)0);
        Arrays.fill(tempErrorType, (byte)0);

        // エラー種別チェック
        if(errorType == errorMessage.UNSUPORTED_VERSION.getInt()
                || errorType == errorMessage.UNSUPORTED_SUBPROTOCOL.getInt()
                || errorType == errorMessage.PACKET_FORMAT_ERROR.getInt()
                || errorType == errorMessage.INVALID_DEVICE_ID.getInt()
                || errorType == errorMessage.UNSUPORTED_MESSAGE.getInt()
                || errorType == errorMessage.MESSAGE_FORMAT_ERROR.getInt()
                || errorType == errorMessage.MESSAGE_ORDER_ERROR.getInt()
                || errorType == errorMessage.DATA_FORMAT_ERROR.getInt()
        ) {
            if(size > 0) {
                if(errorData != null) {
                    int j = 2;
                    for(int i = 0; i < size; i++) {
                        // 空白
                        tempData[j++] = ' ';

                        char upper = (char)((errorData[i] >> 4) & 0x0000000F);
                        char lower = (char)(errorData[i] & 0x0000000F);

                        if(upper <= 0x09) {
                            tempData[j++] = (byte)('0' + upper);
                        } else {
                            tempData[j++] = (byte)('A' + upper - 0x0A);
                        }

                        if(lower <= 0x09) {
                            tempData[j++] = (byte)('0' + lower);
                        } else {
                            tempData[j++] = (byte)('A' + lower - 0x0A);
                        }
                    }
                }
            }
            String errorTypeStr = String.format(Locale.JAPAN, "%02d", errorType);
            tempErrorType = errorTypeStr.getBytes();

            System.arraycopy(tempErrorType, 0, tempData, 0, 2);
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
        int length = GetLength();

        if(message == null && 0 < length) {
            bRetVal = false;
        } else {
            bRetVal = IsAsciiCode(message, length);
        }

        return bRetVal;
    }
}
