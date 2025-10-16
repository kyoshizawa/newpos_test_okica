package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.FeliCaClient;

public class MsgFeliCaExCommandThruRW extends TCAPMessage {
    private static final int MESSAGE_MIN_LENGTH = 3;        // メッセージ最小長
    private static final int MESSAGE_MAX_LENGTH = 252;      // メッセージ最大長

    public MsgFeliCaExCommandThruRW(TCAPMessage feliCaExCommandThruMessage) {
        super(feliCaExCommandThruMessage);
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
     * パラメータ埋め込み位置取得
     * パラメータ埋め込み位置を取得します。
     *
     * @return パラメータ埋め込み位置
     */
    public char GetInsertPosition() {
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
        } else if(GetLength() < 3) {
            // データ長不正
        } else {
            retVal = (char)data[2];
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
        } else if(GetLength() < data[2] + 2) {
            // データ長不正
        } else {
            retVal = new byte[data.length-2];
            System.arraycopy(data, 2, retVal, 0, retVal.length);
        }

        return retVal;
    }

    /**
     * FeliCaチップに通知するFeliCaコマンド設定
     * FeliCaチップに通知するFeliCaコマンドを設定します。
     *
     * @param command FeliCaコマンド
     *
     * @return エラーコード
     */
    public long SetCommand(byte[] command) {
        long lRetVal = TCAPPacket.FC_ERR_TCAP_UNKNOWN;

        if(command == null) {
            // データなし
        } else {
            lRetVal = UpdateMessageData(command, command.length, 2 + GetInsertPosition());
        }

        return lRetVal;
    }

    /**
     * PreCommand設定
     * コマンドパラメータを上書きします。
     *
     * @param param       PreCommand実行で取得したパラメータ
     * @param paramLength PreCommand実行で取得したパラメータ長さ
     *
     * @return エラーコード
     */
    public long SetPreCommand(byte[] param, char paramLength) {
        long lRetVal = TCAPPacket.FC_ERR_TCAP_UNKNOWN;
        byte[] data = GetCommand();

        if(param == null || paramLength < 1) {
            lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        } else if(data == null) {
            // データなし
        } else if(GetLength()-2 < paramLength) {    // -2はパラメータIDとパラメータ埋め込み位置分の2byte
            // レングス不正
        } else if(GetLength()-2 <= GetInsertPosition()) {
            // 埋め込み位置がコマンド長を超えた
        } else if(GetLength()-2 < GetInsertPosition() + paramLength) {
            // 埋め込み位置からコピーすると現在のメッセージ長を超える
        } else {
            // コマンドパラメータを上書き
            lRetVal = SetCommand(param);
        }

        return lRetVal;
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
