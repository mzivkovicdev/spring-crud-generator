<#assign uncapModelName = strippedModelName?uncap_first>
<#assign serviceClass = strippedModelName?cap_first + "Service">
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign businessServiceClass = strippedModelName?cap_first + "BusinessService">
<#assign businessServiceField = strippedModelName?uncap_first + "BusinessService">
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
<#assign mapperClass = strippedModelName?cap_first + "RestMapper">
<#assign mapperField = strippedModelName?uncap_first + "RestMapper">
<#assign openApiModel = strippedModelName + "Payload">
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

${baseImports}
${testImports}
${projectImports}
import tools.jackson.databind.json.JsonMapper;<#if dataGenerator == "PODAM">
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;</#if>

@WebMvcTest(excludeAutoConfiguration = {
        OAuth2ClientAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
        <#if isGlobalExceptionHandlerEnabled>GlobalRestExceptionHandler.class, </#if>${controllerClassName}.class
})
class ${className} {

    <#if dataGenerator == "PODAM">
    private static final PodamFactory PODAM_FACTORY = new PodamFactoryImpl();
    
    </#if><#t>
    private final ${mapperClass} ${mapperField} = Mappers.getMapper(${mapperClass}.class);

    @MockitoBean
    private ${serviceClass?cap_first} ${serviceField};

    @MockitoBean
    private ${businessServiceClass?cap_first} ${businessServiceField};

    @Autowired
    private JsonMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void after() {
        verifyNoMoreInteractions(this.${businessServiceField});
        verifyNoInteractions(this.${serviceField});
    }

    @Test
    void ${methodName}() throws Exception {

        final ${modelName} ${modelName?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${modelName}.class);
        final ${idType} ${idField?uncap_first} = ${modelName?uncap_first}.get${idField?cap_first}();
        <#if swagger>
        final ${strippedRelationClassName}Input body = ${generatorFieldName}.${singleObjectMethodName}(${strippedRelationClassName}Input.class);
        <#else>
        final ${strippedRelationClassName}InputTO body = ${generatorFieldName}.${singleObjectMethodName}(${strippedRelationClassName}InputTO.class);
        </#if>

        when(this.${businessServiceField}.add${relationFieldModel}(${idField?uncap_first}, body.<#if swagger>getId<#else>id</#if>())).thenReturn(${modelName?uncap_first});
        
        final ResultActions resultActions = this.mockMvc.perform(post("${basePath}/${uncapModelName}s/{id}/${strippedRelationClassName?uncap_first}s", ${idField?uncap_first})
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(this.mapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        final <#if !swagger>${transferObjectClass}<#else>${openApiModel}</#if> result = this.mapper.readValue(
                resultActions.andReturn().getResponse().getContentAsString(),
                <#if !swagger>${transferObjectClass?cap_first}<#else>${openApiModel}</#if>.class
        );

        verify${strippedModelName}(result, ${modelName?uncap_first});

        verify(this.${businessServiceField}).add${relationFieldModel}(${idField?uncap_first}, body.<#if swagger>getId<#else>id</#if>());
    }

    @Test
    void ${methodName}_invalid${idField?cap_first}Format() throws Exception {

        final ${invalidIdType} ${idField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${invalidIdType}.class);
        <#if swagger>
        final ${strippedRelationClassName}Input body = ${generatorFieldName}.${singleObjectMethodName}(${strippedRelationClassName}Input.class);
        <#else>
        final ${strippedRelationClassName}InputTO body = ${generatorFieldName}.${singleObjectMethodName}(${strippedRelationClassName}InputTO.class);
        </#if>
        
        this.mockMvc.perform(post("${basePath}/${uncapModelName}s/{id}/${strippedRelationClassName?uncap_first}s", ${idField?uncap_first})
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(this.mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ${methodName}_noRequestBody() throws Exception {

        final ${idType} ${idField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${idType}.class);
        
        this.mockMvc.perform(post("${basePath}/${uncapModelName}s/{id}/${strippedRelationClassName?uncap_first}s", ${idField?uncap_first})
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