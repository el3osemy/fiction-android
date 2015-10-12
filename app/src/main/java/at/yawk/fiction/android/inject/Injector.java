package at.yawk.fiction.android.inject;

import android.app.Activity;
import android.app.Application;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import at.yawk.fiction.android.download.DownloadManagerNotification;
import at.yawk.fiction.android.provider.ProviderLoader;
import at.yawk.fiction.android.storage.StoryManager;
import butterknife.ButterKnife;
import com.google.common.base.Supplier;
import com.google.common.collect.MapMaker;
import dagger.ObjectGraph;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
public class Injector {
    private static Injector injector;

    private final BaseModule base;
    private final ObjectGraph global;

    private final ConcurrentMap<Object, ObjectGraph> contextMap = new MapMaker().weakKeys().weakValues().makeMap();

    private Injector(Application application) {
        base = new BaseModule(application);
        ObjectGraph baseGraph = ObjectGraph.create(base);
        global = baseGraph.get(ProviderLoader.class).load(baseGraph);

        // make sure the download manager ui is available
        global.get(DownloadManagerNotification.class);
    }

    public static void init(Application application) {
        injector = new Injector(application);

        // initialize the story manager
        injector.global.get(StoryManager.class).initialize();
    }

    private ObjectGraph module(Object context, Supplier<ObjectGraph> factory) {
        ObjectGraph graph = contextMap.get(context);
        if (graph == null) {
            graph = factory.get();
            if (contextMap.putIfAbsent(context, graph) != null) {
                graph = contextMap.get(context);
            }
        }
        return graph;
    }

    private ObjectGraph activity(Activity activity) {
        return module(activity, () -> global.plus(new ActivityModule(activity)));
    }

    private ObjectGraph supportFragment(Fragment fragment) {
        return module(fragment, () -> {
            SupportFragmentModule module = new SupportFragmentModule(fragment);
            return activity(fragment.getActivity()).plus(
                    fragment instanceof ExternalInjectable ?
                            new Object[]{ module, ((ExternalInjectable) fragment).createModule() } :
                            new Object[]{ module }
            );
        });
    }

    private ObjectGraph fragment(android.app.Fragment fragment) {
        return module(fragment, () -> {
            FragmentModule module = new FragmentModule(fragment);
            return activity(fragment.getActivity()).plus(
                    fragment instanceof ExternalInjectable ?
                            new Object[]{ module, ((ExternalInjectable) fragment).createModule() } :
                            new Object[]{ module }
            );
        });
    }

    private void inject(ObjectGraph graph, Object o) {
        graph.inject(o);
        base.eventBus.addWeakListeners(o);
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
