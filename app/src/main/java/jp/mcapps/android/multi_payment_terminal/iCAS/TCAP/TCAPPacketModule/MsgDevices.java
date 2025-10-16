package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.FeliCaClient;

public class MsgDevices extends TCAPMessage {
    private final int DEVICES_LIST_NUM = 64;            // デバイスリスト数の最大値

    private int _length;                                // データサイズ
    private final List<DeviceInfo> _deviceInfoList;     // デバイスリスト

    public MsgDevices() {
        super((char) TCAPPacket.TCAP_MSG_EXT_STANDARD, (char) TCAPPacket.TCAP_MSG_DEVICEID_INVALID, (char) TCAPPacket.TCAP_MSG_MT_DEVICES);

        _deviceInfoList = new ArrayList<>(DEVICES_LIST_NUM);
        _length = 0;
    }

    /**
     * DEVICESメッセージのバイナリ化
     * DEVICESヘッダのバイナリ化。
     *
     * @param outBuffer 文字列情報格納先
     *
     * @return 格納結果
     */
    public long Dump(ByteBuffer outBuffer) {
        long lRetVal;

        byte[] data = new byte[6];
        data[0] = (byte)GetExtension();
        data[1] = (byte)(GetDeviceID() >> 8);
        data[2] = (byte)GetDeviceID();
        data[3] = (byte)GetMessageType();
        data[4] = (byte)(GetLength() >> 8);
        data[5] = (byte)GetLength();
        lRetVal = 6;

        outBuffer.put(data);
        lRetVal += DeviceListDump(outBuffer);

        return lRetVal;
    }

    /**
     * データのバイナリ化
     * データをバイナリ化します。
     *
     * @param outBuffer 文字列格納先
     *
     * @return 格納結果
     */
    private long DeviceListDump(ByteBuffer outBuffer) {
        long lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        long lTotalSize = 0;

        for(DeviceInfo deviceInfo : _deviceInfoList) {
            lRetVal = deviceInfo.Dump(outBuffer);
            if(lRetVal < 0) {
                lRetVal = TCAPPacket.FC_ERR_TCAP_DUMP;
                break;
            }
            lTotalSize += lRetVal;
        }

        if(lRetVal >= 0) {
            lRetVal = lTotalSize;
        }

        return lRetVal;
    }

    /**
     * データ数取得
     * DEVICESメッセージで通知するデータの数を取得します。
     *
     * @return データの数
     */
    public char GetDataNum() {
        return (char)_deviceInfoList.size();
    }

    /**
     * デバイスデータ追加
     * デバイスデータを追加します。
     *
     * @param deviceID   デバイスID
     * @param deviceType 種類
     * @param deviceName デバイス名
     *
     * @return true :成功
     *         false:失敗
     */
    public boolean AddData(char deviceID, byte[] deviceType, byte[] deviceName) {
        boolean bRetVal = false;

        if(deviceType != null && deviceName != null) {
            // デバイスデータ追加
            DeviceInfo deviceInfo = new DeviceInfo(deviceID, deviceType, deviceName);

            if(_deviceInfoList.add(deviceInfo)) {
                _length += deviceInfo.GetNameLength() + deviceInfo.GetTypeLength() + 6;     // 種類名 + デバイス名 + (ID,予約,種類･デバイス名長)
                bRetVal = true;
            }
        }

        return bRetVal;
    }

    @Override
    public int GetLength() {
        return _length;
    }

    @Override
    public long GetSize() {
        return _length + 6;
    }

    /**
     * データリスト取得
     * Deviceデータリストを取得します。
     *
     * @return データの数
     */
    public List<DeviceInfo> GetData() {
        return _deviceInfoList;
    }
}
