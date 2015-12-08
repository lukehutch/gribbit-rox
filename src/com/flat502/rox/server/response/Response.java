package com.flat502.rox.server.response;

public interface Response {
    /** The content of the response. */
    public byte[] getContent();

    /** The mimetype of the content. */
    public String getContentType();
}
