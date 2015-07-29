package at.yawk.fiction.android.storage;

import at.yawk.fiction.Story;
import at.yawk.fiction.impl.fanfiction.FfnStory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.File;

/**
 * @author yawkat
 */
public class StorageManager {
    final ObjectMapper objectMapper;
    final ObjectStorageManager objectStorage;
    final LoadingCache<String, StoryWrapper> storyCache;
    final PojoMerger pojoMerger;
    final TextStorage textStorage;
    final QueryManager queryManager;

    public StorageManager(ObjectMapper objectMapper, File root) {
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
        if (story instanceof FfnStory) {
            builder.append("ffn/").append(((FfnStory) story).getId());
        } else {
            throw new UnsupportedOperationException("Unsupported story type " + story.getClass().getName());
        }

        return builder.toString();
    }

    /**
     * Get the saved story that has the same identifier as the given one, merging changes from the passed story into
     * the saved story if necessary.
     */
    public StoryWrapper mergeStory(Story story) {
        StoryWrapper wrapper = storyCache.getUnchecked(getObjectId(story));
        wrapper.updateStory(story);
        return wrapper;
    }

    public TextStorage getTextStorage() {
        return textStorage;
    }

    public QueryManager getQueryManager() {
        return queryManager;
    }
}
