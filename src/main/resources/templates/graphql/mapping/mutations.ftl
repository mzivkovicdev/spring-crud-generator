<#assign transferObjectClass = strippedModelName?cap_first + "TO">
<#assign mapperClass = strippedModelName?uncap_first + "Mapper">
<#assign serviceField = strippedModelName?uncap_first + "Service">
<#assign createInputToClass = strippedModelName + "CreateTO">
<#assign updateInputToClass = strippedModelName + "UpdateTO">

    @MutationMapping
    public ${transferObjectClass} create${strippedModelName}(@Argument ${createInputToClass} input) {
        return ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
            this.${serviceField}.create(
                <#list inputFieldsWithRelations as arg>${arg}<#if arg_has_next>, </#if></#list>
            )
        )
    }

    @MutationMapping
    public ${transferObjectClass} update${strippedModelName}(@Argument final ${idType} id,
                                    @Argument final ${updateInputToClass} input) {

        return ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
                this.${serviceField}.updateById(id, <#list inputFieldsWithoutRelations as arg>${arg}<#if arg_has_next>, </#if></#list>)
        );
    }

    @MutationMapping
    public boolean delete${strippedModelName}(@Argument final ${idType} id) {
        
        this.${serviceField}.deleteById(id);
        
        return true;
    }
