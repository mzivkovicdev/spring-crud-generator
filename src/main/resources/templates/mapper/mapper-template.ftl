<#if auditEnabled?? && auditEnabled && swagger>
import java.time.${auditType};
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
</#if><#t>
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

    <#if auditEnabled?? && auditEnabled>
    <#if auditType == "Instant">
    default Instant map(final OffsetDateTime odt) {
        return odt == null ? null : odt.toInstant();
    }

    default OffsetDateTime map(final Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
    </#if><#t>
    <#if auditType == "LocalDate">
    default OffsetDateTime map(final LocalDate date) {
        return date == null ? null : date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    default LocalDate map(final OffsetDateTime odt) {
        return odt == null ? null : odt.toLocalDate();
    }
    </#if><#t>
    <#if auditType == "LocalDateTime">
    default OffsetDateTime map(final LocalDateTime ldt) {
        return ldt == null ? null : ldt.atOffset(ZoneOffset.UTC);
    }

    default LocalDateTime mapToLocalDateTime(final OffsetDateTime odt) {
        return odt == null ? null : odt.toLocalDateTime();
    }
    </#if><#t>
    </#if>
    </#if><#t>
    <#if generateAllHelperMethods?? && generateAllHelperMethods>

    ${modelName} map${swaggerModel}To${modelName}(final ${swaggerModel} ${swaggerModel?uncap_first});

    List<${modelName}> map${swaggerModel}To${modelName}(final List<${swaggerModel}> ${swaggerModel?uncap_first});
    </#if><#t>
    
}