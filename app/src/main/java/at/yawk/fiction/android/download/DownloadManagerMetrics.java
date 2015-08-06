package at.yawk.fiction.android.download;

import java.util.List;

/**
 * @author yawkat
 */
public interface DownloadManagerMetrics {
    List<Task> getTasks();

    interface Task {
        boolean isRunning();

        String getName();

        long getCurrentProgress();

        /**
         * @return The current maximum progress or <code>-1</code> if the progress is indeterminate.
         */
        long getMaxProgress();
    }
}
