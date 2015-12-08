package com.flat502.rox.http.exception;

import java.net.Socket;

import com.flat502.rox.processing.HttpProcessor;

/**
 * Encapsulates a general processing exception.
 * <p>
 * An exception that wraps up another exception that was raised during processing performed in the central
 * processing loop of an {@link com.flat502.rox.processing.HttpProcessor} instance and that was not directly related
 * to an HTTP message instance.
 * <p>
 * A processing exception may be associated with a {@link java.net.Socket} instance. If so the socket can be
 * retrieved using the {@link #getSocket()} method.
 */
public class ProcessingException extends Exception {
    private Socket socket;
    private HttpProcessor processor;

    public ProcessingException(Throwable cause) {
        super(cause);
    }

    public ProcessingException(HttpProcessor processor, Socket socket, Throwable cause) {
        super(cause);
        this.processor = processor;
        this.socket = socket;
    }

    /**
     * Get the socket the processing exception is associated with.
     * 
     * @return The socket associated with this exception. May be <code>null</code>.
     */
    public Socket getSocket() {
        return this.socket;
    }

    public HttpProcessor getProcessor() {
        return this.processor;
    }
}
