package com.markozivkovic.codegen.constants;

public final class ImportConstants {

    private ImportConstants() {}

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
    }

    public static final class Params {
        private Params() {}
        public static final String PARAMETERIZED_TEST = "org.junit.jupiter.params.ParameterizedTest";
        public static final String ENUM_SOURCE = "org.junit.jupiter.params.provider.EnumSource";
    }

    public static final class SpringAnnotation {
        private SpringAnnotation() {}
        public static final String AUTOWIRED = "org.springframework.beans.factory.annotation.Autowired";
        public static final String IMPORT = "org.springframework.context.annotation.Import";
        public static final String BEAN = "org.springframework.context.annotation.Bean";
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