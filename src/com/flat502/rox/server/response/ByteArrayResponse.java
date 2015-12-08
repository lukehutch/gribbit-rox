package com.flat502.rox.server.response;

public class ByteArrayResponse implements Response {
    private byte[] byteArray;
    private String contentType;

    public ByteArrayResponse(byte[] byteArray, String contentType) {
        this.byteArray = byteArray;
        this.contentType = contentType;
    }

    @Override
    public byte[] getContent() {
        return byteArray;
    }

    @Override
    public String getContentType() {
        return contentType;
    }
}
