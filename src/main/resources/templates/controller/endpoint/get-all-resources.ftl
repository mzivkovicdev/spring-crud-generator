
<#assign uncapModelName = strippedModelName?uncap_first>
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign transferObjectClass = strippedModelName?cap_first + "TO">

    @GetMapping
    public ResponseEntity<PageTO<${transferObjectClass}>> ${uncapModelName}sGet(@RequestParam final Integer pageNumber, @RequestParam final Integer pageSize) {

        final Page<${modelName?cap_first}> pageObject = this.${serviceField}.getAll(pageNumber, pageSize);

        return ResponseEntity.ok().body(
            new PageTO<>(
                pageObject.getTotalPages(),
                pageObject.getTotalElements(),
                pageObject.getSize(),
                pageObject.getNumber(),
                mapper.map${modelName?cap_first}To${transferObjectClass}(pageObject.getContent())
            )
        );
    }