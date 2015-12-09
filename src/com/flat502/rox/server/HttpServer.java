package com.flat502.rox.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

import com.flat502.rox.Version;
import com.flat502.rox.encoding.Encoding;
import com.flat502.rox.http.HttpConstants;
import com.flat502.rox.http.HttpMessageBuffer;
import com.flat502.rox.http.HttpRequestBuffer;
import com.flat502.rox.http.HttpResponse;
import com.flat502.rox.http.exception.HttpResponseException;
import com.flat502.rox.log.Log;
import com.flat502.rox.log.LogFactory;
import com.flat502.rox.processing.HttpProcessor;
import com.flat502.rox.processing.RemoteSocketClosedException;
import com.flat502.rox.processing.ResourcePool;
import com.flat502.rox.processing.SSLConfiguration;
import com.flat502.rox.server.response.Response;
import com.flat502.rox.utils.Utils;

/**
 * This is the server-side RPC interface.
 * <p>
 * This class supports both synchronous and asynchronous handling of method calls. An instance is backed by at least
 * two threads: a selecting thread and a worker thread. Both threads are daemon threads.
 * <p>
 * The selecting thread handles all low-level network I/O. As soon as this thread identifies a complete HTTP request
 * the request is passed to the worker thread (or threads).
 * <p>
 * The number of worker threads may be adjusted dynamically using the inherited {@link HttpProcessor#addWorker()}
 * and {@link HttpProcessor#removeWorker()} methods. Worker threads are responsible for notification of registered
 * {@link com.flat502.rox.server.RequestHandler}s.
 */
public class HttpServer extends HttpProcessor {
    private static Log log = LogFactory.getLog(HttpServer.class);

    // Asynchronous request handlers that test for matching request URIs.
    private ConcurrentLinkedDeque<AsynchronousRequestHandler> uriHandlers = new ConcurrentLinkedDeque<>();

    // The address (and name) and port we bind on.
    // We store the host along with the host so we
    // don't have to continually check for a null address.
    private InetAddress hostAddress;
    private String host;
    private int port;

    private ServerSocketChannel serverChannel;

    // The value used for the HTTP Header field.
    // This is stored separately so we don't "forget"
    // that the user initialized us with a null host.
    private String headerHostValue;

    // Maps Sockets to incomplete HttpRequestBuffer instances.
    private Map<Socket, HttpRequestBuffer> requestBuffers = new HashMap<>();

    // Maps Sockets to ByteBuffer instances that are
    // ready to be written out on the socket.
    // Placing a ByteBuffer in here effectively 'queues' it for
    // delivery to the associated SocketChannel when it next
    // becomes available for writing.
    private Map<Socket, List<ByteBuffer>> responseBuffers = new HashMap<>();

    private AcceptPolicy acceptPolicy;

    private ServerEncodingMap contentEncodingMap = new ServerEncodingMap();
    private boolean encodeResponses;

    private int idleClientTimeout;
    private Timer idleClientTimer;

    // Maps Sockets to Timer instances that are reset whenever there's
    // activity on the socket. Used to enforce idle client timeouts.
    private Map<Socket, TimerTask> socketActivity = new HashMap<>();

    // Maps Sockets to an object responsible for coordinating responses
    // so we handle pipelined requests correctly.
    private Map<Socket, ResponseCoordinator> socketResponseCoordinators = new HashMap<>();

    // Thread pool for handling asynchronous requests. Requests are passed off to the application pool
    // from threads in the worker pool.
    private ExecutorService applicationThreadPool = Executors.newCachedThreadPool();

    /**
     * Initialize an instance listening for connections on all local addresses. HTTPS (SSL) is disabled.
     * <p>
     * The server will not attempt to bind to a local port or being accepting connections (and by implication
     * processing requests) until {@link Thread#start()} is invoked on this instance.
     * 
     * @param port
     *            The port to listen on.
     * @throws IOException
     *             if an error occurs initializing the underlying server socket.
     */
    public HttpServer(int port) throws IOException {
        this(null, port, false, null, null);
    }

