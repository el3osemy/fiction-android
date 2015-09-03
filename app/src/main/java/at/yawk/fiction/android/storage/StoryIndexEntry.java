package at.yawk.fiction.android.storage;

import at.yawk.fiction.android.ProgressStatus;
import lombok.Data;

/**
 * @author yawkat
 */
@Data
public class StoryIndexEntry {
    private int totalChapterCount;
    private int readChapterCount;
    private int downloadedChapterCount;

    public ProgressStatus getReadProgressType() {
        return ProgressStatus.of(getReadChapterCount(), getTotalChapterCount());
    }

    public ProgressStatus getDownloadProgressType() {
        return ProgressStatus.of(getDownloadedChapterCount(), getTotalChapterCount());
    }
}
