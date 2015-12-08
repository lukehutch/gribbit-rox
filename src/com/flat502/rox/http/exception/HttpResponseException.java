package com.flat502.rox.http.exception;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.flat502.rox.http.HttpConstants;
import com.flat502.rox.http.HttpRequestBuffer;
import com.flat502.rox.http.HttpResponse;


public class HttpResponseException extends HttpMessageException {
	private Map<String, String> headers;
	private int statusCode;
	private String reasonPhrase;

	public HttpResponseException(int statusCode, String reasonPhrase) {
		this(statusCode, reasonPhrase, (HttpRequestBuffer)null);
	}

	public HttpResponseException(int statusCode, String reasonPhrase, HttpRequestBuffer req) {
		this(statusCode, reasonPhrase, req, (Map<String, String>)null);
	}
	
	public HttpResponseException(int statusCode, String reasonPhrase, HttpRequestBuffer req, Map<String, String> headers) {
		super(statusCode+": "+reasonPhrase, req);
		this.statusCode = statusCode;
		this.reasonPhrase = reasonPhrase;
		this.headers = headers;
	}
	
	public HttpResponseException(int statusCode, String reasonPhrase, Throwable e) {
		this(statusCode, reasonPhrase, null, e);
	}

	public HttpResponseException(int statusCode, String reasonPhrase, HttpRequestBuffer req, Throwable e) {
		this(statusCode, reasonPhrase);
		this.initCause(e);
	}

	public int getStatusCode() {
		return this.statusCode;
	}
	
	public String getReasonPhrase() {
		return this.reasonPhrase;
	}
	
	public HttpResponse toHttpResponse(String httpVersion) {
		HttpResponse rsp = new HttpResponse(httpVersion, this.getStatusCode(), this.getReasonPhrase());
		if (this.headers != null) {
		    Iterator<Entry<String, String>> iter = this.headers.entrySet().iterator();
			while (iter.hasNext()) {
				Entry<String, String> entry = iter.next();
				rsp.addHeader(entry.getKey(), entry.getValue());
			}
			
			if (!this.headers.containsKey(HttpConstants.Headers.CONNECTION)) {
				rsp.addHeader(HttpConstants.Headers.CONNECTION, "close");
			}
		}
		return rsp;
	}
}
