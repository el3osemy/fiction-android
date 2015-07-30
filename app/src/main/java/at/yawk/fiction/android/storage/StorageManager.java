package at.yawk.fiction.android.storage;

import at.yawk.fiction.Story;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.provider.ProviderManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.File;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
public class StorageManager {
    final ProviderManager providerManager;
    final ObjectMapper objectMapper;
    final ObjectStorageManager objectStorage;
    final LoadingCache<String, StoryWrapper> storyCache;
    @Getter final PojoMerger pojoMerger;
    final TextStorage textStorage;
    final QueryManager queryManager;

    public StorageManager(ProviderManager providerManager, ObjectMapper objectMapper, File root) {
        this.providerManager = providerManager;
        this.objectMapper = objectMapper;
        this.objectStorage = new ObjectStorageManager(root, objectMapper);
        this.storyCache = CacheBuilder.newBuilder().softValues().build(CacheLoader.from(input -> {
            StoryWrapper wrapper;
            try {
                wrapper = objectStorage.load(StoryWrapper.class, input);
            } catch (NotFoundException e) {
                wrapper = new StoryWrapper();
            }
            wrapper.manager = StorageManager.this;
            return wrapper;
        }));
        this.pojoMerger = new PojoMerger(objectMapper);
        this.textStorage = new TextStorage(objectStorage);
        this.queryManager = QueryManager.load(objectStorage);
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
        return storyCache.getUnchecked(getObjectId(story));
    }

    public TextStorage getTextStorage() {
        return textStorage;
    }

    public QueryManager getQueryManager() {
        return queryManager;
    }
}
