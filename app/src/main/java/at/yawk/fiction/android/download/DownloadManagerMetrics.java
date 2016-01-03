package at.yawk.fiction.android.download;

import com.google.inject.ImplementedBy;
import java.util.Collection;
import javax.annotation.Nullable;

/**
 * @author yawkat
 */
@ImplementedBy(DownloadManagerImpl.class)
public interface DownloadManagerMetrics {
    Collection<Task> getTasks();

    interface Task {
        boolean isRunning();

        String getName();

        @Nullable
        String getStatusMessage();

        long getCurrentProgress();

        /**
         * @return The current maximum progress or <code>-1</code> if the progress is indeterminate.
         */
        long getMaxProgress();

        void cancel();
    }
}
