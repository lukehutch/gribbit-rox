package com.flat502.rox.marshal.xmlrpc;

public class Test_Base64Codec extends TestBase_XmlRpcMethod {
	public Test_Base64Codec(String name) {
		super(name);
	}

	public void testDecodeEntireArray() throws Exception {
		byte[] clear = Base64Codec.decode("eW91IGNhbid0IHJlYWQgdGhpcyE=".toCharArray());
		assertEquals("you can't read this!", new String(clear, "UTF-8"));
	}

	public void testEncodeEntireArray() throws Exception {
		String clear = Base64Codec.encode("you can't read this!".getBytes("UTF-8"));
		assertEquals("eW91IGNhbid0IHJlYWQgdGhpcyE=", clear);
	}

	public void testDecodeArraySubset() throws Exception {
		char[] input = "<?xml version=\"1.0\"?><methodCall>	<methodName>testMethod</methodName>	<params>		<param>			<value><base64>eW91IGNhbid0IHJlYWQgdGhpcyE=</base64></value>		</param>	</params></methodCall>".toCharArray();
		byte[] clear = Base64Codec.decode(input, 105, 28);
		assertEquals("you can't read this!", new String(clear, "UTF-8"));
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_Base64Codec.class);
	}
}
