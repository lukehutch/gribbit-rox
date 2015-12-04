package com.flat502.rox.server;

/**
 * Interface for asynchronous RPC method call handlers.
 * @see com.flat502.rox.server.HttpRpcServer#registerHandler(String, String, AsynchronousRequestHandler)
 */
public interface AsynchronousRequestHandler {
	/**
	 * Invoked to handle an HTTP request.
	 * <p>
	 * This method is responsible for processing the request
	 * asynchronously. Responses can be sent using the
	 * {@link ResponseChannel#respond(Response)}
	 * method.
	 * <p>
	 * Although an implement could do all of its work using
	 * the calling thread the intention of this interface is
	 * to support handing off the work to an application thread.
	 * <p>
	 * The caller is one of the underlying worker threads
	 * (see {@link com.flat502.rox.processing.HttpRpcProcessor#addWorker()})
	 * and as such should process the method as quickly as possible.
	 * <p>
	 * If an exception is raised it will be returned to the
	 * caller as a request fault.
	 * @param rspChannel
	 * 	A handle to a logic channel that can be used
	 * 	when a response is ready.
	 * @param context
	 * 	An {@link RequestContext} instance providing information about
	 * 	the call context.
	 * @return
	 *  Returns true if this handler was able to handle the URI in context.getHttpRequest().getURI(), false if not.
	 * @throws Exception
	 * 	Implementations are permitted to raise
	 * 	an exception as part of their processing.
	 */
	boolean handleRequest(RequestContext context, ResponseChannel rspChannel) throws Exception;
}
