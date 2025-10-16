package jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPPacket;

public class Device extends DeviceElement implements IDevice {
//    private Handler _handler;           // メインスレッドへのイベントハンドラ
    private IFeliCaClientEventListener _eventListener;  // イベントリスナ
    private byte[] _deviceType;         // デバイスタイプ
    private byte[] _deviceName;         // デバイス名称

    public Device(char deviceID) {
        super(deviceID);
        _deviceType = null;
        _deviceName = null;
    }

//    public void SetHandler(Handler handler) { _handler = handler; }
    public void SetListener(IFeliCaClientEventListener listener) { _eventListener = listener; }

    @Override
    public byte[] GetType() {
        return _deviceType;
    }

    @Override
    public void SetType(byte[] deviceType) {
        if(deviceType != null) {
            _deviceType = new byte[deviceType.length];
            System.arraycopy(deviceType, 0, _deviceType, 0, _deviceType.length);
        }
    }

    @Override
    public byte[] GetName() {
        return _deviceName;
    }

    @Override
    public void SetName(byte[] deviceName) {
        if(deviceName != null) {
            _deviceName = new byte[deviceName.length];
            System.arraycopy(deviceName, 0, _deviceName, 0, _deviceName.length);
        }
    }

    @Override
    public long Operate(TCAPPacket replyPacket, byte[] param, byte[] data, long lDataLen) {
        long lRetVal;
        DeviceOperate deviceOperate = new DeviceOperate(GetDeviceID(), _deviceName, param, data, lDataLen);

        /*
        Message message = new Message();
        message.what = FeliCaClient.FC_MSG_TO_MAIN_ON_OPERATE;
        message.obj = deviceOperate;
        _handler.sendMessage(message);
*/
        lRetVal = _eventListener.OnDeviceOperate(deviceOperate, replyPacket);

        return lRetVal;
    }

    @Override
    public byte[] GetResponse() {
        return null;
    }

    @Override
    public long GetResponseLength() {
        return 0;
    }

    @Override
    public void Cancel(){}
}
