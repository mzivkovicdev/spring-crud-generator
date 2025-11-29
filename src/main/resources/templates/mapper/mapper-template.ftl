import java.util.List;

import org.mapstruct.Mapper;

${projectImports}
@Mapper(<#if parameters??>uses = { ${parameters} }</#if>)
public interface ${mapperName} {

    ${transferObjectName} map${modelName}To${transferObjectName}(final ${modelName} model);

    List<${transferObjectName}> map${modelName}To${transferObjectName}(final List<${modelName}> model);

    ${modelName} map${transferObjectName}To${modelName}(final ${transferObjectName} transferObject);

    List<${modelName}> map${transferObjectName}To${modelName}(final List<${transferObjectName}> transferObject);
    <#if swagger?? && swagger>

    ${swaggerModel} map${transferObjectName}To${swaggerModel}(final ${transferObjectName} transferObject);

    List<${swaggerModel}> map${transferObjectName}To${swaggerModel}(final List<${transferObjectName}> transferObject);
    </#if><#t>
    <#if generateAllHelperMethods?? && generateAllHelperMethods>

    ${modelName} map${swaggerModel}To${modelName}(final ${swaggerModel} ${swaggerModel?uncap_first});

    List<${modelName}> map${swaggerModel}To${modelName}(final List<${swaggerModel}> ${swaggerModel?uncap_first});
    </#if><#t>
    
}