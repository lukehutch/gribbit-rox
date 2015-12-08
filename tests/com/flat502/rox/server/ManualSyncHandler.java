package com.flat502.rox.server;

import java.util.Map;

@SuppressWarnings("deprecation")
public class ManualSyncHandler implements SyncRequestHandler {
    public Request call;

    @Override
    public RpcResponse handleRequest(Request call) throws Exception {
        this.call = call;
        if (call.getName().equals("server.toUpper")) {
            return new XmlRpcMethodResponse(((String) call.getParameters()[0]).toUpperCase());
        } else if (call.getName().equals("server.returnFault")) {
            return new XmlRpcMethodFault(0, "return error");
        } else if (call.getName().equals("server.raiseFault")) {
            throw new XmlRpcFaultException(0, "raise error");
        } else if (call.getName().equals("server.map")) {
            return new XmlRpcMethodResponse(((Map) call.getParameters()[0]).size());
        } else if (call.getName().equals("server.raiseNoSuchMethodException")) {
            throw new NoSuchMethodException(call.getName());
        }

        return null;
    }
}
