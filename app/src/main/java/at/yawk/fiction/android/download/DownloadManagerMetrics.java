package at.yawk.fiction.android.download;

import java.util.Collection;

/**
 * @author yawkat
 */
public interface DownloadManagerMetrics {
    Collection<Task> getTasks();

    interface Task {
        boolean isRunning();

        String getName();

        long getCurrentProgress();

        /**
         * @return The current maximum progress or <code>-1</code> if the progress is indeterminate.
         */
        long getMaxProgress();

        void cancel();
    }
}
