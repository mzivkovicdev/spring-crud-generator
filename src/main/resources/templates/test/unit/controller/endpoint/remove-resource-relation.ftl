<#assign uncapModelName = strippedModelName?uncap_first>
<#assign serviceClass = strippedModelName?cap_first + "Service">
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign businessServiceClass = strippedModelName?cap_first + "BusinessService">
<#assign businessServiceField = strippedModelName?uncap_first + "BusinessService">
import static org.mockito.Mockito.verify;<#if hasRelations>
import static org.mockito.Mockito.verifyNoInteractions;</#if>
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

${baseImports}
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

    @MockitoBean
    private ${serviceClass?cap_first} ${serviceField};

    <#if hasRelations>
    @MockitoBean
    private ${businessServiceClass?cap_first} ${businessServiceField};
    </#if>

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void after() {
        <#if isCollection>
        verifyNoMoreInteractions(this.${businessServiceField});
        verifyNoInteractions(this.${serviceField});
        <#else>
        verifyNoMoreInteractions(this.${serviceField});
        verifyNoInteractions(this.${businessServiceField});
        </#if>
    }

    @Test
    void ${methodName}() throws Exception {

        final ${idType} ${idField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${idType}.class);
        <#if isCollection>
        final ${relIdType} ${relIdField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${relIdType}.class);
        </#if>

        this.mockMvc.perform(delete("/api/v1/${uncapModelName}s/{id}/${strippedRelationClassName?uncap_first}s<#if isCollection>/{relationId}</#if>", ${idField?uncap_first}<#if isCollection>, ${relIdField?uncap_first}</#if>))
                .andExpect(status().isNoContent());

        <#if isCollection>
        verify(this.${businessServiceField}).remove${relationFieldModel}(${idField?uncap_first}, ${relIdField?uncap_first});
        <#else>
        verify(this.${serviceField}).remove${relationFieldModel}(${idField?uncap_first});
        </#if>
    }

    @Test
    void ${methodName}_invalid${idField?cap_first}Format() throws Exception {

        final ${invalidIdType} ${idField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${invalidIdType}.class);
        <#if isCollection>
        final ${relIdType} ${relIdField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${relIdType}.class);
        </#if>

        this.mockMvc.perform(delete("/api/v1/${uncapModelName}s/{id}/${strippedRelationClassName?uncap_first}s<#if isCollection>/{relationId}</#if>", ${idField?uncap_first}<#if isCollection>, ${relIdField?uncap_first}</#if>))
                .andExpect(status().isBadRequest());
    }

    <#if isCollection>
    @Test
    void ${methodName}_invalid${relIdField?cap_first}Format() throws Exception {

        final ${idType} ${idField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${idType}.class);
        final ${invalidRelIdType} ${relIdField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${invalidRelIdType}.class);

        this.mockMvc.perform(delete("/api/v1/${uncapModelName}s/{id}/${strippedRelationClassName?uncap_first}s/{relationId}", ${idField?uncap_first}<#if isCollection>, ${relIdField?uncap_first}</#if>))
                .andExpect(status().isBadRequest());
    }
    </#if>

}