package com.flat502.rox.utils;

import java.util.HashMap;
import java.util.Map;

import com.flat502.rox.log.Log;
import com.flat502.rox.log.LogFactory;

public class LoggingProfiler implements Profiler {
	private static Log log = LogFactory.getLog(LoggingProfiler.class);

	private Map<Object, Entry> begunMap = new HashMap<>();
	
	private class Entry {
		@SuppressWarnings("unused")
        private String operation;
		private long start;

		public Entry(String operation) {
			this.operation = operation;
			this.start = System.nanoTime();
		}
		
		public double elapsed() {
			return (System.nanoTime() - start) / 1000000.0;
		}
	}
	
	@Override
    public void begin(long id, String operation) {
		this.begunMap.put(id, new Entry(operation));
	}

	@Override
    public void end(long id, String operation) {
		Entry entry = this.begunMap.remove(id);
		if (entry != null) {
			log.info("DURATION: "  + operation + ": " + entry.elapsed() + "ms");
		}
	}

	@Override
    public void count(long id, String operation) {
		log.info("COUNT: " + operation);
	}
}
