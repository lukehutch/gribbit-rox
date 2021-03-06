package com.flat502.rox.server;

import java.io.IOException;

import com.flat502.rox.encoding.Encoding;
import com.flat502.rox.http.HttpRequestBuffer;
import com.flat502.rox.http.exception.HttpResponseException;
import com.flat502.rox.server.response.Response;

/**
 * A response channel wrapping an underlying SocketChannel. A new instance of this is passed to an async handler,
 * providing an opaque mapping between that handler and the appropriate socket channel for responses.
 */
class SocketResponseChannel implements ResponseChannel {
    private ResponseCoordinator coord;
    private int rspId;

    private HttpRequestBuffer request;
    // private Socket socket;
    private Encoding encoding;

    SocketResponseChannel(ResponseCoordinator coord, HttpRequestBuffer request, Encoding encoding) {
        this.coord = coord;
        this.rspId = coord.nextId();
        this.request = request;
        this.encoding = encoding;
    }

    @Override
    public void respond(Response rsp) throws IOException {
        //		HttpResponse httpRsp = this.server.toHttpResponse(this.request, rsp, this.encoding);
        //		this.server.queueResponse(socket, httpRsp.marshal(), httpRsp.mustCloseConnection());
        this.coord.respond(this.rspId, this.request, rsp, this.encoding);
    }

    @Override
    public void respond(HttpResponseException e) throws IOException {
        //		HttpResponse httpRsp = this.server.newHttpResponse(this.request, e);
        //		this.server.queueResponse(socket, httpRsp.marshal(), true);
        this.coord.respond(this.rspId, this.request, e);
    }

    @Override
    public void close() throws IOException {
        //		this.socket.getChannel().close();
        this.coord.close();
    }
}
