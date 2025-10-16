package jp.mcapps.android.multi_payment_terminal.devices;

import com.epson.epos2.Epos2Exception;

public class CashChangerDispenseError {
    private int mEpos2ExceptionCode;
    private int mEpos2CalllbackCode;
    private int mOposErrorCode;

    public CashChangerDispenseError(int epose2ExceptionCode, int epos2CalllbackCode, int oposErrorCode) {
        mEpos2ExceptionCode = epose2ExceptionCode;
        mEpos2CalllbackCode = epos2CalllbackCode;
        mOposErrorCode = oposErrorCode;
    }

    public int getEpos2ExceptionCode() {
        return mEpos2ExceptionCode;
    }

    public int getEpos2CalllbackCode() {
        return mEpos2CalllbackCode;
    }

    public int getOposErrorCode() {
        return mOposErrorCode;
    }

    public boolean isError() {
        return mEpos2ExceptionCode != 0 || mEpos2CalllbackCode != 0 || mOposErrorCode != 0;
    }
}
