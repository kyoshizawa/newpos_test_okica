package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPPacket;
import timber.log.Timber;

public class DeviceWrapper extends DeviceElement {
    private final IDevice _iDevice;           // 操作するデバイスへの参照

    public DeviceWrapper(char deviceID, IDevice iDevice) {
        super(deviceID);
        _iDevice = iDevice;
    }

    /**
     * デバイスオブジェクト取得
     * デバイスオブジェクトを取得します。
     *
     * @return デバイスオブジェクト
     */
    public IDevice GetDevice() {
        return _iDevice;
    }

    /**
     * デバイス比較
     * デバイスオブジェクトを比較します。
     *
     * @param iDevice デバイスオブジェクト
     *
     * @return true  : 一致
     *         false : 不一致
     */
    public boolean Compare(IDevice iDevice) {
        return Compare(iDevice.GetType(), iDevice.GetName());
    }

    /**
     * デバイス比較
     * デバイスオブジェクトを比較します。
     *
     * @param deviceType デバイス種別文字列
     * @param deviceName デバイス名称文字列
     *
     * @return true  : 一致
     *         false : 不一致
     */
    public boolean Compare(byte[] deviceType, byte[] deviceName) {
        boolean bRetVal = false;
        final String type = new String(_iDevice.GetType());
        final String name = new String(_iDevice.GetName());

        if(deviceType == null || deviceName == null) {
            // パラメータエラー
            Timber.tag("iCAS").d("DeviceWrapper Compare parameter error");
        } else if(!type.equals(new String(deviceType))) {
            // タイプ違い
            Timber.tag("iCAS").d("DeviceWrapper Compare type unmatch");
        } else if(!name.equals(new String(deviceName))) {
            // 名称違い
            Timber.tag("iCAS").d("DeviceWrapper Compare name unmatch");
        } else {
            bRetVal = true;
        }

        return bRetVal;
    }

    @Override
    public byte[] GetType() {
        return _iDevice.GetType();
    }

    @Override
    public byte[] GetName() {
        return _iDevice.GetName();
    }

    /**
     * デバイス操作実行
     * デバイスに対する操作を行います。
     * FeliCaサーバからの操作要求により呼び出されます。
     *
     * @param replayPacket 応答パケット
     * @param param    操作用パラメータ文字列
     * @param data     操作用データ
     * @param len      操作用データ長
     *
     * @return 操作処理結果 IDevice::ERROR_CODE 参照
     */
    public long Operate(TCAPPacket replayPacket, byte[] param, final byte[] data, long len) {
        return _iDevice.Operate(replayPacket, param, data, len);
    }

    /**
     * FeliCaChipデバイス操作実行結果取得
     * FeliCaChipデバイス実行結果を取得します。
     *
     * @return 実行結果が入ったバッファ
     */
    public byte[] GetResponse() {
        return _iDevice.GetResponse();
    }

    /**
     * FeliCaChipデバイス操作実行結果長取得
     * FeliCaChipデバイス実行結果長を取得します。
     *
     * @return 実行結果データ長
     */
    public long GetResponseLength() {
        return _iDevice.GetResponseLength();
    }

    @Override
    public void Cancel() {
        // サンプル未実装 必要になったら実装すること
    }
}
