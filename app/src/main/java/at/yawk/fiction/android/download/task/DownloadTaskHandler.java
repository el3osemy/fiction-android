package at.yawk.fiction.android.download.task;

import at.yawk.fiction.android.download.ProgressListener;

/**
 * @author yawkat
 */
public interface DownloadTaskHandler<T extends DownloadTask> {
    void run(T task, ProgressListener progressListener) throws Exception;
}
