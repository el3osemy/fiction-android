package at.yawk.fiction.android.download.task;

import at.yawk.fiction.android.storage.StoryWrapper;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
public class StoryListUpdateTask implements SplittableDownloadTask {
    private final List<StoryWrapper> stories;

    @Override
    public List<DownloadTask> getTasks() {
        return Lists.transform(stories, StoryUpdateTask::new);
    }

    @Override
    public String getName() {
        if (stories.size() == 1) {
            return "'" + stories.get(0).getStory().getTitle() + "'";
        }
        return "Updating " + stories.size() + " stories";
    }
}
