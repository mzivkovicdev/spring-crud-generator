<#assign uncapModelName = strippedModelName?uncap_first>
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
<#assign serviceClass = strippedModelName?cap_first + "Service">
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign businessServiceClass = strippedModelName?cap_first + "BusinessService">
<#assign businessServiceField = strippedModelName?uncap_first + "BusinessService">
<#assign mapperClass = strippedModelName?cap_first + "RestMapper">
<#assign mapperField = strippedModelName?uncap_first + "RestMapper">
<#assign openApiModel = strippedModelName + "Payload">
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;<#if hasRelations>
import static org.mockito.Mockito.verifyNoInteractions;</#if>
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

<#if hasCollectionRelations>
import java.util.List;

</#if><#t>
${testImports}
${projectImports}<#if dataGenerator == "PODAM">
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;</#if>

@WebMvcTest(excludeAutoConfiguration = {
        OAuth2ClientAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
        GlobalRestExceptionHandler.class, ${controllerClassName}.class
})
class ${className} {

    <#if dataGenerator == "PODAM">
    private static final PodamFactory PODAM_FACTORY = new PodamFactoryImpl();</#if>

    private final ${mapperClass} ${mapperField} = Mappers.getMapper(${mapperClass}.class);
    <#list jsonFields as jsonField>
    <#assign jsonFieldMapperClass = jsonField?cap_first + "RestMapper">
    <#assign jsonFieldMapper = jsonField?cap_first + "Mapper">
    private final ${jsonFieldMapperClass} ${jsonFieldMapper?uncap_first} = Mappers.getMapper(${jsonFieldMapperClass}.class);
    </#list>

    @MockitoBean
    private ${serviceClass?cap_first} ${serviceField};

    <#if hasRelations>
    @MockitoBean
    private ${businessServiceClass?cap_first} ${businessServiceField};
    </#if>

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void after() {
        <#if hasRelations>
        verifyNoMoreInteractions(this.${businessServiceField});
        verifyNoInteractions(this.${serviceField});
        <#else>
        verifyNoMoreInteractions(this.${serviceField});
        </#if>
    }

    @Test
    void ${uncapModelName}sPost() throws Exception {

        final ${modelName} ${modelName?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${modelName}.class);
        <#if swagger>
        final ${openApiModel} body = ${generatorFieldName}.${singleObjectMethodName}(${openApiModel}.class);
        <#else>
        final ${transferObjectClass} body = ${generatorFieldName}.${singleObjectMethodName}(${transferObjectClass}.class);
        </#if><#t>

        <#list inputFields?filter(f -> f.isRelation) as rel>
        <#if !swagger><#assign relationTransferObject = rel.strippedModelName + "TO"><#else><#assign relationTransferObject = rel.strippedModelName + "Payload"></#if>
        <#if rel.isCollection>
        final List<${rel.relationIdType}> ${rel.field}Ids = <#if !swagger>(body.${rel.field}() != null && !body.${rel.field}().isEmpty())<#else>(body.get${rel.field?cap_first}() != null && !body.get${rel.field?cap_first}().isEmpty())</#if> ? 
                List.of() :
                body.<#if !swagger>${rel.field}<#else>get${rel.field?cap_first}</#if>().stream()
                    <#if !swagger>.map(${relationTransferObject}::${rel.relationIdField})<#else>.map(${relationTransferObject}::get${rel.relationIdField?cap_first})</#if>
                    .toList();
        <#else>
        final ${rel.relationIdType} ${rel.field}Id = <#if !swagger>body.${rel.field}() != null ? body.${rel.field}().id() : null;<#else>body.get${rel.field?cap_first}() != null ? body.get${rel.field?cap_first}().getId() : null;</#if>
        </#if>
        </#list>
        <#list inputFields?filter(f -> f.isEnum) as rel>
        <#if swagger>
        final ${rel.fieldType} ${rel.field}Enum = body.get${rel.field?cap_first}() != null ?
                ${rel.fieldType?cap_first}.valueOf(body.get${rel.field?cap_first}().name()) : null;
        </#if>
        </#list>

        when(this.<#if hasRelations>${businessServiceField}<#else>${serviceField}</#if>.create(
                <#if !swagger>
                <#list inputFields as arg><#if arg.isRelation><#if arg.isCollection>${arg.field}Ids<#else>${arg.field}Id</#if><#else><#if arg.isJsonField><#assign jsonMapperClass = arg.fieldType?uncap_first + "Mapper">${jsonMapperClass}.map${arg.fieldType?cap_first}TOTo${arg.fieldType?cap_first}(body.${arg.field}())<#else>body.${arg.field}()</#if></#if><#if arg_has_next>, </#if></#list>
                <#else>
                <#list inputFields as arg><#if arg.isRelation><#if arg.isCollection>${arg.field}Ids<#else>${arg.field}Id</#if><#else><#if arg.isJsonField><#assign jsonMapperClass = arg.fieldType?uncap_first + "Mapper">${jsonMapperClass}.map${arg.fieldType?cap_first}PayloadTo${arg.fieldType?cap_first}(body.get${arg.field?cap_first}())<#else><#if !arg.isEnum>body.get${arg.field?cap_first}()<#else>${arg.field}Enum</#if></#if></#if><#if arg_has_next>, </#if></#list>
                </#if>
        )).thenReturn(${modelName?uncap_first});