    /**
     * Initialize an instance listening for connections on a specified local address. HTTPS (SSL) is disabled.
     * <p>
     * The server will not attempt to bind to a local port or being accepting connections (and by implication
     * processing requests) until {@link Thread#start()} is invoked on this instance.
     * 
     * @param hostAddress
     *            The address to listen on. If this is <code>null</code> this instance will listen on all local
     *            addresses.
     * @param port
     *            The port to listen on.
     * @throws IOException
     *             if an error occurs initializing the underlying server socket.
     */
    public HttpServer(InetAddress hostAddress, int port) throws IOException {
        this(hostAddress, port, false, null, null);
    }

    /**
     * Initialize an instance listening for connections on a specified local address.
     * <p>
     * The server will not attempt to bind to a local port or being accepting connections (and by implication
     * processing requests) until {@link Thread#start()} is invoked on this instance.
     * 
     * @param hostAddress
     *            The address to listen on. If this is <code>null</code> this instance will listen on all local
     *            addresses.
     * @param port
     *            The port to listen on.
     * @param useHttps
     *            Whether to use HTTPS. If true, looks up the keystore and truststore in the system properties
     *            javax.net.ssl.keyStore, javax.net.ssl.keyStorePassword, javax.net.ssl.trustStore and
     *            javax.net.ssl.trustStorePassword.
     * @throws IOException
     *             if an error occurs initializing the underlying server socket.
     * @throws GeneralSecurityException
     *             if an error occurs while loading the keys or certs from the keystore or the trust store.
     */
    public HttpServer(InetAddress hostAddress, int port, boolean useHttps) throws IOException,
            GeneralSecurityException {
        this(hostAddress, port, useHttps, null, null);
    }

    /**
     * Initialize an instance listening for connections on a specified local address.
     * <p>
     * The server will not attempt to bind to a local port or being accepting connections (and by implication
     * processing requests) until {@link Thread#start()} is invoked on this instance.
     * 
     * @param hostAddress
     *            The address to listen on. If this is <code>null</code> this instance will listen on all local
     *            addresses.
     * @param port
     *            The port to listen on.
     * @param useHttps
     *            Whether to use HTTPS. If true, looks up the keystore and truststore in the system properties
     *            javax.net.ssl.keyStore, javax.net.ssl.keyStorePassword, javax.net.ssl.trustStore and
     *            javax.net.ssl.trustStorePassword.
     * @throws IOException
     *             if an error occurs initializing the underlying server socket.
     * @throws GeneralSecurityException
     *             if an error occurs while loading the keys or certs from the keystore or the trust store.
     */
    public HttpServer(InetAddress hostAddress, int port, SSLConfiguration sslConfig) throws IOException,
            GeneralSecurityException {
        this(hostAddress, port, (sslConfig != null), sslConfig, null);
    }

    /**
     * Initialize a new HTTP RPC server.
     * <p>
     * The server will not attempt to bind to a local port or being accepting connections (and by implication
     * processing requests) until {@link Thread#start()} is invoked on this instance.
     * 
     * @param hostAddress
     *            An {@link InetAddress} this instance should bind to when listening for connections.
     *            <code>null</code> is interpreted as "listen on all interfaces".
     * @param port
     *            The port to listen on.
     * @param useHttps
     *            Whether to use HTTPS. If true, looks up the keystore and truststore in the system properties
     *            javax.net.ssl.keyStore, javax.net.ssl.keyStorePassword, javax.net.ssl.trustStore and
     *            javax.net.ssl.trustStorePassword.
     * @param workerPool
     *            The worker pool to use, or null to use hte default pool.
     * @throws IOException
     *             if an error occurs initializing the underlying server socket.
     * @throws GeneralSecurityException
     *             if an error occurs while loading the keys or certs from the keystore or the trust store.
     */
    public HttpServer(InetAddress hostAddress, int port, boolean useHttps, ServerResourcePool workerPool)
            throws IOException, GeneralSecurityException {
        this(hostAddress, port, useHttps, null, workerPool);
    }

