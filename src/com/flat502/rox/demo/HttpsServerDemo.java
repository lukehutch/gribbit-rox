package com.flat502.rox.demo;

import java.net.InetAddress;

import com.flat502.rox.http.HttpConstants;
import com.flat502.rox.http.HttpRequestBuffer;
import com.flat502.rox.http.HttpResponse;
import com.flat502.rox.http.exception.HttpResponseException;
import com.flat502.rox.log.Level;
import com.flat502.rox.log.Log;
import com.flat502.rox.log.LogFactory;
import com.flat502.rox.log.LogFactoryImpl;
import com.flat502.rox.log.StreamLog;
import com.flat502.rox.processing.SSLConfiguration;
import com.flat502.rox.server.AsynchronousRequestHandler;
import com.flat502.rox.server.HttpServer;
import com.flat502.rox.server.RequestContext;
import com.flat502.rox.server.response.Response;

/**
 * A demo synchronous server illustrating the {@link com.flat502.rox.server.SynchronousRequestHandler} interface.
 */
public class HttpsServerDemo {

    /**
     * Start an instance of this demo server.
     * <p>
     * The following XML-RPC methods are supported by this server:
     * <ul>
     * <li>{@link RMIServerInterface#sum(int[]) rmi.sum}</li>
     * <li>{@link RMIServerInterface#getDate() rmi.getDate}</li>
     * <li>{@link RMIServerInterface#getVersionInfo(boolean) rmi.getVersion}</li>
     * </ul>
     *
     * @param args
     *            A list of parameters indicating the <code>host/address</code> and <code>port</code> to bind to.
     *            These default to <code>localhost</code> and <code>8080</code> if not specified.
     */
    public static void main(String[] args) {
        LogFactory.configure(new LogFactoryImpl() {
            @Override
            public Log newLog(String name) {
                return new StreamLog(System.out, Level.TRACE);
            }
        });
        try {
            String host = "localhost";
            int httpPort = 8080;
            int httpsPort = 8443;

            if (args != null && args.length > 0) {
                host = args[0];
                if (args.length > 1) {
                    httpPort = Integer.parseInt(args[1]);
                }
            }
            System.out.println("Starting server on " + host + ":" + httpsPort);

            // Redirect HTTP requests to HTTPS
            final String hostFinal = host;
            new HttpServer(InetAddress.getByName(host), httpPort).registerHandler(new AsynchronousRequestHandler() {
                @Override
                public Response handleRequest(RequestContext context) throws Exception {
                    final HttpRequestBuffer req = context.getHttpRequest();
                    String redirectURI = "https://" + hostFinal + ":" + httpsPort + req.getURI();
                    System.out.println("Redirecting HTTP request " + req.getURI() + " to " + redirectURI);
                    throw new HttpResponseException(HttpConstants.StatusCodes._302_FOUND, "Found") {
                        @Override
                        protected void setExtraHeaders(HttpResponse rsp) {
                            rsp.addHeader("Location", redirectURI);
                        }
                    };
                }
            }).start();

            HttpServer server = new HttpServer(InetAddress.getByName(host), httpsPort,
                    SSLConfiguration.createSelfSignedCertificate());
            server.registerHandler(new AsynchronousRequestHandler() {
                @Override
                public Response handleRequest(RequestContext context) throws Exception {
                    final HttpRequestBuffer req = context.getHttpRequest();
                    String body = new String(req.getContent());
                    System.out.println("Got request " + req.getURI() + " with body:\n" + body);

                    return new Response() {
                        @Override
                        public byte[] getContent() {
                            return ("<html><body><p>Got request: " + req.getURI() + "</p></body></html>")
                                    .getBytes();
                        }

                        @Override
                        public String getContentType() {
                            return "text/html";
                        }
                    };
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
