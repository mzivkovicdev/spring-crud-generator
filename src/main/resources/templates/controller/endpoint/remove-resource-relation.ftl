<#assign idType = model.idType>
<#assign businessServiceField = model.strippedModelName?uncap_first + "BusinessService">
<#assign serviceField = model.strippedModelName?uncap_first + "Service">
<#list relations as rel>
<#assign relationFieldModel = rel.relationFieldModel?uncap_first>
<#assign relationName = rel.strippedRelationClassName?uncap_first>
<#assign relationField = rel.relationField>
<#assign relationIdType = rel.relationIdType>
    
    <#if swagger>@Override<#else><#if rel.isCollection>@DeleteMapping("/{id}/${relationName}s/{${relationField}}")<#else>@DeleteMapping("/{id}/${relationName}s")</#if></#if>
    public ResponseEntity<Void> ${rel.methodName}(<#if !swagger>@PathVariable </#if>final ${idType} id<#if rel.isCollection>, <#if !swagger>@PathVariable </#if>final ${relationIdType} ${relationField}</#if>) {

        <#if rel.isCollection>
        this.${businessServiceField}.remove${relationFieldModel?cap_first}(id, ${relationField});
        <#else>
        this.${serviceField}.remove${relationFieldModel?cap_first}(id);
        </#if>
        return ResponseEntity.noContent().build();
    }
</#list>