package com.flat502.rox.marshal.xmlrpc;

import java.io.InputStream;

public class Test_DomMethodResponseUnmarshaller extends TestBase_MethodResponseUnmarshaller {
    public Test_DomMethodResponseUnmarshaller(String name) {
        super(name);
    }

    @Override
    protected RpcResponse unmarshal(String xml, final Class type) throws Exception {
        return new DomMethodResponseUnmarshaller().unmarshal(xml, new MethodResponseUnmarshallerAid() {
            @Override
            public Class getReturnType() {
                return type;
            }
        });
    }

    @Override
    protected RpcResponse unmarshal(InputStream xml, final Class type) throws Exception {
        return new DomMethodResponseUnmarshaller().unmarshal(xml, new MethodResponseUnmarshallerAid() {
            @Override
            public Class getReturnType() {
                return type;
            }
        });
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Test_DomMethodResponseUnmarshaller.class);
    }
}
