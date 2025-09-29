<#assign uncapModelName = modelName?uncap_first>
<#assign serviceField = modelName?uncap_first + "Service">
    
    <#if swagger>@Override<#else>@DeleteMapping("/{id}")</#if>
    public ResponseEntity<Void> ${uncapModelName}sIdDelete(<#if !swagger>@PathVariable </#if>final ${idType} id) {

        this.${serviceField}.deleteById(id);

        return ResponseEntity.noContent().build();
    }