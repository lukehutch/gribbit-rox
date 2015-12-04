package com.flat502.rox.client;

import java.io.IOException;

/**
 * An exception raised when a synchronous request fails.
 * <p>
 * This exception is only raised if a 
 * {@link com.flat502.rox.client.HttpClient#execute(String, Object[]) synchronous}
 * request fails for some reason other than a timeout.
 */
public class RequestFailedException extends IOException {
	public RequestFailedException(Throwable cause) {
		super("RPC call failed");
		this.initCause(cause);
	}
}