    /**
     * Initialize a new HTTP RPC server.
     * <p>
     * The server will not attempt to bind to a local port or being accepting connections (and by implication
     * processing requests) until {@link Thread#start()} is invoked on this instance.
     * 
     * @param hostAddress
     *            An {@link InetAddress} this instance should bind to when listening for connections.
     *            <code>null</code> is interpreted as "listen on all interfaces".
     * @param port
     *            The port to listen on.
     * @param useHttps
     *            Whether to use HTTPS. If true, looks up the keystore and truststore in the system properties
     *            javax.net.ssl.keyStore, javax.net.ssl.keyStorePassword, javax.net.ssl.trustStore and
     *            javax.net.ssl.trustStorePassword, unless a non-null value is given for sslConfig.
     * @param sslConfig
     *            The SSLConfiguration to use, or null to use the system properties specified under useHttps, if
     *            useHttps is true.
     * @param workerPool
     *            The worker pool to use, or null to use hte default pool.
     * @throws IOException
     *             if an error occurs initializing the underlying server socket.
     * @throws GeneralSecurityException
     *             if an error occurs while loading the keys or certs from the keystore or the trust store.
     */
    private HttpServer(InetAddress hostAddress, int port, boolean useHttps, SSLConfiguration sslConfig,
            ServerResourcePool workerPool) throws IOException {
        super(useHttps, sslConfig, workerPool);

        this.hostAddress = hostAddress;
        if (hostAddress != null) {
            this.host = hostAddress.getHostName();
            this.headerHostValue = this.host;
        } else {
            this.headerHostValue = InetAddress.getLocalHost().getCanonicalHostName();
        }
        this.port = port;

        this.initialize();
    }

    public synchronized void registerAcceptPolicy(AcceptPolicy policy) {
        if (this.isStarted()) {
            throw new IllegalStateException("Can't modify policy: server has been started");
        }
        this.acceptPolicy = policy;
    }

    public void registerContentEncoding(Encoding encoding) {
        this.contentEncodingMap.addEncoding(encoding);
    }

    public void setEncodeResponses(boolean encode) {
        this.encodeResponses = encode;
    }

