package at.yawk.fiction.android.inject;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import at.yawk.fiction.android.context.ObjectMapperProvider;
import at.yawk.fiction.android.download.DownloadManagerUi;
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
        injects = { ProviderLoader.class, StoryWrapper.class, DownloadManagerUi.class }
)
public class BaseModule {
    final EventBus eventBus = new EventBus(new Handler(Looper.getMainLooper()));
    final Application application;

    BaseModule(Application application) {
        this.application = application;
    }

    @Provides
    @Singleton
    public EventBus eventBus() {
        return eventBus;
    }

    @Provides
    @Singleton
    public Application application() {
        return application;
    }

    @Provides
    @Singleton
    PageParserProvider pageParserProvider() {
        return new PageParserProvider();
    }

    @Provides
    @Singleton
    SharedPreferences preferences() {
        return PreferenceManager.getDefaultSharedPreferences(application);
    }
}
