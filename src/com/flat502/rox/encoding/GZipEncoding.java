package com.flat502.rox.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.flat502.rox.http.HttpConstants;

public class GZipEncoding implements Encoding {
	@Override
    public String getName() {
		return HttpConstants.ContentEncoding.GZIP;
	}

	@Override
    public InputStream getDecoder(InputStream in) throws IOException {
		return new GZIPInputStream(in);
	}

	@Override
    public OutputStream getEncoder(OutputStream out) throws IOException {
		return new GZIPOutputStream(out);
	}
}
