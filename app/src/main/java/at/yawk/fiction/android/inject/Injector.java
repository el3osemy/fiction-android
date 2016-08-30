package at.yawk.fiction.android.inject;

import android.app.Activity;
import android.app.Application;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import at.yawk.fiction.android.context.ObjectMapperProvider;
import at.yawk.fiction.android.download.DownloadManagerNotification;
import at.yawk.fiction.android.event.EventBus;
import at.yawk.fiction.android.provider.ProviderLoader;
import at.yawk.fiction.android.provider.ProviderManager;
import at.yawk.fiction.android.storage.RootFile;
import at.yawk.fiction.android.storage.StoryManager;
import butterknife.ButterKnife;
import com.google.common.base.Supplier;
import com.google.common.collect.MapMaker;
import com.google.inject.Guice;
import com.google.inject.Module;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
public class Injector {
    private static Injector injector;

    private final com.google.inject.Injector global;

    private final ConcurrentMap<Object, com.google.inject.Injector> contextMap =
            new MapMaker().weakKeys().weakValues().makeMap();

    private Injector(Application application) {
        Module base = new BaseModule(application);
        global = Guice.createInjector(base, new ObjectMapperProvider());
        ProviderLoader providerLoader = global.getInstance(ProviderLoader.class);
        providerLoader.load(global);
        global.getInstance(ProviderManager.class).loadProviders(providerLoader);

        // make sure the download manager ui is available
        global.getInstance(DownloadManagerNotification.class);
    }

    public static void init(Application application) {
        injector = new Injector(application);

        injector.global.getInstance(RootFile.class).getRoot(f -> {
            // initialize the story manager
            injector.global.getInstance(StoryManager.class).initialize();
        });
    }

    private com.google.inject.Injector module(Object context, Supplier<com.google.inject.Injector> factory) {
        com.google.inject.Injector graph = contextMap.get(context);
        if (graph == null) {
            graph = factory.get();
            if (contextMap.putIfAbsent(context, graph) != null) {
                graph = contextMap.get(context);
            }
        }
        return graph;
    }

    private com.google.inject.Injector activity(Activity activity) {
        return module(activity, () -> global.createChildInjector(new ActivityModule(activity)));
    }

    private com.google.inject.Injector supportFragment(Fragment fragment) {
        return module(fragment, () -> activity(fragment.getActivity())
                .createChildInjector(new SupportFragmentModule(fragment)));
    }

    private com.google.inject.Injector fragment(android.app.Fragment fragment) {
        return module(fragment, () -> activity(fragment.getActivity())
                .createChildInjector(new FragmentModule(fragment)));
    }

    private void inject(com.google.inject.Injector graph, Object o) {
        graph.injectMembers(o);
        graph.getInstance(EventBus.class).addWeakListeners(o);
    }

    public static void inject(Object o) {
        injector.inject(injector.global, o);
    }

    public static void injectFragment(Fragment fragment) {
        injector.inject(injector.supportFragment(fragment), fragment);
    }

    public static void injectFragment(android.app.Fragment fragment) {
        injector.inject(injector.fragment(fragment), fragment);
    }

    public static void injectActivity(Activity activity) {
        injector.inject(injector.activity(activity), activity);
    }

    public static View buildAndInjectContentView(Object o, LayoutInflater inflater, ViewGroup group) {
        View contentView = inflater.inflate(o.getClass().getAnnotation(ContentView.class).value(), group, false);
        injectViews(contentView, o);
        return contentView;
    }

    private static void injectViews(View contentView, Object o) {
        ButterKnife.bind(o, contentView);
    }
}
