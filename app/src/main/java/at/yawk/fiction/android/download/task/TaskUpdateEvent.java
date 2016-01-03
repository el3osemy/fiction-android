package at.yawk.fiction.android.download.task;

import at.yawk.fiction.android.download.DownloadManagerMetrics;
import lombok.Value;

/**
 * @author yawkat
 */
@Value
public class TaskUpdateEvent {
    private final DownloadManagerMetrics.Task task;

    public TaskUpdateEvent(DownloadManagerMetrics.Task task) {
        this.task = task;
    }
}
