package at.yawk.fiction.android.download.task;

import at.yawk.fiction.Story;
import at.yawk.fiction.android.download.ProgressListener;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.storage.PojoMerger;
import javax.inject.Inject;

/**
 * @author yawkat
 */
public class StoryUpdateTaskHandler implements DownloadTaskHandler<StoryUpdateTask> {
    @Inject PojoMerger pojoMerger;

    @Override
    public void run(StoryUpdateTask task, ProgressListener progressListener) throws Exception {
        progressListener.progressIndeterminate(false);
        Story storyClone = pojoMerger.clone(task.getWrapper().getStory());
        AndroidFictionProvider provider = task.getWrapper().getProvider();
        provider.fetchStory(storyClone);
        task.getWrapper().updateStory(storyClone);
        progressListener.progressIndeterminate(true);
    }
}
