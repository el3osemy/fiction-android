package at.yawk.fiction.android.download.task;

import at.yawk.fiction.android.storage.StoryWrapper;
import lombok.Value;

/**
 * @author yawkat
 */
@Value
public class ChapterDownloadTask implements DownloadTask {
    private final StoryWrapper wrapper;
    private final int index;

    @Override
    public String getName() {
        return "'" + wrapper.getStory().getTitle() + "' chapter " + index;
    }
}
