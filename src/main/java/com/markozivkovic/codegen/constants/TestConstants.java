package com.markozivkovic.codegen.constants;

public class TestConstants {

    private TestConstants(){

    }

    public static final String COM_FASTERXML = "com.fasterxml";
    public static final String COM_FASTERXML_JACKSON = COM_FASTERXML + ".jackson";
    public static final String COM_FASTERXML_JACKSON_DATABIND = COM_FASTERXML_JACKSON + ".databind";
    public static final String COM_FASTERXML_JACKSON_DATABIND_OBJECTMAPPER =
            COM_FASTERXML_JACKSON_DATABIND + ".ObjectMapper";

    public static final String ORG_MAPSTRUCT = "org.mapstruct";
    public static final String ORG_MAPSTRUCT_FACTORY = ORG_MAPSTRUCT + ".factory";
    public static final String ORG_MAPSTRUCT_FACTORY_MAPPERS = ORG_MAPSTRUCT_FACTORY + ".Mappers";

    public static final String JUNIT_JUPITER = "org.junit.jupiter";
    public static final String JUNIT_JUPITER_API = JUNIT_JUPITER + ".api";
    public static final String JUNIT_JUPITER_API_TEST = JUNIT_JUPITER_API + ".Test";
    public static final String JUNIT_JUPITER_API_AFTER_EACH = JUNIT_JUPITER_API + ".AfterEach";
    public static final String JUNIT_JUPITER_API_BEFORE_EACH = JUNIT_JUPITER_API + ".BeforeEach";

    public static final String JUNIT_JUPITER_API_EXTENSION = JUNIT_JUPITER_API + ".extension";
    public static final String JUNIT_JUPITER_API_EXTENSION_EXTEND_WITH = JUNIT_JUPITER_API_EXTENSION + ".ExtendWith";
    public static final String JUNIT_JUPITER_PARAMS = JUNIT_JUPITER + ".params";
    public static final String JUNIT_JUPITER_PARAMS_PARAMETERIZED_TEST = JUNIT_JUPITER_PARAMS + ".ParameterizedTest";
    public static final String JUNIT_JUPITER_PARAMS_PROVIDER_ENUM_SOURCE = JUNIT_JUPITER_PARAMS + ".provider.EnumSource";

    public static final String SPRINGFRAMEWORK = "org.springframework";
    public static final String SPRINGFRAMEWORK_BEANS = SPRINGFRAMEWORK + ".beans";
    public static final String SPRINGFRAMEWORK_BEANS_FACTORY = SPRINGFRAMEWORK_BEANS + ".factory";
    public static final String SPRINGFRAMEWORK_BEANS_FACTORY_ANNOTATION_AUTOWIRED =
            SPRINGFRAMEWORK_BEANS_FACTORY + ".annotation.Autowired";

    public static final String SPRINGFRAMEWORK_HTTP = SPRINGFRAMEWORK + ".http";
    public static final String SPRINGFRAMEWORK_HTTP_MEDIA_TYPE = SPRINGFRAMEWORK_HTTP + ".MediaType";

    public static final String SPRINGFRAMEWORK_BOOT = SPRINGFRAMEWORK + ".boot";
    public static final String SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE =
            SPRINGFRAMEWORK_BOOT + ".autoconfigure";
    public static final String SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE_SECURITY = 
            SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE + ".security";
    public static final String SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE_SECURITY_OAUTH2 = 
            SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE_SECURITY + ".oauth2";
    public static final String SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE_SECURITY_OAUTH2_CLIENT =
            SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE_SECURITY_OAUTH2 + ".client";
    public static final String SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE_SECURITY_OAUTH2_CLIENT_SERVLET =
            SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE_SECURITY_OAUTH2_CLIENT + ".servlet";
    public static final String SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE_SECURITY_OAUTH2_CLIENT_OAUTH2CLIENTAUTOCONFIGURATION =
            SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE_SECURITY_OAUTH2_CLIENT + ".OAuth2ClientAutoConfiguration";
    public static final String SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE_SECURITY_OAUTH2_RESOURCE = 
            SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE_SECURITY_OAUTH2 + ".resource";
    public static final String SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE_SECURITY_OAUTH2_RESOURCE_SERVLET =
            SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE_SECURITY_OAUTH2_RESOURCE + ".servlet";
    public static final String SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE_SECURITY_OAUTH2_RESOURCE_SERVLET_OAUTH2RESOURCEAUTOCONFIGURATION =
            SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE_SECURITY_OAUTH2_RESOURCE_SERVLET + ".OAuth2ResourceServerAutoConfiguration";

    public static final String SPRINGFRAMEWORK_BOOT_TEST = SPRINGFRAMEWORK_BOOT + ".test";
    public static final String SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE_WEB_SERVLET =
            SPRINGFRAMEWORK_BOOT_TEST + ".autoconfigure.web.servlet";
    public static final String SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE_WEB_SERVLET_AUTOCONFIGUREMOCKMVC = 
            SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE_WEB_SERVLET + ".AutoConfigureMockMvc";

    public static final String SPRINGFRAMEWORK_TEST_MOCK_MOCKITO_MOCKITO_BEAN =
            SPRINGFRAMEWORK + ".test.context.bean.override.mockito.MockitoBean";
    public static final String SPRINGFRAMEWORK_BOOT_TEST_CONTEXT_JUNIT_JUPITER_SPRING_EXTENSION =
            SPRINGFRAMEWORK + ".test.context.junit.jupiter.SpringExtension";
    public static final String SPRINGFRAMEWORK_TEST_CONTEXT_CONTEXTCONFIGURATION = 
            SPRINGFRAMEWORK + ".test.context.ContextConfiguration";
    public static final String SPRINGFRAMEWORK_TEST_WEB_SERVLET_MOCKMVC = 
            SPRINGFRAMEWORK + ".test.web.servlet.MockMvc";
    public static final String SPRINGFRAMEWORK_TEST_WEB_SERVLET_RESULT_ACTIONS = 
            SPRINGFRAMEWORK + ".test.web.servlet.ResultActions";

    public static final String SPRINGFRAMEWORK_BOOT_TEST_AUTOCONFIGURE = 
            SPRINGFRAMEWORK_BOOT_TEST + ".autoconfigure";
    public static final String SPRINGFRAMEWORK_BOOT_TEST_AUTOCONFIGURE_WEB_SERVLET =
            SPRINGFRAMEWORK_BOOT_TEST_AUTOCONFIGURE + ".web.servlet";
    public static final String SPRINGFRAMEWORK_BOOT_TEST_AUTOCONFIGURE_WEB_SERVLET_WEBMVC_TEST =
            SPRINGFRAMEWORK_BOOT_TEST_AUTOCONFIGURE_WEB_SERVLET + ".WebMvcTest";

}
