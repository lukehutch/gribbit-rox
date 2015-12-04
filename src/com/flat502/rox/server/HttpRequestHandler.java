package com.flat502.rox.server;

import com.flat502.rox.http.HttpConstants;
import com.flat502.rox.http.HttpMessageBuffer;
import com.flat502.rox.http.HttpRequestBuffer;
import com.flat502.rox.http.HttpResponse;
import com.flat502.rox.http.HttpResponseException;
import com.flat502.rox.log.Log;
import com.flat502.rox.log.LogFactory;
import com.flat502.rox.processing.HttpMessageHandler;
import com.flat502.rox.utils.BlockingQueue;

class HttpRequestHandler extends HttpMessageHandler {
	private static Log log = LogFactory.getLog(HttpRequestHandler.class);

	// private ByteBuffer readBuf = ByteBuffer.allocate(1024);

	HttpRequestHandler(BlockingQueue queue) {
		super(queue);
	}

	@Override
    protected void handleMessage(HttpMessageBuffer msg) throws Exception {
		if (!(msg instanceof HttpRequestBuffer)) {
			throw new IllegalArgumentException("Expected instance of " + HttpRequestBuffer.class.getName() + ", got "
					+ msg.getClass().getName());
		}
		HttpRequestBuffer request = (HttpRequestBuffer) msg;
		
		if (!(request.getOrigin() instanceof HttpServer)) {
			throw new IllegalArgumentException("Expected instance of " + HttpServer.class.getName() + ", got "
					+ request.getOrigin().getClass().getName());
		}
		HttpServer server = (HttpServer) request.getOrigin();

		if (!request.isComplete()) {
			throw new IllegalStateException("Incomplete request on my queue");
		}
		
		HttpResponse httpRsp;
		try {
			server.routeRequest(request.getSocket(), request);
			// Server handlers are always asynchronous and will queue a response when done. We're done here.
			return;
		} catch (HttpResponseException e) {
			httpRsp = server.newHttpResponse(msg, e);
		} catch (NoSuchMethodError e) {
			httpRsp = server.newHttpResponse(msg, HttpConstants.StatusCodes._404_NOT_FOUND,
					"Not Found (" + e.getMessage() + ")", null);
		} catch (NoSuchMethodException e) {
			httpRsp = server.newHttpResponse(msg, HttpConstants.StatusCodes._404_NOT_FOUND,
					"Not Found (" + e.getMessage() + ")", null);
		} catch (Exception e) {
			httpRsp = server.newHttpResponse(msg, HttpConstants.StatusCodes._500_INTERNAL_SERVER_ERROR,
					"Internal Server Error (" + e.getMessage() + ")", null);
			log.error("Error routing HTTP request:\n" + request.toString(), e);
		}

		server.queueResponse(request.getSocket(), httpRsp.marshal(), httpRsp.mustCloseConnection());
	}
}