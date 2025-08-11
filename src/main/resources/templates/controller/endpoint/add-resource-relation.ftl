<#assign modelName = model.modelName>
<#assign idType = model.idType>
<#assign transferObjectClass = model.strippedModelName?cap_first + "TO">
<#assign mapperClass = model.strippedModelName?uncap_first + "Mapper">
<#assign businessServiceField = model.strippedModelName?uncap_first + "BusinessService">
<#list relations as rel>
<#assign relationFieldModel = rel.relationFieldModel?uncap_first>
<#assign relationField = rel.strippedRelationClassName?uncap_first>
<#assign relationInputTO = rel.strippedRelationClassName?cap_first + "InputTO">

    @PostMapping("/{id}/${relationField}s")
    public ResponseEntity<${transferObjectClass}> ${rel.methodName}(@PathVariable final ${idType} id,
            @RequestBody final ${relationInputTO} body) {

        return ResponseEntity.ok(
            ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
                this.${businessServiceField}.add${relationFieldModel?cap_first}(id, body.id())
            )
        );
    }
</#list>