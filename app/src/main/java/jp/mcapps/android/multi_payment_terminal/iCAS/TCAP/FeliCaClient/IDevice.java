package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPPacket;

public interface IDevice {
    enum errorCode
    {
        ERR_NONE(0),                // エラーなし
        ERR_UNKNOWN(-1),            // 予期せぬエラー
        ERR_BAD_PARAM(-2),          // パラメータエラー
        ERR_EXCEPTION(-3),          // 実行エラー
        ;

        private final int _val;

        errorCode(final int val) {
            _val = val;
        }

        public int getInt() {
            return _val;
        }
    }

    /**
     * デバイス種別取得
     * デバイスの種別を取得します。
     *
     * @return デバイス種別文字列
     */
    byte[] GetType();

    /**
     * デバイス種別設定
     * デバイスの種別を設定します。
     *
     * @param deviceType デバイス種別文字列
     */
    void SetType(byte[] deviceType);

    /**
     * デバイス名称取得
     * デバイスの名称を取得します。
     *
     * @return デバイス名称文字列
     */
    byte[] GetName();

    /**
     * デバイス名称設定
     * デバイスの名称を設定します。
     *
     * @param deviceName デバイス名称文字列
     */
    void SetName(byte[] deviceName);

    /**
     * デバイス操作実行
     * デバイスに対する操作を行います。
     * FeliCaサーバからの操作要求により呼び出されます。
     *
     * @param replayPacket 応答パケット
     * @param param    操作用パラメータ文字列
     * @param data     操作用データ
     * @param lDataLen 操作用データ長
     *
     * @return 操作処理結果 IDevice::ERROR_CODE 参照
     */
    long Operate(TCAPPacket replayPacket, byte[] param, byte[] data, long lDataLen);

    /**
     * FeliCaChipデバイス操作実行結果取得
     * FeliCaChipデバイス実行結果を取得します。
     *
     * @return 実行結果
     */
    byte[] GetResponse();

    /**
     * FeliCaChipデバイス操作実行結果長取得
     * FeliCaChipデバイス実行結果長を取得します。
     *
     * @return 実行結果データ長
     */
    long GetResponseLength();

    class DeviceOperate {
        private final char   _deviceID;
        private final byte[] _deviceName;
        private final byte[] _param;
        private final byte[] _data;
        long _lDatalen;

        public DeviceOperate(char deviceID, byte[] deviceName, byte[] param, byte[] data, long datalen) {
            _deviceID = deviceID;
            _deviceName = deviceName;
            _param = param;
            _data = data;
            _lDatalen = datalen;
        }

        public char GetDeviceID() {
            return _deviceID;
        }

        public byte[] GetDeviceName() {
            return _deviceName;
        }

        public byte[] GetParam() {
            return _param;
        }

        public byte[] GetData() {
            return _data;
        }

        public long GetDataLen() {
            return _lDatalen;
        }
    }
}
