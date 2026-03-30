<#if isSpringBoot3>
import java.io.IOException;
</#if><#t>
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

<#if isSpringBoot3>
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
<#else>
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;
</#if>

@SuppressWarnings({ "rawtypes" })
public final class HibernateLazyNullModule extends SimpleModule {

    public HibernateLazyNullModule() {
        super("HibernateLazyNullModule");
        addSerializer(PersistentCollection.class, new PersistentCollectionToNullSerializer());
        addSerializer(HibernateProxy.class, new HibernateProxyToNullOrIdSerializer(true));
    }

    private static final class PersistentCollectionToNullSerializer extends StdSerializer<PersistentCollection> {

        PersistentCollectionToNullSerializer() { super(PersistentCollection.class); }

        @Override
        public void serialize(final PersistentCollection value, final JsonGenerator gen, <#if isSpringBoot3>final SerializerProvider provider<#else>final SerializationContext ctxt</#if>)<#if isSpringBoot3> throws IOException</#if> {
            
            if (value == null || !Hibernate.isInitialized(value)) {
                gen.writeNull();
                return;
            }

            <#if isSpringBoot3>
            Object underlying = value;
            if (value instanceof org.hibernate.collection.spi.PersistentMap persistentMap) {
                underlying = persistentMap;
            }

            if (underlying instanceof Map<?, ?> map) {
                gen.writeStartObject();
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    gen.writeFieldName(String.valueOf(e.getKey()));
                    provider.defaultSerializeValue(e.getValue(), gen);
                }
                gen.writeEndObject();
                return;
            }
            <#else>
            if (value instanceof Map<?, ?> map) {
                gen.writeStartObject();
                for (var e : map.entrySet()) {
                    gen.writeName(String.valueOf(e.getKey()));
                    ctxt.writeValue(gen, e.getValue());
                }
                gen.writeEndObject();
                return;
            }
            </#if>
            
            gen.writeStartArray();
            for (Object elem : (Iterable<?>) value) {
                <#if isSpringBoot3>
                provider.defaultSerializeValue(elem, gen);
                <#else>
                ctxt.writeValue(gen, elem);
                </#if>
            }
            gen.writeEndArray();
        }
    }

    private static final class HibernateProxyToNullOrIdSerializer extends StdSerializer<HibernateProxy> {
        
        private final boolean writeIdWhenUninitialized;

        HibernateProxyToNullOrIdSerializer(final boolean writeIdWhenUninitialized) {
            super(HibernateProxy.class);
            this.writeIdWhenUninitialized = writeIdWhenUninitialized;
        }

        @Override
        public void serialize(final HibernateProxy proxy, final JsonGenerator gen, <#if isSpringBoot3>final SerializerProvider provider<#else>final SerializationContext ctxt</#if>)<#if isSpringBoot3> throws IOException</#if> {
            
            if (proxy == null || !Hibernate.isInitialized(proxy)) {
                if (!writeIdWhenUninitialized) {
                    gen.writeNull();
                    return;
                }
                final LazyInitializer lazyInitializer = (proxy != null) ? proxy.getHibernateLazyInitializer() : null;
                final Object id = (lazyInitializer != null) ? lazyInitializer.getIdentifier() : null;
                if (id == null) gen.writeNull();
                <#if isSpringBoot3>
                else provider.defaultSerializeValue(id, gen);
                <#else>
                else ctxt.writeValue(gen, id);
                </#if>
                return;
            }

            final LazyInitializer lazyInitializer = proxy.getHibernateLazyInitializer();
            final Object impl = (lazyInitializer != null) ? lazyInitializer.getImplementation() : proxy;
            <#if isSpringBoot3>
            provider.defaultSerializeValue(impl, gen);
            <#else>
            ctxt.writeValue(gen, impl);
            </#if>
        }
    }
}
