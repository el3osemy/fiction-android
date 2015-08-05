package at.yawk.fiction.android.inject;

import android.os.Handler;
import android.os.Looper;
import at.yawk.fiction.android.context.ObjectMapperProvider;
import at.yawk.fiction.android.event.EventBus;
import at.yawk.fiction.android.provider.ProviderLoader;
import at.yawk.fiction.android.storage.StoryWrapper;
import at.yawk.fiction.impl.PageParserProvider;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/**
 * @author yawkat
 */
@Module(
        library = true,
        includes = { ObjectMapperProvider.class },
        injects = { ProviderLoader.class, StoryWrapper.class }
)
public class BaseModule {
    final EventBus eventBus = new EventBus(new Handler(Looper.getMainLooper()));

    @Provides
    @Singleton
    public EventBus eventBus() {
        return eventBus;
    }

    @Provides
    @Singleton
    PageParserProvider pageParserProvider() {
        return new PageParserProvider();
    }
}