package jp.mcapps.android.multi_payment_terminal.httpserver;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import jp.mcapps.android.multi_payment_terminal.httpserver.device.DeviceRouter;
import jp.mcapps.android.multi_payment_terminal.httpserver.driver.DriverRouter;
import jp.mcapps.android.multi_payment_terminal.httpserver.events.EventBroker;
import jp.mcapps.android.multi_payment_terminal.httpserver.version.VersionRouter;

import fi.iki.elonen.NanoHTTPD;
import timber.log.Timber;

public class HttpServer extends NanoHTTPD {
    private static HttpServer _instance = null;
    public static boolean started() {
        return _instance != null;
	}

	public class ParsedUri {
		private final ApiVersions _version;
		private final String _routerName;
		private final String _route;

		public ApiVersions getVersion() {
			return _version;
		}

		public String getRouterName() {
			return _routerName;
		}

		public String getRoute() {
			return _route;
		}

		public ParsedUri(ApiVersions version, String routerName, String route) {
			_version = version;
			_routerName = routerName;
			_route = route;
		}
	}

	private final Map<String, Router> _routerMap = new HashMap<String, Router>() {
		{
			put("version", new VersionRouter());
			put("driver", new DriverRouter());
			put("device", new DeviceRouter());
		}
	};

    private final EventBroker _eventBroker = new EventBroker();

    public static boolean startServer(int port) {
		synchronized (HttpServer.class) {
			if (_instance == null) {
				try {
				    _instance = new HttpServer(port);
					Timber.d("created server port: %s", port);
					return true;
				} catch (IOException e) {
					Timber.e(e);
				}
			} else {
				Timber.i("HttpServer is already started: %s", port);
			}
		}
		return false;
	}

	public static void stopServer() {
		synchronized (HttpServer.class) {
			Timber.i("stop HttpServer");
			if (_instance != null) {
				_instance.stop();
				_instance = null;
			}
		}
	}


	private HttpServer(int port) throws IOException {
		super(port);
		start(NanoHTTPD.SOCKET_READ_TIMEOUT, true);
	}

	@Override
	public Response serve(IHTTPSession session) {
		Timber.i("receive request uri: %s", session.getUri());
		ParsedUri uri = parseUri(session.getUri());
		Result result;

		if (uri == null) {
			result = ResultUtils.notFound();
		} else {
			Request.Builder requestBuilder = new Request.Builder()
					.setVersion(uri.getVersion())
					.setRoute(uri.getRoute());

			setRequestParams(requestBuilder, session);

			try {
				Request r = requestBuilder.build();

				final Router router = _routerMap.get(uri.getRouterName());
				result = router.execute(r);
			} catch (Exception e) {
				result = ResultUtils.internalServerError();
			}
		}

		try (Response response = newFixedLengthResponse(result.getStatus(), result.getMimeType(), result.getBody())) {
			return response;
		} catch (IOException e) {
		    Timber.e(e);
		    return null;
		}
	}

	private interface Invokable<ReturnType> {
		ReturnType invoke(Object... args)
				throws ClassCastException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;
	}

	private class ClassMethod<ReturnType> implements Invokable<ReturnType> {
		private final java.lang.reflect.Method _method;

		public ClassMethod(Class<?> cls, String name, Class<?>... parameterTypes)
				throws NoSuchMethodException, SecurityException, IllegalAccessException {
			_method = cls.getDeclaredMethod(name, parameterTypes);
			_method.setAccessible(true);
		}

