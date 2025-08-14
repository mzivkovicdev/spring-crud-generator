<#assign uncapModelName = strippedModelName?uncap_first>
<#if relations>
    <#assign serviceField = strippedModelName?uncap_first + "BusinessService">
<#else>
    <#assign serviceField = strippedModelName?uncap_first + "Service">
</#if>
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
<#assign mapperClass = strippedModelName?uncap_first + "Mapper">
@PostMapping
    public ResponseEntity<${transferObjectClass}> ${uncapModelName}Post(@RequestBody final ${transferObjectClass} body) {

        <#list inputFields?filter(f -> f.isRelation) as rel>
        <#assign relationTransferObject = rel.strippedModelName + "TO">
        <#if rel.isCollection>
        final List<${rel.relationIdType}> ${rel.field}Ids = (body.${rel.field}() != null && !body.${rel.field}().isEmpty()) ? 
                List.of() :
                body.${rel.field}().stream()
                    .map(${relationTransferObject}::${rel.relationIdField})
                    .collect(Collectors.toList());
        <#else>
        final ${rel.relationIdType} ${rel.field}Id = body.${rel.field}() != null ? body.${rel.field}().id() : null;
        </#if>
        </#list>
        
        return ResponseEntity.ok(
            ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
                this.${serviceField}.create(
                    <#list inputFields as arg><#if arg.isRelation><#if arg.isCollection>${arg.field}Ids<#else>${arg.field}Id</#if><#else><#if arg.isJsonField><#assign jsonMapperClass = arg.fieldType?uncap_first + "Mapper">${jsonMapperClass}.map${arg.fieldType?cap_first}TOTo${arg.fieldType?cap_first}(body.${arg.field}())<#else>body.${arg.field}()</#if></#if><#if arg_has_next>, </#if></#list>
                )
            )
        );
    }