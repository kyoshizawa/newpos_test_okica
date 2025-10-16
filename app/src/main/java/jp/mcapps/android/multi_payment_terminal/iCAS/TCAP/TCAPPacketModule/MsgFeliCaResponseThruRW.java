package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule;

import java.util.ArrayList;
import java.util.List;

public class MsgFeliCaResponseThruRW extends TCAPMessage {
    public MsgFeliCaResponseThruRW(final TCAPMessage tcapMessage) {
        super(tcapMessage);
    }

    public MsgFeliCaResponseThruRW(char deviceID, final byte[] responseMessage, int responseMessageLength) {
        super((char)TCAPPacket.TCAP_MSG_EXT_FELICA, deviceID, (char)TCAPPacket.TCAP_MSG_MT_FELICA_RESPONSE_THRURW);

        SetMessageData(responseMessage, responseMessageLength);
    }

    /**
     * レスポンスの長さ取得
     * レスポンスの長さを取得します。
     *
     * @return レスポンスの長さ
     */
    public char GetResponseLength() {
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
     * レスポンスコード取得
     * レスポンスコードを取得します。
     *
     * @return レスポンスコード
     */
    public char GetResponseCode() {
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
}
