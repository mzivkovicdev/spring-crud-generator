<#assign uncapModelName = strippedModelName?uncap_first>
<#if relations>
    <#assign serviceField = strippedModelName?uncap_first + "BusinessService">
<#else>
    <#assign serviceField = strippedModelName?uncap_first + "Service">
</#if>
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
<#assign mapperClass = strippedModelName?uncap_first + "Mapper">
<#assign openApiResponse = strippedModelName + "Payload">
<#assign openApiRequest = strippedModelName + "Payload">
<#if swagger>@Override<#else>@PostMapping</#if>
    public ResponseEntity<<#if !swagger>${transferObjectClass}<#else>${openApiResponse}</#if>> ${uncapModelName}sPost(<#if !swagger>@RequestBody </#if>final <#if !swagger>${transferObjectClass}<#else>${openApiRequest}</#if> body) {

        <#list inputFields?filter(f -> f.isRelation) as rel>
        <#if !swagger><#assign relationTransferObject = rel.strippedModelName + "TO"><#else><#assign relationTransferObject = rel.strippedModelName + "Payload"></#if>
        <#if rel.isCollection>
        final List<${rel.relationIdType}> ${rel.field}Ids = <#if !swagger>(body.${rel.field}() != null && !body.${rel.field}().isEmpty())<#else>(body.get${rel.field?cap_first}() != null && !body.get${rel.field?cap_first}().isEmpty())</#if> ? 
                body.<#if !swagger>${rel.field}<#else>get${rel.field?cap_first}</#if>().stream()
                    <#if !swagger>.map(${relationTransferObject}::${rel.relationIdField})<#else>.map(${relationTransferObject}::get${rel.relationIdField?cap_first})</#if>
                    .collect(Collectors.toList()) :
                List.of();
        <#else>
        final ${rel.relationIdType} ${rel.field}Id = <#if !swagger>body.${rel.field}() != null ? body.${rel.field}().id() : null;<#else>body.get${rel.field?cap_first}() != null ? body.get${rel.field?cap_first}().getId() : null;</#if>
        </#if>
        </#list>
        <#list inputFields?filter(f -> f.isEnum) as rel>
        <#if swagger>
        final ${rel.fieldType} ${rel.field}Enum = body.get${rel.field?cap_first}() != null ?
                ${rel.fieldType?cap_first}.valueOf(body.get${rel.field?cap_first}().name()) : null;
        </#if>
        </#list>
        <#if !swagger>
        return ResponseEntity.ok(
            ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
                this.${serviceField}.create(
                    <#list inputFields as arg><#if arg.isRelation><#if arg.isCollection>${arg.field}Ids<#else>${arg.field}Id</#if><#else><#if arg.isJsonField><#assign jsonMapperClass = arg.fieldType?uncap_first + "Mapper">${jsonMapperClass}.map${arg.fieldType?cap_first}TOTo${arg.fieldType?cap_first}(body.${arg.field}())<#else>body.${arg.field}()</#if></#if><#if arg_has_next>, </#if></#list>
                )
            )
        );<#else>
        return ResponseEntity.ok(
            ${mapperClass}.map${transferObjectClass}To${openApiResponse}(
                ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
                    this.${serviceField}.create(
                        <#list inputFields as arg><#if arg.isRelation><#if arg.isCollection>${arg.field}Ids<#else>${arg.field}Id</#if><#else><#if arg.isJsonField><#assign jsonMapperClass = arg.fieldType?uncap_first + "Mapper">${jsonMapperClass}.map${arg.fieldType?cap_first}PayloadTo${arg.fieldType?cap_first}(body.get${arg.field?cap_first}())<#else><#if !arg.isEnum>body.get${arg.field?cap_first}()<#else>${arg.field}Enum</#if></#if></#if><#if arg_has_next>, </#if></#list>
                    )
                )
            )
        );
        </#if>
    }