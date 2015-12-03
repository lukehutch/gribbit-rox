package com.flat502.rox.marshal.xmlrpc;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;

import net.n3.nanoxml.IXMLElement;
import net.n3.nanoxml.IXMLParser;
import net.n3.nanoxml.IXMLReader;
import net.n3.nanoxml.StdXMLReader;
import net.n3.nanoxml.XMLParserFactory;

import com.flat502.rox.marshal.ExtendedMethodResponseUnmarshaller;
import com.flat502.rox.marshal.FieldNameCodec;
import com.flat502.rox.marshal.MethodResponseUnmarshallerAid;
import com.flat502.rox.marshal.RpcResponse;
import com.flat502.rox.utils.Utils;

/**
 * A DOM based{@link com.flat502.rox.marshal.MethodResponseUnmarshaller} 
 * implementation.
 */
public class DomMethodResponseUnmarshaller implements
		ExtendedMethodResponseUnmarshaller {
	private DomUnmarshaller unmarshaller;
	
	public DomMethodResponseUnmarshaller() {
		this(null);
	}

	public DomMethodResponseUnmarshaller(FieldNameCodec fieldNameCodec) {
		this.unmarshaller = XmlRpcUtils.newDomUnmarshaller(fieldNameCodec);
	}
	
	@Override
    public FieldNameCodec getDefaultFieldNameCodec() {
		return this.unmarshaller.getDefaultFieldNameCodec();
	}

	@Override
    public RpcResponse unmarshal(InputStream in) throws Exception {
		return unmarshal(in, null);
	}

	@Override
    public RpcResponse unmarshal(InputStream in, MethodResponseUnmarshallerAid aid)
			throws Exception {
		IXMLParser parser = XMLParserFactory.createDefaultXMLParser();
		IXMLReader reader = new StdXMLReader(Utils.newXmlReader(in, Charset
				.forName("UTF-8")));
		parser.setReader(reader);
		XmlNode root = new NanoXmlNode((IXMLElement) parser.parse());
		return this.unmarshaller.parseMethodResponse(root, aid);
	}

	@Override
    public RpcResponse unmarshal(Reader in) throws Exception {
		return unmarshal(in, null);
	}

	@Override
    public RpcResponse unmarshal(Reader in, MethodResponseUnmarshallerAid aid) throws Exception {
		IXMLParser parser = XMLParserFactory.createDefaultXMLParser();
		IXMLReader reader = new StdXMLReader(in);
		parser.setReader(reader);
		XmlNode root = new NanoXmlNode((IXMLElement) parser.parse());
		return this.unmarshaller.parseMethodResponse(root, aid);
	}

	@Override
    public RpcResponse unmarshal(String xml) throws Exception {
		return unmarshal(xml, null);
	}

	@Override
    public RpcResponse unmarshal(String xml, MethodResponseUnmarshallerAid aid) throws Exception {
		IXMLParser parser = XMLParserFactory.createDefaultXMLParser();
		IXMLReader reader = new StdXMLReader(new StringReader(xml));
		parser.setReader(reader);
		XmlNode root = new NanoXmlNode((IXMLElement) parser.parse());
		return this.unmarshaller.parseMethodResponse(root, aid);
	}
}
