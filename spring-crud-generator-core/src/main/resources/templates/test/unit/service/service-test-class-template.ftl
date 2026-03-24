import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

${baseImport}
${testImports}
${projectImports}<#if dataGenerator == "PODAM">

import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;</#if>

@ExtendWith(SpringExtension.class)
class ${className} {
    
    <#if dataGenerator == "PODAM">
    private static final PodamFactory PODAM_FACTORY = new PodamFactoryImpl();
    
    </#if><#t>
    <#if isSpringBoot3>@MockBean<#else>@MockitoBean</#if>
    private ${strippedModelName?cap_first}Repository ${strippedModelName?uncap_first}Repository;

    private ${strippedModelName?cap_first}Service ${strippedModelName?uncap_first}Service;

    @AfterEach
    void after() {
        verifyNoMoreInteractions(this.${strippedModelName?uncap_first}Repository);
    }

    @BeforeEach
    void before() {
        ${strippedModelName?uncap_first}Service = new ${strippedModelName?cap_first}Service(this.${strippedModelName?uncap_first}Repository);
    }

    <#if getByIdMethod?? && getByIdMethod?has_content>${getByIdMethod}</#if>
    <#if getAllMethod?? && getAllMethod?has_content>${getAllMethod}</#if>
    <#if createMethod?? && createMethod?has_content>${createMethod}</#if>
    <#if updateMethod?? && updateMethod?has_content>${updateMethod}</#if>
    <#if deleteMethod?? && deleteMethod?has_content>${deleteMethod}</#if>
    <#if addRelationMethod?? && addRelationMethod?has_content>${addRelationMethod}</#if><#if removeRelationMethod?? && removeRelationMethod?has_content>${removeRelationMethod}</#if><#if getAllByIds?? && getAllByIds?has_content>${getAllByIds}</#if>

    private void verify${strippedModelName?cap_first}(final ${modelName} result, final ${modelName} ${modelName?uncap_first}) {

        assertThat(result).isNotNull();
        <#list fieldNames as field>
        assertThat(result.get${field?cap_first}()).isEqualTo(${modelName?uncap_first}.get${field?cap_first}());
        </#list>
        <#list collectionFields as field>
        assertThat(result.get${field?cap_first}()).containsAll(${modelName?uncap_first}.get${field?cap_first}());
        </#list>
    }
}