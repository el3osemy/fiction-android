package at.yawk.fiction.android.download;

import at.yawk.fiction.Chapter;
import at.yawk.fiction.Story;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.provider.ProviderManager;
import at.yawk.fiction.android.storage.PojoMerger;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author yawkat
 */
@Singleton
class ChapterDownloadTaskHandler implements DownloadTaskHandler<ChapterDownloadTask> {
    @Inject PojoMerger pojoMerger;
    @Inject ProviderManager providerManager;

    @Override
    public void run(ChapterDownloadTask task, ProgressListener progressListener) throws Exception {
        task.getWrapper().setDownloading(task.getIndex(), true);
        try {
            progressListener.progressIndeterminate(false);
            Story storyClone = pojoMerger.clone(task.getWrapper().getStory());
            Chapter chapter = storyClone.getChapters().get(task.getIndex());
            AndroidFictionProvider provider = providerManager.getProvider(storyClone);
            provider.fetchChapter(storyClone, chapter);
            task.getWrapper().updateStory(storyClone);
            progressListener.progressIndeterminate(true);
        } finally {
            task.getWrapper().setDownloading(task.getIndex(), false);
        }
    }
}
