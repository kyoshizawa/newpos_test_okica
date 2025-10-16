package jp.mcapps.android.multi_payment_terminal.httpserver;

public abstract class Router extends ResultUtils {
	public abstract Result execute(Request request);
}
