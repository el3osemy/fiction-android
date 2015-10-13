package org.slf4j.impl;

import android.util.Log;
import at.yawk.fiction.android.storage.RootFile;
import com.google.common.base.Charsets;
import java.io.*;
import lombok.SneakyThrows;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

/**
 * @author yawkat
 */
class AndroidLoggingProviderImpl implements AndroidLoggingProvider {
    private static final int FILE_LOG_LEVEL = Log.INFO;

    private final BufferedWriter fileOutput;

    @SneakyThrows
    AndroidLoggingProviderImpl() {
        File root = RootFile.makeRootDirectory();
        File logDirectory = new File(root, "log");
        //noinspection ResultOfMethodCallIgnored
        logDirectory.mkdirs();

        String timestamp = LocalDateTime.now(DateTimeZone.UTC).toString();
        File logFile = new File(logDirectory, timestamp + ".log");

        fileOutput = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile), Charsets.UTF_8));
    }

    @Override
    public int getLevel() {
        return Log.VERBOSE;
    }

    @Override
    public void log(String tag, int level, String msg) {
        Log.println(level, tag, msg);
        if (level >= FILE_LOG_LEVEL) {
            try {
                writeToLog(tag, level, msg);
            } catch (IOException e) {
                Log.e("LOG", "Could not write log to file", e);
            }
        }
    }

    private synchronized void writeToLog(String tag, int level, String msg) throws IOException {
        fileOutput.write('[');
        fileOutput.write(getLevelName(level));
        fileOutput.write("] [");
        fileOutput.write(tag);
        fileOutput.write("] ");
        fileOutput.write(msg);
        fileOutput.write('\n');
        fileOutput.flush();
    }

    private static String getLevelName(int level) {
        switch (level) {
        case Log.VERBOSE:
            return "VERBOSE";
        case Log.DEBUG:
            return "  DEBUG";
        case Log.INFO:
            return "   INFO";
        case Log.WARN:
            return "   WARN";
        case Log.ERROR:
            return "  ERROR";
        case Log.ASSERT:
            return " ASSERT";
        default:
            return "UNKNOWN";
        }
    }
}
