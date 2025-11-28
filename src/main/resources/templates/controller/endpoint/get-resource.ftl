<#assign uncapModelName = strippedModelName?uncap_first>
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
<#assign mapperClass = strippedModelName?uncap_first + "Mapper">
<#assign openApiResponse = strippedModelName + "Payload">

    <#if swagger>@Override<#else>@GetMapping("/{id}")</#if>
    public ResponseEntity<<#if !swagger>${transferObjectClass}<#else>${openApiResponse}</#if>> ${uncapModelName}sIdGet(<#if !swagger>@PathVariable </#if>final ${idType} id) {
        <#if !swagger>
        return ResponseEntity.ok(
            ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
                this.${serviceField}.getById(id)
            )
        );
        <#else>
        return ResponseEntity.ok(
            ${mapperClass}.map${transferObjectClass}To${openApiResponse}(
                ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
                    this.${serviceField}.getById(id)
                )
            )
        );
        </#if>
    }