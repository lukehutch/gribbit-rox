package com.flat502.rox.log.log4j;

import org.apache.logging.log4j.LogManager;

import com.flat502.rox.log.Log;

/**
 * A <a href="http://logging.apache.org/log4j">Log4J</a> adapter.
 * <p>
 * This class implements the {@link com.flat502.rox.log.Log} interface and defers all logging to the Log$J
 * {@link org.apache.logging.log4j.Logger} class.
 * <p>
 * The <code>context</code> parameter on each of the methods on the {@link com.flat502.rox.log.Log} interface is
 * used to retrieve the appropriate Log4J logger in all cases exception {@link #logWarn()} and {@link #logError()}.
 * Those methods always return <code>true</code> (Log4J always assumes warnings and errors should be logged).
 */
public class Log4JAdapter implements Log {
    private String name;

    public Log4JAdapter(String name) {
        this.name = name;
    }

    @Override
    public boolean logTrace() {
        return LogManager.getLogger(name).isTraceEnabled();
    }

    @Override
    public boolean logDebug() {
        return LogManager.getLogger(name).isDebugEnabled();
    }

    @Override
    public boolean logInfo() {
        return LogManager.getLogger(name).isInfoEnabled();
    }

    /**
     * @return Always returns <code>true</code>
     */
    @Override
    public boolean logWarn() {
        return true;
    }

    /**
     * @return Always returns <code>true</code>
     */
    @Override
    public boolean logError() {
        return true;
    }

    @Override
    public void trace(String msg) {
        LogManager.getLogger(name).trace(msg);
    }

    @Override
    public void trace(String msg, Throwable e) {
        LogManager.getLogger(name).trace(msg, e);
    }

    @Override
    public void debug(String msg) {
        LogManager.getLogger(name).debug(msg);
    }

    @Override
    public void debug(String msg, Throwable e) {
        LogManager.getLogger(name).debug(msg, e);
    }

    @Override
    public void info(String msg) {
        LogManager.getLogger(name).info(msg);
    }

    @Override
    public void info(String msg, Throwable e) {
        LogManager.getLogger(name).info(msg, e);
    }

    @Override
    public void warn(String msg) {
        LogManager.getLogger(name).warn(msg);
    }

    @Override
    public void warn(String msg, Throwable e) {
        LogManager.getLogger(name).warn(msg, e);
    }

    @Override
    public void error(String msg) {
        LogManager.getLogger(name).error(msg);
    }

    @Override
    public void error(String msg, Throwable e) {
        LogManager.getLogger(name).error(msg, e);
    }
}
