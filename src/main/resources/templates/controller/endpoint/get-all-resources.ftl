
<#assign uncapModelName = strippedModelName?uncap_first>
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
<#assign mapperClass = strippedModelName?uncap_first + "Mapper">
<#if swagger><#assign responseClass = strippedModelName + "sGet200Response"></#if>
<#assign openApiModel = strippedModelName + "Payload">

    <#if swagger>@Override<#else>@GetMapping</#if>
    public ResponseEntity<<#if !swagger>PageTO<${transferObjectClass}><#else>${responseClass}</#if>> ${uncapModelName}sGet(<#if !swagger>@RequestParam </#if>final Integer pageNumber, <#if !swagger>@RequestParam </#if>final Integer pageSize) {

        final Page<${modelName?cap_first}> pageObject = this.${serviceField}.getAll(pageNumber, pageSize);
        <#if !swagger>
        return ResponseEntity.ok().body(
            new PageTO<>(
                pageObject.getTotalPages(),
                pageObject.getTotalElements(),
                pageObject.getSize(),
                pageObject.getNumber(),
                ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(pageObject.getContent())
            )
        );
        <#else>
        return ResponseEntity.ok().body(
            new ${responseClass}()
                .totalPages(pageObject.getTotalPages())
                .totalElements(pageObject.getTotalElements())
                .size(pageObject.getSize())
                .number(pageObject.getNumber())
                .content(
                    ${mapperClass}.map${transferObjectClass}To${openApiModel}(
                        ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
                            pageObject.getContent()
                        )
                    )
                )
        );
        </#if>
    }