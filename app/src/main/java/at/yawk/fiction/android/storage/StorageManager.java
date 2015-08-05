package at.yawk.fiction.android.storage;

import at.yawk.fiction.Story;
import at.yawk.fiction.android.event.EventBus;
import at.yawk.fiction.android.inject.Injector;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.provider.ProviderManager;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
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

    @Inject EventBus eventBus;

    final LoadingCache<String, StoryWrapper> storyCache;

    @Inject
    StorageManager() {
        storyCache = CacheBuilder.newBuilder()
                .softValues().build(CacheLoader.from(input -> {
                    StoryWrapper wrapper;
                    try {
                        wrapper = objectStorageManager.load(StoryWrapper.class, input);
                    } catch (NotFoundException e) {
                        wrapper = new StoryWrapper();
                    }
                    wrapper.eventBus = eventBus;
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
        return getStory0(getObjectId(story));
    }

    private StoryWrapper getStory0(String id) {
        return storyCache.getUnchecked(id);
    }

    public Iterable<StoryWrapper> listStories() {
        return Iterables.transform(objectStorageManager.list("story"), this::getStory0);
    }
}
