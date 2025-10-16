package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule;

import java.util.Locale;

public class MsgUnexpectedError extends TCAPMessage {
    public enum errorMessage
    {
        UNEXPECTED_ERROR(0),                // 予期しない不正
        INTERRUPTED_BY_USER(1),             // ユーザによる中断
        OPERATE_DEVICE_FAILED(2),           // デバイス操作失敗
        HANDSHAKE_ERROR(3),                 // ハンドシェイク失敗
        PRE_EXCOMMAND_ERROR(4),             // パラメータ埋め込み失敗
        ;

        private final int _val;

        errorMessage(final int val) {
            _val = val;
        }

        public int getInt() {
            return _val;
        }
    }

    public MsgUnexpectedError(TCAPMessage tcapMessage) {
        super(tcapMessage);
    }

    public MsgUnexpectedError(String error) {
        super((char) TCAPPacket.TCAP_MSG_EXT_STANDARD,
              (char) TCAPPacket.TCAP_MSG_DEVICEID_INVALID,
              (char) TCAPPacket.TCAP_MSG_MT_UNEXPECTED_ERROR);

        if(error != null) {
            SetMessageData(error.getBytes(), error.length());
        }
    }

    public MsgUnexpectedError(int errorType, byte[] errorData, long size) {

        super((char) TCAPPacket.TCAP_MSG_EXT_STANDARD,
              (char) TCAPPacket.TCAP_MSG_DEVICEID_INVALID,
              (char) TCAPPacket.TCAP_MSG_MT_UNEXPECTED_ERROR);

        // エラー種別チェック
        if(errorType == errorMessage.UNEXPECTED_ERROR.getInt()
        || errorType == errorMessage.INTERRUPTED_BY_USER.getInt()
        || errorType == errorMessage.OPERATE_DEVICE_FAILED.getInt()
        || errorType == errorMessage.HANDSHAKE_ERROR.getInt()
        || errorType == errorMessage.PRE_EXCOMMAND_ERROR.getInt()) {
            String errorMessage;
            String strData;
            String errorTypeStr = String.format(Locale.JAPAN, "%02d", errorType);

            if(errorData != null && size > 0) {
                strData = new String(errorData);
                strData = " " + strData;
                errorMessage = errorTypeStr + strData;
            } else {
                errorMessage = errorTypeStr;
            }
            SetMessageData(errorMessage.getBytes(), errorMessage.length());
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
