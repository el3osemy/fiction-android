package at.yawk.fiction.android.provider;

import android.app.Application;
import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.ObjectGraph;
import dalvik.system.DexFile;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@Singleton
public class ProviderLoader {
    @Inject ObjectMapper objectMapper;
    @Getter private final List<AndroidFictionProvider> providers = new ArrayList<>();
    @Inject Application application;

    @Inject
    public ProviderLoader() {}

    public ObjectGraph load(ObjectGraph graph) {
        try {
            DexFile dexFile = new DexFile(application.getApplicationInfo().sourceDir);
            Enumeration<String> entries = dexFile.entries();

            Map<AndroidFictionProvider, Provider> annotations = new HashMap<>();
            while (entries.hasMoreElements()) {
                String providerClassName = entries.nextElement();
                Class<?> providerClass;
                try {
                    providerClass = Class.forName(providerClassName, false, ProviderManager.class.getClassLoader());
                } catch (Throwable e) {
                    continue;
                }

                Provider providerAnnotation = providerClass.getAnnotation(Provider.class);
                if (providerAnnotation == null) {
                    continue;
                }

                log.info("Adding provider {}", providerClass.getName());
                AndroidFictionProvider provider = (AndroidFictionProvider) providerClass.newInstance();

                annotations.put(provider, providerAnnotation);

                //noinspection Convert2streamapi
                for (Class<?> provided : provider.getProvidingClasses()) {
                    objectMapper.registerSubtypes(provided);
                }

                providers.add(provider);
            }

            Collections.sort(providers, (lhs, rhs) ->
                    annotations.get(lhs).priority() - annotations.get(rhs).priority());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Object[] modules = new Object[providers.size()];
        for (int i = 0; i < providers.size(); i++) {
            modules[i] = providers.get(i).createModule();
        }
        ObjectGraph combined = graph.plus(modules);
        for (AndroidFictionProvider provider : providers) {
            combined.inject(provider);
        }
        return combined;
    }
}
