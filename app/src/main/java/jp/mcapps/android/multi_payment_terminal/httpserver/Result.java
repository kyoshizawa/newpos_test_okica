package jp.mcapps.android.multi_payment_terminal.httpserver;

import fi.iki.elonen.NanoHTTPD.Response.IStatus;

public class Result {
	private final IStatus _status;
	private final String _body;
	private final String _mimeType;

	public Result(IStatus status, String body) {
		_status = status;
		_body = body;
		_mimeType = "application/json";
	}

	public Result(IStatus status, String body, String mimeType) {
		_status = status;
		_body = body;
		_mimeType = mimeType;
	}

	public IStatus getStatus() {
		return _status;
	}

	public String getBody() {
		return _body;
	}

	public String getMimeType() {
		return _mimeType;
	}
}
