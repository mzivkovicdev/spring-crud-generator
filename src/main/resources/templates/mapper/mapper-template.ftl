import java.util.List;

import org.mapstruct.Mapper;

${modelImport}${transferObjectImport}
@Mapper
public interface ${mapperName} {

    ${transferObjectName} map${modelName}To${transferObjectName}(final ${modelName} model);

    List<${transferObjectName}> map${modelName}To${transferObjectName}(final List<${modelName}> model);

}