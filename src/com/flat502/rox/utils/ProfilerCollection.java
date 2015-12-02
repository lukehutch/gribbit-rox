package com.flat502.rox.utils;

import java.util.HashSet;
import java.util.Set;

public class ProfilerCollection implements Profiler {
	private Set<Profiler> profilers = null;

	public void addProfiler(Profiler p) {
		if (this.profilers == null) {
			this.profilers = new HashSet<Profiler>();
		}
		this.profilers.add(p);
	}

	@Override
    public void begin(long id, String operation) {
		if (this.profilers == null) {
			return;
		}
		for (Profiler p : this.profilers) {
			p.begin(id, operation);
		}
	}

	@Override
    public void end(long id, String operation) {
		if (this.profilers == null) {
			return;
		}
		for (Profiler p : this.profilers) {
			p.end(id, operation);
		}
	}

	@Override
    public void count(long id, String operation) {
		if (this.profilers == null) {
			return;
		}
		for (Profiler p : this.profilers) {
			p.count(id, operation);
		}
	}
}
