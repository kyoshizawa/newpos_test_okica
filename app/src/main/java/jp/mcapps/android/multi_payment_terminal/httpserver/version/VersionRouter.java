package jp.mcapps.android.multi_payment_terminal.httpserver.version;

import jp.mcapps.android.multi_payment_terminal.httpserver.Executor;
import jp.mcapps.android.multi_payment_terminal.httpserver.Request;
import jp.mcapps.android.multi_payment_terminal.httpserver.Result;
import jp.mcapps.android.multi_payment_terminal.httpserver.Router;

public class VersionRouter extends Router {

	@Override
	public Result execute(Request request) {
		Executor executor = null;
		final String route = request.getRoute();

		if (route.equals("/")) {
			switch (request.getMethod()) {
				case GET:
					executor = new VersionExecutor();
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
