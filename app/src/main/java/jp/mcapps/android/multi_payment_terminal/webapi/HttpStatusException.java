package jp.mcapps.android.multi_payment_terminal.webapi;

public class HttpStatusException extends RuntimeException {
    private final int _statusCode;
    private final String _body;
    public int getStatusCode() {
        return _statusCode;
    }
    public String getBody() {
        return _body;
    }

    public HttpStatusException(int statusCode , String body) {
        _statusCode = statusCode;
        _body = body;
    }
}
