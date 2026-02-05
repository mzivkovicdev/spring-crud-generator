<#if auditEnabled?? && auditEnabled && swagger && auditType != "LocalDate">
import java.time.${auditType};
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
</#if><#t>
import java.util.List;

<#if openInViewEnabled?? && !openInViewEnabled>
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
<#if openInViewEnabled?? && !openInViewEnabled && lazyFields?? && lazyFields?has_content>
import org.mapstruct.Mapping;
</#if><#t>
import org.mapstruct.Named;
<#else>
import org.mapstruct.Mapper;
</#if><#t>

${projectImports}
@Mapper(<#if parameters??>uses = { ${parameters} }</#if>)
public interface ${mapperName} {

    <#if openInViewEnabled?? && !openInViewEnabled && lazyFields?? && lazyFields?has_content>
    <#list lazyFields as lazyField>
    <#if baseCollectionFields?? && !baseCollectionFields?seq_contains(lazyField)>
    @Mapping(target = "${lazyField}", source = "${lazyField}", qualifiedByName = "simple")
    </#if><#t>
    </#list>
    <#list eagerFields as eagerField>
    @Mapping(target = "${eagerField}", qualifiedByName = "simple")
    </#list>
    </#if><#t>
    ${transferObjectName} map${modelName}To${transferObjectName}(final ${modelName} model);

    <#if openInViewEnabled?? && !openInViewEnabled && lazyFields?? && lazyFields?has_content>
    <#list lazyFields as lazyField>
    <#if baseCollectionFields?? && !baseCollectionFields?seq_contains(lazyField)>
    @Mapping(target = "${lazyField}", qualifiedByName = "simple")
    </#if><#t>
    </#list>
    <#list eagerFields as eagerField>
    @Mapping(target = "${eagerField}", qualifiedByName = "simple")
    </#list>
    </#if><#t>
    List<${transferObjectName}> map${modelName}To${transferObjectName}(final List<${modelName}> model);
    
    <#if openInViewEnabled?? && !openInViewEnabled>
    @Named("simple")
    <#list lazyFields as lazyField>
    @Mapping(target = "${lazyField}", source = "${lazyField}", ignore = true)
    </#list>
    <#list eagerFields as eagerField>
    @Mapping(target = "${eagerField}", qualifiedByName = "simple")
    </#list>
    ${transferObjectName} map${modelName}To${transferObjectName}Simple(final ${modelName} model);

    @Named("simpleList")
    @IterableMapping(qualifiedByName = "simple")
    List<${transferObjectName}> map${modelName}To${transferObjectName}Simple(final List<${modelName}> model);

    </#if><#t>
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