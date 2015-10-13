package org.slf4j.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
public class AndroidLoggerFactory implements ILoggerFactory {
    private final ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap<>();
    private final AndroidLoggingProvider provider = new AndroidLoggingProviderImpl();

    @Override
    public Logger getLogger(String name) {
        Logger logger = loggers.get(name);
        if (logger == null) {
            logger = new AndroidLogger(name, provider);
            if (loggers.putIfAbsent(name, logger) != null) {
                logger = loggers.get(name);
            }
        }
        return logger;
    }

}
