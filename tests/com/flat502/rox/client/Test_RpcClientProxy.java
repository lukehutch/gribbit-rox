package com.flat502.rox.client;

import java.io.IOException;
import java.net.URL;

import junit.framework.TestCase;

import com.flat502.rox.marshal.IProxyObject;
import com.flat502.rox.processing.ThreadUtils;

public class Test_RpcClientProxy extends TestCase {
    private static class MockHttpRpcClient extends XmlRpcClient {
        public String name;
        public Object[] params;
        public Class retClass;

        public MockHttpRpcClient() throws IOException {
            super(new URL("http://localhost"));
        }

        @Override
        public Object execute(String name, Object[] params, Class retClass) throws Exception {
            this.name = name;
            this.params = params;
            this.retClass = retClass;
            return null;
        }
    }

    @Override
    protected void setUp() throws Exception {
        ThreadUtils.assertZeroThreads();
    }

    @Override
    protected void tearDown() throws Exception {
        ThreadUtils.assertZeroThreads();
    }

    public void testObjectReturnType() throws Exception {
        MockHttpRpcClient mock = new MockHttpRpcClient();
        try {
            IProxyObject proxy = (IProxyObject) new RpcClientProxy(IProxyObject.class, mock).getProxiedTarget();

            proxy.returnsObject("foo");

            assertEquals("returnsObject", mock.name);
            assertEquals("foo", mock.params[0]);
        } finally {
            mock.stop();
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Test_RpcClientProxy.class);
    }
}
