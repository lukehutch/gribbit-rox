package com.flat502.rox.server;

import java.util.HashMap;
import java.util.Map;

import com.flat502.rox.encoding.Encoding;
import com.flat502.rox.encoding.EncodingMap;

class ServerEncodingMap implements EncodingMap {
	private Map<String, Encoding> map = new HashMap<String, Encoding>();
	
	public Encoding addEncoding(Encoding encoding) {
		return this.map.put(encoding.getName(), encoding);
	}
	
	@Override
    public Encoding getEncoding(String name) {
		return this.map.get(name);
	}
}
