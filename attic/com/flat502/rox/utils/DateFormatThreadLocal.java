package com.flat502.rox.utils;

import java.text.DateFormat;

// TODO: Document
public class DateFormatThreadLocal {
	private ThreadLocal<DateFormat> threadLocal;

	public DateFormatThreadLocal(final DateFormat formatter) {
		this.threadLocal = new ThreadLocal<DateFormat>() {
			@Override
            protected DateFormat initialValue() {
				return (DateFormat) formatter.clone();
			};
		};
	}
	
	public DateFormat getFormatter() {
		return this.threadLocal.get();
	}
}
