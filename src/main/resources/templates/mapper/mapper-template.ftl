import java.util.List;

import org.mapstruct.Mapper;

<#if swagger?? && swagger && !(generateAllHelperMethods?? && !generateAllHelperMethods)>${generatedModelImport}</#if><#if helperMapperImports??>${helperMapperImports}</#if>${modelImport}${transferObjectImport}
@Mapper(componentModel = "spring"<#if parameters??>, uses = { ${parameters} }</#if>)
public interface ${mapperName} {

    ${transferObjectName} map${modelName}To${transferObjectName}(final ${modelName} model);

    List<${transferObjectName}> map${modelName}To${transferObjectName}(final List<${modelName}> model);

    ${modelName} map${transferObjectName}To${modelName}(final ${transferObjectName} transferObject);

    List<${modelName}> map${transferObjectName}To${modelName}(final List<${transferObjectName}> transferObject);
    
    <#if swagger?? && swagger>
    ${swaggerModel} map${transferObjectName}To${swaggerModel}(final ${transferObjectName} transferObject);

    List<${swaggerModel}> map${transferObjectName}To${swaggerModel}(final List<${transferObjectName}> transferObject);
    </#if>

    <#if generateAllHelperMethods?? && generateAllHelperMethods>
    ${modelName} map${swaggerModel}To${modelName}(final ${generatedModelImport} ${swaggerModel?uncap_first});

    List<${modelName}> map${swaggerModel}To${modelName}(final List<${generatedModelImport}> ${swaggerModel?uncap_first});
    </#if>
}