package jp.mcapps.android.multi_payment_terminal.httpserver;

public abstract class Executor extends ResultUtils {
	public abstract Result execute(Request request);
}
