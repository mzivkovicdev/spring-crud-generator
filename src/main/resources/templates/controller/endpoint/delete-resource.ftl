<#assign uncapModelName = modelName?uncap_first>
<#assign serviceField = modelName?uncap_first + "Service">
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> ${uncapModelName}sIdDelete(@PathVariable final ${idType} id) {

        this.${serviceField}.deleteById(id);

        return ResponseEntity.noContent().build();
    }