package org.slf4j.impl;

/**
 * @author yawkat
 */
public interface AndroidLoggingProvider {
    int getLevel();

    void log(String tag, int level, String msg);
}
