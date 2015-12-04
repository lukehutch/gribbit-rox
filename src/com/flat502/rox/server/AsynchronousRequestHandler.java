package com.flat502.rox.server;

/**
 * Interface for asynchronous RPC method call handlers.
 * @see com.flat502.rox.server.HttpRpcServer#registerHandler(String, String, AsynchronousRequestHandler)
 */
public interface AsynchronousRequestHandler {
    /**
     * Returns true if this request handler can handle the given URI. 
     */
    boolean canHandleURI(String uri);
    
	/**
	 * Invoked to handle a method call.
	 * <p>
	 * This method is responsible for processing the method
	 * asynchronously. Responses can be sent using the
	 * {@link ResponseChannel#respond(RpcResponse)}
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
	 * @throws Exception
	 * 	Implementations are permitted to raise
	 * 	an exception as part of their processing.
	 */
	void handleRequest(RequestContext context, ResponseChannel rspChannel) throws Exception;
}
