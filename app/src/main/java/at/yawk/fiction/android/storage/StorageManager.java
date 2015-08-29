package at.yawk.fiction.android.storage;

import at.yawk.fiction.Story;
import at.yawk.fiction.android.inject.Injector;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.provider.ProviderManager;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@Singleton
public class StorageManager {
    @Inject ObjectStorageManager objectStorageManager;
    @Inject ProviderManager providerManager;
    Index index;

    final LoadingCache<String, StoryWrapper> storyCache;

    @Inject
    StorageManager(Index index) {
        this.index = index;
        index.storageManager = this;

        storyCache = CacheBuilder.newBuilder().softValues().build(CacheLoader.from(input -> {
            StoryWrapper wrapper;
            try {
                StoryWrapper.StoryData data = objectStorageManager.load(StoryWrapper.StoryData.class, input);
                wrapper = new StoryWrapper(input, data);
                wrapper.provider = providerManager.getProvider(data.getStory());
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

    StoryWrapper getStory(String id) {
        return storyCache.getUnchecked(id);
    }

    public Iterable<StoryWrapper> listStories() {
        return listStories(null);
    }

    /**
     * List all stories based on a given condition.
     *
     * Returned stories are not guaranteed to satisfy this condition - this is up to the implementation.
     */
    public Iterable<StoryWrapper> listStories(@Nullable Predicate<StoryIndexEntry> indexFilter) {
        Iterable<String> keys = objectStorageManager.list("story");
        if (indexFilter != null) {
            keys = Iterables.filter(keys, key -> indexFilter.apply(index.findIndexEntry(key)));
        }
        return Iterables.transform(keys, this::getStory);
    }
}
