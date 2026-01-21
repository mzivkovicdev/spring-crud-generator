<#assign uncapModelName = strippedModelName?uncap_first>
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
<#assign mapperClass = strippedModelName?uncap_first + "Mapper">
<#assign openApiResponse = strippedModelName + "Payload">
<#assign openApiRequest = strippedModelName + "UpdatePayload">

    <#if swagger>@Override<#else>@PutMapping("/{id}")</#if>
    public ResponseEntity<<#if !swagger>${transferObjectClass}<#else>${openApiResponse}</#if>> ${uncapModelName}sIdPut(<#if !swagger>@PathVariable </#if>final ${idType} id, <#if !swagger>@RequestBody </#if>final <#if !swagger>${transferObjectClass}<#else>${openApiRequest}</#if> body) {
        <#if !swagger>
        return ResponseEntity.ok(
            ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
                this.${serviceField}.updateById(id, <#list inputFields as arg>${arg}<#if arg_has_next>, </#if></#list>)
            )
        );
        <#else>
        return ResponseEntity.ok(
            ${mapperClass}.map${transferObjectClass}To${openApiResponse}(
                ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
                    this.${serviceField}.updateById(id, <#list inputFields as arg>${arg}<#if arg_has_next>, </#if></#list>)
                )
            )
        );
        </#if>
    }