    // TODO: Document
    public void setIdleClientTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout is negative");
        }

        this.idleClientTimer = this.getTimer();
        this.idleClientTimeout = timeout;
    }

    public void registerHandler(AsynchronousRequestHandler handler) {
        this.uriHandlers.add(handler);
    }

    /**
     * Routes an HTTP request to the appropriate handler.
     */
    void routeRequest(final Socket socket, final HttpRequestBuffer request) throws Exception {
        SocketChannel channel = socket == null ? null : socket.getChannel();
        final RequestContext context = new RequestContext(channel, this.newSSLSession(socket), request);
        final SocketResponseChannel rspChannel = this.newSocketResponseChannel(socket, request);

        // Pass request to the application thread pool so it can be handled asynchronously
        applicationThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                String uri = request.getURI();
                if (log.logDebug()) {
                    log.debug("Look up handler for URI [" + uri + "] and method [" + request.getMethod() + "]");
                }
                try {
                    for (AsynchronousRequestHandler handler : uriHandlers) {
                        try {
                            Response response = handler.handleRequest(context);
                            if (response != null) {
                                rspChannel.respond(response);
                                // Return after first successful handler response
                                return;
                            }
                        } catch (HttpResponseException e) {
                            rspChannel.respond(e);
                        } catch (Exception e) {
                            // TODO: improve error handling
                            if (log.logDebug()) {
                                log.debug("Internal server error for uri [" + uri + "]", e);
                            }
                            rspChannel.respond(new HttpResponseException(
                                    HttpConstants.StatusCodes._500_INTERNAL_SERVER_ERROR, "Internal Server Error"));
                        }
                    }
                    if (log.logDebug()) {
                        log.debug("Nothing registered for uri [" + uri + "]");
                    }
                    rspChannel.respond(new HttpResponseException(HttpConstants.StatusCodes._404_NOT_FOUND,
                            "Not Found: " + uri));
                } catch (IOException e1) {
                }
            }
        });
    }

    private SocketResponseChannel newSocketResponseChannel(Socket socket, HttpRequestBuffer request) {
        Encoding rspEncoding = this.selectResponseEncoding(request);

        ResponseCoordinator rc;
        synchronized (socketResponseCoordinators) {
            rc = socketResponseCoordinators.get(socket);
            if (rc == null) {
                socketResponseCoordinators.put(socket, rc = this.newResponseCoordinator(socket));
            }
        }
        return new SocketResponseChannel(rc, request, rspEncoding);
    }

    private ResponseCoordinator newResponseCoordinator(Socket socket) {
        return new ResponseCoordinator(this, socket);
    }

    /**
     * This package private method exists purely to route calls to the protected {@link #queueWrite(Socket, byte[])}
     * method from classes within this package without forcing it to be public.
     * 
     * @param socket
     * @param rspData
     */
    void queueResponse(Socket socket, byte[] rspData, boolean close) {
        this.queueWrite(socket, rspData, close);
    }

    /**
     * Constructs a new {@link HttpResponse} containing the given XML-RPC method response.
     * <p>
     * This implementation encodes the response using <code>UTF-8</code>, sets the status code to <code>200</code>,
     * and sets <code>Content-Type</code> header to <code>text/xml</code> as required. No other headers are set.
     * 
     * @param rsp
     *            The XML-RPC method response to be returned in the HTTP response.
     * @param encoding
     *            An {@link Encoding} describing the encoding to use when constructing this message. This also
     *            informs the <code>Content-Encoding</code> header value. May be <code>null</code>.
     * @return A new {@link HttpResponse} with the marshalled XML-RPC response as its content.
     * @throws IOException
     *             if an error occurs while marshalling the XML-RPC response.
     * @throws MarshallingException
     */
    protected HttpResponse toHttpResponse(HttpMessageBuffer origMsg, Response rsp, Encoding encoding)
            throws IOException {
        HttpResponse httpRsp = this.newHttpResponse(origMsg, 200, "OK", encoding);
        httpRsp.addHeader(HttpConstants.Headers.CONTENT_TYPE, rsp.getContentType());
        httpRsp.setContent(rsp.getContent());
        return httpRsp;
    }

    // TODO: Document
    protected Encoding selectResponseEncoding(HttpRequestBuffer request) {
        Map<String, Float> accepted = request.getAcceptedEncodings();
        if (accepted == null) {
            return null;
        }

        Iterator<String> encodings = accepted.keySet().iterator();
        while (encodings.hasNext()) {
            String encodingName = encodings.next();
            Encoding encoding = this.contentEncodingMap.getEncoding(encodingName);
            if (encoding != null) {
                return encoding;
            }
        }

        return null;
    }

    @Override
    protected ResourcePool newWorkerPool() {
        return new ServerResourcePool();
    }

    /**
     * This implementation defers to the {@link #accept(SelectionKey)} method if a connection is pending. In all
     * other cases it defers to its parent.
     * 
     * @param key
     *            The {@link SelectionKey} for the socket on which an I/O operation is pending.
     * @throws IOException
     *             if an error occurs while processing the pending event.
     */
    @Override
    protected void handleSelectionKeyOperation(SelectionKey key) throws IOException {
        if (key.isValid() && key.isAcceptable()) {
            this.accept(key);
        } else {
            super.handleSelectionKeyOperation(key);
        }
    }

    @Override
    protected void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        this.resetClientTimer(socketChannel.socket());
        super.read(key);
    }

    @Override
    protected void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        this.resetClientTimer(socketChannel.socket());
        super.write(key);
    }

    @Override
    protected void deregisterSocket(Socket socket) {
        TimerTask task = this.socketActivity.remove(socket);
        if (task != null) {
            task.cancel();
        }

        super.deregisterSocket(socket);
    }

    protected void deregisterResponseCoordinator(ResponseCoordinator coordinator) {
        synchronized (socketResponseCoordinators) {
            socketResponseCoordinators.remove(coordinator);
        }
    }

    /**
     * Called when a new connection is pending on the underlying {@link ServerSocketChannel}.
     * 
     * @param key
     *            The {@link SelectionKey} for the socket on which a connection is pending.
     * @throws IOException
     *             if an error occurs while accepting the new connection.
     */
    protected void accept(SelectionKey key) throws IOException {
        // Pull out the socket channel that has a connection pending
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        // Accept the connection
        SocketChannel socketChannel = serverSocketChannel.accept();
        Socket socket = socketChannel.socket();

        // Check if our AcceptPolicy will allow this new connection
        if (this.acceptPolicy != null
                && !this.acceptPolicy.shouldRetain(socketChannel, this.getSocketSelector().keys().size())) {
            if (log.logTrace()) {
                log.trace("Closing accepted connection (accept policy enforced)");
            }
            socketChannel.close();
            return;
        }

        this.registerChannel(socketChannel);

        // Register the new socket. This will promote it to an SSLSocket
        // if we're configured for HTTPS.
        this.registerSocket(socket, this.host, this.port, false);

        // Add the new SocketChannel to our Selector
        socketChannel.configureBlocking(false);
        @SuppressWarnings("unused")
        SelectionKey acceptKey = socketChannel.register(this.getSocketSelector(), SelectionKey.OP_READ);

        this.resetClientTimer(socketChannel.socket());
    }

    private void resetClientTimer(Socket socket) {
        if (this.idleClientTimer == null) {
            if (log.logTrace()) {
                log.trace("No idle client timeout configured, skipping timer reset");
            }
            return;
        }

        if (log.logTrace()) {
            log.trace("Resetting idle client timer: " + System.identityHashCode(socket));
        }

        // Store this in a local so we don't have to worry about
        // the value changing underneath us.
        long timeout = this.idleClientTimeout;

        // Cancel the existing task for this socket ...
        TimerTask curTask = this.socketActivity.get(socket);
        if (curTask != null) {
            curTask.cancel();
        }

        // And schedule a new one.
        TimerTask task = new IdleClientTimerTask(socket);
        this.socketActivity.put(socket, task);
        this.idleClientTimer.schedule(task, timeout);
    }

    private class IdleClientTimerTask extends TimerTask {
        private Socket socket;

        IdleClientTimerTask(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                if (log.logTrace()) {
                    log.trace("Idle client timer expired: " + System.identityHashCode(socket));
                }
                SocketChannel socketChannel = this.socket.getChannel();
                socketChannel.keyFor(HttpServer.this.getSocketSelector()).cancel();
                // This (shutting down the output stream) seems unnecessary but 
                // without it the client never sees a disconnect under Linux.
                // For good measure we shutdown the input stream too.
                socketChannel.socket().shutdownOutput();
                socketChannel.socket().shutdownInput();
                socketChannel.close();
                HttpServer.this.deregisterSocket(socket);
            } catch (Exception e) {
                log.warn("IdleClientTimerTask caught an exception", e);
            }
        }
    }

    /**
     * Converts an exception raised while processing an HTTP request into a suitable HTTP response.
     * <p>
     * The response is marshalled and queued for writing on the socket associated with the original request.
     * 
     * @param msg
     *            The HTTP request being processed when the exception occurred.
     * @param e
     *            The exception that was raised.
     * @throws IOException
     *             if an error occurs marshalling or writing the response.
     */
    @Override
    protected void handleMessageException(HttpMessageBuffer msg, Exception e) throws IOException {
        HttpResponse httpRsp;
        if (e instanceof HttpResponseException) {
            if (log.logWarn()) {
                log.warn("HttpResponseException", e);
            }
            httpRsp = this.newHttpResponse(msg, (HttpResponseException) e);
            this.queueWrite(msg.getSocket(), httpRsp.marshal(), true);
        } else if (e instanceof RemoteSocketClosedException) {
            if (log.logTrace()) {
                log.trace("Remote entity closed connection", e);
            }
        } else {
            if (log.logError()) {
                log.error("Internal Server Error", e);
            }
            httpRsp = this.newHttpResponse(msg, new HttpResponseException(
                    HttpConstants.StatusCodes._500_INTERNAL_SERVER_ERROR, "Internal Server Error", e));
            this.queueWrite(msg.getSocket(), httpRsp.marshal(), true);
        }
    }

    @Override
    protected void handleProcessingException(Socket socket, Exception e) {
        log.error("Exception on selecting thread (socket=" + Utils.toString(socket) + ")", e);
    }

    @Override
    protected void handleTimeout(Socket socket, Exception cause) {
        log.debug("Timeout on " + Utils.toString(socket), cause);
    }

    @Override
    protected void handleSSLHandshakeFinished(Socket socket, SSLEngine engine) {
        this.queueRead(socket);
    }

    @Override
    protected void stopImpl() throws IOException {
        this.deregisterChannel(this.serverChannel);

        this.serverChannel.close();

        // Queue a cancellation for serverChannel
        this.queueCancellation(this.serverChannel);
    }

    /**
     * Creates and initializes a {@link ServerSocketChannel} for accepting connections on.
     */
    @Override
    protected void initSelector(Selector selector) throws IOException {
        // Create a new server socket and set to non blocking mode
        this.serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        // Bind the server socket to the local host and port
        InetSocketAddress isa = new InetSocketAddress(this.hostAddress, this.port);
        serverChannel.socket().bind(isa);

        // Register accepts on the server socket with the selector. This
        // step tells the selector that the socket wants to be put on the
        // ready list when accept operations occur, so allowing multiplexed
        // non-blocking I/O to take place.
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        this.registerChannel(serverChannel);
    }

    @Override
    protected SSLEngine initSocketSSLEngine(Socket socket) throws SSLException {
        SSLEngine engine = super.initSocketSSLEngine(socket);
        engine.setUseClientMode(false);

        switch (this.getSSLConfiguration().getClientAuthentication()) {
        case NONE:
            engine.setNeedClientAuth(false);
            engine.setWantClientAuth(false);
            break;
        case REQUEST:
            engine.setWantClientAuth(true);
            break;
        case REQUIRE:
            engine.setNeedClientAuth(true);
            break;
        }

        return engine;
    }

    @Override
    protected HttpMessageBuffer getReadBuffer(Socket socket) {
        synchronized (this.requestBuffers) {
            HttpRequestBuffer request = this.requestBuffers.get(socket);
            if (request == null) {
                request = new HttpRequestBuffer(this, socket, this.contentEncodingMap);
                this.requestBuffers.put(socket, request);
            }
            return request;
        }
    }

    @Override
    protected void removeReadBuffer(Socket socket) {
        synchronized (this.requestBuffers) {
            this.requestBuffers.remove(socket);
        }
    }

    @Override
    protected void removeReadBuffers(Socket socket) {
        this.removeReadBuffer(socket);
    }

    @Override
    protected void putWriteBuffer(Socket socket, ByteBuffer data) {
        synchronized (this.responseBuffers) {
            List<ByteBuffer> existing = this.responseBuffers.get(socket);
            if (existing != null) {
                existing.add(data);
            } else {
                LinkedList<ByteBuffer> list = new LinkedList<>();
                list.add(data);
                this.responseBuffers.put(socket, list);
            }
        }
    }

    @Override
    protected boolean isWriteQueued(Socket socket) {
        synchronized (this.responseBuffers) {
            return this.responseBuffers.containsKey(socket);
        }
    }

    @Override
    protected ByteBuffer getWriteBuffer(Socket socket) {
        synchronized (this.responseBuffers) {
            List<ByteBuffer> writeBufferList = this.responseBuffers.get(socket);
            if (writeBufferList.isEmpty()) {
                return null;
            } else {
                return writeBufferList.get(0);
            }
        }
    }

    @Override
    protected void removeWriteBuffer(Socket socket) {
        synchronized (this.responseBuffers) {
            List<ByteBuffer> existing = this.responseBuffers.get(socket);
            if (existing != null && !existing.isEmpty()) {
                existing.remove(0);
            } else {
                this.responseBuffers.remove(socket);
            }
        }
    }

    @Override
    protected void removeWriteBuffers(Socket socket) {
        synchronized (this.responseBuffers) {
            this.responseBuffers.remove(socket);
        }
    }

    protected HttpResponse newHttpResponse(HttpMessageBuffer msg, HttpResponseException e) {
        HttpResponse httpRsp = e.toHttpResponse(msg.getHttpVersionString());
        if (msg.getHttpVersion() > 1.0) {
            httpRsp.addHeader(HttpConstants.Headers.HOST, this.headerHostValue);
        }
        httpRsp.addHeader(HttpConstants.Headers.SERVER, this.getServerName());
        httpRsp.addHeader(HttpConstants.Headers.CONNECTION, "close");
        return httpRsp;
    }

    protected HttpResponse newHttpResponse(HttpMessageBuffer origMsg, int statusCode, String reasonPhrase,
            Encoding encoding) {
        Encoding responseEncoding = null;
        if (this.encodeResponses) {
            responseEncoding = encoding;
        }
        HttpResponse httpRsp;
        if (origMsg == null) {
            httpRsp = new HttpResponse(statusCode, reasonPhrase, responseEncoding);
        } else {
            httpRsp = new HttpResponse(origMsg.getHttpVersionString(), statusCode, reasonPhrase, responseEncoding);
        }
        if (origMsg.getHttpVersion() > 1.0) {
            httpRsp.addHeader(HttpConstants.Headers.HOST, this.headerHostValue);
        }
        httpRsp.addHeader(HttpConstants.Headers.SERVER, this.getServerName());
        return httpRsp;
    }

    protected String getServerName() {
        return Version.getDescription();
    }
}
