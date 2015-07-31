package at.yawk.fiction.android.context;

import at.yawk.fiction.android.provider.local.LocalSearchQuery;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.inject.AbstractModule;
import javax.inject.Singleton;

/**
 * @author yawkat
 */
@Singleton
public class ObjectMapperProvider extends AbstractModule {
    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

        objectMapper.registerSubtypes(LocalSearchQuery.class);
    }

    static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    protected void configure() {
        bind(ObjectMapper.class).toInstance(objectMapper);
    }
}