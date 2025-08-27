import java.util.List;

import org.mapstruct.Mapper;

<#if helperMapperImports??>${helperMapperImports}</#if>${modelImport}${transferObjectImport}
@Mapper(<#if parameters??>uses = { ${parameters} }</#if>)
public interface ${mapperName} {

    ${transferObjectName} map${modelName}To${transferObjectName}(final ${modelName} model);

    List<${transferObjectName}> map${modelName}To${transferObjectName}(final List<${modelName}> model);

    ${modelName} map${transferObjectName}To${modelName}(final ${transferObjectName} transferObject);

    List<${modelName}> map${transferObjectName}To${modelName}(final List<${transferObjectName}> transferObject);

}