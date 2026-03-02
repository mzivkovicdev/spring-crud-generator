import java.io.IOException;

<#if isSpringBoot3>
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
<#else>
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
</#if><#t>

public class HazelcastJacksonGlobalSerializer implements StreamSerializer<Object> {

    private static final int TYPE_ID = 99001;

    private final ObjectMapper objectMapper;

    public HazelcastJacksonGlobalSerializer() {
        this.objectMapper = JsonMapper.builder()
                .addModule(new HibernateLazyNullModule())
                <#if isSpringBoot3>
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                <#else>
                .changeDefaultVisibility(v -> v.withFieldVisibility(JsonAutoDetect.Visibility.ANY))
                </#if>
                .build();
        <#if isSpringBoot3>

        this.objectMapper.setVisibility(
                PropertyAccessor.FIELD,
                JsonAutoDetect.Visibility.ANY
        );

        this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        </#if>
    }

    @Override
    public int getTypeId() {
        return TYPE_ID;
    }

    @Override
    public void write(final ObjectDataOutput out, final Object object) throws IOException {
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