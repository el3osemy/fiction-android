package at.yawk.fiction.android.download.task;

import at.yawk.fiction.android.download.DownloadManagerMetrics;
import lombok.Value;

/**
 * @author yawkat
 */
@Value
public class TaskUpdateEvent {
    private DownloadManagerMetrics.Task task;
}
