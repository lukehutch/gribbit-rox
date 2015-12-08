package com.flat502.rox.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import com.flat502.rox.http.HttpConstants;

public class DeflaterEncoding implements Encoding {
    @Override
    public String getName() {
        return HttpConstants.ContentEncoding.DEFLATE;
    }

    @Override
    public InputStream getDecoder(InputStream in) throws IOException {
        return new InflaterInputStream(in);
    }

    @Override
    public OutputStream getEncoder(OutputStream out) throws IOException {
        return new DeflaterOutputStream(out);
    }
}
