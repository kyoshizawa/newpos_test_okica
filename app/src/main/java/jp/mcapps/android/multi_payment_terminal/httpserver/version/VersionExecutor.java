package jp.mcapps.android.multi_payment_terminal.httpserver.version;

import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.httpserver.Executor;
import jp.mcapps.android.multi_payment_terminal.httpserver.Request;
import jp.mcapps.android.multi_payment_terminal.httpserver.Result;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class VersionExecutor extends Executor {
	public class V1Response {
		@Expose
		@SerializedName("application_id")
		public String applicationId;

		@Expose
		@SerializedName("version_name")
		public String versionName;

		@Expose
		@SerializedName("version_code")
		public int versionCode;
	}

	public Result execute(Request request) {
		final V1Response v = new V1Response();
		v.applicationId = BuildConfig.APPLICATION_ID;
		v.versionName = BuildConfig.VERSION_NAME;
		v.versionCode = BuildConfig.VERSION_CODE;

		return ok(v);
	}
}
