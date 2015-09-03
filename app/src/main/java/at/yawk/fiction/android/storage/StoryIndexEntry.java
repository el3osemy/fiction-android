package at.yawk.fiction.android.storage;

import at.yawk.fiction.android.ProgressStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * @author yawkat
 */
@Data
public class StoryIndexEntry {
    private int totalChapterCount;
    private int readChapterCount;
    private int downloadedChapterCount;

    @JsonIgnore
    public ProgressStatus getReadProgressType() {
        return ProgressStatus.of(getReadChapterCount(), getTotalChapterCount());
    }

    @JsonIgnore
    public ProgressStatus getDownloadProgressType() {
        return ProgressStatus.of(getDownloadedChapterCount(), getTotalChapterCount());
    }
}
