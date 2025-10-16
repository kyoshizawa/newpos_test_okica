package jp.mcapps.android.multi_payment_terminal.thread.credit;

import com.pos.device.SDKException;

import jp.mcapps.android.multi_payment_terminal.thread.emv.EmvProcessing;

public interface CreditState {
    public abstract int stateMethod() throws SDKException;
}
