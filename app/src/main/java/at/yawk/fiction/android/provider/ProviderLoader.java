package at.yawk.fiction.android.provider;

import android.app.Application;
import at.yawk.fiction.android.provider.ao3.Ao3AndroidFictionProvider;
import at.yawk.fiction.android.provider.ffn.FfnAndroidFictionProvider;
import at.yawk.fiction.android.provider.fim.FimAndroidFictionProvider;
import at.yawk.fiction.android.provider.local.LocalAndroidFictionProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(application.getAssets().open("providers")));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isEmpty()) {
                        Class<? extends AndroidFictionProvider> cl =
                                Class.forName(line).asSubclass(AndroidFictionProvider.class);
                        providerClasses.put(cl, cl.getAnnotation(Provider.class));
                    }
                }
            } finally {
                reader.close();
            }
        } catch (Exception e) {
            log.error("Could not find providers file", e);
            for (Class<? extends AndroidFictionProvider> cl : Arrays.asList(
                    LocalAndroidFictionProvider.class,
                    FimAndroidFictionProvider.class,
                    FfnAndroidFictionProvider.class,
                    Ao3AndroidFictionProvider.class
            )) {
                providerClasses.put(cl, cl.getAnnotation(Provider.class));
            }
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
