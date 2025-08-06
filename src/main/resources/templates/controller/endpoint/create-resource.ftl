<#assign uncapModelName = strippedModelName?uncap_first>
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
@PostMapping
    public ResponseEntity<${transferObjectClass}> ${uncapModelName}Post(@RequestBody final ${transferObjectClass} body) {

        return ResponseEntity.ok(
            mapper.map${modelName?cap_first}To${transferObjectClass}(
                this.${serviceField}.create(<#list inputFields as arg>body.${arg}()<#if arg_has_next>, </#if></#list>)
            )
        );
    }