<#assign modelNameUncapFirst = modelName?uncap_first>
<#assign transferObjectUncapFirst = transferObjectName?uncap_first>
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

<#if dataGenerator == "INSTANCIO">
import org.instancio.Instancio;
</#if>import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

${projectImports}
<#if dataGenerator == "PODAM">

import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

</#if><#t>
class ${className} {
    <#if dataGenerator == "PODAM">

    private static final PodamFactory PODAM_FACTORY = new PodamFactoryImpl();
    </#if><#t>
    <#if isGraphQL>

    private final ${strippedModelName?cap_first}GraphQLMapper ${strippedModelName?uncap_first}Mapper = Mappers.getMapper(${strippedModelName?cap_first}GraphQLMapper.class);
    <#else>

    private final ${strippedModelName?cap_first}RestMapper ${strippedModelName?uncap_first}Mapper = Mappers.getMapper(${strippedModelName?cap_first}RestMapper.class);
    </#if><#t>

    @Test
    void map${modelName}To${transferObjectName}() {

        final ${modelName} ${modelNameUncapFirst} = ${generatorFieldName}.${singleObjectMethodName}(${modelName}.class);

        final ${transferObjectName} result = this.${strippedModelName?uncap_first}Mapper.map${modelName}To${transferObjectName}(${modelNameUncapFirst});

        verify${transferObjectName}(result, ${modelNameUncapFirst});
    }

    @Test
    void map${modelName}To${transferObjectName}_list() {

        <#if dataGenerator == "PODAM">
        final List<${modelName}> ${modelNameUncapFirst}s = ${generatorFieldName}.${multipleObjectsMethodName}(List.class, ${modelName}.class);
        <#else>
        final List<${modelName}> ${modelNameUncapFirst}s = ${generatorFieldName}.${multipleObjectsMethodName}(${modelName}.class)
                .size(10)
                .create();
        </#if>

        final List<${transferObjectName}> results = this.${strippedModelName?uncap_first}Mapper.map${modelName}To${transferObjectName}(${modelNameUncapFirst}s);

        results.forEach(result -> {

            final ${modelName} ${modelNameUncapFirst} = ${modelName?uncap_first}s.stream()
                    .filter(obj -> obj.get${idField?cap_first}().equals(result.${idField?uncap_first}()))
                    .findFirst()
                    .orElseThrow();
            
            verify${transferObjectName}(result, ${modelNameUncapFirst});
        });
    }

    @Test
    void map${transferObjectName}To${modelName}() {

        final ${transferObjectName} ${transferObjectUncapFirst} = ${generatorFieldName}.${singleObjectMethodName}(${transferObjectName}.class);

        final ${modelName} result = this.${strippedModelName?uncap_first}Mapper.map${transferObjectName}To${modelName}(${transferObjectUncapFirst});

        verify${modelName}(result, ${transferObjectUncapFirst});
    }

    @Test
    void map${transferObjectName}To${modelName}_list() {

        <#if dataGenerator == "PODAM">
        final List<${transferObjectName}> ${transferObjectUncapFirst}s = ${generatorFieldName}.${multipleObjectsMethodName}(List.class, ${transferObjectName}.class);
        <#else>
        final List<${transferObjectName}> ${transferObjectUncapFirst}s = ${generatorFieldName}.${multipleObjectsMethodName}(${transferObjectName}.class)
                .size(10)
                .create();
        </#if>

        final List<${modelName}> results = this.${strippedModelName?uncap_first}Mapper.map${transferObjectName}To${modelName}(${transferObjectUncapFirst}s);

        results.forEach(result -> {

            final ${transferObjectName} ${transferObjectUncapFirst} = ${transferObjectUncapFirst}s.stream()
                    .filter(${strippedModelName?uncap_first} -> ${strippedModelName?uncap_first}.${idField?uncap_first}().equals(result.get${idField?cap_first}()))
                    .findFirst()
                    .orElseThrow();
            
            verify${modelName}(result, ${transferObjectUncapFirst});
        });
    }

    <#if swagger?? && swagger>
    @Test
    void map${transferObjectName}To${swaggerModel}() {

        final ${transferObjectName} ${transferObjectUncapFirst} = ${generatorFieldName}.${singleObjectMethodName}(${transferObjectName}.class);

        final ${swaggerModel} result = this.${strippedModelName?uncap_first}Mapper.map${transferObjectName}To${swaggerModel}(${transferObjectUncapFirst});

        verify${swaggerModel}(result, ${transferObjectUncapFirst});
    }

