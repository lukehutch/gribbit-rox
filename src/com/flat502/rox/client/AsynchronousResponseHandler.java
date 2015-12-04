package com.flat502.rox.client;

import com.flat502.rox.http.HttpResponseBuffer;


/**
 * This interface represents an HTTP response handler.
 * <p>
 * Callers that want to be notified asynchronously when
 * an HTTP request returns should pass an implementation
 * of this interface to the 
 * {@link com.flat502.rox.client.HttpClient#execute(String, Object[], Class, AsynchronousResponseHandler)}
 * or
 * {@link com.flat502.rox.client.HttpClient#execute(String, Object[], AsynchronousResponseHandler)}
 * methods.
 */
public interface AsynchronousResponseHandler {
	/**
	 * This method is called when a successful response is received from the
	 * server.
	 * <P>
	 * A successful response is defined as one that contains a return value
	 * that is not an RPC fault.
	 * @param call
	 * 	The original request that this response applies to.
	 * @param response
	 * 	The response to the request.
	 */
	public void handleResponse(Request request, HttpResponseBuffer response, ResponseContext context);
	
	/**
	 * This method is called for both local and remote exceptions.
	 * <p>
	 * A local exception is one that is raised within this JVM as a result 
	 * of a failure while handling either the request or response. This may
	 * be an instance of any sub-class of {@link Throwable} <i>other</i>
	 * than {@link com.flat502.rox.processing.RequestFaultException}.
	 * <p>
	 * A remote exception is always an instance of 
	 * {@link com.flat502.rox.processing.RequestFaultException}
	 * wrapping an RPC fault response.
	 * @param request
	 * 	The original request that this response applies to.
	 * @param e
	 * 	An instance of {@link com.flat502.rox.processing.RequestFaultException} 
	 * 	for a remote exception. For a local exception any other exception type.
	 */
	public void handleException(Request request, Throwable e, ResponseContext context);
}
