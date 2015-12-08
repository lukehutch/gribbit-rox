package com.flat502.rox.client;

import java.util.concurrent.BlockingQueue;

import com.flat502.rox.http.HttpMessageBuffer;
import com.flat502.rox.http.HttpResponseBuffer;
import com.flat502.rox.http.exception.ProcessingException;
import com.flat502.rox.processing.HttpMessageHandler;

class HttpResponseHandler extends HttpMessageHandler {
	// private ByteBuffer readBuf = ByteBuffer.allocate(1024);

	public HttpResponseHandler(BlockingQueue<?> queue) {
		super(queue);
	}

	@Override
    protected void handleMessage(HttpMessageBuffer msg) throws Exception {
		HttpResponseBuffer response = getResponse(msg);
		if (!response.isComplete()) {
			throw new IllegalStateException("Incomplete request on my queue");
		}

		this.getClient(response).handleResponse(response);
	}

	@Override
    protected void handleHttpMessageException(HttpMessageBuffer msg, Throwable exception) {
		HttpResponseBuffer response = getResponse(msg);
		this.getClient(response).handleException((HttpResponseBuffer) msg, exception);
	}
	
	@Override
    protected void handleProcessingException(ProcessingException exception) {
		((HttpClient)exception.getProcessor()).handleException(exception);
	}
	
	private HttpResponseBuffer getResponse(HttpMessageBuffer msg) {
		if (!(msg instanceof HttpResponseBuffer)) {
			throw new IllegalArgumentException("Expected instance of " + HttpResponseBuffer.class.getName() + ", got "
					+ msg.getClass().getName());
		}
		return (HttpResponseBuffer) msg;
	}
	
	private HttpClient getClient(HttpResponseBuffer response) {
		if (!(response.getOrigin() instanceof HttpClient)) {
			throw new IllegalArgumentException("Expected instance of " + HttpClient.class.getName() + ", got "
					+ response.getOrigin().getClass().getName());
		}
		return (HttpClient) response.getOrigin();
	}
}