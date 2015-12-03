package com.flat502.rox.marshal.xmlrpc;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;

/**
 * A simple replacement for java.util.Stack that isn't synchronized.
 *
 */
class Stack {
	private List<Object> items = new ArrayList<>();

	public Object peek() throws EmptyStackException {
		try {
			return items.get(size() - 1);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new EmptyStackException();
		}
	}

	public Object safePeek() {
		try {
			return items.get(size() - 1);
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}

	public void clear() {
		items.clear();
	}

	public Object push(Object obj) {
		items.add(obj);
		return obj;
	}

	public Object pop() throws EmptyStackException {
		Object item = peek();
		items.remove(size() - 1);
		return item;
	}

	public int size() {
		return items.size();
	}

	@Override
    public String toString() {
		return this.items.toString();
	}
}
