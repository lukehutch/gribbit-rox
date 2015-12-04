package com.flat502.rox.client;

import com.flat502.rox.http.HttpResponseBuffer;
import com.flat502.rox.log.Log;
import com.flat502.rox.log.LogFactory;

class AsynchronousNotifier implements Notifiable {
	private static Log log = LogFactory.getLog(AsynchronousNotifier.class);

	private HttpClient client;
	private AsynchronousResponseHandler handler;
	private Request request;

	public AsynchronousNotifier(HttpClient client, AsynchronousResponseHandler handler, Request request) {
		this.client = client;
		this.handler = handler;
		this.request = request;
	}

	@Override
    public void notify(HttpResponseBuffer response, ResponseContext context) {
		try {
			this.client.validateHttpResponse(this.request, response);
            this.handler.handleResponse(this.request, response, context);
		} catch (Exception e) {
            notify(e, context);
		}
	}

	@Override
    public void notify(Throwable e, ResponseContext context) {
		try {
			this.handler.handleException(this.request, e, context);
		} catch (Throwable t) {
			log.error("handler.handleException() threw error", t);
		}
	}

	@Override
    public void notifyTimedOut(Throwable cause, ResponseContext context) {
		this.notify(new RequestTimeoutException(cause), context);
	}
}
