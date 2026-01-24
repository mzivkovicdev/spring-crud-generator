<#setting number_format="computer">
<#assign uncapModelName = strippedModelName?uncap_first>
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
<#assign updateTransferObjectClass = strippedModelName?cap_first + "UpdateTO">
<#assign serviceClass = strippedModelName?cap_first + "Service">
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign businessServiceClass = strippedModelName?cap_first + "BusinessService">
<#assign businessServiceField = strippedModelName?uncap_first + "BusinessService">
<#assign mapperClass = strippedModelName?cap_first + "RestMapper">
<#assign mapperField = strippedModelName?uncap_first + "RestMapper">
<#assign requestModelName = strippedModelName?cap_first + "UpdatePayload">
<#assign responseModelName = strippedModelName?cap_first + "Payload">
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;<#if hasRelations>
import static org.mockito.Mockito.verifyNoInteractions;</#if>
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
<#if isIdUuid>

import java.util.UUID;
</#if><#t>

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
    
    </#if><#t>
    @Autowired
    private JsonMapper mapper;

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
        final ${requestModelName} body = ${generatorFieldName}.${singleObjectMethodName}(${requestModelName}.class);
        <#if fieldsWithLength??>
        <#list fieldsWithLength as fieldWithLength>
        body.${fieldWithLength.field}(generateString(${fieldWithLength.length}));
        </#list>
        </#if><#t>
        <#else>
        <#if fieldsWithLength??>
        final ${updateTransferObjectClass} body = generate${updateTransferObjectClass}();
        <#else>
        final ${updateTransferObjectClass} body = ${generatorFieldName}.${singleObjectMethodName}(${updateTransferObjectClass}.class);
        </#if><#t>
        </#if>

        when(this.${serviceField}.updateById(${idField?uncap_first}, <#list inputFields as arg>${arg}<#if arg_has_next>, </#if></#list>)).thenReturn(${modelName?uncap_first});

        final ResultActions resultActions = this.mockMvc.perform(put("${basePath}/${uncapModelName}s/{id}", ${idField?uncap_first})
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(this.mapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        final <#if !swagger>${transferObjectClass}<#else>${responseModelName}</#if> result = this.mapper.readValue(
                resultActions.andReturn().getResponse().getContentAsString(),
                <#if !swagger>${transferObjectClass?cap_first}<#else>${responseModelName}</#if>.class
        );

        verify${strippedModelName}(result, ${modelName?uncap_first});

        verify(this.${serviceField}).updateById(${idField?uncap_first}, <#list inputFields as arg>${arg}<#if arg_has_next>, </#if></#list>);
    }

    @Test
    void ${uncapModelName}sIdPut_invalid${idField?cap_first}Format() throws Exception {

        final ${invalidIdType} ${idField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${invalidIdType}.class);
        <#if swagger>
        final ${requestModelName} body = ${generatorFieldName}.${singleObjectMethodName}(${requestModelName}.class);
        <#else>
        final ${updateTransferObjectClass} body = ${generatorFieldName}.${singleObjectMethodName}(${updateTransferObjectClass}.class);
        </#if>

        this.mockMvc.perform(put("${basePath}/${uncapModelName}s/{id}", ${idField?uncap_first})
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(this.mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
    <#if fieldsWithLength??>

    @Test
    void ${uncapModelName}sIdPut_validationFails() throws Exception {

        final ${modelName} ${modelName?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${modelName}.class);
        final ${idType} ${idField?uncap_first} = ${modelName?uncap_first}.get${idField?cap_first}();
        <#if swagger>
        final ${requestModelName} body = ${generatorFieldName}.${singleObjectMethodName}(${requestModelName}.class);
        <#if fieldsWithLength??>
        <#list fieldsWithLength as fieldWithLength>
        body.${fieldWithLength.field}(generateString(${fieldWithLength.length + 10}));
        </#list>
        </#if><#t>
        <#else>
        final ${updateTransferObjectClass} body = generateInvalid${updateTransferObjectClass}();
        </#if>

        this.mockMvc.perform(put("${basePath}/${uncapModelName}s/{id}", ${idField?uncap_first})
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(this.mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
    </#if>

    @Test
    void ${uncapModelName}sIdPut_noRequestBody() throws Exception {

        final ${idType} ${idField?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${idType}.class);

        this.mockMvc.perform(put("${basePath}/${uncapModelName}s/{id}", ${idField?uncap_first})
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());
    }

    private void verify${strippedModelName}(final <#if swagger>${responseModelName}<#else>${transferObjectClass?cap_first}</#if> result, final ${modelName} ${modelName?uncap_first}) {
        
        assertThat(result).isNotNull();
        <#if swagger>
        final ${responseModelName} mapped${modelName?cap_first} = ${mapperField}.map${transferObjectClass}To${responseModelName}(
                ${mapperField}.map${modelName?cap_first}To${transferObjectClass}(${modelName?uncap_first})
        );
        <#else>
        final ${transferObjectClass} mapped${modelName?cap_first} = ${mapperField}.map${modelName?cap_first}To${transferObjectClass}(
                ${modelName?uncap_first}
        );
        </#if>
        assertThat(result).isEqualTo(mapped${modelName?cap_first});
    }

    <#if fieldsWithLength?? && !swagger>

    private static ${updateTransferObjectClass} generate${updateTransferObjectClass}() {
        <#assign needInput = (fieldsWithLength?default([])?size != fieldNames?default([])?size)>
        <#if needInput>
        final ${updateTransferObjectClass} input = ${generatorFieldName}.${singleObjectMethodName}(${updateTransferObjectClass}.class);
        </#if>
        return new ${updateTransferObjectClass}(
            <#list fieldNames as fieldName>
                <#assign matched = false>
                <#list (fieldsWithLength?default([])) as fwl><#if fwl.field == fieldName>generateString(${fwl.length})<#assign matched = true><#break></#if></#list><#if !matched>input.${fieldName}()</#if><#if fieldName_has_next>,</#if>
            </#list>
        );
    }

    private static ${updateTransferObjectClass} generateInvalid${updateTransferObjectClass}() {
        <#assign needInput = (fieldsWithLength?default([])?size != fieldNames?default([])?size)>
        <#if needInput>
        final ${updateTransferObjectClass} input = ${generatorFieldName}.${singleObjectMethodName}(${updateTransferObjectClass}.class);
        </#if>
        return new ${updateTransferObjectClass}(
            <#list fieldNames as fieldName>
                <#assign matched = false>
                <#list (fieldsWithLength?default([])) as fwl><#if fwl.field == fieldName>generateString(${fwl.length + 10})<#assign matched = true><#break></#if></#list><#if !matched>input.${fieldName}()</#if><#if fieldName_has_next>,</#if>
            </#list>
        );
    }

    </#if><#t>
    <#if fieldsWithLength?? && dataGenerator == "PODAM">
    private static String generateString(final int n) {
        final PodamFactory p = new PodamFactoryImpl();
        p.getStrategy().addOrReplaceTypeManufacturer(String.class, (strategy, attributeMetadata, genericTypesArgumentsMap) -> {
            final java.security.SecureRandom rnd = new java.security.SecureRandom();
            final char[] alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

            final StringBuilder sb = new StringBuilder(n);
            for (int i = 0; i < n; i++) {
                sb.append(alphabet[rnd.nextInt(alphabet.length)]);
            }
            return sb.toString();
        });
        return p.manufacturePojo(String.class);
    }
    </#if><#t>
    <#if fieldsWithLength?? && dataGenerator == "INSTANCIO">
    private static String generateString(final int n) {
        return Instancio.gen().string()
                .length(n)
                .get();
    }
    </#if><#t>
}