package at.yawk.fiction.android.inject;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import at.yawk.fiction.android.event.EventBus;
import at.yawk.fiction.impl.PageParserProvider;
import com.google.inject.Binder;
import com.google.inject.Module;

/**
 * @author yawkat
 */
public class BaseModule implements Module {
    final EventBus eventBus = new EventBus(new Handler(Looper.getMainLooper()));
    final Application application;

    BaseModule(Application application) {
        this.application = application;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(SharedPreferences.class).toInstance(PreferenceManager.getDefaultSharedPreferences(application));
        binder.bind(PageParserProvider.class).toInstance(new PageParserProvider());
        binder.bind(Application.class).toInstance(application);
        binder.bind(EventBus.class).toInstance(eventBus);
    }
}
