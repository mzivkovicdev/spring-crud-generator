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

${testImports}
${projectImports}<#if dataGenerator == "PODAM">
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
        <#if hasRelations>verifyNoInteractions(this.${businessServiceField});</#if>
        verifyNoMoreInteractions(this.${serviceField});
    }

    @Test
    void ${uncapModelName}sIdDelete() throws Exception {

        final ${idType} ${idField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${idType}.class);

        this.mockMvc.perform(delete("${basePath}/${uncapModelName}s/{id}", ${idField?uncap_first}))
                .andExpect(status().isNoContent());

        verify(this.${serviceField}).deleteById(${idField?uncap_first});
    }

    @Test
    void ${uncapModelName}sIdDelete_invalid${idField?cap_first}Format() throws Exception {

        final ${invalidIdType} ${idField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${invalidIdType}.class);

        this.mockMvc.perform(delete("${basePath}/${uncapModelName}s/{id}", ${idField?uncap_first}))
                .andExpect(status().isBadRequest());
    }
}