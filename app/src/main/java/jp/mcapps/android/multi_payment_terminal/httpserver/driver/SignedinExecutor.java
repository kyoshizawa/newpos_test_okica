package jp.mcapps.android.multi_payment_terminal.httpserver.driver;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.httpserver.Executor;
import jp.mcapps.android.multi_payment_terminal.httpserver.Request;
import jp.mcapps.android.multi_payment_terminal.httpserver.Result;
import com.google.gson.annotations.Expose;

public class SignedinExecutor extends Executor {
	public static class V1Response {
		@Expose
		public String code;

		@Expose
		public String name;
	}

	@Override
	public Result execute(Request request) {
		final V1Response response = new V1Response();
		response.code = AppPreference.getDriverCode();
		response.name = AppPreference.getDriverName();
		return ok(response);
	}
}
