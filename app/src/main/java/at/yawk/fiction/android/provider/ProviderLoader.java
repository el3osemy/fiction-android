package at.yawk.fiction.android.provider;

import android.app.Application;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import dalvik.system.DexFile;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@Singleton
public class ProviderLoader {
    @Inject ObjectMapper objectMapper;
    @Inject Application application;

    @Getter private final List<AndroidFictionProvider> providers = new ArrayList<>();

    public ProviderLoader() {}

    @SneakyThrows
    private Map<Class<? extends AndroidFictionProvider>, Provider> findProviderClasses() {
        Map<Class<? extends AndroidFictionProvider>, Provider> providerClasses = new HashMap<>();
        DexFile dexFile = new DexFile(application.getApplicationInfo().sourceDir);
        Enumeration<String> entries = dexFile.entries();

        Map<AndroidFictionProvider, Provider> annotations = new HashMap<>();
        while (entries.hasMoreElements()) {
            String providerClassName = entries.nextElement();
            if (!providerClassName.startsWith("at.yawk.fiction.android.provider")) { continue; }
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

            //noinspection unchecked
            providerClasses.put((Class<? extends AndroidFictionProvider>) providerClass, providerAnnotation);
        }

        return providerClasses;
    }

    public void load(Injector injector) {
        log.info("Scanning for fiction providers...");

        List<Map.Entry<Class<? extends AndroidFictionProvider>, Provider>> entries =
                new ArrayList<>(findProviderClasses().entrySet());
        Collections.sort(entries, (lhs, rhs) -> lhs.getValue().priority() - rhs.getValue().priority());
        for (Map.Entry<Class<? extends AndroidFictionProvider>, Provider> entry : entries) {
            log.info("Adding provider {}", entry.getKey().getName());
            AndroidFictionProvider provider = injector.getInstance(entry.getKey());

            for (Class<?> provided : provider.getProvidingClasses()) {
                objectMapper.registerSubtypes(provided);
            }

            providers.add(provider);
        }

        log.info("Scan complete");
    }
}
