<#setting number_format="computer">
<#assign uncapModelName = strippedModelName?uncap_first>
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
<#assign createTransferObjectClass = strippedModelName?cap_first + "CreateTO">
<#assign serviceClass = strippedModelName?cap_first + "Service">
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign businessServiceClass = strippedModelName?cap_first + "BusinessService">
<#assign businessServiceField = strippedModelName?uncap_first + "BusinessService">
<#assign mapperClass = strippedModelName?cap_first + "RestMapper">
<#assign mapperField = strippedModelName?uncap_first + "RestMapper">
<#assign requestModelName = strippedModelName?cap_first + "CreatePayload">
<#assign responseModelName = strippedModelName?cap_first + "Payload">
<#assign mockitoAnnotation = isSpringBoot3?then("@MockBean", "@MockitoBean")>
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;<#if hasRelations>
import static org.mockito.Mockito.verifyNoInteractions;</#if>
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

${testImports}
${projectImports}
<#if isSpringBoot3>import com.fasterxml.jackson.databind.ObjectMapper;<#else>import tools.jackson.databind.json.JsonMapper;</#if><#if dataGenerator == "PODAM">
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

    ${mockitoAnnotation}
    private ${serviceClass?cap_first} ${serviceField};

    <#if hasRelations>
    ${mockitoAnnotation}
    private ${businessServiceClass?cap_first} ${businessServiceField};
    
    </#if><#t>
    @Autowired
    private <#if isSpringBoot3>ObjectMapper<#else>JsonMapper</#if> mapper;

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
    void ${uncapModelName}sBulkPost() throws Exception {

        final ${modelName} ${modelName?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${modelName}.class);
        final List<${modelName}> ${modelName?uncap_first}s = List.of(${modelName?uncap_first});
        <#if swagger>
        final ${requestModelName} bodyItem = ${generatorFieldName}.${singleObjectMethodName}(${requestModelName}.class);
        <#if validationOverrides??>
        <#list validationOverrides as ov>
        bodyItem.${ov.field}(${ov.validValue});
        </#list>
        </#if>
        final List<${requestModelName}> body = List.of(bodyItem);
        <#else>
        <#if validationOverrides??>
        final List<${createTransferObjectClass}> body = List.of(generate${createTransferObjectClass}());
        <#else>
        final ${createTransferObjectClass} bodyItem = ${generatorFieldName}.${singleObjectMethodName}(${createTransferObjectClass}.class);
        final List<${createTransferObjectClass}> body = List.of(bodyItem);
        </#if><#t>
        </#if><#t>

        when(this.<#if hasRelations>${businessServiceField}<#else>${serviceField}</#if>.bulkCreate(any()))
                .thenReturn(${modelName?uncap_first}s);

        final ResultActions resultActions = this.mockMvc.perform(post("${basePath}/${uncapModelName}s/bulk")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(this.mapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        final <#if !swagger>${transferObjectClass}<#else>${responseModelName}</#if>[] results = this.mapper.readValue(
                resultActions.andReturn().getResponse().getContentAsString(),
                <#if !swagger>${transferObjectClass?cap_first}<#else>${responseModelName}</#if>[].class
        );

        assertThat(results).isNotNull();
        assertThat(results).hasSize(${modelName?uncap_first}s.size());

        for (int i = 0; i < results.length; i++) {
            verify${strippedModelName}(results[i], ${modelName?uncap_first}s.get(i));
        }

        verify(this.<#if hasRelations>${businessServiceField}<#else>${serviceField}</#if>).bulkCreate(any());
    }
    <#if validationOverrides??>

    @Test
    void ${uncapModelName}sBulkPost_validationFails() throws Exception {

        <#if swagger>
        final ${requestModelName} bodyItem = ${generatorFieldName}.${singleObjectMethodName}(${requestModelName}.class);
        <#list validationOverrides as ov>
        bodyItem.${ov.field}(${ov.invalidValue});
        </#list>
        final List<${requestModelName}> body = List.of(bodyItem);
        <#else>
        final List<${createTransferObjectClass}> body = List.of(generateInvalid${createTransferObjectClass}());
        </#if><#t>

        this.mockMvc.perform(post("${basePath}/${uncapModelName}s/bulk")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(this.mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
    </#if>

    @Test
    void ${uncapModelName}sBulkPost_noRequestBody() throws Exception {

        this.mockMvc.perform(post("${basePath}/${uncapModelName}s/bulk")
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

    <#if validationOverrides?? && !swagger>

    private static ${createTransferObjectClass} generate${createTransferObjectClass}() {
        final ${createTransferObjectClass} input = ${generatorFieldName}.${singleObjectMethodName}(${createTransferObjectClass}.class);
        return new ${createTransferObjectClass}(
            <#list fieldNames as fieldName>
                <#assign matched = false>
                <#list (validationOverrides?default([])) as ov><#if ov.field == fieldName>${ov.validValue}<#assign matched = true><#break></#if></#list><#if !matched>input.${fieldName}()</#if><#if fieldName_has_next>,</#if>
            </#list>
        );
    }

    private static ${createTransferObjectClass} generateInvalid${createTransferObjectClass}() {
        final ${createTransferObjectClass} input = ${generatorFieldName}.${singleObjectMethodName}(${createTransferObjectClass}.class);
        return new ${createTransferObjectClass}(
            <#list fieldNames as fieldName>
                <#assign matched = false>
                <#list (validationOverrides?default([])) as ov><#if ov.field == fieldName>${ov.invalidValue}<#assign matched = true><#break></#if></#list><#if !matched>input.${fieldName}()</#if><#if fieldName_has_next>,</#if>
            </#list>
        );
    }

    </#if><#t>
    <#if validationOverrides?? && hasGenerateString?? && hasGenerateString && dataGenerator == "PODAM">
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
    <#if validationOverrides?? && hasGenerateString?? && hasGenerateString && dataGenerator == "INSTANCIO">
    private static String generateString(final int n) {
        return Instancio.gen().string()
                .length(n)
                .get();
    }
    </#if><#t>
    <#if validationOverrides?? && hasGenerateList?? && hasGenerateList>

    private static <T> List<T> generateList(final int n, final java.util.function.Supplier<T> supplier) {
        if (n <= 0) {
            return List.of();
        }
        final java.util.ArrayList<T> list = new java.util.ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(supplier.get());
        }
        return list;
    }
    
    </#if>
}
