/**
 *
 */
package com.flat502.rox.client;

class TestResponseHandler implements AsynchronousResponseHandler {
    private boolean shouldWait = true;
    private Object response;
    private Throwable exception;

    @Override
    public synchronized void handleResponse(Request call, RpcResponse rsp, ResponseContext context) {
        this.response = rsp.getReturnValue();
        this.shouldWait = false;
        this.notify();
    }

    @Override
    public synchronized void handleException(Request call, Throwable e, ResponseContext context) {
        this.exception = e;
        this.shouldWait = false;
        this.notify();
    }

    public synchronized Object waitForResponse(long timeout) throws Throwable {
        long start = System.currentTimeMillis();
        while (this.shouldWait) {
            try {
                this.wait(timeout);
                long elapsed = System.currentTimeMillis() - start;
                if (this.shouldWait && elapsed >= timeout) {
                    throw new IllegalStateException("Timed out in waitForResponse: " + elapsed + " >= " + timeout);
                }
            } catch (InterruptedException e) {
            }
        }

        if (this.response != null) {
            return this.response;
        } else if (this.exception != null) {
            throw this.exception;
        } else {
            throw new IllegalStateException("What the hell?");
        }
    }
}