package com.flat502.rox.server;

import com.flat502.rox.marshal.RpcCall;

@SuppressWarnings("deprecation")
class AsynchronousAsyncAdapter implements AsynchronousRequestHandler {
	private AsyncRequestHandler target;

	public AsynchronousAsyncAdapter(AsyncRequestHandler target) {
		this.target = target;
	}

	@Override
    public void handleRequest(RpcCall call, RpcCallContext context, ResponseChannel rspChannel) throws Exception {
		target.handleRequest(call, rspChannel);
	}
}
