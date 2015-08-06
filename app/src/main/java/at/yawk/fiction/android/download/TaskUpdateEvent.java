package at.yawk.fiction.android.download;

import lombok.Value;

/**
 * @author yawkat
 */
@Value
public class TaskUpdateEvent {
    private DownloadManagerMetrics.Task task;
}
