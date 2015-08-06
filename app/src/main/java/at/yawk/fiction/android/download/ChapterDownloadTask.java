package at.yawk.fiction.android.download;

import at.yawk.fiction.android.storage.StoryWrapper;
import lombok.Value;

/**
 * @author yawkat
 */
@Value
public class ChapterDownloadTask implements DownloadTask {
    private final StoryWrapper wrapper;
    private final int index;
}
