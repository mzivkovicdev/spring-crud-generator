import java.util.List;

import org.mapstruct.Mapper;

<#if swagger?? && swagger>${generatedModelImport}</#if><#if helperMapperImports??>${helperMapperImports}</#if>${modelImport}${transferObjectImport}
@Mapper(<#if parameters??>uses = { ${parameters} }</#if>)
public interface ${mapperName} {

    ${transferObjectName} map${modelName}To${transferObjectName}(final ${modelName} model);

    List<${transferObjectName}> map${modelName}To${transferObjectName}(final List<${modelName}> model);

    ${modelName} map${transferObjectName}To${modelName}(final ${transferObjectName} transferObject);

    List<${modelName}> map${transferObjectName}To${modelName}(final List<${transferObjectName}> transferObject);
    
    <#if swagger?? && swagger>
    ${swaggerModel} map${transferObjectName}To${swaggerModel}(final ${transferObjectName} transferObject);

    List<${swaggerModel}> map${transferObjectName}To${swaggerModel}(final List<${transferObjectName}> transferObject);
    </#if>
}