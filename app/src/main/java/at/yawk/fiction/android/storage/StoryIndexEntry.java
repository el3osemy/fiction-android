package at.yawk.fiction.android.storage;

import lombok.Data;

/**
 * @author yawkat
 */
@Data
public class StoryIndexEntry {
    private int totalChapterCount;
    private int readChapterCount;
    private int downloadedChapterCount;
}
