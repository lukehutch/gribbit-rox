package com.flat502.rox.marshal;

import com.flat502.rox.server.RequestContext;

public class ContextualProxyObject {
	public String lastMethod;
	public Object[] lastParams;
	
	public String fooBar(String name, RequestContext context) {
		this.lastMethod = "fooBar";
		this.lastParams = new Object[]{name, context};
		return "Done";
	}
}
