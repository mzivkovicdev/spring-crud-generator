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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        <#if hasRelations>verifyNoInteractions(this.${businessServiceField});</#if>
        verifyNoMoreInteractions(this.${serviceField});
    }

    @Test
    void ${uncapModelName}sIdPut() throws Exception {

        final ${modelName} ${modelName?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${modelName}.class);
        final ${idType} ${idField?uncap_first} = ${modelName?uncap_first}.get${idField?cap_first}();
        <#if swagger>
        final ${openApiModel} body = ${generatorFieldName}.${singleObjectMethodName}(${openApiModel}.class);
        <#else>
        final ${transferObjectClass} body = ${generatorFieldName}.${singleObjectMethodName}(${transferObjectClass}.class);
        </#if>

        when(this.${serviceField}.updateById(${idField?uncap_first}, <#list inputFields as arg>${arg}<#if arg_has_next>, </#if></#list>)).thenReturn(${modelName?uncap_first});

        final ResultActions resultActions = this.mockMvc.perform(put("/api/v1/${uncapModelName}s/{id}", ${idField?uncap_first})
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(this.objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        final <#if !swagger>${transferObjectClass}<#else>${openApiModel}</#if> result = this.objectMapper.readValue(
                resultActions.andReturn().getResponse().getContentAsString(),
                <#if !swagger>${transferObjectClass?cap_first}<#else>${openApiModel}</#if>.class
        );

        verify${strippedModelName}(result, ${modelName?uncap_first});

        verify(this.${serviceField}).updateById(${idField?uncap_first}, <#list inputFields as arg>${arg}<#if arg_has_next>, </#if></#list>);
    }

    @Test
    void ${uncapModelName}sIdPut_invalid${idField?cap_first}Format() throws Exception {

        final ${invalidIdType} ${idField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${invalidIdType}.class);
        <#if swagger>
        final ${openApiModel} body = ${generatorFieldName}.${singleObjectMethodName}(${openApiModel}.class);
        <#else>
        final ${transferObjectClass} body = ${generatorFieldName}.${singleObjectMethodName}(${transferObjectClass}.class);
        </#if>

        this.mockMvc.perform(put("/api/v1/${uncapModelName}s/{id}", ${idField?uncap_first})
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(this.objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ${uncapModelName}sIdPut_noRequestBody() throws Exception {

        final ${idType} ${idField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${idType}.class);

        this.mockMvc.perform(put("/api/v1/${uncapModelName}s/{id}", ${idField?uncap_first})
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