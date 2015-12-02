package com.flat502.rox.marshal.xmlrpc;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import junit.framework.TestCase;

public abstract class TestBase_Unmarshaller extends TestCase {
	public TestBase_Unmarshaller(String name) {
		super(name);
	}
	
	protected void assertEquals(byte[] expected, byte[] actual) {
		if (expected == null ^ actual == null) {
			fail();
		}
		if (expected == actual) {
			fail();
		}
		if (expected.length != actual.length) {
			fail();
		}
		for (int i = 0; i < actual.length; i++) {
			if (expected[i] != actual[i]) {
				fail();
			}
		}
	}

	protected Date newDate(int year, int month, int date, int hour,
			int min, int sec) {
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month - 1);
		cal.set(Calendar.DATE, date);
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, min);
		cal.set(Calendar.SECOND, sec);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	protected String toString(String[] lines) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < lines.length; i++) {
			sb.append(lines[i]);
		}
		return sb.toString();
	}
}
