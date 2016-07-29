package at.yawk.fiction.android.context;

import at.yawk.fiction.android.provider.local.LocalSearchQuery;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.google.inject.Binder;
import com.google.inject.Module;
import javax.inject.Singleton;

/**
 * @author yawkat
 */
@Singleton
public class ObjectMapperProvider implements Module {
    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.registerModule(new KotlinModule());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

        objectMapper.registerSubtypes(LocalSearchQuery.class);
    }

    static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(ObjectMapper.class).toInstance(objectMapper);
    }
}
