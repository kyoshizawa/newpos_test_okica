package jp.mcapps.android.multi_payment_terminal.httpserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fi.iki.elonen.NanoHTTPD.Response.Status;;

public abstract class ResultUtils {
	protected static final Gson _gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	
	// 200
	protected static final Result ok(Object obj) {
		return new Result(Status.OK, _gson.toJson(obj));
	}

	// 201
	protected static final Result created() {
		return new Result(Status.CREATED, null);
	}

	// 202
	protected static final Result accepted() {
		return new Result(Status.ACCEPTED, null);
	}

	// 400
	protected static final Result badRequest() {
		return new Result(Status.BAD_REQUEST, "null");
	}
	protected static final Result badRequest(Object obj) {
		return new Result(Status.BAD_REQUEST, _gson.toJson(obj));
	}

	// 404
	protected static final Result notFound() {
		return new Result(Status.NOT_FOUND, "null");
	}
	protected static final Result notFound(Object obj) {
		return new Result(Status.NOT_FOUND, _gson.toJson(obj));
	}

	// 500
	protected static final Result internalServerError() {
		return new Result(Status.INTERNAL_ERROR, "null");
	}
	protected static final Result internalServerError(Object obj) {
		return new Result(Status.INTERNAL_ERROR, _gson.toJson(obj));
	}
}
