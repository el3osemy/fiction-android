package org.slf4j.impl;

import android.util.Log;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
public class AndroidLoggerFactory implements ILoggerFactory {
    private final ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap<>();
    @Setter
    private AndroidLoggingProvider provider = new AndroidLoggingProvider() {
        @Override
        public int getLevel() {
            return Log.VERBOSE;
        }

        @Override
        public void log(String tag, int level, String msg) {
            Log.println(level, tag, msg);
        }
    };

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
