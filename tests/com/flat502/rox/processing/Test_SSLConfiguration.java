package com.flat502.rox.processing;

import junit.framework.TestCase;

import com.flat502.rox.processing.SSLConfiguration.ClientAuth;

public class Test_SSLConfiguration extends TestCase {
    public void testDefaultHandshakeTimeout() throws Exception {
        assertEquals(10000, new SSLConfiguration().getHandshakeTimeout());
    }

    public void testDefaultClientAuth() throws Exception {
        assertEquals(ClientAuth.NONE, new SSLConfiguration().getClientAuthentication());
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Test_SSLConfiguration.class);
    }
}
