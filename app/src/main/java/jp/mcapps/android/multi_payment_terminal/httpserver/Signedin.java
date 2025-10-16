package jp.mcapps.android.multi_payment_terminal.httpserver;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Signedin {
	public static class Request {
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
}
