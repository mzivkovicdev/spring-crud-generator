<#assign modelName = model.modelName>
<#assign idType = model.idType>
<#assign transferObjectClass = model.strippedModelName?cap_first + "TO">
<#assign mapperClass = model.strippedModelName?uncap_first + "Mapper">
<#assign businessServiceField = model.strippedModelName?uncap_first + "BusinessService">
<#list relations as rel>
<#assign relationFieldModel = rel.relationFieldModel?uncap_first>
<#assign relationField = rel.strippedRelationClassName?uncap_first>
<#assign relationInput = rel.strippedRelationClassName?cap_first + "Input">
<#assign relationInputTO = rel.strippedRelationClassName?cap_first + "InputTO">
    <#if swagger>@Override<#else>@PostMapping("/{id}/${relationField}s")</#if>
    public ResponseEntity<<#if !swagger>${transferObjectClass}<#else>${model.strippedModelName}</#if>> ${rel.methodName}(<#if !swagger>@PathVariable </#if>final ${idType} id,
            <#if !swagger>@RequestBody </#if>final <#if !swagger>${relationInputTO}<#else>${relationInput}</#if> body) {
        <#if !swagger>
        return ResponseEntity.ok(
            ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
                this.${businessServiceField}.add${relationFieldModel?cap_first}(id, body.id())
            )
        );
        <#else>
        return ResponseEntity.ok(
            ${mapperClass}.map${transferObjectClass}To${model.strippedModelName}(
                ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
                    this.${businessServiceField}.add${relationFieldModel?cap_first}(id, body.id())
                )
            )
        );
        </#if>
    }
</#list>