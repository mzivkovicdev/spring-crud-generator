<#assign uncapModelName = strippedModelName?uncap_first>
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
<#assign serviceClass = strippedModelName?cap_first + "Service">
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign businessServiceClass = strippedModelName?cap_first + "BusinessService">
<#assign businessServiceField = strippedModelName?uncap_first + "BusinessService">
<#assign mapperClass = strippedModelName?cap_first + "RestMapper">
<#assign mapperField = strippedModelName?uncap_first + "RestMapper">
<#if swagger><#assign responseClass = strippedModelName + "sGet200Response"></#if>
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;<#if hasRelations>
import static org.mockito.Mockito.verifyNoInteractions;</#if>
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

${testImports}
${projectImports}
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@WebMvcTest(excludeAutoConfiguration = {
        OAuth2ClientAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
        GlobalRestExceptionHandler.class, ${controllerClassName}.class
})
class ${className} {

    private static final PodamFactory PODAM_FACTORY = new PodamFactoryImpl();

    private final ${mapperClass} ${mapperField} = Mappers.getMapper(${mapperClass}.class);

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
    void ${uncapModelName}sIdGet() throws Exception {

        final ${modelName} ${modelName?uncap_first} = PODAM_FACTORY.manufacturePojo(${modelName}.class);
        final ${idType} ${idField?uncap_first} = ${modelName?uncap_first}.get${idField?cap_first}();

        when(this.${serviceField}.getById(${idField?uncap_first})).thenReturn(${modelName?uncap_first});

        final ResultActions resultActions = this.mockMvc.perform(get("/api/v1/${uncapModelName}s/{id}", ${idField?uncap_first}))
                .andExpect(status().isOk());

        final <#if !swagger>${transferObjectClass}<#else>${strippedModelName?cap_first}</#if> result = this.objectMapper.readValue(
                resultActions.andReturn().getResponse().getContentAsString(),
                <#if !swagger>${transferObjectClass?cap_first}<#else>${strippedModelName?cap_first}</#if>.class
        );

        verify${strippedModelName}(result, ${modelName?uncap_first});

        verify(this.${serviceField}).getById(${idField?uncap_first});
    }

    @Test
    void ${uncapModelName}sIdGet_invalid${idField?cap_first}Format() throws Exception {

        final ${invalidIdType} ${idField?uncap_first} = PODAM_FACTORY.manufacturePojo(${invalidIdType}.class);

        this.mockMvc.perform(get("/api/v1/${uncapModelName}s/{id}", ${idField?uncap_first}))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ${uncapModelName}sGet() throws Exception {

        final List<${modelName}> ${modelName?uncap_first}s = PODAM_FACTORY.manufacturePojo(List.class, ${modelName}.class);
        final Page<${modelName}> page${modelName}s = new PageImpl<>(${modelName?uncap_first}s);
        final Integer pageNumber = PODAM_FACTORY.manufacturePojo(Integer.class);
        final Integer pageSize = PODAM_FACTORY.manufacturePojo(Integer.class);

        when(this.${serviceField}.getAll(pageNumber, pageSize)).thenReturn(page${modelName}s);

        final ResultActions resultActions = this.mockMvc.perform(get("/api/v1/${uncapModelName}s")
                                .queryParam("pageNumber", String.format("%s", pageNumber))
                                .queryParam("pageSize", String.format("%s", pageSize)))
                        .andExpect(status().isOk());

        <#if !swagger>
        final PageTO<${transferObjectClass}> results = this.objectMapper.readValue(
                resultActions.andReturn().getResponse().getContentAsString(),
                ${transferObjectClass?cap_first}.class
        );
        <#else>
        final ${responseClass} results = this.objectMapper.readValue(
                resultActions.andReturn().getResponse().getContentAsString(),
                ${responseClass}.class
        );
        </#if>

        assertThat(results).isNotNull();
        assertThat(results.<#if swagger>getTotalPages<#else>totalPages</#if>()).isNotNegative();
        assertThat(results.<#if swagger>getTotalElements<#else>totalElements</#if>()).isNotNegative();
        assertThat(results.<#if swagger>getSize<#else>size</#if>()).isNotNegative();
        assertThat(results.<#if swagger>getNumber<#else>number</#if>()).isNotNegative();
        assertThat(results.<#if swagger>getContent<#else>content</#if>()).isNotEmpty();

        results.getContent().forEach(result -> {

            final ${modelName} ${modelName?uncap_first} = ${modelName?uncap_first}s.stream()
                    .filter(${strippedModelName?uncap_first} -> ${strippedModelName?uncap_first}.get${idField?cap_first}().toString().equals(result.<#if !swagger>${idField?uncap_first}<#else>get${idField?cap_first}</#if>().toString()))
                    .findFirst()
                    .orElseThrow();

            verify${strippedModelName}(result, ${modelName?uncap_first});
        });

        verify(this.${serviceField}).getAll(pageNumber, pageSize);
    }

    @Test
    void ${uncapModelName}sGet_missingPageNumberParameter() throws Exception {

        final Integer pageSize = PODAM_FACTORY.manufacturePojo(Integer.class);

        this.mockMvc.perform(get("/api/v1/${uncapModelName}s")
                                .queryParam("pageSize", String.format("%s", pageSize)))
                        .andExpect(status().isBadRequest());
    }

    @Test
    void ${uncapModelName}sGet_missingPageSizeParameter() throws Exception {

        final Integer pageNumber = PODAM_FACTORY.manufacturePojo(Integer.class);

        this.mockMvc.perform(get("/api/v1/${uncapModelName}s")
                                .queryParam("pageNumber", String.format("%s", pageNumber)))
                        .andExpect(status().isBadRequest());
    }

    @Test
    void ${uncapModelName}sGet_typeMissmatch() throws Exception {

        final String pageNumber = PODAM_FACTORY.manufacturePojo(String.class);
        final String pageSize = PODAM_FACTORY.manufacturePojo(String.class);

        this.mockMvc.perform(get("/api/v1/${uncapModelName}s")
                                .queryParam("pageSize", String.format("%s", pageSize))
                                .queryParam("pageNumber", String.format("%s", pageNumber)))
                        .andExpect(status().isBadRequest());
    }

    private void verify${strippedModelName}(final <#if swagger>${strippedModelName?cap_first}<#else>${transferObjectClass?cap_first}</#if> result, final ${modelName} ${modelName?uncap_first}) {
        
        assertThat(result).isNotNull();
        <#if swagger>
        final ${strippedModelName?cap_first} mapped${modelName?cap_first} = ${mapperField}.map${transferObjectClass}To${strippedModelName}(
                ${mapperField}.map${modelName?cap_first}To${transferObjectClass}(${modelName?uncap_first})
        );
        <#else>
        final ${transferObjectClass} mapped${modelName?cap_first} = ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
                ${modelName?uncap_first}
        );
        </#if>
        assertThat(result).isEqualTo(mapped${modelName?cap_first});
    }

}