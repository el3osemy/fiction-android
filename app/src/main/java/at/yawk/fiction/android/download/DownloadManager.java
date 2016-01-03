package at.yawk.fiction.android.download;

import com.google.inject.ImplementedBy;

/**
 * @author yawkat
 */
@ImplementedBy(DownloadManagerImpl.class)
public interface DownloadManager {
    void enqueue(Task task);
}
