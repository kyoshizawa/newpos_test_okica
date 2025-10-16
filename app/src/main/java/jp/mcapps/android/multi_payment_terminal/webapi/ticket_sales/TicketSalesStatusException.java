package jp.mcapps.android.multi_payment_terminal.webapi.ticket_sales;

public class TicketSalesStatusException extends Exception {

    public TicketSalesStatusException(int code, String message) {
        super(message);
        this._code = code;
    }

    private final int _code;

    public int getCode() {
        return _code;
    }
}
