package com.flat502.rox.client;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

import org.omg.CORBA.portable.ResponseHandler;

import com.flat502.rox.Version;
import com.flat502.rox.client.exception.ConnectionPoolTimeoutException;
import com.flat502.rox.client.exception.RequestTimeoutException;
import com.flat502.rox.encoding.Encoding;
import com.flat502.rox.http.HttpConstants;
import com.flat502.rox.http.HttpMessageBuffer;
import com.flat502.rox.http.HttpRequest;
import com.flat502.rox.http.HttpResponseBuffer;
import com.flat502.rox.http.exception.HttpMessageException;
import com.flat502.rox.http.exception.ProcessingException;
import com.flat502.rox.log.Log;
import com.flat502.rox.log.LogFactory;
import com.flat502.rox.processing.HttpProcessor;
import com.flat502.rox.processing.ResourcePool;
import com.flat502.rox.processing.SSLConfiguration;

/**
 * This is the client-side RPC interface.
 * <p>
 * This class supports both synchronous and asynchronous method calls. An instance is backed by at least two
 * threads: a selecting thread and a worker thread. Both threads are daemon threads.
 * <p>
 * The selecting thread handles all low-level network I/O. As soon as this thread identifies a complete HTTP
 * response the response is passed to the worker thread (or threads).
 * <p>
 * The number of worker threads may be adjusted dynamically using the inherited {@link HttpProcessor#addWorker()}
 * and {@link HttpProcessor#removeWorker()} methods. Worker threads are responsible for callbacks when the
 * asynchronous API is used.
 */
public class HttpClient extends HttpProcessor {
    private static Log log = LogFactory.getLog(HttpClient.class);

    // A mutex used to protect access to the notificationMap
    // and activity within the connection pool. These two are
    // bound together to prevent a race condition discussed
    // in the body of executeAsync().
    private Object notifierMutex;

    // Maps Sockets to Notifiable implementations
    // so complete responses can be passed back to
    // the caller (synchronously or asynchronously).
    private Map<Socket, Notifiable> notificationMap = new HashMap<>();

    // Maps Sockets to incomplete HttpResponseBuffer instances.
    private Map<Socket, HttpResponseBuffer> responseBuffers = new HashMap<>();

    // Maps Sockets to ByteBuffer instances that are
    // ready to be written out on the socket.
    // Placing a ByteBuffer in here effectively 'queues' it for
    // delivery to the associated SocketChannel when it next
    // becomes available for writing.
    private Map<Socket, ByteBuffer> requestBuffers = new HashMap<>();

    private URL url;

    private ClientResourcePool resourcePool;

    private SharedSocketChannelPool connPool;

    private long requestTimeout;
    private Timer requestTimer;
    private Map<Socket, TimerTask> activeRequestTimers = new HashMap<>();

    private Encoding contentEncoding;
    private boolean acceptEncodedResponses = true;

    /**
     * Initializes a new instance using the given URL.
     * <p>
     * Currently only HTTP is supported as a transport.
     * 
     * @param url
     *            The URL to send method call requests to.
     * @throws IllegalArgumentException
     *             if the protocol specified by the given URL is not supported.
     * @throws IOException
     *             if an error occurs while initializing the underlying {@link Selector}.
     */
    // TODO: Document extra param
    protected HttpClient(URL url, ClientResourcePool pool) throws IOException {
        this(url, pool, null);
    }

    protected HttpClient(URL url, ClientResourcePool pool, SSLConfiguration sslCfg) throws IOException {
        super(url.getProtocol().equals("https"), pool, sslCfg == null ? new SSLConfiguration() : sslCfg);

        if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https")) {
            throw new IllegalArgumentException("Unsupported protocol: " + url.getProtocol());
        }

        this.url = url;
        if (this.resourcePool == null) {
            // If pool is not null then the constructor has called
            // newWorkerPool(), initializing this.resourcePool.
            this.resourcePool = pool;
            long poolRequestTimeout = pool.getRequestTimeout();
            if (poolRequestTimeout != 0) {
                this.setRequestTimeout(poolRequestTimeout);
            }
        }
        this.notifierMutex = this.resourcePool.getNotifierMutex();

