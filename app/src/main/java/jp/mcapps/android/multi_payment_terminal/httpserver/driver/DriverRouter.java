package jp.mcapps.android.multi_payment_terminal.httpserver.driver;

import jp.mcapps.android.multi_payment_terminal.httpserver.Executor;
import jp.mcapps.android.multi_payment_terminal.httpserver.Request;
import jp.mcapps.android.multi_payment_terminal.httpserver.Result;
import jp.mcapps.android.multi_payment_terminal.httpserver.Router;

public class DriverRouter extends Router {

	public Result execute(Request request) {
		Executor executor = null;
		final String route = request.getRoute();

		if (route.equals("/signedin")) {
			switch (request.getMethod()) {
				case GET:
					executor = new SignedinExecutor();
					break;
				default:
					break;
			}
		}

		if (route.equals("/events/signings")) {
			switch (request.getMethod()) {
				case POST:
					executor = new SingingsExecutor();
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
