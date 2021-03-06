package com.flat502.rox.marshal.xmlrpc;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.flat502.rox.marshal.ArrayParameterTypeMapper;
import com.flat502.rox.marshal.ExtendedMethodCallUnmarshaller;
import com.flat502.rox.marshal.FieldNameCodec;
import com.flat502.rox.marshal.MarshallingException;
import com.flat502.rox.marshal.MethodCallUnmarshallerAid;
import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.utils.Utils;

/**
 * A DOM based{@link com.flat502.rox.marshal.MethodCallUnmarshaller} 
 * implementation.
 */
public class DomMethodCallUnmarshaller extends DomUnmarshaller implements
		ExtendedMethodCallUnmarshaller {
	public DomMethodCallUnmarshaller() {
		this(null);
	}

	public DomMethodCallUnmarshaller(FieldNameCodec fieldNameCodec) {
		super(fieldNameCodec);
	}
	
	@Override
    public RpcCall unmarshal(InputStream in) throws Exception {
		return unmarshal(in, (MethodCallUnmarshallerAid) null);
	}

	@Override
    public RpcCall unmarshal(InputStream in, Class<?>[] structClasses)
			throws Exception {
		return unmarshal(in, new ArrayParameterTypeMapper(structClasses));
	}

	@Override
    public RpcCall unmarshal(InputStream in,
			MethodCallUnmarshallerAid aid) throws Exception {
		XmlNode root = this.parse(Utils
				.newXmlReader(in, Charset.forName("UTF-8")));
		return this.parseMethodCall(root, aid);
	}

	@Override
    public RpcCall unmarshal(Reader in) throws Exception {
		return unmarshal(in, (MethodCallUnmarshallerAid) null);
	}

	@Override
    public RpcCall unmarshal(Reader in, Class<?>[] structClasses)
			throws Exception {
		return unmarshal(in, new ArrayParameterTypeMapper(structClasses));
	}

	@Override
    public RpcCall unmarshal(Reader in, MethodCallUnmarshallerAid aid)
			throws Exception {
		XmlNode root = this.parse(in);
		return this.parseMethodCall(root, aid);
	}

	@Override
    public RpcCall unmarshal(String xml) throws Exception {
		return unmarshal(xml, (MethodCallUnmarshallerAid) null);
	}

	@Override
    public RpcCall unmarshal(String xml, Class<?>[] structClasses)
			throws Exception {
		return unmarshal(xml, new ArrayParameterTypeMapper(structClasses));
	}

	@Override
    public RpcCall unmarshal(String xml, MethodCallUnmarshallerAid aid)
			throws Exception {
		XmlNode root = this.parse(new StringReader(xml));
		return this.parseMethodCall(root, aid);
	}

	private XmlRpcMethodCall parseMethodCall(XmlNode node, MethodCallUnmarshallerAid aid)
			throws MarshallingException {
		expectTag(node, "methodCall");
        Iterator<XmlNode> children = node.enumerateChildren();
		String name = this.parseMethodName(children.next());
		Object[] params = null;
		if (children.hasNext()) {
			params = this.parseParams(name, children.next(), aid);
		}
		
		return new XmlRpcMethodCall(name, params, this.selectCodec(aid, name));
	}

	private String parseMethodName(XmlNode node) throws MarshallingException {
		expectTag(node, "methodName");
		return node.getContent();
	}

	private Object[] parseParams(String methodName, XmlNode node, MethodCallUnmarshallerAid aid)
			throws MarshallingException {
		expectTag(node, "params");
        Iterator<XmlNode> params = node.enumerateChildren();
		List<Object> paramObjects = new ArrayList<>();
		int paramIdx = 0;
		while (params.hasNext()) {
			Class<?> structClass = aid == null ? null : aid.getType(methodName, paramIdx);
			if (structClass == Object.class) {
				structClass = null;
			}

			Object param = this.parseParam(params.next(), structClass, aid);
			paramObjects.add(param);
			paramIdx++;
		}
		return paramObjects.toArray();
	}
	
	private FieldNameCodec selectCodec(MethodCallUnmarshallerAid aid, String methodName) {
		if (aid == null) {
			return this.getDefaultFieldNameCodec();
		}
		
		FieldNameCodec codec = aid.getFieldNameCodec(methodName);
		if (codec == null) {
			codec = this.getDefaultFieldNameCodec();
		}
		return codec;
	}
}
