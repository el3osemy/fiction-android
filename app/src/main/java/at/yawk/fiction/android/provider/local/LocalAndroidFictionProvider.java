package at.yawk.fiction.android.provider.local;

import at.yawk.fiction.FictionProvider;
import at.yawk.fiction.Pageable;
import at.yawk.fiction.SearchQuery;
import at.yawk.fiction.Story;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.provider.Provider;
import at.yawk.fiction.android.storage.StoryIndexEntry;
import at.yawk.fiction.android.storage.StoryManager;
import at.yawk.fiction.android.storage.StoryWrapper;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import com.j256.ormlite.stmt.Where;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@Singleton
@Provider(priority = 0)
public class LocalAndroidFictionProvider extends AndroidFictionProvider {
    private static final long LOCAL_QUERY_PAGE_SIZE = 20;

    @Inject StoryManager storyManager;

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
        log.debug("Query: {}", localSearchQuery);
        return i -> {
            Pageable.Page<StoryWrapper> page = new Pageable.Page<>();

            List<StoryWrapper> stories = new ArrayList<>();
            for (StoryWrapper wrapper : storyManager.listStories(builder -> {
                Where<StoryIndexEntry, String> where = builder.where();
                localSearchQuery.apply(builder, where);

                try {
                    builder.offset(LOCAL_QUERY_PAGE_SIZE * i);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                builder.limit(LOCAL_QUERY_PAGE_SIZE);
            })) {
                if (localSearchQuery.accept(wrapper)) {
                    stories.add(wrapper);
                }
            }
            page.setEntries(stories);
            page.setLast(stories.size() < LOCAL_QUERY_PAGE_SIZE);
            return page;
        };
    }

    @Override
    public boolean isQueryOfflineCacheable(SearchQuery query) {
        return false;
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
