package jp.mcapps.android.multi_payment_terminal.data.sam;

import androidx.annotation.NonNull;

import com.pos.device.apdu.ResponseApdu;

import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamExceptions.IllegalStatusWordException;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamExceptions.RWStatusException;
import jp.mcapps.android.multi_payment_terminal.data.sam.SamExceptions.SyntaxErrorException;
import jp.mcapps.android.multi_payment_terminal.devices.SamRW;
import jp.mcapps.android.multi_payment_terminal.util.McUtils;
import timber.log.Timber;

public class SamResponse <T extends SamResponseTypes.ISamResponseType> {
    private T data;
    public T getData() {
        return this.data;
    }

    private final byte[] rawData;
    public byte[] getRawData() {
        return rawData;
    }

    private int statusWord;
    public int getStatusWord() {
        return statusWord;
    }

    private Throwable error = null;
    public Throwable getError() {
        return error;
    }
    public void setError(Throwable error) {
        this.error = error;
    }
    public boolean hasError() {
        return error != null;
    }

    public SamResponse(ResponseApdu apdu, T responseType) {
        byte[] rawData = apdu.getData();
        this.rawData = rawData;

        try {
            statusWord = (apdu.getSW1() << 8) + apdu.getSW2();

            if (statusWord != 0x9000) {
                this.data = null;
                this.error = new IllegalStatusWordException();
                return;
            }

            if (rawData[3] == 0x7F) {
                this.data = null;
                error = new SyntaxErrorException();
                return;
            }

            responseType.parse(rawData);
            this.data = responseType;
        } catch (RWStatusException | Exception e) {
            this.error = e;
            Timber.e("SamResponse error %s", e);
            // 次回以降通信できなくなる可能性があるのでSAM認証をやり直す
            MainApplication.getInstance().isInitFeliCaSAM(SamRW.open(Constants.MC_NORMAL_KEY, SamRW.States.Normal) == SamRW.OpenResult.SUCCESS);
        }
    }

    public SamResponse(Throwable error) {
        this.error = error;
        this.rawData = new byte[0];
    }

    @NonNull
    public String toString() {
        return McUtils.bytesToHexString(this.rawData);
    }
}
