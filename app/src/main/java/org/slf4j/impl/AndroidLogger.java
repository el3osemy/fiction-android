package org.slf4j.impl;

import android.util.Log;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
final class AndroidLogger implements Logger {
    private final String name;
    private final AndroidLoggingProvider provider;

    @Override
    public String getName() {
        return name;
    }

    @SuppressWarnings("UnusedParameters")
    private String getTag(Marker marker) {
        return name;
    }

    @SuppressWarnings("UnusedParameters")
    private boolean isEnabled(Marker marker, int level) {
        return provider.getLevel() <= level;
    }

    private void doLog(Marker marker, int level, String msg, Throwable throwable) {
        if (throwable != null) {
            msg = msg + '\n' + Log.getStackTraceString(throwable);
        }

        provider.log(getTag(marker), level, msg);
    }

    private void log(Marker marker, int level, String msg) {
        if (isEnabled(marker, level)) {
            doLog(marker, level, msg, null);
        }
    }

    private void log(Marker marker, int level, String format, Object arg) {
        if (isEnabled(marker, level)) {
            FormattingTuple tuple = MessageFormatter.format(format, arg);
            doLog(marker, level, tuple.getMessage(), tuple.getThrowable());
        }
    }

    private void log(Marker marker, int level, String format, Object arg1, Object arg2) {
        if (isEnabled(marker, level)) {
            FormattingTuple tuple = MessageFormatter.format(format, arg1, arg2);
            doLog(marker, level, tuple.getMessage(), tuple.getThrowable());
        }
    }

    private void log(Marker marker, int level, String format, Object[] argArray) {
        if (isEnabled(marker, level)) {
            FormattingTuple tuple = MessageFormatter.arrayFormat(format, argArray);
            doLog(marker, level, tuple.getMessage(), tuple.getThrowable());
        }
    }

    private void log(Marker marker, int level, String msg, Throwable t) {
        if (isEnabled(marker, level)) {
            doLog(marker, level, msg, t);
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return isEnabled(null, Log.VERBOSE);
    }

    @Override
    public void trace(String msg) {
        log(null, Log.VERBOSE, msg);
    }

    @Override
    public void trace(String format, Object arg) {
        log(null, Log.VERBOSE, format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        log(null, Log.VERBOSE, format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        log(null, Log.VERBOSE, format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        log(null, Log.VERBOSE, msg, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return isEnabled(marker, Log.VERBOSE);
    }

    @Override
    public void trace(Marker marker, String msg) {
        log(marker, Log.VERBOSE, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        log(marker, Log.VERBOSE, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        log(marker, Log.VERBOSE, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        log(marker, Log.VERBOSE, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        log(marker, Log.VERBOSE, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return isEnabled(null, Log.DEBUG);
    }

    @Override
    public void debug(String msg) {
        log(null, Log.DEBUG, msg);
    }

    @Override
    public void debug(String format, Object arg) {
        log(null, Log.DEBUG, format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        log(null, Log.DEBUG, format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        log(null, Log.DEBUG, format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        log(null, Log.DEBUG, msg, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return isEnabled(marker, Log.DEBUG);
    }

    @Override
    public void debug(Marker marker, String msg) {
        log(marker, Log.DEBUG, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        log(marker, Log.DEBUG, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        log(marker, Log.DEBUG, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        log(marker, Log.DEBUG, format, arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        log(marker, Log.DEBUG, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return isEnabled(null, Log.INFO);
    }

    @Override
    public void info(String msg) {
        log(null, Log.INFO, msg);
    }

    @Override
    public void info(String format, Object arg) {
        log(null, Log.INFO, format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        log(null, Log.INFO, format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        log(null, Log.INFO, format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        log(null, Log.INFO, msg, t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return isEnabled(marker, Log.INFO);
    }

    @Override
    public void info(Marker marker, String msg) {
        log(marker, Log.INFO, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        log(marker, Log.INFO, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        log(marker, Log.INFO, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        log(marker, Log.INFO, format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        log(marker, Log.INFO, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return isEnabled(null, Log.WARN);
    }

    @Override
    public void warn(String msg) {
        log(null, Log.WARN, msg);
    }

    @Override
    public void warn(String format, Object arg) {
        log(null, Log.WARN, format, arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        log(null, Log.WARN, format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        log(null, Log.WARN, format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        log(null, Log.WARN, msg, t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return isEnabled(marker, Log.WARN);
    }

    @Override
    public void warn(Marker marker, String msg) {
        log(marker, Log.WARN, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        log(marker, Log.WARN, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        log(marker, Log.WARN, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        log(marker, Log.WARN, format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        log(marker, Log.WARN, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return isEnabled(null, Log.ERROR);
    }

    @Override
    public void error(String msg) {
        log(null, Log.ERROR, msg);
    }

    @Override
    public void error(String format, Object arg) {
        log(null, Log.ERROR, format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        log(null, Log.ERROR, format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        log(null, Log.ERROR, format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        log(null, Log.ERROR, msg, t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return isEnabled(marker, Log.ERROR);
    }

    @Override
    public void error(Marker marker, String msg) {
        log(marker, Log.ERROR, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        log(marker, Log.ERROR, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        log(marker, Log.ERROR, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        log(marker, Log.ERROR, format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        log(marker, Log.ERROR, msg, t);
    }
}
