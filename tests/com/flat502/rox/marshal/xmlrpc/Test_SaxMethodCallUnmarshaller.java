package com.flat502.rox.marshal.xmlrpc;

import java.io.InputStream;

public class Test_SaxMethodCallUnmarshaller extends TestBase_MethodCallUnmarshaller {
    static {
        System.getProperties().remove("javax.xml.parsers.SAXParserFactory");
        SaxParserPool.reset();
    }

    public Test_SaxMethodCallUnmarshaller(String name) {
        super(name);
    }

    @Override
    protected Request unmarshal(String xml, Class[] types) throws Exception {
        return new SaxMethodCallUnmarshaller().unmarshal(xml, types);
    }

    @Override
    protected Request unmarshal(InputStream xml, Class[] types) throws Exception {
        return new SaxMethodCallUnmarshaller().unmarshal(xml, types);
    }

    @Override
    protected Request unmarshalWithAid(String xml, MethodCallUnmarshallerAid aid) throws Exception {
        return new SaxMethodCallUnmarshaller().unmarshal(xml, aid);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Test_SaxMethodCallUnmarshaller.class);
    }
}
