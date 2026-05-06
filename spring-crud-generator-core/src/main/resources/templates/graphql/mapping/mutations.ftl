<#assign transferObjectClass = strippedModelName?cap_first + "TO">
<#assign mapperClass = strippedModelName?uncap_first + "Mapper">
<#assign baseServiceField = strippedModelName?uncap_first + "Service">
<#assign createInputToClass = strippedModelName + "CreateTO">
<#assign updateInputToClass = strippedModelName + "UpdateTO">
<#if relations?has_content>
    <#assign serviceField = strippedModelName?uncap_first + "BusinessService">
<#else>
    <#assign serviceField = strippedModelName?uncap_first + "Service">
</#if>

    <#if createPreAuthorize?? && createPreAuthorize?has_content>
    @PreAuthorize("${createPreAuthorize}")
    </#if>
    @MutationMapping
    @Validated
    public ${transferObjectClass} create${strippedModelName}(@Argument @Valid final ${createInputToClass} input) {
        return ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
            this.${serviceField}.create(
                <#list inputFieldsWithRelations as arg>${arg}<#if arg_has_next>, </#if></#list>
            )
        );
    }
    <#if bulkCreateEnabled?? && bulkCreateEnabled>

    <#if createPreAuthorize?? && createPreAuthorize?has_content>
    @PreAuthorize("${createPreAuthorize}")
    </#if>
    @MutationMapping
    @Validated
    public List<${transferObjectClass}> createBulk${strippedModelName}(@Argument @Valid final List<${createInputToClass}> input) {
        return input.stream()
                .map(item -> ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
                        this.${serviceField}.create(
                                <#list inputFieldsWithRelations as arg>${arg?replace("input.", "item.")}<#if arg_has_next>, </#if></#list>
                        )
                ))
                .toList();
    }
    </#if><#t>

    <#if updatePreAuthorize?? && updatePreAuthorize?has_content>
    @PreAuthorize("${updatePreAuthorize}")
    </#if>
    @MutationMapping
    @Validated
    public ${transferObjectClass} update${strippedModelName}(@Argument final ${idType} id, @Argument @Valid final ${updateInputToClass} input) {

        return ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
                this.${baseServiceField}.updateById(id, <#list inputFieldsWithoutRelations as arg>${arg}<#if arg_has_next>, </#if></#list>)
        );
    }

    <#if deletePreAuthorize?? && deletePreAuthorize?has_content>
    @PreAuthorize("${deletePreAuthorize}")
    </#if>
    @MutationMapping
    public boolean delete${strippedModelName}(@Argument final ${idType} id) {
        
        this.${baseServiceField}.deleteById(id);
        
        return true;
    }
    <#if bulkDeleteEnabled?? && bulkDeleteEnabled>

    <#if deletePreAuthorize?? && deletePreAuthorize?has_content>
    @PreAuthorize("${deletePreAuthorize}")
    </#if>
    @MutationMapping
    public boolean deleteBulk${strippedModelName}(@Argument final List<${idType}> ids) {
        
        this.${baseServiceField}.bulkDelete(ids);
        
        return true;
    }
    </#if><#t>
<#if relations?has_content>
<#list relations as rel>
<#assign relationField = rel.relationField?uncap_first>
<#assign relationIdType = rel.relationIdType>

    <#if addRelationPreAuthorize?? && addRelationPreAuthorize?has_content>
    @PreAuthorize("${addRelationPreAuthorize}")
    </#if>
    @MutationMapping
    public ${transferObjectClass} add${relationField?cap_first}To${strippedModelName?cap_first}(@Argument final ${idType} id, @Argument final ${relationIdType} ${relationField}Id) {
        return ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
            this.${serviceField}.add${relationField?cap_first}(id, ${relationField}Id)
        );
    }

    <#if removeRelationPreAuthorize?? && removeRelationPreAuthorize?has_content>
    @PreAuthorize("${removeRelationPreAuthorize}")
    </#if>
    @MutationMapping
    public ${transferObjectClass} remove${relationField?cap_first}From${strippedModelName?cap_first}(@Argument final ${idType} id<#if rel.isCollection>, @Argument final ${relationIdType} ${relationField}Id</#if>) {

        return ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
            <#if rel.isCollection>
            this.${serviceField}.remove${relationField?cap_first}(id, ${relationField}Id)
            <#else>
            this.${baseServiceField}.remove${relationField?cap_first}(id)
            </#if>
        );
    }
</#list>
</#if><#t>