        final ResultActions resultActions = this.mockMvc.perform(post("/api/v1/${uncapModelName}s")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(this.objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        final <#if !swagger>${transferObjectClass}<#else>${openApiModel}</#if> result = this.objectMapper.readValue(
                resultActions.andReturn().getResponse().getContentAsString(),
                <#if !swagger>${transferObjectClass?cap_first}<#else>${openApiModel}</#if>.class
        );

        verify${strippedModelName}(result, ${modelName?uncap_first});

        verify(this.<#if hasRelations>${businessServiceField}<#else>${serviceField}</#if>).create(
                <#if !swagger>
                <#list inputFields as arg><#if arg.isRelation><#if arg.isCollection>${arg.field}Ids<#else>${arg.field}Id</#if><#else><#if arg.isJsonField><#assign jsonMapperClass = arg.fieldType?uncap_first + "Mapper">${jsonMapperClass}.map${arg.fieldType?cap_first}TOTo${arg.fieldType?cap_first}(body.${arg.field}())<#else>body.${arg.field}()</#if></#if><#if arg_has_next>, </#if></#list>
                <#else>
                <#list inputFields as arg><#if arg.isRelation><#if arg.isCollection>${arg.field}Ids<#else>${arg.field}Id</#if><#else><#if arg.isJsonField><#assign jsonMapperClass = arg.fieldType?uncap_first + "Mapper">${jsonMapperClass}.map${arg.fieldType?cap_first}PayloadTo${arg.fieldType?cap_first}(body.get${arg.field?cap_first}())<#else><#if !arg.isEnum>body.get${arg.field?cap_first}()<#else>${arg.field}Enum</#if></#if></#if><#if arg_has_next>, </#if></#list>
                </#if>
        );
    }

    @Test
    void ${uncapModelName}sPost_noRequestBody() throws Exception {

        this.mockMvc.perform(post("/api/v1/${uncapModelName}s")
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());
    }

    private void verify${strippedModelName}(final <#if swagger>${openApiModel}<#else>${transferObjectClass?cap_first}</#if> result, final ${modelName} ${modelName?uncap_first}) {
        
        assertThat(result).isNotNull();
        <#if swagger>
        final ${openApiModel} mapped${modelName?cap_first} = ${mapperField}.map${transferObjectClass}To${openApiModel}(
                ${mapperField}.map${modelName?cap_first}To${transferObjectClass}(${modelName?uncap_first})
        );
        <#else>
        final ${transferObjectClass} mapped${modelName?cap_first} = ${mapperField}.map${modelName?cap_first}To${transferObjectClass}(
                ${modelName?uncap_first}
        );
        </#if>
        assertThat(result).isEqualTo(mapped${modelName?cap_first});
    }
}