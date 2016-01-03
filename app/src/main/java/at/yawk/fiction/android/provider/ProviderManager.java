package at.yawk.fiction.android.provider;

import at.yawk.fiction.SearchQuery;
import at.yawk.fiction.Story;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@Singleton
public class ProviderManager {
    private final Map<Class<?>, AndroidFictionProvider> providersByProvidingClass
            = new LinkedHashMap<>();
    private final Map<Class<? extends AndroidFictionProvider>, AndroidFictionProvider> providers
            = new LinkedHashMap<>();

    public void loadProviders(ProviderLoader loader) {
        log.info("Loading providers...");

        for (AndroidFictionProvider provider : loader.getProviders()) {
            if (this.providers.put(provider.getClass(), provider) != null) {
                throw new AssertionError("Duplicate entry for class " + provider.getClass().getName());
            }

            for (Class<?> provided : provider.getProvidingClasses()) {
                providersByProvidingClass.put(provided, provider);
            }
        }
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
        AndroidFictionProvider provider = providersByProvidingClass.get(o.getClass());
        if (provider == null) {
            throw new NoSuchElementException("No provider for type " + o.getClass().getName() + " available");
        }
        return provider;
    }
}
