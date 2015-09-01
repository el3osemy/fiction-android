package at.yawk.fiction.android.download.task;

import at.yawk.fiction.android.storage.StoryWrapper;
import lombok.Value;

/**
 * @author yawkat
 */
@Value
public class StoryUpdateTask implements DownloadTask {
    private final StoryWrapper wrapper;

    @Override
    public String getName() {
        return "Updating '" + wrapper.getStory().getTitle() + "'";
    }
}
