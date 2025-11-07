<#assign uncapModelName = strippedModelName?uncap_first>
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
<#assign serviceClass = strippedModelName?cap_first + "Service">
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign businessServiceClass = strippedModelName?cap_first + "BusinessService">
<#assign businessServiceField = strippedModelName?uncap_first + "BusinessService">
<#assign resolverClassName = strippedModelName?cap_first + "Resolver">
<#assign createInputTO = strippedModelName?cap_first + "CreateTO">
<#assign updateInputTO = strippedModelName?cap_first + "UpdateTO">
<#assign createInputGraphQL = strippedModelName?cap_first + "CreateInput">
<#assign updateInputGraphQL = strippedModelName?cap_first + "UpdateInput">
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

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
class ${strippedModelName}ResolverMutationTest {

    <#if dataGenerator == "PODAM">
    private static final PodamFactory PODAM_FACTORY = new PodamFactoryImpl();</#if>

    <#list jsonFields as jsonField>
    <#assign jsonFieldMapperClass = jsonField?cap_first + "GraphQLMapper">
    <#assign jsonFieldMapper = jsonField?cap_first + "Mapper">
    private final ${jsonFieldMapperClass} ${jsonFieldMapper?uncap_first} = Mappers.getMapper(${jsonFieldMapperClass}.class);
    </#list>

    @MockitoBean
    private ${serviceClass} ${serviceField};

    <#if hasRelations>
    @MockitoBean
    private ${businessServiceClass} ${businessServiceField};
    </#if>

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GraphQlTester graphQlTester;

    @AfterEach
    void after() {
        verifyNoMoreInteractions(this.${serviceField}<#if hasRelations>, this.${businessServiceField}</#if>);
    }

    @Test
    void create${strippedModelName}() {

        final ${modelName} saved = ${generatorFieldName}.${singleObjectMethodName}(${modelName}.class);
        final ${createInputTO} input = ${generatorFieldName}.${singleObjectMethodName}(${createInputTO}.class);
        final Map<String, Object> inputVars = this.objectMapper.convertValue(
                input, new TypeReference<Map<String,Object>>() {}
        );

        final String mutation = """
            mutation($input: ${createInputGraphQL}!) {
              create${strippedModelName}(input: $input) {
                ${idField?uncap_first}
              }
            }
        """;

        when(<#if hasRelations>${businessServiceField}<#else>${serviceField}</#if>.create(
            <#list createArgs as createArg>${createArg}<#if createArg_has_next>, </#if></#list>
        )).thenReturn(saved);

        final ${transferObjectClass} result = this.graphQlTester.document(mutation)
            .variable("input", inputVars)
            .execute()
            .path("create${strippedModelName}")
            .entity(${transferObjectClass}.class)
            .get();

        verify(this.<#if hasRelations>${businessServiceField}<#else>${serviceField}</#if>).create(
            <#list createArgs as createArg>${createArg}<#if createArg_has_next>, </#if></#list>
        );

        assertThat(result).isNotNull();
        assertThat(result.${idField?uncap_first}()).isEqualTo(saved.get${idField?cap_first}());
    }

