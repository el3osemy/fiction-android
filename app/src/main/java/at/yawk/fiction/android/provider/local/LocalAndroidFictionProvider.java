package at.yawk.fiction.android.provider.local;

import at.yawk.fiction.FictionProvider;
import at.yawk.fiction.Pageable;
import at.yawk.fiction.SearchQuery;
import at.yawk.fiction.Story;
import at.yawk.fiction.android.inject.BaseModule;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.storage.StorageManager;
import at.yawk.fiction.android.storage.StoryWrapper;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import dagger.Module;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@Singleton
public class LocalAndroidFictionProvider extends AndroidFictionProvider {
    @Inject StorageManager storageManager;

    public LocalAndroidFictionProvider() {
        super("local", "Local",
              LocalSearchQuery.class);
    }

    @Override
    public FictionProvider getFictionProvider() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LocalSearchQuery createQuery() {
        return new LocalSearchQuery();
    }

    @Override
    public Pageable<StoryWrapper> searchWrappers(SearchQuery searchQuery) {
        LocalSearchQuery localSearchQuery = (LocalSearchQuery) searchQuery;
        log.info("Query: {}", localSearchQuery);
        return i -> {
            Pageable.Page<StoryWrapper> page = new Pageable.Page<>();
            page.setLast(true);
            page.setPageCount(1);
            if (i != 0) {
                page.setEntries(Collections.emptyList());
                return page;
            }

            List<StoryWrapper> stories = new ArrayList<>();
            for (StoryWrapper wrapper : storageManager.listStories()) {
                if (localSearchQuery.accept(wrapper)) {
                    stories.add(wrapper);
                }
            }
            Collections.sort(stories, localSearchQuery.getOrder());
            page.setEntries(stories);
            return page;
        };
    }

    @Override
    public QueryEditorFragment<?> createQueryEditorFragment() {
        return new LocalQueryEditorFragment();
    }

    @Override
    public String getStoryId(Story story, String separator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getTags(Story story) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object createModule() {
        return new M();
    }

    @Module(addsTo = BaseModule.class, injects = LocalAndroidFictionProvider.class)
    static class M {}
}