    @Test
    void map${transferObjectName}To${swaggerModel}_list() {

        <#if dataGenerator == "PODAM">
        final List<${transferObjectName}> ${transferObjectUncapFirst}s = ${generatorFieldName}.${multipleObjectsMethodName}(List.class, ${transferObjectName}.class);
        <#else>
        final List<${transferObjectName}> ${transferObjectUncapFirst}s = ${generatorFieldName}.${multipleObjectsMethodName}(${transferObjectName}.class)
                .size(10)
                .create();
        </#if>

        final List<${swaggerModel}> results = this.${strippedModelName?uncap_first}Mapper.map${transferObjectName}To${swaggerModel}(${transferObjectUncapFirst}s);

        results.forEach(result -> {

            final ${transferObjectName} ${transferObjectUncapFirst} = ${transferObjectUncapFirst}s.stream()
                    .filter(${strippedModelName?uncap_first} -> ${strippedModelName?uncap_first}.${idField?uncap_first}().equals(result.get${idField?cap_first}()))
                    .findFirst()
                    .orElseThrow();
            
            verify${swaggerModel}(result, ${transferObjectUncapFirst});
        });
    }
    </#if>
    <#if generateAllHelperMethods?? && generateAllHelperMethods>
    @Test
    void map${swaggerModel}To${modelName}() {

        final ${swaggerModel} ${swaggerModel?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${swaggerModel}.class);

        final ${modelName} result = this.${strippedModelName?uncap_first}Mapper.map${swaggerModel}To${modelName}(${swaggerModel?uncap_first});

        verify${modelName}(result, ${swaggerModel?uncap_first});
    }

    @Test
    void map${swaggerModel}To${modelName}_list() {

        <#if dataGenerator == "PODAM">
        final List<${swaggerModel}> ${swaggerModel?uncap_first}s = ${generatorFieldName}.${multipleObjectsMethodName}(List.class, ${swaggerModel}.class);
        <#else>
        final List<${swaggerModel}> ${swaggerModel?uncap_first}s = ${generatorFieldName}.${multipleObjectsMethodName}(${swaggerModel}.class)
                .size(10)
                .create();
        </#if>

        final List<${modelName}> results = this.${strippedModelName?uncap_first}Mapper.map${swaggerModel}To${modelName}(${swaggerModel?uncap_first}s);

        results.forEach(result -> {

            final ${swaggerModel} ${swaggerModel?uncap_first} = ${swaggerModel?uncap_first}s.stream()
                    .filter(obj -> obj.get${idField?cap_first}().equals(result.get${idField?cap_first}()))
                    .findFirst()
                    .orElseThrow();

            verify${modelName}(result, ${swaggerModel?uncap_first});
        });
    }

    private void verify${modelName}(final ${modelName} result, final ${swaggerModel} ${swaggerModel?uncap_first}) {

        assertThat(result).isNotNull();
        <#list fieldNames as field>
        assertThat(result.get${field?cap_first}()).isEqualTo(${swaggerModel?uncap_first}.get${field?cap_first}());
        </#list>
        <#list enumFields as field>
        assertThat(result.get${field?cap_first}().name()).isEqualTo(${swaggerModel?uncap_first}.get${field?cap_first}().name());
        </#list>
    }
    </#if>

    private void verify${transferObjectName}(final ${transferObjectName} result, final ${modelName} ${modelNameUncapFirst}) {

        assertThat(result).isNotNull();
        <#list fieldNames as field>
        assertThat(result.${field?uncap_first}()).isEqualTo(${modelName?uncap_first}.get${field?cap_first}());
        </#list>
        <#list enumFields as field>
        assertThat(result.${field?uncap_first}()).isEqualTo(${modelName?uncap_first}.get${field?cap_first}());
        </#list>
    }

    private void verify${modelName}(final ${modelName} result, final ${transferObjectName} ${transferObjectUncapFirst}) {

        assertThat(result).isNotNull();
        <#list fieldNames as field>
        assertThat(result.get${field?cap_first}()).isEqualTo(${transferObjectUncapFirst?uncap_first}.${field?uncap_first}());
        </#list>
        <#list enumFields as field>
        assertThat(result.get${field?cap_first}()).isEqualTo(${transferObjectUncapFirst?uncap_first}.${field?uncap_first}());
        </#list>
    }

    <#if swagger?? && swagger>
    private void verify${swaggerModel}(final ${swaggerModel} result, final ${transferObjectName} ${transferObjectUncapFirst}) {

        assertThat(result).isNotNull();
        <#list fieldNames as field>
        assertThat(result.get${field?cap_first}()).isEqualTo(${transferObjectUncapFirst?uncap_first}.${field?uncap_first}());
        </#list>
        <#list enumFields as field>
        assertThat(result.get${field?cap_first}().name()).isEqualTo(${transferObjectUncapFirst?uncap_first}.${field?uncap_first}().name());
        </#list>
    }
    </#if>
}