    @Test
    void create${strippedModelName}_missingInput_error() {

        final String mutation = """
            mutation {
              create${strippedModelName}(input: null) {
                ${idField?uncap_first}
              }
            }
        """;

        this.graphQlTester.document(mutation)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).isNotEmpty());
    }

    @Test
    void update${strippedModelName}() {

        final ${modelName} updated = ${generatorFieldName}.${singleObjectMethodName}(${modelName}.class);
        final ${idType} ${idField?uncap_first} = updated.get${idField?cap_first}();
        final ${updateInputTO} input = ${generatorFieldName}.${singleObjectMethodName}(${updateInputTO}.class);
        final Map<String, Object> inputVars = this.objectMapper.convertValue(
                input, new TypeReference<Map<String,Object>>() {}
        );

        final String mutation = """
            mutation($id: ID!, $input: ${updateInputGraphQL}!) {
              update${strippedModelName}(id: $id, input: $input) {
                ${idField?uncap_first}
              }
            }
        """;

        when(${serviceField}.updateById(
            ${idField?uncap_first},
            <#list updateArgs as updateArg>${updateArg}<#if updateArg_has_next>, </#if></#list>
        )).thenReturn(updated);

        final ${transferObjectClass} result = this.graphQlTester.document(mutation)
            .variable("id", ${idField?uncap_first})
            .variable("input", inputVars)
            .execute()
            .path("update${strippedModelName}")
            .entity(${transferObjectClass}.class)
            .get();

        verify(this.${serviceField}).updateById(
                ${idField?uncap_first}, <#list updateArgs as updateArg>${updateArg}<#if updateArg_has_next>, </#if></#list>
        );

        assertThat(result).isNotNull();
        assertThat(result.${idField?uncap_first}()).isEqualTo(updated.get${idField?cap_first}());
    }

    @Test
    void update${strippedModelName}_idTypeMismatch_error() {

        final ${updateInputTO} input = ${generatorFieldName}.${singleObjectMethodName}(${updateInputTO}.class);
        final ${invalidIdType} ${idField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${invalidIdType}.class);

        final String mutation = """
            mutation($id: ID!, $input: ${updateInputGraphQL}!) {
              update${strippedModelName}(id: $id, input: $input) { ${idField?uncap_first} }
            }
        """;

        this.graphQlTester.document(mutation)
            .variable("id", ${idField?uncap_first})
            .variable("input", input)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).isNotEmpty());
    }

    @Test
    void delete${strippedModelName}() {

        final ${idType} ${idField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${idType}.class);

        final String mutation = """
            mutation($id: ID!) { delete${strippedModelName}(id: $id) }
        """;

        final Boolean deleted = this.graphQlTester.document(mutation)
            .variable("id", ${idField?uncap_first})
            .execute()
            .path("delete${strippedModelName}")
            .entity(Boolean.class)
            .get();

        verify(this.${serviceField}).deleteById(${idField?uncap_first});
        
        assertThat(deleted).isTrue();
    }

    @Test
    void delete${strippedModelName}_idTypeMismatch_error() {

        final ${invalidIdType} ${idField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${invalidIdType}.class);
        final String mutation = """
            mutation($id: ID!) { delete${strippedModelName}(id: $id) }
        """;

        this.graphQlTester.document(mutation)
            .variable("id", ${idField?uncap_first})
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).isNotEmpty());
    }
    <#if relations?has_content>
    <#list relations as rel>
    <#assign relationField = rel.relationField?uncap_first>
    <#assign relationIdType = rel.relationIdType>

    @Test
    void add${relationField?cap_first}To${strippedModelName}() {

        final ${modelName} saved = ${generatorFieldName}.${singleObjectMethodName}(${modelName}.class);
        final ${idType} ${idField?uncap_first} = saved.get${idField?cap_first}();
        final ${relationIdType} ${relationField}Id = ${generatorFieldName}.${singleObjectMethodName}(${relationIdType}.class);

        when(this.${businessServiceField}.add${relationField?cap_first}(
            ${idField?uncap_first}, ${relationField}Id
        )).thenReturn(saved);

        final String mutation = """
            mutation($id: ID!, $relId: ID!) {
              add${relationField?cap_first}To${strippedModelName}(id: $id, ${relationField}Id: $relId) {
                ${idField?uncap_first}
              }
            }
        """;

        final ${transferObjectClass} result = this.graphQlTester.document(mutation)
            .variable("id", ${idField?uncap_first})
            .variable("relId", ${relationField}Id)
            .execute()
            .path("add${relationField?cap_first}To${strippedModelName}")
            .entity(${transferObjectClass}.class)
            .get();

        verify(this.${businessServiceField}).add${relationField?cap_first}(
            ${idField?uncap_first}, ${relationField}Id
        );

        assertThat(result).isNotNull();
        assertThat(result.${idField?uncap_first}()).isEqualTo(saved.get${idField?cap_first}());
    }

    @Test
    void add${relationField?cap_first}To${strippedModelName}_idTypeMismatch_error() {

        final ${invalidIdType} ${idField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${invalidIdType}.class);
        final ${relationIdType} ${relationField}Id = ${generatorFieldName}.${singleObjectMethodName}(${relationIdType}.class);

        final String mutation = """
            mutation($id: ID!, $relId: ID!) {
              add${relationField?cap_first}To${strippedModelName}(id: $id, ${relationField}Id: $relId) { ${idField?uncap_first} }
            }
        """;

        this.graphQlTester.document(mutation)
            .variable("id", ${idField?uncap_first})
            .variable("relId", ${relationField}Id)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).isNotEmpty());
    }

    @Test
    void remove${relationField?cap_first}From${strippedModelName}() {

        final ${modelName} saved = ${generatorFieldName}.${singleObjectMethodName}(${modelName}.class);
        final ${idType} ${idField?uncap_first} = saved.get${idField?cap_first}();<#if rel.isCollection>
        final ${relationIdType} ${relationField}Id = ${generatorFieldName}.${singleObjectMethodName}(${relationIdType}.class)</#if>;

        final String mutation = """
            mutation(<#if rel.isCollection>$id: ID!, $relId: ID!<#else>$id: ID!</#if>) {
              remove${relationField?cap_first}From${strippedModelName}(id: $id<#if rel.isCollection>, ${relationField}Id: $relId</#if>) {
                ${idField?uncap_first}
              }
            }
        """;

        <#if rel.isCollection>
        when(this.${businessServiceField}.remove${relationField?cap_first}(${idField?uncap_first}, ${relationField}Id)).thenReturn(saved);
        <#else>
        when(this.${serviceField}.remove${relationField?cap_first}(${idField?uncap_first})).thenReturn(saved);
        </#if>

        final ${transferObjectClass} result = this.graphQlTester.document(mutation)
            .variable("id", ${idField?uncap_first})
            <#if rel.isCollection>.variable("relId", ${relationField}Id)</#if>
            .execute()
            .path("remove${relationField?cap_first}From${strippedModelName}")
            .entity(${transferObjectClass}.class)
            .get();

        <#if rel.isCollection>
        verify(this.${businessServiceField}).remove${relationField?cap_first}(${idField?uncap_first}, ${relationField}Id);
        <#else>
        verify(this.${serviceField}).remove${relationField?cap_first}(${idField?uncap_first});
        </#if>

        assertThat(result).isNotNull();
        assertThat(result.${idField?uncap_first}()).isEqualTo(saved.get${idField?cap_first}());
    }

    @Test
    void remove${relationField?cap_first}From${strippedModelName}_${idField?uncap_first}TypeMismatch_error() {

        final ${invalidIdType} ${idField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${invalidIdType}.class);
        <#if rel.isCollection>;
        final ${relationIdType} ${relationField}Id = ${generatorFieldName}.${singleObjectMethodName}(${relationIdType}.class)</#if>;

        final String mutation = """
            mutation(<#if rel.isCollection>$id: ID!, $relId: ID!<#else>$id: ID!</#if>) {
              remove${relationField?cap_first}From${strippedModelName}(id: $id<#if rel.isCollection>, ${relationField}Id: $relId</#if>) {
                ${idField?uncap_first}
              }
            }
        """;

        this.graphQlTester.document(mutation)
            .variable("id", ${idField?uncap_first})
            <#if rel.isCollection>.variable("relId", ${relationField}Id)</#if>
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).isNotEmpty());
    }
    <#if rel.isCollection>
    @Test
    void remove${relationField?cap_first}From${strippedModelName}_${relationField?uncap_first}IdTypeMismatch_error() {

        final ${idType} ${idField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${idType}.class);
        final ${rel.invalidRelationIdType} ${relationField}Id = ${generatorFieldName}.${singleObjectMethodName}(${rel.invalidRelationIdType}.class);

        final String mutation = """
            mutation($id: ID!, $relId: ID!$id: ID!) {
              remove${relationField?cap_first}From${strippedModelName}(id: $id, ${relationField}Id: $relId) {
                ${idField?uncap_first}
              }
            }
        """;

        this.graphQlTester.document(mutation)
            .variable("id", ${idField?uncap_first})
            .variable("relId", ${relationField}Id)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).isNotEmpty());
    }</#if>
    </#list>
    </#if>

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
