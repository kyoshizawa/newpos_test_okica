package jp.mcapps.android.multi_payment_terminal.httpserver.driver;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.httpserver.events.EventBroker;
import jp.mcapps.android.multi_payment_terminal.httpserver.Executor;
import jp.mcapps.android.multi_payment_terminal.httpserver.Request;
import jp.mcapps.android.multi_payment_terminal.httpserver.Result;
import jp.mcapps.android.multi_payment_terminal.httpserver.events.Types;
import timber.log.Timber;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SingingsExecutor extends Executor {
	public static class Events {
		public static final String SignedIn = "signed-in";
		public static final String SignedOut = "signed-out";
	}

	public static class V1Request {
		@Expose
		public String event;

		@Expose
		public Driver driver;
	}

	public static class Driver {
		@Expose
		public String id;

		@Expose
		@SerializedName("office_id")
		public String officeId;

		@Expose
		public String code;

		@Expose
		public String name;
	}

	@Override
	public Result execute(Request request) {
	    Timber.d("/events/signings request: %s", request.getBodyString());
		V1Request body = request.getBodyObject(V1Request.class);
		if (body == null) {
		}

		if (body.event.equals(Events.SignedIn)) {
			EventBroker.signIn.onNext(new Types.SignIn() {{
			    driverCode = body.driver.code;
				driverName = body.driver.name;
			}});
		}
		else if (body.event.equals(Events.SignedOut)) {
			// Todo タブレットが帰庫したときの動作を考える 業務終了を促す？
			System.out.println("signedout");
		}
		else {
			return badRequest();
		}

		return created();
	}
}
