package com.flat502.rox.marshal.xmlrpc;

import java.io.InputStream;

import com.flat502.rox.marshal.MethodCallUnmarshallerAid;
import com.flat502.rox.marshal.RpcCall;

public class Test_DomMethodCallUnmarshaller extends TestBase_MethodCallUnmarshaller {
	public Test_DomMethodCallUnmarshaller(String name) {
		super(name);
	}
	
	@Override
    protected RpcCall unmarshal(String xml, Class[] types) throws Exception {
		return new DomMethodCallUnmarshaller().unmarshal(xml, types);
	}
	
	@Override
    protected RpcCall unmarshal(InputStream xml, Class[] types) throws Exception {
		return new DomMethodCallUnmarshaller().unmarshal(xml, types);
	}

	@Override
    protected RpcCall unmarshalWithAid(String xml, MethodCallUnmarshallerAid aid) throws Exception {
		return new DomMethodCallUnmarshaller().unmarshal(xml, aid);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_DomMethodCallUnmarshaller.class);
	}
}
