package com.flat502.rox.server.response;

import java.nio.charset.Charset;

public class HtmlResponse extends ByteArrayResponse {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    public HtmlResponse(String str) {
        super(str.getBytes(UTF8), "text/html;charset=utf-8");
    }
}
