package com.flat502.rox.marshal.xmlrpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.flat502.rox.marshal.EnumConstants;
import com.flat502.rox.marshal.SimpleStruct;

public class Test_XmlRpcMethodResponseJ5 extends TestBase_XmlRpcMethod {
    public Test_XmlRpcMethodResponseJ5(String name) {
        super(name);
    }

    public void testPlatformVersion() {
        assertTrue(XmlRpcUtils.newMarshaller(null) instanceof XmlRpcMarshallerJ5);
    }

    public void testEnumValues() throws Exception {
        @SuppressWarnings("unused")
        SimpleStruct struct = new SimpleStruct("test");
        XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(EnumConstants.BAR);
        String xml = new String(rsp.marshal(), "UTF-8");

        assertXpathEvaluatesTo("BAR", "/methodResponse/params/param/value/string", xml);
    }

    public void testGenericArray() throws Exception {
        List<String> array = new ArrayList<String>() {
            {
                add("hey");
                add("there");
                add("bob");
            }
        };
        XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(array);
        String xml = new String(rsp.marshal(), "UTF-8");

        assertXpathEvaluatesTo("hey", "/methodResponse/params/param/value/array/data/value[1]/string", xml);
        assertXpathEvaluatesTo("there", "/methodResponse/params/param/value/array/data/value[2]/string", xml);
        assertXpathEvaluatesTo("bob", "/methodResponse/params/param/value/array/data/value[3]/string", xml);
    }

    public void testGenericMap() throws Exception {
        Map<String, Integer> struct = new HashMap<>();
        struct.put("foo", new Integer(13));
        struct.put("bar", new Integer(42));
        XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(struct);
        String xml = new String(rsp.marshal(), "UTF-8");

        String member1 = "<member><name>bar</name><value><int>42</int></value></member>";
        String member2 = "<member><name>foo</name><value><int>13</int></value></member>";
        String prefix = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><methodResponse><params><param><value><struct>";
        String suffix = "</struct></value></param></params></methodResponse>";
        String order12 = prefix + member1 + member2 + suffix;
        String order21 = prefix + member1 + member2 + suffix;
        assertTrue(xml.equals(order21) || xml.equals(order12));
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Test_XmlRpcMethodResponseJ5.class);
    }
}
