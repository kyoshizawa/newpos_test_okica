package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

public abstract class DeviceElement {
    private final char _deviceID;          // デバイスID

    public DeviceElement(char deviceID) {
        _deviceID = deviceID;
    }

    /**
     * デバイスIDの取得
     *
     * @return デバイスID
     */
    public char GetDeviceID() {
        return _deviceID;
    }

    /**
     * デバイス種別取得
     * デバイス種別を取得します。
     *
     * @return デバイス種別文字列
     */
    public abstract byte[] GetType();

    /**
     * デバイス名称取得
     * デバイス名称を取得します。
     *
     * @return デバイス名称文字列
     */
    public abstract byte[] GetName();

    /**
     * フェリカデバイスか否か
     * フェリカデバイスかを判定して返却する。
     *
     * @return true  : フェリカデバイス
     *         false : フェリカデバイス以外
     */
    public boolean IsFeliCaChip() {
        boolean bRetVal = false;
        byte[] type = GetType();

        if(type != null) {
            String typeStr = new String(type);
            if(typeStr.equals("FeliCa")) {
                bRetVal = true;
            }
        }

        return bRetVal;
    }

    /**
     * 処理の中断
     * デバイスに対する処理を中断します。
     */
    public abstract void Cancel();

}
