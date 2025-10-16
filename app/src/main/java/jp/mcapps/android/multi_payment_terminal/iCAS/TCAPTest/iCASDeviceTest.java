package jp.mcapps.android.multi_payment_terminal.iCAS.TCAPTest;

import android.os.Handler;
import android.os.Message;

import com.google.gson.Gson;
import com.google.gson.internal.Primitives;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;

import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaChip.FeliCaChip;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.Device;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.FeliCaClient;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.FeliCaClient.IDevice;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPMessage;
import jp.mcapps.android.multi_payment_terminal.iCAS.TCAP.TCAPPacketModule.TCAPPacket;

public class iCASDeviceTest {
    private static final HashMap<Integer, String> _deviceMap = new HashMap<>();

    // デバイス定義
    ////////////////////////////////////////////////////////////////////////
    // デバイスID
    public static final int DEVICE_ID_FELICA    = 0x0001;
    public static final int DEVICE_ID_TEST      = 0x0002;

    private IDevice.DeviceOperate _deviceOperate;
    private final Gson _gson = new Gson();

    public iCASDeviceTest(IDevice.DeviceOperate deviceOperate) {
        _deviceOperate = deviceOperate;
    }

    static {
        _deviceMap.put(DEVICE_ID_TEST, "x");
//        _deviceMap.put(DEVICE_ID_FELICA, "R/W");
    }

    public static long AddDevice(FeliCaClient feliCaClient, JSONObject params) throws JSONException {
        long lRetVal = FeliCaClient.FC_ERR_SUCCESS;


        for(int i = 1; i < 65; i++) {
            if(params.has("deviceId" + i)) {
                int id = Integer.valueOf(params.getString("deviceId" + i));
                if(params.getString("deviceType" + i).equals("FeliCa")) {
                    FeliCaChip device = new FeliCaChip((char) id);
                    device.SetName(params.getString("deviceName" + i).getBytes());
                    device.SetType(params.getString("deviceType" + i).getBytes());
                    lRetVal = feliCaClient.AddFeliCaChip(device);
                } else {
                    Device device = new Device((char) id);
                    device.SetName(params.getString("deviceName" + i).getBytes());
                    device.SetType(params.getString("deviceType" + i).getBytes());
                    lRetVal = feliCaClient.AddDevice(device);
                }
                if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
                    break;
                }
            } else {
                break;
            }
        }

/*

        for(Integer key : _deviceMap.keySet()) {
            int val = key;

            if(key == DEVICE_ID_TEST) {
                Device device = new Device((char) val);
                device.SetName(_deviceMap.get(key).getBytes());
                device.SetType("a".getBytes());

                lRetVal = feliCaClient.AddDevice(device);
            } else if(key == DEVICE_ID_FELICA) {
                FeliCaChip feliCaChip = new FeliCaChip((char) val);
                // FeliCaデバイス追加
                feliCaChip.SetName(_deviceMap.get(key).getBytes());
                feliCaChip.SetType("FeliCa".getBytes());

                lRetVal = feliCaClient.AddFeliCaChip(feliCaChip);
            }

            if(lRetVal != FeliCaClient.FC_ERR_SUCCESS) {
                break;
            }
        }

*/

        return lRetVal;
    }

    public long Operate(JSONObject params, boolean bIsAborted, Handler handlerToMain, TCAPPacket replayPacket) throws IOException, JSONException {
        long lRetVal = FeliCaClient.FC_ERR_SUCCESS;
        String paramName = "";
        Message message = new Message();

        if(_deviceOperate.GetParam() != null) {
            paramName = new String(_deviceOperate.GetParam());
        }

        for(int i = 1; i < 128; i++) {
            if(params.has("opeDeviceId" + i)) {
                if(paramName.equals(params.getString("opeDeviceParam" + i))) {
                    lRetVal = FeliCaClient.FC_ERR_SUCCESS;

                    if(paramName.equals("exception")) {
                        lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
                    } else if(paramName.equals("data")) {
                        // TCAP検定試験用 トータルサイズが65535+5を超えるケース
                        TCAPMessage tcapMessage = new TCAPMessage((char) TCAPPacket.TCAP_MSG_EXT_STANDARD, (char) _deviceOperate.GetDeviceID(), (char) TCAPPacket.TCAP_MSG_MT_DEVICE_RESPONSE);
                        int length = ((_deviceOperate.GetData()[0] & 0x00000000000000FF) << 8) | (_deviceOperate.GetData()[1] & 0x00000000000000FF);
                        byte[] data = new byte[2 + 2 + length];
                        // データサイズ
                        data[2] = (byte) ((length & 0x000000000000FF00) >> 8);
                        data[3] = (byte) (length & 0x00000000000000FF);

                        tcapMessage.SetMessageData(data, data.length);
                        replayPacket.AddMessage(tcapMessage);
                    } else {
                        TCAPMessage tcapMessage = new TCAPMessage((char) TCAPPacket.TCAP_MSG_EXT_STANDARD, (char) _deviceOperate.GetDeviceID(), (char) TCAPPacket.TCAP_MSG_MT_DEVICE_RESPONSE);
                        byte[] data = new byte[2 + 2 + (int) _deviceOperate.GetDataLen()];     // 予約2byte + 応答データの長さ2byte + data
                        // データサイズ
                        data[2] = (byte) ((_deviceOperate.GetDataLen() & 0x000000000000FF00) >> 8);
                        data[3] = (byte) (_deviceOperate.GetDataLen() & 0x00000000000000FF);
                        if (_deviceOperate.GetDataLen() > 0) {
                            System.arraycopy(_deviceOperate.GetData(), 0, data, 4, (int) _deviceOperate.GetDataLen());
                        }
                        tcapMessage.SetMessageData(data, data.length);
                        replayPacket.AddMessage(tcapMessage);
                    }
                    break;
                } else {
                    // 不明なパラメータ名
                    lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
                }
            } else {
                // 不明なパラメータ名
                lRetVal = FeliCaClient.FC_ERR_CLIENT_MSG_FORMAT;
                break;
            }
        }
/*
        switch (_deviceOperate.GetDeviceID()) {
            case DEVICE_ID_TEST:
                if(paramName.equals("a")) {
                    // 試験用
                    TCAPMessage tcapMessage = new TCAPMessage((char)TCAPPacket.TCAP_MSG_EXT_STANDARD, (char)_deviceOperate.GetDeviceID(),  (char)TCAPPacket.TCAP_MSG_MT_DEVICE_RESPONSE);
                    byte[] data = new byte[2+2+(int)_deviceOperate.GetDataLen()];     // 予約2byte + 応答データの長さ2byte + data
                    data[3] = 0x01;     // データサイズ
                    System.arraycopy(_deviceOperate.GetData(), 0, data, 4, (int)_deviceOperate.GetDataLen());
                    tcapMessage.SetMessageData(data, data.length);
                    replayPacket.AddMessage(tcapMessage);
                } else {
                    // 不明なパラメータ名
                    lRetVal = FeliCaClient.FC_ERR_CLIENT_UNKNOWN;
                }
                break;
            default:
                // 不明なID
                break;
        }
*/

        return lRetVal;
    }

    public <T> T getJsonObject(byte[] data, Class<T> classOfT) throws IOException {
        final String json = new String(data);
        final Object object = _gson.fromJson(json, (Type) classOfT);
        return Primitives.wrap(classOfT).cast(object);
    }
}
