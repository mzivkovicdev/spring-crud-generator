import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

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
        public void serialize(final PersistentCollection value, final JsonGenerator gen, final SerializationContext ctxt) {
            
            if (value == null || !Hibernate.isInitialized(value)) {
                gen.writeNull();
                return;
            }

            if (value instanceof Map<?, ?> map) {
                gen.writeStartObject();
                for (var e : map.entrySet()) {
                    gen.writeName(String.valueOf(e.getKey()));
                    ctxt.writeValue(gen, e.getValue());
                }
                gen.writeEndObject();
                return;
            }
            
            gen.writeStartArray();
            for (Object elem : (Iterable<?>) value) {
                ctxt.writeValue(gen, elem);
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
        public void serialize(final HibernateProxy proxy, final JsonGenerator gen, final SerializationContext ctxt) {
            
            if (proxy == null || !Hibernate.isInitialized(proxy)) {
                if (!writeIdWhenUninitialized) {
                    gen.writeNull();
                    return;
                }
                final LazyInitializer lazyInitializer = proxy.getHibernateLazyInitializer();
                final Object id = (lazyInitializer != null) ? lazyInitializer.getIdentifier() : null;
                if (id == null) gen.writeNull();
                else ctxt.writeValue(gen, id);
                return;
            }

            final LazyInitializer lazyInitializer = proxy.getHibernateLazyInitializer();
            final Object impl = (lazyInitializer != null) ? lazyInitializer.getImplementation() : proxy;
            ctxt.writeValue(gen, impl);
        }
    }
}
