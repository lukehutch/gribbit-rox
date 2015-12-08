package com.flat502.rox.client;

import java.nio.channels.SocketChannel;

import com.flat502.rox.http.HttpResponseBuffer;
import com.flat502.rox.processing.Context;
import com.flat502.rox.processing.SSLSession;

public class ResponseContext extends Context {
    private HttpResponseBuffer httpResponse;

    public ResponseContext(SocketChannel channel, SSLSession sslSession, HttpResponseBuffer rsp) {
        super(channel, sslSession);
        this.httpResponse = rsp;
    }

    public HttpResponseBuffer getHttpRequest() {
        return this.httpResponse;
    }
}
