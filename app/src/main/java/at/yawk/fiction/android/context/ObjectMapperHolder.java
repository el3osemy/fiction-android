package at.yawk.fiction.android.context;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author yawkat
 */
class ObjectMapperHolder {
    private static final ObjectMapper OBJECT_MAPPER;

    static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.findAndRegisterModules();
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        OBJECT_MAPPER.enableDefaultTyping(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.PROPERTY);
    }
}
