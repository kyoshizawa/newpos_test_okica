package jp.mcapps.android.multi_payment_terminal.encoding;

public class ErrorDetail {

    public ErrorDetail(String code) {
        this(code, "", "");
    }

    public ErrorDetail(String code, String value) {
        this(code, "", value);
    }

    public ErrorDetail(String code, String location, String value) {
        this.errorCode = code;
        this.errorLocation = location;
        this.errorValue = value;
    }

    private final String errorCode;
    private final String errorLocation;
    private final String errorValue;

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorLocation() {
        return errorLocation;
    }

    public String getErrorValue() {
        return errorValue;
    }
}
