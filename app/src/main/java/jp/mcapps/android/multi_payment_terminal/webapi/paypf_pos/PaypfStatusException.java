package jp.mcapps.android.multi_payment_terminal.webapi.paypf_pos;

public class PaypfStatusException extends Exception {

    public PaypfStatusException(int code, String message) {
        super(message);
        this._code = code;
    }

    private final int _code;

    public int getCode() {
        return _code;
    }
}
