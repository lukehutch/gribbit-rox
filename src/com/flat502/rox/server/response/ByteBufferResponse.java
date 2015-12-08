package com.flat502.rox.server.response;

import java.nio.ByteBuffer;

public class ByteBufferResponse implements Response {
    private ByteBuffer byteBuffer;
    private String contentType;

    public ByteBufferResponse(ByteBuffer byteBuffer, String contentType) {
        this.byteBuffer = byteBuffer;
        this.contentType = contentType;
    }

    @Override
    public byte[] getContent() {
        return byteBuffer.array();
    }

    @Override
    public String getContentType() {
        return contentType;
    }
}
