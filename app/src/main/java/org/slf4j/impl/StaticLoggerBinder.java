package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

/**
 * @author yawkat
 */
@SuppressWarnings("unused")
public class StaticLoggerBinder implements LoggerFactoryBinder {
    private static final StaticLoggerBinder singleton = new StaticLoggerBinder();

    public static final String REQUESTED_API_VERSION = "1.7";

    public static StaticLoggerBinder getSingleton() {
        return singleton;
    }

    private final AndroidLoggerFactory factory = new AndroidLoggerFactory();

    @Override
    public ILoggerFactory getLoggerFactory() {
        return factory;
    }

    @Override
    public String getLoggerFactoryClassStr() {
        return AndroidLoggerFactory.class.getName();
    }
}
