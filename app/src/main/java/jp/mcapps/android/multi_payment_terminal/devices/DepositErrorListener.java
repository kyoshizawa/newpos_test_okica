package jp.mcapps.android.multi_payment_terminal.devices;

public interface DepositErrorListener {
    void onErrorDeposit(int errorCode, int extendErrorCode);
}
