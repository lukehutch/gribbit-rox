package com.flat502.rox.client;

import java.net.URL;

import junit.framework.TestCase;

public class Test_SyncClientWithRPC2URI extends TestCase {
    private static final String PREFIX = "test";
    private static final int PORT = 8080;
    private static final String URL = "http://localhost:" + PORT + "/RPC2";

    private TestServer server;

    @Override
    protected void setUp() throws Exception {
        this.server = new TestServer("/RPC2", PREFIX, PORT);
    }

    @Override
    protected void tearDown() throws Exception {
        this.server.stop();
    }

    public void testStringResponse() throws Exception {
        XmlRpcClient client = new XmlRpcClient(new URL(URL));
        try {
            Object rsp = client.execute("test.stringResponse", null);
            assertNotNull(rsp);
            assertEquals("bar", rsp);
        } finally {
            client.stop();
        }
    }

    public void testStringResponseWithTrailingSlashOnURL() throws Exception {
        XmlRpcClient client = new XmlRpcClient(new URL("http://localhost:" + PORT + "/RPC2/"));
        try {
            Object rsp = client.execute("test.stringResponse", null);
            assertNotNull(rsp);
            assertEquals("bar", rsp);
        } finally {
            client.stop();
        }
    }

    public void testNullResponse() throws Exception {
        XmlRpcClient client = new XmlRpcClient(new URL(URL));
        try {
            Object rsp = client.execute("test.nullResponse", null);
            fail();
        } catch (UnsupportedOperationException e) {
        } finally {
            client.stop();
        }
    }

    public void testXmlRpcFaultResponse() throws Exception {
        XmlRpcClient client = new XmlRpcClient(new URL(URL));
        try {
            Object rsp = client.execute("test.returnXmlRpcMethofFault", null);
            fail();
        } catch (RpcFaultException e) {
            assertEquals(21, e.getFaultCode());
            assertEquals("Half the meaning of Life", e.getFaultString());
        } finally {
            client.stop();
        }
    }

    public void testXmlRpcFaultException() throws Exception {
        XmlRpcClient client = new XmlRpcClient(new URL(URL));
        try {
            Object rsp = client.execute("test.raiseXmlRpcFaultException", null);
            fail();
        } catch (RpcFaultException e) {
            assertEquals(42, e.getFaultCode());
            assertEquals("The meaning of Life", e.getFaultString());
        } finally {
            client.stop();
        }
    }

    public void testOtherException() throws Exception {
        XmlRpcClient client = new XmlRpcClient(new URL(URL));
        try {
            Object rsp = client.execute("test.raiseOtherException", null);
            fail();
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().indexOf("Another Exception") != -1);
        } finally {
            client.stop();
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Test_SyncClientWithRPC2URI.class);
    }
}
