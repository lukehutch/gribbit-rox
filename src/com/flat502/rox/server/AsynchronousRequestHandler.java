package com.flat502.rox.server;

import com.flat502.rox.server.response.Response;


/**
 * Interface for asynchronous RPC method call handlers.
 * @see com.flat502.rox.server.HttpRpcServer#registerHandler(String, String, AsynchronousRequestHandler)
 */
public interface AsynchronousRequestHandler {
	/**
	 * Invoked to handle an HTTP request.
	 * <p>
	 * This method is responsible for processing an asynchronous request.
	 * The caller will call this method on an application thread, so
	 * implementing classes may block before returning.
	 * <p>
	 * If an exception is raised it will be returned to the
	 * caller as a request fault.
	 * @param context
	 * 	An {@link RequestContext} instance providing information about
	 * 	the call context.
	 * @return
	 *  Returns a Response object if this handler was able to handle the URI in context.getHttpRequest().getURI(),
	 *  otherwise returns null.
	 * @throws Exception
	 * 	Implementations are permitted to raise
	 * 	an exception as part of their processing.
	 */
	Response handleRequest(RequestContext context) throws Exception;
}
