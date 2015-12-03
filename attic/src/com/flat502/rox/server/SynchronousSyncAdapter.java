package com.flat502.rox.server;

import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.marshal.RpcResponse;

@SuppressWarnings("deprecation")
class SynchronousSyncAdapter implements SynchronousRequestHandler {
	private SyncRequestHandler target;

	public SynchronousSyncAdapter(SyncRequestHandler target) {
		this.target = target;
	}

	@Override
    public RpcResponse handleRequest(RpcCall call, RpcCallContext context) throws Exception {
		return target.handleRequest(call);
	}
}
