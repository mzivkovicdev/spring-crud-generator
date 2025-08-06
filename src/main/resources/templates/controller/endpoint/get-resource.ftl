<#assign uncapModelName = strippedModelName?uncap_first>
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign transferObjectClass = strippedModelName?cap_first + "TO">

    @GetMapping("/{id}")
    public ResponseEntity<${transferObjectClass}> ${uncapModelName}sIdGet(@PathVariable final ${idType} id) {
    
        return ResponseEntity.ok(
            mapper.map${modelName?cap_first}To${transferObjectClass}(
                this.${serviceField}.getById(id)
            )
        );
    }