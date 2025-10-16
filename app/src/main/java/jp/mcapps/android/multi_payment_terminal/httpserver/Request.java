package jp.mcapps.android.multi_payment_terminal.httpserver;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fi.iki.elonen.NanoHTTPD;

public class Request {
	private static final Gson _gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private NanoHTTPD.Method _method;
	private Map<String, String> _headers;
	private Map<String, String> _query;
	private byte[] _body;
	private ApiVersions _version;
	private String _route;

	public NanoHTTPD.Method getMethod() {
		return _method;
	}

	public Map<String, String> getHeaders() {
		return _headers;
	}

	public Map<String, String> getQuery() {
		return _query;
	}

	public byte[] getBody() {
		return _body;
	}

	public String getBodyString() {
		return new String(_body);
	}

	public <T> T getBodyObject(Class<T> cls) {
		try {
			return _gson.fromJson(getBodyString(), cls);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public ApiVersions getVersion() {
		return _version;
	}

	public String getRoute() {
		return _route;
	}

	public static class Builder {
		private Request _request = new Request();

		public Request build() {
			if (_request._headers == null) {
				_request._headers = new HashMap<>();
			}
			if (_request._query == null) {
				_request._query = new HashMap<>();
			}
			if (_request._body == null) {
				_request._body = new byte[] {};
			}

			return _request;
		}

		public Builder setMethod(NanoHTTPD.Method method) {
			_request._method = method;
			return this;
		}

		public Builder setHeaders(Map<String, String> headers) {
			_request._headers = headers;
			return this;
		}

		public Builder setQuery(Map<String, String> query) {
			_request._query = query;
			return this;
		}

		public Builder setBody(byte[] body) {
			_request._body = body;
			return this;
		}

		public Builder setVersion(ApiVersions version) {
			_request._version = version;
			return this;
		}

		public Builder setRoute(String route) {
			_request._route = route;
			return this;
		}
	};
}
