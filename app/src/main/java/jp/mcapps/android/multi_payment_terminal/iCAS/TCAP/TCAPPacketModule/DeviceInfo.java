package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule;

import java.nio.ByteBuffer;

public class DeviceInfo {
    private char _deviceID;     // デバイスID
    private char _reserved;     // 予約
    private char _typeLength;   // 種類名長
    private char _nameLength;   // デバイス名長
    private char _reserved2;    // 予約2
    private byte[] _deviceType; // 種類名
    private byte[] _deviceName; // デバイス名

    public DeviceInfo() {
        _deviceID = 0;
        _typeLength = 0;
        _nameLength = 0;
        _deviceType = null;
        _deviceName = null;
    }

    public DeviceInfo(char deviceID, byte[] deviceType, byte[] deviceName) {
        _deviceID = deviceID;
        _typeLength = 0;
        _nameLength = 0;
        _deviceType = null;
        _deviceName = null;

        if(deviceType != null && deviceName != null) {
            _typeLength = (char)deviceType.length;
            _deviceType = new byte[_typeLength];
            System.arraycopy(deviceType, 0, _deviceType, 0, _typeLength);

            _nameLength = (char)deviceName.length;
            _deviceName = new byte[_nameLength];
            System.arraycopy(deviceName, 0, _deviceName, 0, _nameLength);
        }
    }

    /**
     * デバイスデータをバイナリ化
     * デバイスデータをバイナリ化します。
     *
     * @param outBuffer 文字列格納先
     *
     * @return 格納結果
     */
    public long Dump(ByteBuffer outBuffer) {
        long lRetVal;
        byte[] deviceData = new byte[4];

        // デバイスID
        deviceData[0] = (byte)(_deviceID >> 8);
        deviceData[1] = (byte)_deviceID;
        // 予約
        deviceData[2] = 0;
        deviceData[3] = 0;
        outBuffer.put(deviceData);

        // 種類名長
        outBuffer.put((byte)_typeLength);
        // 種類名
        outBuffer.put(_deviceType);
        // デバイス名長
        outBuffer.put((byte)_nameLength);
        // デバイス名
        outBuffer.put(_deviceName);

        lRetVal = 4 + 1 + _deviceType.length + 1 + _deviceName.length;

        return lRetVal;
    }

    /**
     * デバイスID取得
     * デバイスIDを取得します。
     *
     * @return デバイスID
     */
    public char GetDeviceID() {
        return _deviceID;
    }

    /**
     * 種類名の長さ取得
     * 種類名の長さを取得します。
     *
     * @return 種類名の長さ
     */
    public char GetTypeLength() {
        return _typeLength;
    }

    /**
     * 種類名取得
     * 種類名を取得します。
     *
     * @return 種類名
     */
    public byte[] GetType() {
        return _deviceType;
    }

    /**
     * デバイス名の長さ取得
     * デバイス名の長さを取得します。
     *
     * @return デバイス名の長さ
     */
    public char GetNameLength() {
        return _nameLength;
    }

    /**
     * デバイス名取得
     * デバイス名を取得します。
     *
     * @return デバイス名
     */
    public byte[] GetName() {
        return _deviceName;
    }
}
