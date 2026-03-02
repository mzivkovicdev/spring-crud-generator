import java.io.IOException;

<#if isSpringBoot3>
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
<#else>
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

import tools.jackson.annotation.JsonAutoDetect;
import tools.jackson.annotation.JsonInclude;
import tools.jackson.annotation.JsonTypeInfo;
import tools.jackson.annotation.PropertyAccessor;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.json.JsonMapper;
</#if><#t>

public class HazelcastJacksonGlobalSerializer implements StreamSerializer<Object> {

    private static final int TYPE_ID = 99001;

    private final ObjectMapper objectMapper;

    public HazelcastJacksonGlobalSerializer() {
        this.objectMapper = JsonMapper.builder()
                .addModule(new HibernateLazyNullModule())
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .build();

        this.objectMapper.setVisibility(
                PropertyAccessor.FIELD,
                JsonAutoDetect.Visibility.ANY
        );

        this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public int getTypeId() {
        return TYPE_ID;
    }

    @Override
    public void write(final ObjectDataOutput out, final Object object)<#if isSpringBoot3> throws IOException</#if> {
        if (object == null) {
            out.writeString(null);
            out.writeByteArray(null);
            return;
        }

        out.writeString(object.getClass().getName());
        out.writeByteArray(this.objectMapper.writeValueAsBytes(object));
    }

    @Override
    public Object read(final ObjectDataInput in) throws IOException {
        final String className = in.readString();
        final byte[] bytes = in.readByteArray();

        if (className == null || bytes == null) {
            return null;
        }

        try {
            final Class<?> clazz = Class.forName(className);
            return this.objectMapper.readValue(bytes, clazz);
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to deserialize Hazelcast cached value. Unknown class: " + className, e);
        }
    }

    @Override
    public void destroy() {}
}