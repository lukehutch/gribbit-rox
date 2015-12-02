package com.flat502.rox.log;


/**
 * An abstract {@link com.flat502.rox.log.Log} implementation intended
 * to simplify developing implementations.
 */
public abstract class AbstractLog implements Log {
	private Level level;

	public AbstractLog(Level level) {
		this.level = level;
	}

	@Override
    public void trace(String msg) {
		if (!this.logTrace()) {
			return;
		}
		this.traceImpl(msg, null);
	}

	@Override
    public void trace(String msg, Throwable e) {
		if (!this.logTrace()) {
			return;
		}
		this.traceImpl(msg, e);
	}

	@Override
    public void debug(String msg) {
		if (!this.logDebug()) {
			return;
		}
		this.debugImpl(msg, null);
	}

	@Override
    public void debug(String msg, Throwable e) {
		if (!this.logDebug()) {
			return;
		}
		this.debugImpl(msg, e);
	}

	@Override
    public void info(String msg) {
		if (!this.logInfo()) {
			return;
		}
		this.traceImpl(msg, null);
	}

	@Override
    public void info(String msg, Throwable e) {
		if (!this.logInfo()) {
			return;
		}
		this.traceImpl(msg, e);
	}

	@Override
    public void warn(String msg) {
		if (!this.logWarn()) {
			return;
		}
		this.warnImpl(msg, null);
	}

	@Override
    public void warn(String msg, Throwable e) {
		if (!this.logWarn()) {
			return;
		}
		this.warnImpl(msg, e);
	}

	@Override
    public void error(String msg) {
		if (!this.logError()) {
			return;
		}
		this.errorImpl(msg, null);
	}

	@Override
    public void error(String msg, Throwable e) {
		if (!this.logError()) {
			return;
		}
		this.errorImpl(msg, e);
	}

	@Override
    public boolean logTrace() {
		return this.level.at(Level.TRACE); 
	}

	@Override
    public boolean logDebug() {
		return this.level.at(Level.DEBUG); 
	}

	@Override
    public boolean logInfo() {
		return this.level.at(Level.INFO); 
	}

	@Override
    public boolean logWarn() {
		return this.level.at(Level.WARNING); 
	}

	@Override
    public boolean logError() {
		return this.level.at(Level.ERROR); 
	}

	protected abstract void traceImpl(String msg, Throwable e);

	protected abstract void debugImpl(String msg, Throwable e);

	protected abstract void infoImpl(String msg, Throwable e);

	protected abstract void warnImpl(String msg, Throwable e);

	protected abstract void errorImpl(String msg, Throwable e);
}
