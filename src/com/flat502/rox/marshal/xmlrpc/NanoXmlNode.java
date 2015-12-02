package com.flat502.rox.marshal.xmlrpc;

import java.util.Enumeration;
import java.util.Iterator;

import net.n3.nanoxml.IXMLElement;

class NanoXmlNode implements XmlNode {
	private IXMLElement element;

	public NanoXmlNode(IXMLElement element) {
		this.element = element;
	}

	@Override
    public String getFullName() {
		return this.element.getFullName();
	}

	@Override
    public String getContent() {
		String content = this.element.getContent();
		if (content == null) {
			return "";
		}
		return content;
	}

	@Override
    public int getChildrenCount() {
		return this.element.getChildrenCount();
	}

	@Override
    public XmlNode getChildAtIndex(int index) {
		return new NanoXmlNode(this.element.getChildAtIndex(index));
	}

	@Override
    public Iterator<XmlNode> enumerateChildren() {
		@SuppressWarnings("unchecked")
        Enumeration<IXMLElement> children = this.element.enumerateChildren();
        return new NanoIterator(children);
	}

	private class NanoIterator implements Iterator<XmlNode> {
		private Enumeration<IXMLElement> enumeration;

		public NanoIterator(Enumeration<IXMLElement> enumeration) {
			this.enumeration = enumeration;
		}

		@Override
        public boolean hasNext() {
			return this.enumeration.hasMoreElements();
		}

		@Override
        public NanoXmlNode next() {
			return new NanoXmlNode(this.enumeration.nextElement());
		}

		@Override
        public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
