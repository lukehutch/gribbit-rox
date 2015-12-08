package com.flat502.rox.client;

import java.net.URL;

import junit.framework.TestCase;

public class Test_SyncClientWithSharedWorkerPool extends TestCase {
    private static final String PREFIX = "test";
    private static final int PORT = 8080;
    private static final String URL = "http://localhost:" + PORT + "/";

    private TestServer server;

    @Override
    protected void setUp() throws Exception {
        this.server = new TestServer("/", PREFIX, PORT);
    }

    @Override
    protected void tearDown() throws Exception {
        this.server.stop();
    }

    public void testImplicitWorker() throws Exception {
        ClientResourcePool pool = new ClientResourcePool();
        XmlRpcClient clientA = new XmlRpcClient(new URL(URL), pool);
        XmlRpcClient clientB = new XmlRpcClient(new URL(URL), pool);

        try {
            Object rsp = clientA.execute("test.stringResponse", null);
            assertNotNull(rsp);
            assertEquals("bar", rsp);
            rsp = clientB.execute("test.stringResponse", null);
            assertNotNull(rsp);
            assertEquals("bar", rsp);
            rsp = clientA.execute("test.stringResponse", null);
            assertNotNull(rsp);
            assertEquals("bar", rsp);
            rsp = clientB.execute("test.stringResponse", null);
            assertNotNull(rsp);
            assertEquals("bar", rsp);
        } finally {
            clientA.stop();
            clientB.stop();
            pool.shutdown();
        }
    }

    public void testExplicitWorker() throws Exception {
        ClientResourcePool pool = new ClientResourcePool();
        pool.addWorker();
        XmlRpcClient clientA = new XmlRpcClient(new URL(URL), pool);
        XmlRpcClient clientB = new XmlRpcClient(new URL(URL), pool);

        try {
            Object rsp = clientA.execute("test.stringResponse", null);
            assertNotNull(rsp);
            assertEquals("bar", rsp);
            rsp = clientB.execute("test.stringResponse", null);
            assertNotNull(rsp);
            assertEquals("bar", rsp);
            rsp = clientA.execute("test.stringResponse", null);
            assertNotNull(rsp);
            assertEquals("bar", rsp);
            rsp = clientB.execute("test.stringResponse", null);
            assertNotNull(rsp);
            assertEquals("bar", rsp);
        } finally {
            clientA.stop();
            clientB.stop();
            pool.shutdown();
        }
    }

    public void testImplicitWorkers() throws Exception {
        ClientResourcePool pool = new ClientResourcePool();
        pool.addWorkers(2);
        XmlRpcClient clientA = new XmlRpcClient(new URL(URL), pool);
        XmlRpcClient clientB = new XmlRpcClient(new URL(URL), pool);

        try {
            Object rsp = clientA.execute("test.stringResponse", null);
            assertNotNull(rsp);
            assertEquals("bar", rsp);
            rsp = clientB.execute("test.stringResponse", null);
            assertNotNull(rsp);
            assertEquals("bar", rsp);
            rsp = clientA.execute("test.stringResponse", null);
            assertNotNull(rsp);
            assertEquals("bar", rsp);
            rsp = clientB.execute("test.stringResponse", null);
            assertNotNull(rsp);
            assertEquals("bar", rsp);
        } finally {
            clientA.stop();
            clientB.stop();
            pool.shutdown();
        }
    }

    public void testStopStartWorkersSerialized() throws Exception {
        ClientResourcePool pool = new ClientResourcePool();

        try {
            XmlRpcClient clientA = new XmlRpcClient(new URL(URL), pool);
            try {
                Object rsp = clientA.execute("test.stringResponse", null);
                assertNotNull(rsp);
                assertEquals("bar", rsp);
            } finally {
                clientA.stop();
            }

            XmlRpcClient clientB = new XmlRpcClient(new URL(URL), pool);
            try {
                Object rsp = clientB.execute("test.stringResponse", null);
                assertNotNull(rsp);
                assertEquals("bar", rsp);
            } finally {
                clientB.stop();
            }
        } finally {
            pool.shutdown();
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Test_SyncClientWithSharedWorkerPool.class);
    }
}
