package com.flat502.rox.marshal.xmlrpc;

import java.io.InputStream;

public class Test_SaxMethodResponseUnmarshaller extends TestBase_MethodResponseUnmarshaller {
    static {
        System.getProperties().remove("javax.xml.parsers.SAXParserFactory");
        SaxParserPool.reset();
    }

    public Test_SaxMethodResponseUnmarshaller(String name) {
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
        junit.textui.TestRunner.run(Test_SaxMethodResponseUnmarshaller.class);
    }
}
