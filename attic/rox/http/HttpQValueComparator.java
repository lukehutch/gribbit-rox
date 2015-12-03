package com.flat502.rox.http;

import java.util.Comparator;

public class HttpQValueComparator implements Comparator<Float> {
	@Override
    public int compare(Float qvalue1, Float qvalue2) {
		if (qvalue1 == null && qvalue2 == null) {
			return 0;
		}
		if (qvalue1 == null) {
			// a > b
			return 1;
		}
		if (qvalue2 == null) {
			// a < b
			return -1;
		}
		return qvalue1.compareTo(qvalue2);
	}
}
