package com.markozivkovic.codegen.constants;

public final class ImportConstants {

    private ImportConstants() {}

    public static final String IMPORT = "import %s;\n";
    public static final String PACKAGE = "package %s;\n\n";

    public static final class Java {
        private Java() {}
        public static final String OBJECTS = "java.util.Objects";
        public static final String OPTIONAL = "java.util.Optional";
        public static final String UUID = "java.util.UUID";
        public static final String LIST = "java.util.List";
        public static final String MAP = "java.util.Map";
        public static final String COLLECTORS = "java.util.stream.Collectors";
        public static final String BIG_DECIMAL = "java.math.BigDecimal";
        public static final String BIG_INTEGER = "java.math.BigInteger";
        public static final String LOCAL_DATE = "java.time.LocalDate";
        public static final String LOCAL_DATE_TIME = "java.time.LocalDateTime";
    }

    public static final class Jakarta {
        private Jakarta() {}
        public static final String ENTITY = "jakarta.persistence.Entity";
        public static final String ENTITY_LISTENERS = "jakarta.persistence.EntityListeners";
        public static final String TABLE = "jakarta.persistence.Table";
        public static final String ID = "jakarta.persistence.Id";
        public static final String GENERATED_VALUE = "jakarta.persistence.GeneratedValue";
        public static final String GENERATION_TYPE = "jakarta.persistence.GenerationType";
        public static final String ENUM_TYPE = "jakarta.persistence.EnumType";
        public static final String ENUMERATED = "jakarta.persistence.Enumerated";
        public static final String FETCH_TYPE = "jakarta.persistence.FetchType";
        public static final String CASCADE_TYPE = "jakarta.persistence.CascadeType";
        public static final String COLUMN = "jakarta.persistence.Column";
        public static final String JOIN_COLUMN = "jakarta.persistence.JoinColumn";
        public static final String JOIN_TABLE = "jakarta.persistence.JoinTable";
        public static final String ONE_TO_ONE = "jakarta.persistence.OneToOne";
        public static final String MANY_TO_MANY = "jakarta.persistence.ManyToMany";
        public static final String ONE_TO_MANY = "jakarta.persistence.OneToMany";
        public static final String MANY_TO_ONE = "jakarta.persistence.ManyToOne";
        public static final String VERSION = "jakarta.persistence.Version";
    }

    public static final class Jackson {
        private Jackson() {}
        public static final String OBJECT_MAPPER = "com.fasterxml.jackson.databind.ObjectMapper";
        public static final String TYPE_REFERENCE = "com.fasterxml.jackson.core.type.TypeReference";
    }

    public static final class MapStruct {
        private MapStruct() {}
        public static final String MAPPER = "org.mapstruct.Mapper";
        public static final String FACTORY_MAPPERS = "org.mapstruct.factory.Mappers";
    }

    public static final class JUnit {
        private JUnit() {}
        public static final String TEST = "org.junit.jupiter.api.Test";
        public static final String AFTER_EACH = "org.junit.jupiter.api.AfterEach";
        public static final String BEFORE_EACH = "org.junit.jupiter.api.BeforeEach";
        public static final class Params {
            private Params() {}
            public static final String PARAMETERIZED_TEST = "org.junit.jupiter.params.ParameterizedTest";
            public static final String ENUM_SOURCE = "org.junit.jupiter.params.provider.EnumSource";
        }
    }


    public static final class SpringAnnotation {
        private SpringAnnotation() {}
        public static final String AUTOWIRED = "org.springframework.beans.factory.annotation.Autowired";
        public static final String IMPORT = "org.springframework.context.annotation.Import";
        public static final String BEAN = "org.springframework.context.annotation.Bean";
        public static final String SERVICE = "org.springframework.stereotype.Service";
        public static final String CACHEABLE = "org.springframework.cache.annotation.Cacheable";
        public static final String CACHE_EVICT = "org.springframework.cache.annotation.CacheEvict";
        public static final String CACHE_PUT = "org.springframework.cache.annotation.CachePut";
    }

    public static final class SpringCore {
        private SpringCore() {}
        public static final String PARAMETERIZED_TYPE_REFERENCE = "org.springframework.core.ParameterizedTypeReference";
    }

    public static final class SpringData {
        private SpringData() {}
        public static final String PAGE = "org.springframework.data.domain.Page";
        public static final String PAGE_IMPL = "org.springframework.data.domain.PageImpl";
        public static final String PAGE_REQUEST = "org.springframework.data.domain.PageRequest";
    }

    public static final class SpringBootTest {
        private SpringBootTest() {}
        public static final String WEB_MVC_TEST = "org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest";
        public static final String AUTO_CONFIGURE_MOCK_MVC = "org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc";
        public static final String TEST_CONFIGURATION = "org.springframework.boot.test.context.TestConfiguration";
    }

    public static final class SpringTest {
        private SpringTest() {}
        public static final String CONTEXT_CONFIGURATION = "org.springframework.test.context.ContextConfiguration";
        public static final String MOCKITO_BEAN = "org.springframework.test.context.bean.override.mockito.MockitoBean";
        public static final String MOCKMVC = "org.springframework.test.web.servlet.MockMvc";
        public static final String RESULT_ACTIONS = "org.springframework.test.web.servlet.ResultActions";
        public static final String TEST_PROPERTY_SORUCE = "org.springframework.test.context.TestPropertySource";
    }

    public static final class SecurityAutoConfig {
        private SecurityAutoConfig() {}
        public static final String OAUTH2_CLIENT_AUTO_CONFIGURATION =
                "org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration";
        public static final String OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION =
                "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration";
    }

    public static final class SpringFrameworkGraphQL {
        private SpringFrameworkGraphQL() {}
        public static final String RUNTIME_WIRING_CONFIGURER = "org.springframework.graphql.execution.RuntimeWiringConfigurer";
    }

    public static final class GraphQLTest {
        private GraphQLTest() {}
        public static final String GRAPH_QL_TEST = "org.springframework.boot.test.autoconfigure.graphql.GraphQlTest";
        public static final String AUTO_CONFIGURE_GRAPH_QL_TESTER = "org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester";
        public static final String GRAPH_QL_TESTER = "org.springframework.graphql.test.tester.GraphQlTester";
    }
    
}