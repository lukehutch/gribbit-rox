package com.flat502.rox.server.response;

import java.nio.charset.Charset;

public class PlaintextResponse extends ByteArrayResponse {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    public PlaintextResponse(String str) {
        super(str.getBytes(UTF8), "text/plain;charset=utf-8");
    }
}
