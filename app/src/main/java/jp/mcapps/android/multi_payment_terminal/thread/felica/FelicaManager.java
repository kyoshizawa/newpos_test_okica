package jp.mcapps.android.multi_payment_terminal.thread.felica;

import com.pos.device.SDKException;
import com.pos.device.picc.FeliCa;
import com.pos.device.picc.PiccReader;

import java.util.Arrays;

import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import timber.log.Timber;

public class FelicaManager {
    private static PiccReader _piccReader = null;
    private static final byte[] TIMEOUT_RESPONSE = {0x63, 0x00};

    public FelicaManager(){
        _piccReader = PiccReader.getInstance();
    }

    public byte[] executeCommand(byte[] command, long timeout) {
        byte[] retcmd;

        _piccReader.selectCarrierType(PiccReader.MIF_TYPE_C1);
        int felicaCmdTime = (int)timeout * 100 + ((int)timeout - 1) * 10;
        //long start = System.currentTimeMillis();
        try {
            byte[] resp = FeliCa.getInstance().transmit(command, felicaCmdTime);
            retcmd = resp != null ? resp : TIMEOUT_RESPONSE;
        } catch (SDKException e) {
            //e.printStackTrace();
            //Timber.d("timeout milli sec : %d", System.currentTimeMillis() - start);
            Timber.tag("iCAS").d("time out");
            retcmd = TIMEOUT_RESPONSE;
        }

        return  retcmd;
    }

    public void resetReader() {
        try {
            if (_piccReader!=null) {
                _piccReader.reset();
            }
        } catch (SDKException e) {
            e.printStackTrace();
        }
    }

    public void releaseReader(){
        try {
            if (_piccReader!=null) {
                _piccReader.release();
            }
        } catch (SDKException e) {
            e.printStackTrace();
        }
    }

    public String deviceCheck() {
        _piccReader.selectCarrierType(PiccReader.MIF_TYPE_C1);

        String result = null;
        byte[] command = {0x06, 0x00, (byte) 0xff, (byte) 0xff, 0x01, 0x00};

        byte[] resp = executeCommand(command, 1000);
        if (resp != null) {
            Timber.d(Arrays.toString(resp));
            //IDmの要素を抽出
            byte[] idmBytes = Arrays.copyOfRange(resp, 2, 10);
            //バイト列を16進数文字列に変換
            result = McUtils.bytesToHexString(idmBytes);
        }

        return result;
    }
}
