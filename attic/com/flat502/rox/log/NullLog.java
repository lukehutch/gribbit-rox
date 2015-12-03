package com.flat502.rox.log;


/**
 * A {@link com.flat502.rox.log.Log} implementation that swallows
 * all logging and returns false for all logging level checks.
 * <p>
 * This simplifies writing code that would otherwise have to check
 * for a null log handle everywhere.
 */
public class NullLog implements Log {
	@Override
    public void trace(String msg) {
	}

	@Override
    public void trace(String msg, Throwable e) {
	}

	@Override
    public void debug(String msg) {
	}

	@Override
    public void debug(String msg, Throwable e) {
	}

	@Override
    public void info(String msg) {
	}

	@Override
    public void info(String msg, Throwable e) {
	}

	@Override
    public void warn(String msg) {
	}

	@Override
    public void warn(String msg, Throwable e) {
	}

	@Override
    public void error(String msg) {
	}

	@Override
    public void error(String msg, Throwable e) {
	}

	@Override
    public boolean logTrace() {
		return false;
	}

	@Override
    public boolean logDebug() {
		return false;
	}

	@Override
    public boolean logInfo() {
		return false;
	}

	@Override
    public boolean logWarn() {
		return false;
	}

	@Override
    public boolean logError() {
		return false;
	}
}
