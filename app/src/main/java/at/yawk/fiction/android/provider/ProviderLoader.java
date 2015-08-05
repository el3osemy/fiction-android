package at.yawk.fiction.android.provider;

import at.yawk.fiction.android.FictionApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.ObjectGraph;
import dalvik.system.DexFile;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
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

    @Inject
    public ProviderLoader() {}

    public ObjectGraph load(ObjectGraph graph) {
        try {
            DexFile dexFile = new DexFile(FictionApplication.applicationInfo.sourceDir);
            Enumeration<String> entries = dexFile.entries();

            while (entries.hasMoreElements()) {
                String providerClassName = entries.nextElement();
                Class<?> providerClass;
                try {
                    providerClass = Class.forName(providerClassName, false, ProviderManager.class.getClassLoader());
                } catch (Throwable e) {
                    continue;
                }

                if (!AndroidFictionProvider.class.isAssignableFrom(providerClass) ||
                    Modifier.isAbstract(providerClass.getModifiers())) {
                    continue;
                }

                log.info("Adding provider {}", providerClass.getName());
                AndroidFictionProvider provider = (AndroidFictionProvider) providerClass.newInstance();

                //noinspection Convert2streamapi
                for (Class<?> provided : provider.getProvidingClasses()) {
                    objectMapper.registerSubtypes(provided);
                }

                providers.add(provider);
            }
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
