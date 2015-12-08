package com.flat502.rox.http.exception;

import com.flat502.rox.http.HttpConstants;
import com.flat502.rox.http.HttpResponse;

public class MethodNotAllowedResponseException extends HttpResponseException {
    private String allowed;

    public MethodNotAllowedResponseException(String string, String allowed) {
        super(HttpConstants.StatusCodes._405_METHOD_NOT_ALLOWED, "Method Not Allowed" + string);
        this.allowed = allowed;
    }

    @Override
    public HttpResponse toHttpResponse(String httpVersion) {
        HttpResponse rsp = super.toHttpResponse(httpVersion);
        // A 405 response requires that we set the Allow header and provide a list of allowable
        // methods.
        rsp.addHeader(HttpConstants.Headers.ALLOW, this.allowed);
        return rsp;
    }
}
