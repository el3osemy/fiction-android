package at.yawk.fiction.android.download.task;

import java.util.List;

/**
 * @author yawkat
 */
public interface SplittableDownloadTask extends DownloadTask {
    List<DownloadTask> getTasks();
}