        // This initializes the Selector which we need so the pool can look up
        // SelectionKeys when it shuts connections down (see getConnection()).
        this.initialize();
    }

    /**
     * Configure a limit on the number of active connections provided at any given time by the underlying connection
     * pool.
     * <p>
     * If a non-zero limit is any thread requesting a new connection that would cause this limit to be exceeded will
     * be blocked until an existing connection is returned or until a timeout occurs (if a timeout
     * {@link #setConnectionPoolTimeout(long) has been set}).
     * <p>
     * Care should be taken when using the asynchronous execution API without a limit on the connection pool. It's
     * very easy to consume all available local connections like this.
     * <p>
     * If a non-zero limit is placed on the connection pool then you must be sure to set a
     * {@link #setConnectionPoolTimeout(long) timeout} on the pool. Failure to do so will result in threads blocking
     * indefinitely when the connection pool is exhausted.
     * 
     * @param limit
     *            The maximum number of active connections allowed at any given moment. A value of 0 indicates no
     *            limit should be enforced (this is the default value).
     * @throws IllegalArgumentException
     *             If the timeout provided is negative.
     * @throws IllegalStateException
     *             If any of the <code>execute</code> methods were invoked before this method was invoked.
     */
    public void setConnectionPoolLimit(int limit) {
        if (this.isSharedWorkerPool()) {
            throw new IllegalStateException(
                    "Connection pooling attributes should be set on the shared resource pool");
        }
        this.resourcePool.setConnectionPoolLimit(limit);
    }

    /**
     * Configure a timeout value for the underlying connection pool.
     * <p>
     * This timeout only applies if a limit has been set on the number of active connections (using
     * {@link #setConnectionPoolLimit(int)}.
     * <p>
     * If a limit is configured this timeout controls how long a thread will be blocked waiting for a new connection
     * before a {@link ConnectionPoolTimeoutException} is raised.
     * 
     * @param timeout
     *            The timeout (in milliseconds). A value of 0 indicates no timeout should be enforced (this is the
     *            default value).
     * @throws IllegalArgumentException
     *             If the timeout provided is negative.
     * @throws IllegalStateException
     *             If any of the <code>execute</code> methods were invoked before this method was invoked.
     */
    public void setConnectionPoolTimeout(long timeout) {
        if (this.isSharedWorkerPool()) {
            throw new IllegalStateException(
                    "Connection pooling attributes should be set on the shared resource pool");
        }
        this.resourcePool.setConnectionPoolTimeout(timeout);
    }

    /**
     * Configure a timeout value for requests.
     * <p>
     * The new timeout affects only calls executed subsequent to the completion of this method call. Time spent
     * waiting for a connection from the connection pool is not included in this timeout (see
     * {@link #setConnectionPoolTimeout(long)}.
     * <p>
     * If a method call times out as a result of the timeout configured here an instance of
     * {@link RequestTimeoutException} will be raised in the case of synchronous requests or, in the case of
     * asynchronous requests, passed to the appropriate {@link ResponseHandler#handleException(Request, Throwable)
     * response handler}.
     * <p>
     * The connection on which the request that timed out was sent will be closed.
     * 
     * @param timeout
     *            The timeout (in milliseconds). A value of 0 indicates no timeout should be enforced.
     * @throws IllegalArgumentException
     *             If the timeout provided is negative.
     */
    public void setRequestTimeout(long timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout is negative");
        }

        this.requestTimer = this.getTimer();
        this.requestTimeout = timeout;
    }

    /**
     * Set the content encoding to use for
     * 
     * @param encoding
     */
    public void setContentEncoding(Encoding encoding) {
        this.contentEncoding = encoding;
    }

    public void setAcceptEncodedResponses(boolean accept) {
        this.acceptEncodedResponses = accept;
    }

    void handleResponse(HttpResponseBuffer response) {
        Notifiable notifier = this.removeNotifier(response.getSocket());
        if (notifier == null) {
            throw new IllegalStateException("Missing notifier for complete response");
        }
        // TODO: Would it be more efficient (but still correct) to reverse the order of these two?
        try {
            notifier.notify(response, this.newResponseContext(response.getSocket(), response));
        } catch (Throwable e) {
            log.error("Error notifying for HTTP response", e);
        } finally {
            this.finishHttpResponse(response);
        }
    }

    void handleException(HttpResponseBuffer response, Throwable exception) {
        Notifiable notifier = this.removeNotifier(response.getSocket());
        if (notifier == null) {
            throw (IllegalStateException) new IllegalStateException("Missing notifier for processing exception")
                    .initCause(exception);
        }
        notifier.notify(exception, this.newResponseContext(response.getSocket(), response));
        this.finishHttpResponse(response);
    }

    void handleException(ProcessingException e) {
        Socket socket = e.getSocket();
        if (socket != null) {
            // Look up the associated notifier and let it know
            // about this exception.
            Notifiable notifier = this.removeNotifier(e.getSocket());
            if (notifier == null) {
                throw (IllegalStateException) new IllegalStateException("Missing notifier for processing exception")
                        .initCause(e);
            }
            notifier.notify(e.getCause(), this.newResponseContext(socket, null));
        } else {
            // We have two options: tell everyone or tell no-one.
            // The former isn't particularly useful. Let them eat cake!
            synchronized (this.notifierMutex) {
                Iterator<Notifiable> notifiers = this.notificationMap.values().iterator();
                while (notifiers.hasNext()) {
                    Notifiable notifier = notifiers.next();
                    notifier.notify(e.getCause(), this.newResponseContext(null, null));
                }
            }
        }
    }

    /**
     * This method exists to provide classes in this package with access to
     * {@link HttpProcessor#queueRegistration(SocketChannel)} without having to make it public.
     */
    void register(SocketChannel channel) {
        this.registerChannel(channel);
        this.queueRegistration(channel);
    }

    // Overridden only so it's accessible in this package
    @Override
    protected void deregisterChannel(SelectableChannel channel) {
        super.deregisterChannel(channel);
    }

    // Overridden only so it's accessible in this package
    @Override
    protected void registerChannel(SelectableChannel channel) {
        super.registerChannel(channel);
    }

    /**
     * This method exists to provide classes in this package with access to
     * {@link HttpProcessor#queueCancellation(SocketChannel)} without having to make it public.
     */
    void cancel(SocketChannel channel) {
        this.queueCancellation(channel);
        this.deregisterChannel(channel);
    }

    URL getURL() {
        return this.url;
    }

    private void finishHttpResponse(HttpResponseBuffer response) {
        if (log.logTrace()) {
            log.trace("finishHttpResponse(): " + response.getHttpVersionString() + ": close? "
                    + response.mustCloseConnection());
        }
        if (response.mustCloseConnection()) {
            // Shut the channel down
            this.connPool.removeChannel(this, response.getSocket().getChannel());
            if (log.logTrace()) {
                log.trace("Deregistering socket for " + response.getSocket());
            }
            this.deregisterSocket(response.getSocket());
        } else {
            // Return this connection to the underlying connection pool.
            this.connPool.returnChannel(this, response.getSocket().getChannel());
        }
    }

    public HttpResponseBuffer executeSync(Request request) throws Exception {
        HttpRequest httpReq = this.newHttpRequest(request);

        Socket socket;
        SynchronousNotifier notifier;
        // We synchronize here to avoid the following race condition:
        //      1. A socket is created (and a connection attempt started)
        //      2. The connection attempt fails in connect()
        //      3. We attempt to find the notifier to tell it about the failure
        //      4. The notifier here is put into the map.
        //  If the above order of events occurs we can't find the
        // notifier to tell the caller about the connection failure.
        // By synchronizing on notifiedMutex we ensure that the
        // notifier is registered before connect() can try to call it
        // back.
        synchronized (this.notifierMutex) {
            socket = this.getConnection();

            notifier = new SynchronousNotifier();
            this.registerNotifier(socket, notifier);
        }

        // Queue the request to be sent when the connection
        // completes.
        this.queueWrite(socket, httpReq.marshal(), false);

        // Block until a response is available.
        HttpResponseBuffer response = notifier.waitForResponse();
        this.validateHttpResponse(request, response);

        return response;
    }

    public void executeAsync(Request request, AsynchronousResponseHandler handler) throws Exception {
        HttpRequest httpReq = this.newHttpRequest(request);

        Socket socket;
        // We synchronize here to avoid the following race condition:
        //      1. A socket is created (and a connection attempt started)
        //      2. The connection attempt fails in connect()
        //      3. We attempt to find the notifier to tell it about the failure
        //      4. The notifier here is put into the map.
        //  If the above order of events occurs we can't find the
        // notifier to tell the caller about the connection failure.
        // By synchronizing on notifiedMutex we ensure that the
        // notifier is registered before connect() can try to call it
        // back.
        synchronized (this.notifierMutex) {
            socket = this.getConnection();

            this.registerNotifier(socket, new AsynchronousNotifier(this, handler, request));
        }

        // Queue the request to be sent when the connection
        // completes.
        this.queueWrite(socket, httpReq.marshal(), false);
    }

    /**
     * Validates an HTTP response message, optionally converting it into a local exception.
     * <p>
     * This method centralizes (and exposes for replacement by sub-classes) the logic to validate an HTTP response
     * method.
     * <p>
     * Typically this means converting selected status codes into local exceptions.
     * <p>
     * This implementation maps all status codes other than <code>200</code> onto an exception as follows:
     * <ul>
     * <li><code>404</code> results in a {@link NoSuchMethodException} being raised.</li>
     * <li>All other status codes result in an {@link UnsupportedOperationException} being raised.</li>
     * </ul>
     * 
     * @param request
     *            The request the response applies to.
     * @param response
     *            The buffer containing the HTTP response.
     * @throws Exception
     *             If the status code in the HTTP response is not <code>200</code> an exception is raised as
     *             described above.
     */
    protected void validateHttpResponse(Request request, HttpResponseBuffer response) throws Exception {
        switch (response.getStatusCode()) {
        case HttpConstants.StatusCodes._200_OK:
            break;
        case HttpConstants.StatusCodes._404_NOT_FOUND:
            throw new NoSuchMethodException(response.getStatusCode() + ": " + response.getReasonPhrase() + ": "
                    + this.url);
        default:
            throw new UnsupportedOperationException(response.getStatusCode() + ": " + response.getReasonPhrase());
        }
    }

    private Socket getConnection() throws IOException {
        this.connPool = this.resourcePool.getSocketChannelPool();
        SocketChannel socketChannel = this.connPool.getChannel(this);
        Socket socket = socketChannel.socket();

        // Ensure there's a message buffer for this socket in case
        // the connection attempt fails (we need to recover the socket
        // in that case so we can hand it back to the connection pool).
        @SuppressWarnings("unused")
        HttpMessageBuffer httpMsg = this.getReadBuffer(socket);
        return socket;
    }

    private void registerNotifier(Socket socket, Notifiable notifier) {
        // We already own the mutex for this.
        this.notificationMap.put(socket, notifier);
        this.startRequestTimer(socket);
    }

    private void startRequestTimer(Socket socket) {
        // Store this in a local so we don't have to worry about
        // the value changing underneath us.
        long timeout = this.requestTimeout;

        if (timeout > 0) {
            // Set up a timer for this request
            TimerTask task = new NotifiableTimerTask(socket);
            this.activeRequestTimers.put(socket, task);
            this.requestTimer.schedule(task, timeout);
        }
    }

    private void stopRequestTimer(Socket socket) {
        TimerTask task = this.activeRequestTimers.remove(socket);
        if (task != null) {
            task.cancel();
        }
    }

    Notifiable removeNotifier(Socket socket) {
        this.stopRequestTimer(socket);
        synchronized (this.notifierMutex) {
            return this.notificationMap.remove(socket);
        }
    }

    private class NotifiableTimerTask extends TimerTask {
        private Socket socket;

        NotifiableTimerTask(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                handleTimeout(socket, null);
            } catch (Exception e) {
                log.warn("NotifiableTimerTask caught an exception", e);
            }
        }
    }

    @Override
    protected void handleTimeout(Socket socket, Exception cause) {
        // This might return null if a response has just been handled
        // but the timeout logic in Timer had already been set in motion.
        Notifiable notifier = HttpClient.this.removeNotifier(socket);

        // Rather safe than sorry. Close the connection and remove it from
        // the connection pool
        this.connPool.removeChannel(HttpClient.this, socket.getChannel());

        // And notify the client
        if (notifier != null) {
            notifier.notifyTimedOut(cause, this.newResponseContext(socket, null));
        }
    }

    /**
     * This implementation defers to the parent implementation before marking this thread as a daemon thread and
     * starting the thread.
     * 
     * @throws IOException
     *             if an error occurs during initialization.
     */
    @Override
    protected void initialize() throws IOException {
        super.initialize();
        this.start();
    }

    @Override
    protected void stopImpl() throws IOException {
        this.resourcePool.detach(this);
    }

    @Override
    protected SSLEngine initSocketSSLEngine(Socket socket) throws SSLException {
        SSLEngine engine = super.initSocketSSLEngine(socket);
        engine.setUseClientMode(true);
        return engine;
    }

    @Override
    protected ResourcePool newWorkerPool() {
        return this.resourcePool = new ClientResourcePool();
    }

    /**
     * This implementation defers to the {@link #connect(SelectionKey)} method if a connection is ready for
     * completion. In all other cases it defers to it's parent.
     * 
     * @param key
     *            The {@link SelectionKey} for the socket on which an I/O operation is pending.
     * @throws IOException
     *             if an error occurs while processing the pending event.
     */
    @Override
    protected void handleSelectionKeyOperation(SelectionKey key) throws IOException {
        if (key.isValid() && key.isConnectable()) {
            this.connect(key);
        } else {
            super.handleSelectionKeyOperation(key);
        }
    }

    /**
     * Called when a requested connect operation on a {@link SocketChannel} is eligible for completion.
     * 
     * @param key
     *            The {@link SelectionKey} for the socket on which a connection is pending.
     * @throws IOException
     *             if an error occurs while completing the connection.
     */
    protected void connect(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        Socket socket = socketChannel.socket();

        if (log.logTrace()) {
            log.trace("Finishing connection on socket " + System.identityHashCode(socket) + ": wq="
                    + isWriteQueued(socket));
        }

        // Finish the connection. If the connection operation failed
        // this will raise an IOException.
        try {
            socketChannel.finishConnect();
        } catch (IOException e) {
            HttpResponseBuffer httpMsg = (HttpResponseBuffer) this.getReadBuffer(socket);
            this.handleException(httpMsg, e);
            return;
        }

        // Register the new socket. This will promote it to an SSLSocket
        // if we're configured for HTTPS.
        this.registerSocket(socket, this.url.getHost(), this.url.getPort(), true);

        // Update the interest operations to indicate we can accept
        // data. We can do this directly because the caller is the
        // selecting thread.
        if (this.isWriteQueued(socketChannel.socket())) {
            key.interestOps(SelectionKey.OP_WRITE);
        } else {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    /**
     * Creates a new instance of {@link HttpMessageException} and enqueues it on the queue shared by worker threads.
     * 
     * @param msg
     *            The HTTP response being processed when the exception occurred.
     * @param e
     *            The exception that was raised.
     */
    @Override
    protected void handleMessageException(HttpMessageBuffer msg, Exception e) {
        this.getQueue().add(new HttpMessageException(msg, e));
    }

    @Override
    protected void handleProcessingException(Socket socket, Exception e) {
        this.getQueue().add(new ProcessingException(this, socket, e));
    }

    @Override
    protected void handleSSLHandshakeFinished(Socket socket, SSLEngine engine) {
        this.queueWrite(socket);
    }

    @Override
    protected HttpMessageBuffer getReadBuffer(Socket socket) {
        synchronized (this.responseBuffers) {
            HttpResponseBuffer response = this.responseBuffers.get(socket);
            if (response == null) {
                Encoding acceptableEncoding = null;
                if (this.acceptEncodedResponses) {
                    acceptableEncoding = this.contentEncoding;
                }
                response = new HttpResponseBuffer(this, socket, acceptableEncoding);
                this.responseBuffers.put(socket, response);
            }
            return response;
        }
    }

    @Override
    protected void removeReadBuffer(Socket socket) {
        synchronized (this.responseBuffers) {
            this.responseBuffers.remove(socket);
        }
    }

    @Override
    protected void removeReadBuffers(Socket socket) {
        this.removeReadBuffer(socket);
    }

    @Override
    protected void putWriteBuffer(Socket socket, ByteBuffer data) {
        synchronized (this.requestBuffers) {
            ByteBuffer existing = this.requestBuffers.get(socket);
            if (existing != null) {
                existing.put(data);
            } else {
                this.requestBuffers.put(socket, data);
            }
        }
    }

    @Override
    protected boolean isWriteQueued(Socket socket) {
        synchronized (this.requestBuffers) {
            return this.requestBuffers.containsKey(socket);
        }
    }

    @Override
    protected ByteBuffer getWriteBuffer(Socket socket) {
        synchronized (this.requestBuffers) {
            return this.requestBuffers.get(socket);
        }
    }

    @Override
    protected void removeWriteBuffer(Socket socket) {
        synchronized (this.requestBuffers) {
            this.requestBuffers.remove(socket);
        }
    }

    @Override
    protected void removeWriteBuffers(Socket socket) {
        this.removeWriteBuffer(socket);
    }

    protected HttpRequest newHttpRequest(Request request) throws Exception {
        HttpRequest httpReq = new HttpRequest(request.getHttpMethod(), request.getHttpURI(this.url),
                this.contentEncoding);

        String ct = request.getContentType();
        if (ct != null) {
            httpReq.addHeader(HttpConstants.Headers.CONTENT_TYPE, ct);
        }
        String host = this.url.getHost();
        if (this.url.getPort() != -1) {
            host += ":" + this.url.getPort();
        }
        httpReq.addHeader(HttpConstants.Headers.HOST, host);

        String ua = this.getUserAgent();
        if (ua != null) {
            httpReq.addHeader(HttpConstants.Headers.USER_AGENT, ua);
        }
        httpReq.setContent(request.getContent());
        return httpReq;
    }

    protected String getUserAgent() {
        return Version.getDescription();
    }

    protected ResponseContext newResponseContext(Socket socket, HttpResponseBuffer rsp) {
        SocketChannel channel = socket == null ? null : socket.getChannel();
        return new ResponseContext(channel, this.newSSLSession(socket), rsp);
    }
}
