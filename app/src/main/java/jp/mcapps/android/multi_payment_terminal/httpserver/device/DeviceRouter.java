package jp.mcapps.android.multi_payment_terminal.httpserver.device;

import jp.mcapps.android.multi_payment_terminal.httpserver.Executor;
import jp.mcapps.android.multi_payment_terminal.httpserver.Request;
import jp.mcapps.android.multi_payment_terminal.httpserver.Result;
import jp.mcapps.android.multi_payment_terminal.httpserver.Router;

public class DeviceRouter extends Router {

	public Result execute(Request request) {
		Executor executor = null;
		final String route = request.getRoute();

		if (route.equals("/ima820")) {
			switch (request.getMethod()) {
				case POST:
					executor = new IMA820Executor();
					break;
				default:
					break;
			}
		}

		if (executor == null) {
			return notFound();
		}

		return executor.execute(request);
	}
}
