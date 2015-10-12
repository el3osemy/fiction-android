package at.yawk.fiction.android.storage;

import at.yawk.fiction.Story;
import at.yawk.fiction.android.Consumer;
import at.yawk.fiction.android.inject.Injector;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.provider.ProviderManager;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.j256.ormlite.dao.CloseableIterable;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.stmt.QueryBuilder;
import java.sql.SQLException;
import java.util.Iterator;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@Singleton
public class StoryManager {
    private final FileSystemStorage fileSystemStorage;
    private final ProviderManager providerManager;
    private final Index index;
    private final LoadingCache<String, StoryWrapper> storyCache;

    @Inject
    StoryManager(FileSystemStorage fileSystemStorage, ProviderManager providerManager, Index index) {
        this.fileSystemStorage = fileSystemStorage;
        this.providerManager = providerManager;
        this.index = index;
        index.storyManager = this;

        storyCache = CacheBuilder.newBuilder().softValues().build(CacheLoader.from(input -> {
            StoryWrapper wrapper;
            try {
                StoryWrapper.StoryData data = fileSystemStorage.load(StoryWrapper.StoryData.class, input);
                wrapper = new StoryWrapper(input, data);
                wrapper.provider = this.providerManager.getProvider(data.getStory());
                wrapper.bakeDownloadedChapterCount();
                wrapper.bakeReadChapterCount();
            } catch (NotFoundException e) {
                wrapper = new StoryWrapper(input, new StoryWrapper.StoryData());
            } catch (UnreadableException e) {
                throw new RuntimeException(e);
            }
            Injector.inject(wrapper);
            return wrapper;
        }));
    }

    public void initialize() {
        index.initStoryManager();
    }

    String getObjectId(Story story) {
        StringBuilder builder = new StringBuilder("story/");
        AndroidFictionProvider provider = providerManager.getProvider(story);
        builder.append(provider.getId()).append('/');
        builder.append(provider.getStoryId(story, "/"));

        return builder.toString();
    }

    /**
     * Get the saved story that has the same identifier as the given one, merging changes from the passed story into
     * the saved story if necessary.
     */
    public StoryWrapper mergeStory(Story story) {
        StoryWrapper wrapper = getStory(story);
        wrapper.updateStory(story);
        return wrapper;
    }

    public StoryWrapper getStory(Story story) {
        return getStory(getObjectId(story));
    }

    public StoryWrapper getStory(String id) {
        return storyCache.getUnchecked(id);
    }

    public Iterable<StoryWrapper> listStories() {
        return Iterables.transform(fileSystemStorage.list("story"), this::getStory);
    }

    /**
     * List all stories based on a given condition.
     *
     * Returned stories are not guaranteed to satisfy this condition - this is up to the implementation.
     */
    public CloseableIterable<StoryWrapper> listStories(Consumer<QueryBuilder<StoryIndexEntry, String>> queryBuilder) {
        QueryBuilder<StoryIndexEntry, String> builder = index.indexDao.queryBuilder();
        queryBuilder.consume(builder);
        return new CloseableIterable<StoryWrapper>() {
            @Override
            @SneakyThrows(SQLException.class)
            public CloseableIterator<StoryWrapper> closeableIterator() {
                CloseableIterator<StoryIndexEntry> handle = builder.iterator();
                return new TransformedCloseableIterator<StoryIndexEntry, StoryWrapper>(handle) {
                    @Override
                    protected StoryWrapper map(StoryIndexEntry entry) {
                        return getStory(entry.getStoryId());
                    }
                };
            }

            @Override
            public Iterator<StoryWrapper> iterator() {
                return closeableIterator();
            }
        };
    }
}
