package at.yawk.fiction.android.provider;

import at.yawk.fiction.SearchQuery;
import at.yawk.fiction.Story;
import at.yawk.fiction.android.provider.ffn.FfnAndroidFictionProvider;
import at.yawk.fiction.android.provider.local.LocalAndroidFictionProvider;
import at.yawk.fiction.android.provider.local.LocalSearchQuery;
import at.yawk.fiction.android.storage.StorageManager;
import at.yawk.fiction.impl.fanfiction.FfnAuthor;
import at.yawk.fiction.impl.fanfiction.FfnChapter;
import at.yawk.fiction.impl.fanfiction.FfnSearchQuery;
import at.yawk.fiction.impl.fanfiction.FfnStory;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author yawkat
 */
@Singleton
public class ProviderManager {
    private static final Map<Class<?>, Class<? extends AndroidFictionProvider>> PROVIDERS_BY_PROVIDING_CLASS =
            ImmutableMap.of(
                    FfnSearchQuery.class, FfnAndroidFictionProvider.class,
                    FfnStory.class, FfnAndroidFictionProvider.class,
                    FfnChapter.class, FfnAndroidFictionProvider.class,
                    FfnAuthor.class, FfnAndroidFictionProvider.class,
                    LocalSearchQuery.class, LocalAndroidFictionProvider.class
            );

    private ProviderContext context;
    private final Map<Class<? extends AndroidFictionProvider>, AndroidFictionProvider> providers = new HashMap<>();

    private void addProvider(AndroidFictionProvider provider) {
        AndroidFictionProvider old = providers.put(provider.getClass(), provider);
        if (old != null) {
            throw new IllegalStateException("Duplicate entry for class " + provider.getClass().getName());
        }
        provider.init(context);
    }

    @Inject
    ProviderManager(StorageManager storageManager) {
        context = new ProviderContext(storageManager);
        addProvider(new FfnAndroidFictionProvider());
        addProvider(new LocalAndroidFictionProvider());
    }

    public AndroidFictionProvider getProvider(SearchQuery query) {
        return getProvider0(query);
    }

    public AndroidFictionProvider getProvider(Story story) {
        return getProvider0(story);
    }

    public Collection<AndroidFictionProvider> getProviders() {
        return providers.values();
    }

    private AndroidFictionProvider getProvider0(Object o) {
        Class<? extends AndroidFictionProvider> providerType = PROVIDERS_BY_PROVIDING_CLASS.get(o.getClass());
        if (providerType == null) {
            throw new NoSuchElementException("No provider for type " + o.getClass().getName() + " available");
        }
        return findProvider(providerType);
    }

    @SuppressWarnings("unchecked")
    public  <T extends AndroidFictionProvider> T findProvider(Class<T> type) {
        AndroidFictionProvider provider = providers.get(type);
        assert provider != null;
        return (T) provider;
    }
}
