<#assign uncapModelName = strippedModelName?uncap_first>
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
<#assign serviceClass = strippedModelName?cap_first + "Service">
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign businessServiceClass = strippedModelName?cap_first + "BusinessService">
<#assign businessServiceField = strippedModelName?uncap_first + "BusinessService">
<#assign resolverClassName = strippedModelName?cap_first + "Resolver">
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;<#if hasRelations>
import static org.mockito.Mockito.verifyNoInteractions;</#if>
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

${testImports}
${projectImports}

import graphql.scalars.ExtendedScalars;<#if dataGenerator == "PODAM">
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;</#if>

@GraphQlTest(
    controllers = ${resolverClassName}.class,
    excludeAutoConfiguration = {
        OAuth2ClientAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class
    }
)
@AutoConfigureGraphQlTester
@Import(GlobalGraphQlExceptionHandler.class)
@TestPropertySource(properties = {
    "spring.graphql.schema.locations=classpath:graphql/"
})
class ${className} {

    <#if dataGenerator == "PODAM">
    private static final PodamFactory PODAM_FACTORY = new PodamFactoryImpl();</#if>

    @MockitoBean
    private ${serviceClass} ${serviceField};

    <#if hasRelations>
    @MockitoBean
    private ${businessServiceClass?cap_first} ${businessServiceField};
    </#if>

    @Autowired
    private GraphQlTester graphQlTester;

    @AfterEach
    void after() {
        <#if hasRelations>verifyNoInteractions(this.${businessServiceField});</#if>
        verifyNoMoreInteractions(this.${serviceField});
    }

    @Test
    void ${uncapModelName}ById() {

        final ${modelName} ${modelName?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${modelName}.class);
        final ${idType} ${idField?uncap_first} = ${modelName?uncap_first}.get${idField?cap_first}();

        when(${serviceField}.getById(${idField?uncap_first})).thenReturn(${modelName?uncap_first});

        final String query = """
            query($id: ID!) {
            ${uncapModelName}ById(id: $id) {
                ${idField?uncap_first}
            }
            }
        """;

        final ${transferObjectClass} result = this.graphQlTester.document(query)
                .variable("id", ${idField?uncap_first})
                .execute()
                .path("${uncapModelName}ById")
                .entity(${transferObjectClass}.class)
                .get();

        verify${strippedModelName}(result, ${modelName?uncap_first});

        verify(${serviceField}).getById(${idField?uncap_first});
    }

    @Test
    void ${uncapModelName}ById_typeMismatch_error() {

        final String query = """
            query($id: ${idType}!) {
            ${uncapModelName}ById(id: $id) { ${idField?uncap_first} }
            }
        """;
        final ${invalidIdType} ${idField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${invalidIdType}.class);

        this.graphQlTester.document(query)
            .variable("id", ${idField?uncap_first})
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).isNotEmpty());
    }

    @Test
    void ${uncapModelName}sPage() {

        <#if dataGenerator == "PODAM">
        final List<${modelName}> ${modelName?uncap_first}s = ${generatorFieldName}.${multipleObjectsMethodName}(List.class, ${modelName}.class);
        <#else>
        final List<${modelName}> ${modelName?uncap_first}s = ${generatorFieldName}.${multipleObjectsMethodName}(${modelName}.class)
                .size(10)
                .create();
        </#if>
        final Page<${modelName}> pageObject = new PageImpl<>(${modelName?uncap_first}s);
        final Integer pageNumber = ${generatorFieldName}.${singleObjectMethodName}(Integer.class);
        final Integer pageSize = ${generatorFieldName}.${singleObjectMethodName}(Integer.class);

        when(${serviceField}.getAll(pageNumber, pageSize)).thenReturn(pageObject);

        final String query = """
            query($pageNumber: Int!, $pageSize: Int!) {
            ${uncapModelName}sPage(pageNumber: $pageNumber, pageSize: $pageSize) {
                totalPages
                totalElements
                size
                number
                content { ${idField?uncap_first} }
            }
            }
        """;

        final PageTO<${transferObjectClass}> result = this.graphQlTester.document(query)
                .variable("pageNumber", pageNumber)
                .variable("pageSize", pageSize)
                .execute()
                .path("${uncapModelName}sPage")
                .entity(new ParameterizedTypeReference<PageTO<${transferObjectClass}>>() {})
                .get();

        assertThat(result).isNotNull();
        assertThat(result.totalPages()).isGreaterThanOrEqualTo(0);
        assertThat(result.totalElements()).isGreaterThanOrEqualTo(0);
        assertThat(result.size()).isGreaterThanOrEqualTo(0);
        assertThat(result.number()).isGreaterThanOrEqualTo(0);
        assertThat(result.content()).isNotEmpty();

        result.content().forEach(item -> {

            final ${modelName} src = ${modelName?uncap_first}s.stream()
                .filter(m -> String.valueOf(m.get${idField?cap_first}()).equals(String.valueOf(item.${idField?uncap_first}())))
                .findFirst()
                .orElseThrow();

            verify${strippedModelName}(item, src);
        });

        verify(${serviceField}).getAll(pageNumber, pageSize);
    }

    @Test
    void ${uncapModelName}sPage_typeMismatch_error() {

        final String query = """
            query($pageNumber: Int!, $pageSize: Int!) {
            ${uncapModelName}sPage(pageNumber: $pageNumber, pageSize: $pageSize) { totalPages }
            }
        """;

        final String pageNumber = ${generatorFieldName}.${singleObjectMethodName}(String.class);
        final String pageSize = ${generatorFieldName}.${singleObjectMethodName}(String.class);

        this.graphQlTester.document(query)
            .variable("pageNumber", pageNumber)
            .variable("pageSize", pageSize)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).isNotEmpty());
    }

    @Test
    void ${uncapModelName}sPage_missingPageSize() {

        final Integer pageNumber = ${generatorFieldName}.${singleObjectMethodName}(Integer.class);
        final String queryMissingPage = """
            query($pageSize: Int!) {
            ${uncapModelName}sPage(pageNumber: 0, pageSize: $pageSize) { totalPages }
            }
        """;

        this.graphQlTester.document(queryMissingPage)
            .variable("pageNumber", pageNumber)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).isNotEmpty());
    }

    @Test
    void ${uncapModelName}sPage_missingPageNumber() {

        final Integer pageSize = ${generatorFieldName}.${singleObjectMethodName}(Integer.class);
        final String queryMissingSize = """
            query($pageNumber: Int!) {
            ${uncapModelName}sPage(pageNumber: $pageNumber, pageSize: 10) { totalPages }
            }
        """;

        this.graphQlTester.document(queryMissingSize)
            .variable("pageSize", pageSize)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).isNotEmpty());
    }
    
    private void verify${strippedModelName}(final ${transferObjectClass} result, final ${modelName} src) {
        
        assertThat(result).isNotNull();
        assertThat(result.${idField?uncap_first}()).isEqualTo(src.get${idField?cap_first}());
    }

    @TestConfiguration
    static class RuntimeWiringTestConfig {
        @Bean
        RuntimeWiringConfigurer scalarWiring() {
            return builder -> builder
                    .scalar(ExtendedScalars.GraphQLLong)
                    .scalar(ExtendedScalars.UUID)
                    .scalar(ExtendedScalars.Date)
                    .scalar(ExtendedScalars.DateTime)
                    .scalar(ExtendedScalars.GraphQLBigDecimal)
                    .scalar(ExtendedScalars.Json);
        }
    }

}
