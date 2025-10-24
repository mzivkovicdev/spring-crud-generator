<#assign uncapModelName = strippedModelName?uncap_first>
<#assign serviceClass = strippedModelName?cap_first + "Service">
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign businessServiceClass = strippedModelName?cap_first + "BusinessService">
<#assign businessServiceField = strippedModelName?uncap_first + "BusinessService">
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;<#if hasRelations>
import static org.mockito.Mockito.verifyNoInteractions;</#if>
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

${testImports}
${projectImports}
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@WebMvcTest(excludeAutoConfiguration = {
        OAuth2ClientAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
        GlobalExceptionHandler.class, ${controllerClassName}.class
})
class ${className} {

    private static final PodamFactory PODAM_FACTORY = new PodamFactoryImpl();

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

        final ${idType} ${idField?uncap_first} = PODAM_FACTORY.manufacturePojo(${idType}.class);

        this.mockMvc.perform(delete("/api/v1/${uncapModelName}s/{id}", ${idField?uncap_first}))
                .andExpect(status().isNoContent());

        verify(this.${serviceField}).deleteById(${idField?uncap_first});
    }

    @Test
    void ${uncapModelName}sIdDelete_invalid${idField?cap_first}Format() throws Exception {

        final ${invalidIdType} ${idField?uncap_first} = PODAM_FACTORY.manufacturePojo(${invalidIdType}.class);

        this.mockMvc.perform(delete("/api/v1/${uncapModelName}s/{id}", ${idField?uncap_first}))
                .andExpect(status().isBadRequest());
    }
}