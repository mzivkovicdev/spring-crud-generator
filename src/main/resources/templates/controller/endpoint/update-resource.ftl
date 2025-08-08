<#assign uncapModelName = strippedModelName?uncap_first>
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
<#assign mapperClass = strippedModelName?uncap_first + "Mapper">

    @PutMapping("/{id}")
    public ResponseEntity<${transferObjectClass}> ${uncapModelName}sIdPut(@PathVariable final ${idType} id, @RequestBody final ${transferObjectClass} body) {

        return ResponseEntity.ok(
            ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
                this.${serviceField}.updateById(id, <#list inputFields as arg>body.${arg}()<#if arg_has_next>, </#if></#list>)
            )
        );
    }