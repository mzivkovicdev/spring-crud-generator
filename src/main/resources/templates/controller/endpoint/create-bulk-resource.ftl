<#assign uncapModelName = strippedModelName?uncap_first>
<#if relations>
    <#assign serviceField = strippedModelName?uncap_first + "BusinessService">
<#else>
    <#assign serviceField = strippedModelName?uncap_first + "Service">
</#if>
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
<#assign createTransferObjectClass = strippedModelName?cap_first + "CreateTO">
<#assign mapperClass = strippedModelName?uncap_first + "Mapper">
<#assign openApiResponse = strippedModelName + "Payload">
<#assign openApiRequest = strippedModelName + "CreatePayload">

    <#if swagger>
    @Override
    <#else>
    @PostMapping("/bulk")
    @Validated
    </#if>
    public ResponseEntity<List<<#if !swagger>${transferObjectClass}<#else>${openApiResponse}</#if>>> ${uncapModelName}sBulkPost(<#if !swagger>@RequestBody </#if>final List<<#if !swagger>@Valid </#if><#if !swagger>${createTransferObjectClass}<#else>${openApiRequest}</#if>> body) {

        final List<${modelName?cap_first}> resources = body.stream()
                .map(item -> {
                    <#list inputFields?filter(f -> f.isRelation) as rel>
                    <#if rel.isCollection>
                    final ${rel.collectionType}<${rel.relationClassName}> ${rel.field} = <#if !swagger>(item.${rel.field}() != null && !item.${rel.field}().isEmpty())<#else>(item.get${rel.field?cap_first}() != null && !item.get${rel.field?cap_first}().isEmpty())</#if> ?
                            item.<#if !swagger>${rel.field}<#else>get${rel.field?cap_first}</#if>().stream()
                                    .map(relInput -> new ${rel.relationClassName}().set${rel.relationIdField?cap_first}(<#if !swagger>relInput.${rel.relationIdField}()<#else>relInput.get${rel.relationIdField?cap_first}()</#if>))
                                    .collect(Collectors.${rel.collectMethod}()) :
                            ${rel.emptyCollection};
                    <#else>
                    final ${rel.relationClassName} ${rel.field} = <#if !swagger>item.${rel.field}() != null<#else>item.get${rel.field?cap_first}() != null</#if> ?
                            new ${rel.relationClassName}().set${rel.relationIdField?cap_first}(<#if !swagger>item.${rel.field}().${rel.relationIdField}()<#else>item.get${rel.field?cap_first}().get${rel.relationIdField?cap_first}()</#if>) :
                            null;
                    </#if>
                    </#list>
                    <#list inputFields?filter(f -> f.isEnum) as enumField>
                    <#if swagger>
                    final ${enumField.fieldType} ${enumField.field}Enum = item.get${enumField.field?cap_first}() != null ?
                            ${enumField.fieldType?cap_first}.valueOf(item.get${enumField.field?cap_first}().name()) : null;
                    </#if>
                    </#list>

                    return new ${modelName?cap_first}(
                        <#list inputFields as arg><#if arg.isRelation>${arg.field}<#else><#if arg.isJsonField><#assign jsonMapperClass = arg.fieldType?uncap_first + "Mapper">${jsonMapperClass}.map${arg.fieldType?cap_first}<#if !swagger>TO<#else>Payload</#if>To${arg.fieldType?cap_first}(item.<#if !swagger>${arg.field}()<#else>get${arg.field?cap_first}()</#if>)<#else><#if swagger && arg.isEnum>${arg.field}Enum<#else>item.<#if !swagger>${arg.field}()<#else>get${arg.field?cap_first}()</#if></#if></#if></#if><#if arg_has_next>, </#if></#list>
                    );
                })
                .toList();
        <#if !swagger>
        return ResponseEntity.ok(
            ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
                this.${serviceField}.bulkCreate(resources)
            )
        );<#else>
        return ResponseEntity.ok(
            ${mapperClass}.map${transferObjectClass}To${openApiResponse}(
                ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
                    this.${serviceField}.bulkCreate(resources)
                )
            )
        );
        </#if>
    }
