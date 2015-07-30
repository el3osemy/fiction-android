package at.yawk.fiction.android.provider.local;

import at.yawk.fiction.FictionProvider;
import at.yawk.fiction.Pageable;
import at.yawk.fiction.SearchQuery;
import at.yawk.fiction.Story;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.provider.ProviderContext;
import at.yawk.fiction.android.storage.StorageManager;
import at.yawk.fiction.android.storage.StoryWrapper;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author yawkat
 */
public class LocalAndroidFictionProvider extends AndroidFictionProvider {
    private StorageManager storageManager;

    @Override
    public String getName() {
        return "Local";
    }

    @Override
    public String getId() {
        return "local";
    }

    @Override
    public void init(ProviderContext context) {
        super.init(context);
        storageManager = context.getStorageManager();
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
    public Pageable<? extends Story> search(SearchQuery searchQuery) {
        LocalSearchQuery localSearchQuery = (LocalSearchQuery) searchQuery;
        return i -> {
            Pageable.Page<Story> page = new Pageable.Page<>();
            page.setLast(true);
            page.setPageCount(1);
            if (i != 0) {
                page.setEntries(Collections.emptyList());
                return page;
            }

            List<Story> stories = new ArrayList<>();
            for (StoryWrapper wrapper : storageManager.listStories()) {
                if (localSearchQuery.accept(wrapper)) {
                    stories.add(wrapper.getStory());
                }
            }
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
}
