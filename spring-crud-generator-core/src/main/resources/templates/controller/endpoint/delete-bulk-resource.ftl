<#assign uncapModelName = modelName?uncap_first>
<#assign serviceField = modelName?uncap_first + "Service">

    <#if preAuthorize?? && preAuthorize?has_content>
    @PreAuthorize("${preAuthorize}")
    </#if>
    <#if swagger>
    @Override
    <#else>
    @DeleteMapping("/bulk")
    </#if>
    public ResponseEntity<Void> ${uncapModelName}sBulkDelete(<#if !swagger>@RequestBody </#if>final List<${idType}> body) {

        this.${serviceField}.bulkDelete(body);

        return ResponseEntity.noContent().build();
    }