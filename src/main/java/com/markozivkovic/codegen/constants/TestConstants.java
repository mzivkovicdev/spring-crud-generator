package com.markozivkovic.codegen.constants;

public class TestConstants {

    private TestConstants(){

    }

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
    public static final String SPRINGFRAMEWORK_BOOT_TEST = SPRINGFRAMEWORK + ".boot.test";
    public static final String SPRINGFRAMEWORK_TEST_MOCK_MOCKITO_MOCKITO_BEAN =
            SPRINGFRAMEWORK + ".test.context.bean.override.mockito.MockitoBean";
    public static final String SPRINGFRAMEWORK_BOOT_TEST_CONTEXT_JUNIT_JUPITER_SPRING_EXTENSION =
            SPRINGFRAMEWORK + ".test.context.junit.jupiter.SpringExtension";

}
