package at.yawk.fiction.android.download;

/**
 * @author yawkat
 */
interface DownloadTaskHandler<T extends DownloadTask> {
    void run(T task, ProgressListener progressListener) throws Exception;
}
