package com.flat502.rox.client;

import java.net.URL;

/**
 * An interface representing a generalized RPC method
 * call.
 * <p>
 * This interface is patterned after XML-RPC and essentially
 * encapsulates a method name and a list of parameters.
 */
public interface Request {

    // TODO: use an enum for this
	public String getHttpMethod();
	
	/**
	 * Map the destination URL onto a URI for use in an HTTP request.
	 * <p>
	 * This method provides implementations with an opportunity
	 * to generate an appropriate URI (possibly tranformed) before
	 * the underlying HTTP request is contructed and sent.
	 * @param baseUrl 
	 * 	The URL the client making the call was	directed at.
	 * @return
	 * 	A string value for use as the URI in an HTTP request that
	 * 	will represent this call.
	 */
	public String getHttpURI(URL baseUrl);
	
    /**
     * Get the content of the request.
     */
    public byte[] getContent();
    
    /**
     * Called to get the value for the <code>Content-Type</code>
     * HTTP header.
     * <p>
     * This is used when constructing requests and responses, and
     * when validating requests and responses.
     * @return
     *  An HTTP <code>Content-Type</code> string description
     *  without the <code>charset</code> attribute.
     */
    public String getContentType();
}