		@SuppressWarnings("unchecked")
		public ReturnType invoke(Object... args)
				throws ClassCastException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			return (ReturnType) _method.invoke(null, args);
		}
	}

	private class InstanceMethod<ReturnType> implements Invokable<ReturnType> {
		private final Object _instance;
		private final java.lang.reflect.Method _method;

		public InstanceMethod(Object instance, String name, Class<?>... parameterTypes)
				throws NoSuchMethodException, SecurityException, IllegalAccessException {
			_instance = instance;
			_method = instance.getClass().getDeclaredMethod(name, parameterTypes);
			_method.setAccessible(true);
		}

		@SuppressWarnings("unchecked")
		public ReturnType invoke(Object... args)
				throws ClassCastException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			return (ReturnType) _method.invoke(_instance, args);
		}
	}

	// HTTPSessionクラスのparseBody()の処理に修正を加えたメソッド
	private void setRequestParams(Request.Builder requestBuilder, IHTTPSession session) {
		try {
			final Class<HTTPSession> cls = HTTPSession.class;
			final HTTPSession sess = cls.cast(session);

			requestBuilder
					.setMethod(sess.getMethod())
					.setHeaders(sess.getHeaders());

			final Map<String, String> files = new HashMap<String, String>();

			// private変数の取得
			final int MEMORY_STORE_LIMIT = getStaticField(cls, "MEMORY_STORE_LIMIT", Integer.class);
			final int REQUEST_BUFFER_LEN = getStaticField(cls, "REQUEST_BUFFER_LEN", Integer.class);
			final Pattern BOUNDARY_PATTERN = getStaticField(NanoHTTPD.class, "BOUNDARY_PATTERN", Pattern.class);
			final Pattern CHARSET_PATTERN = getStaticField(NanoHTTPD.class, "CHARSET_PATTERN", Pattern.class);
			int rlen = getField(sess, "rlen", Integer.class);

			// privateメソッドの取得
			final InstanceMethod<RandomAccessFile> getTmpBucket = new InstanceMethod<>(sess, "getTmpBucket");
			final InstanceMethod<String> getAttributeFromContentHeader = new InstanceMethod<>(sess,
					"getAttributeFromContentHeader", String.class, Pattern.class, String.class);
			final InstanceMethod<Void> decodeMultipartFormData = new InstanceMethod<>(sess, "decodeMultipartFormData",
					String.class, String.class, ByteBuffer.class, Map.class, Map.class);
			final InstanceMethod<Void> decodeParms = new InstanceMethod<>(sess, "decodeParms", String.class, Map.class);
			final InstanceMethod<String> saveTmpFile = new InstanceMethod<>(sess, "saveTmpFile", ByteBuffer.class,
					int.class, int.class, String.class);
			final ClassMethod<Void> safeClose = new ClassMethod<>(NanoHTTPD.class, "safeClose", Object.class);

			RandomAccessFile randomAccessFile = null;
			try {
				long size = sess.getBodySize();
				ByteArrayOutputStream baos = null;
				DataOutput request_data_output = null;
				Map<String, String> queryParameter = new HashMap<>();
				byte[] body = null;

				// Store the request in memory or a file, depending on size
				if (size < MEMORY_STORE_LIMIT) {
					baos = new ByteArrayOutputStream();
					request_data_output = new DataOutputStream(baos);
				} else {
					randomAccessFile = (RandomAccessFile) getTmpBucket.invoke();
					request_data_output = randomAccessFile;
				}

				// Read all the body and write it to request_data_output
				byte[] buf = new byte[REQUEST_BUFFER_LEN];
				while (rlen >= 0 && size > 0) {
					rlen = sess.getInputStream().read(buf, 0, (int) Math.min(size, REQUEST_BUFFER_LEN));
					size -= rlen;
					if (rlen > 0) {
						request_data_output.write(buf, 0, rlen);
					}
				}

				ByteBuffer fbuf = null;
				if (baos != null) {
					fbuf = ByteBuffer.wrap(baos.toByteArray(), 0, baos.size());
				} else {
					fbuf = randomAccessFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0,
							randomAccessFile.length());
					randomAccessFile.seek(0);
				}

				decodeParms.invoke(sess.getQueryParameterString(), queryParameter);
				requestBuilder.setQuery(queryParameter);

				// If the method is POST, there may be parameters
				// in data section, too, read it:
				if (Method.POST.equals(sess.getMethod())) {
					String contentType = "";
					String contentTypeHeader = sess.getHeaders().get("content-type");

					StringTokenizer st = null;
					if (contentTypeHeader != null) {
						st = new StringTokenizer(contentTypeHeader, ",; ");
						if (st.hasMoreTokens()) {
							contentType = st.nextToken();
						}
					}

					if ("multipart/form-data".equalsIgnoreCase(contentType)) {
						// Handle multipart/form-data
						if (!st.hasMoreTokens()) {
							throw new ResponseException(Response.Status.BAD_REQUEST,
									"BAD REQUEST: Content type is multipart/form-data but boundary missing. Usage: GET /example/file.html");
						}
						decodeMultipartFormData.invoke(
								getAttributeFromContentHeader.invoke(contentTypeHeader, BOUNDARY_PATTERN, null), //
								getAttributeFromContentHeader.invoke(contentTypeHeader, CHARSET_PATTERN, "US-ASCII"),
								fbuf, sess.getParms(), files);
					} else {
						// byte[] postBytes = new byte[fbuf.remaining()];
						body = new byte[fbuf.remaining()];
						fbuf.get(body);
						String postLine = new String(body).trim();
						// Handle application/x-www-form-urlencoded
						if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType)) {
							decodeParms.invoke(postLine, sess.getParms());
						} else if ("application/json".equalsIgnoreCase(contentType)) {
							decodeParms.invoke(postLine, sess.getParms());
						} else if (postLine.length() != 0) {
							// Special case for raw POST data => create a
							// special files entry "postData" with raw content
							// data
							files.put("postData", postLine);
						}
					}
				} else if (Method.PUT.equals(sess.getMethod())) {
					String contentType = "";
					String contentTypeHeader = sess.getHeaders().get("content-type");

					StringTokenizer st = null;
					if (contentTypeHeader != null) {
						st = new StringTokenizer(contentTypeHeader, ",; ");
						if (st.hasMoreTokens()) {
							contentType = st.nextToken();
						}
					}

					if ("multipart/form-data".equalsIgnoreCase(contentType)) {
						// Handle multipart/form-data
						if (!st.hasMoreTokens()) {
							throw new ResponseException(Response.Status.BAD_REQUEST,
									"BAD REQUEST: Content type is multipart/form-data but boundary missing. Usage: GET /example/file.html");
						}
						decodeMultipartFormData.invoke(
								getAttributeFromContentHeader.invoke(contentTypeHeader, BOUNDARY_PATTERN, null), //
								getAttributeFromContentHeader.invoke(contentTypeHeader, CHARSET_PATTERN, "US-ASCII"),
								fbuf, sess.getParms(), files);
					} else {
						body = new byte[fbuf.remaining()];
						fbuf.get(body);
						String postLine = new String(body).trim();
						// Handle application/x-www-form-urlencoded
						if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType)) {
							decodeParms.invoke(postLine, sess.getParms());
						} else if ("application/json".equalsIgnoreCase(contentType)) {
							decodeParms.invoke(postLine, sess.getParms());
						} else if (postLine.length() != 0) {
							// Special case for raw POST data => create a
							// special files entry "postData" with raw content
							// data
							files.put("content", (String) saveTmpFile.invoke(fbuf, 0, fbuf.limit(), null));
						}
					}
				}

				requestBuilder.setBody(body);
			} finally {
				safeClose.invoke(randomAccessFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private <T> T getField(Object instance, String name, Class<T> parameterType)
			throws NoSuchFieldException, IllegalAccessException {
		Field field = instance.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return parameterType.cast(field.get(instance));
	}

	private <T> T getStaticField(Class<?> cls, String name, Class<T> parameterType)
			throws NoSuchFieldException, IllegalAccessException {
		Field field = cls.getDeclaredField(name);
		field.setAccessible(true);
		return parameterType.cast(field.get(null));
	}

	// /{version}/{route}/{path} の形式
	private ParsedUri parseUri(String uri) {
		String s = uri.substring(1, uri.length());

		int slash = s.indexOf("/");
		if (slash < 0) {
			Timber.d("Invalid uri schema");
			return null;
		}

		final String v = s.substring(0, slash);
		final ApiVersions version = checkVersion(v);

		if (version == null) {
			Timber.d("Invalid api version");
			return null;
		}

		s = s.substring(slash + 1, s.length());
		slash = s.indexOf("/");
		String routerName;
		if (slash > 0) {
			routerName = s.substring(0, slash);
			s = s.substring(slash, s.length());

			// 文字列の最後に/があれば取り除く
			if (s.lastIndexOf("/") == s.length() - 1) {
				s = s.substring(0, s.length() - 1);
			}
		} else {
			routerName = s;
			s = "";
		}

		if (!checkRouterName(routerName)) {
			Timber.d("Invalid route");
			return null;
		}

		final String path = s.length() > 0 ? s : "/";

		return new ParsedUri(version, routerName, path);
	}

	private ApiVersions checkVersion(String version) {
		for (ApiVersions v : ApiVersions.values()) {
			if (version.equals(v.getValue())) {
				return v;
			}
		}
		return null;
	}

	private boolean checkRouterName(String routerName) {
		for (String r : _routerMap.keySet()) {
			if (r.equals(routerName)) {
				return true;
			}
		}
		return false;
	}
}
