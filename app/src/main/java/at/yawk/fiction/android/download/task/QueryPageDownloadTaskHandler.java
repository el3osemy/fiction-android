package at.yawk.fiction.android.download.task;

import at.yawk.fiction.Pageable;
import at.yawk.fiction.SearchQuery;
import at.yawk.fiction.android.download.ProgressListener;
import at.yawk.fiction.android.provider.ProviderManager;
import at.yawk.fiction.android.storage.OfflineQueryManager;
import at.yawk.fiction.android.storage.StoryWrapper;
import javax.inject.Inject;

/**
 * @author yawkat
 */
public class QueryPageDownloadTaskHandler implements DownloadTaskHandler<QueryPageDownloadTask> {
    @Inject OfflineQueryManager offlineQueryManager;
    @Inject ProviderManager providerManager;

    @Override
    public void run(QueryPageDownloadTask task, ProgressListener progressListener) throws Exception {
        progressListener.progressIndeterminate(false);
        SearchQuery query = task.getWrapper().getQuery();

        // todo: cache the pageable in case the provider does work in searchWrappers()
        Pageable.Page<StoryWrapper> page = providerManager.getProvider(query)
                .searchWrappers(query)
                .getPage(task.getPage());

        offlineQueryManager.save(task.getWrapper(), task.getPage(), page);
        progressListener.progressIndeterminate(true);
    }
}
