package com.flat502.rox.marshal.xmlrpc;

import java.io.InputStream;

public class Test_SaxMethodResponseUnmarshallerWithXerces extends TestBase_MethodResponseUnmarshaller {
    static {
        System.setProperty("javax.xml.parsers.SAXParserFactory",
                "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
    }

    public Test_SaxMethodResponseUnmarshallerWithXerces(String name) {
        super(name);
    }

    @Override
    protected RpcResponse unmarshal(String xml, final Class type) throws Exception {
        return new SaxMethodResponseUnmarshaller().unmarshal(xml, new MethodResponseUnmarshallerAid() {
            @Override
            public Class getReturnType() {
                return type;
            }
        });
    }

    @Override
    protected RpcResponse unmarshal(InputStream xml, final Class type) throws Exception {
        return new SaxMethodResponseUnmarshaller().unmarshal(xml, new MethodResponseUnmarshallerAid() {
            @Override
            public Class getReturnType() {
                return type;
            }
        });
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Test_SaxMethodResponseUnmarshallerWithXerces.class);
    }
}
