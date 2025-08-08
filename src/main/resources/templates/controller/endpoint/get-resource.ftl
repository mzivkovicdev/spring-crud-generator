<#assign uncapModelName = strippedModelName?uncap_first>
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
<#assign mapperClass = strippedModelName?uncap_first + "Mapper">

    @GetMapping("/{id}")
    public ResponseEntity<${transferObjectClass}> ${uncapModelName}sIdGet(@PathVariable final ${idType} id) {
    
        return ResponseEntity.ok(
            ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
                this.${serviceField}.getById(id)
            )
        );
    }