import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

${baseImport}
${testImports}
${projectImports}

import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@ExtendWith(SpringExtension.class)
class ${className} {

    private static final PodamFactory PODAM_FACTORY = new PodamFactoryImpl();

    <#list serviceClasses as serviceClass>
    @MockitoBean
    private ${serviceClass?cap_first} ${serviceClass?uncap_first};

    </#list>
    private ${strippedModelName?cap_first}BusinessService ${strippedModelName?uncap_first}BusinessService;

    @AfterEach
    void after() {
        verifyNoMoreInteractions(
            <#list serviceClasses as serviceClass>this.${serviceClass?uncap_first}<#if serviceClass_has_next>, </#if></#list>
        );
    }

    @BeforeEach
    void before() {
        ${strippedModelName?uncap_first}BusinessService = new ${strippedModelName?cap_first}BusinessService(
            <#list serviceClasses as serviceClass>this.${serviceClass?uncap_first}<#if serviceClass_has_next>, </#if></#list>
        );
    }

    <#if createResource?? && createResource?has_content>${createResource}</#if>
    <#if addRelationMethod?? && addRelationMethod?has_content>${addRelationMethod}</#if>
    <#if removeRelationMethod?? && removeRelationMethod?has_content>${removeRelationMethod}</#if>

    private void verify${strippedModelName?cap_first}(final ${modelName} result, final ${modelName} ${modelName?uncap_first}) {

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(${modelName?uncap_first});
    }
}