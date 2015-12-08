package com.flat502.rox.client.exception;

/**
 * An exception raised when a request times out.
 * <p>
 * This exception is only raised if the execution time of a
 * {@link com.flat502.rox.client.HttpClient#execute(String, Object[]) synchronous} request exceeds the timeout set
 * by calling {@link com.flat502.rox.client.HttpClient#setRequestTimeout(long)}.
 */
public class RequestTimeoutException extends IOTimeoutException {
    public RequestTimeoutException() {
        super("RPC call timed out");
    }

    public RequestTimeoutException(Throwable cause) {
        super("RPC call timed out");
        if (cause != null) {
            this.initCause(cause);
        }
    }
}
