<#assign uncapModelName = strippedModelName?uncap_first>
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign transferObjectClass = strippedModelName?cap_first + "TO">
<#assign mapperClass = strippedModelName?uncap_first + "Mapper">

    @QueryMapping
    public ${transferObjectClass} ${uncapModelName}ById(@Argument final ${idType} id) {
        return ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
            this.${serviceField}.getById(id)
        );
    }

    @QueryMapping
    public PageTO<${transferObjectClass}> ${uncapModelName}sPage(@Argument final Integer pageNumber,
                                    @Argument final Integer pageSize) {
        
        final Page<${modelName?cap_first}> pageObject = this.${serviceField}.getAll(pageNumber, pageSize);

        return new PageTO<>(
            pageObject.getTotalPages(),
            pageObject.getTotalElements(),
            pageObject.getSize(),
            pageObject.getNumber(),
            <#if openInViewEnabled?? && !openInViewEnabled>
            ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}Simple(pageObject.getContent())
            <#else>
            ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(pageObject.getContent())
            </#if>
        );
    }