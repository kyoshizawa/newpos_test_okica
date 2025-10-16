package jp.mcapps.android.multi_payment_terminal.httpserver;

public enum ApiVersions {
	v1("v1");

	private final String version;

	ApiVersions(String version) {
		this.version = version;
	}

	public String getValue() {
		return this.version;
	}
}
