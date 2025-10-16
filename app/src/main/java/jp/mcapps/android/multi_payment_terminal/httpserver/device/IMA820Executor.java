package jp.mcapps.android.multi_payment_terminal.httpserver.device;

import com.google.gson.annotations.Expose;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.httpserver.Executor;
import jp.mcapps.android.multi_payment_terminal.httpserver.Request;
import jp.mcapps.android.multi_payment_terminal.httpserver.Result;
import jp.mcapps.android.multi_payment_terminal.httpserver.driver.SingingsExecutor;
import jp.mcapps.android.multi_payment_terminal.httpserver.events.EventBroker;
import jp.mcapps.android.multi_payment_terminal.httpserver.events.Types;
import jp.mcapps.android.multi_payment_terminal.webapi.tablet.data.ConnectionInfo;
import timber.log.Timber;

public class IMA820Executor extends Executor {
	@Override
	public Result execute(Request request) {
		Timber.d("/device/ima820 request: %s", request.getBodyString());

		ConnectionInfo body = request.getBodyObject(ConnectionInfo.class);
		if (body == null) {
			return badRequest();
		}

		EventBroker.ima820.onNext(body);

		return ok(body);
	}
}
