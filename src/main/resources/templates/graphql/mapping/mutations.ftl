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

    @MutationMapping
    public ${transferObjectClass} create${strippedModelName}(@Argument ${createInputToClass} input) {
        return ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
            this.${serviceField}.create(
                <#list inputFieldsWithRelations as arg>${arg}<#if arg_has_next>, </#if></#list>
            )
        );
    }

    @MutationMapping
    public ${transferObjectClass} update${strippedModelName}(@Argument final ${idType} id,
                                    @Argument final ${updateInputToClass} input) {

        return ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
                this.${baseServiceField}.updateById(id, <#list inputFieldsWithoutRelations as arg>${arg}<#if arg_has_next>, </#if></#list>)
        );
    }

    @MutationMapping
    public boolean delete${strippedModelName}(@Argument final ${idType} id) {
        
        this.${baseServiceField}.deleteById(id);
        
        return true;
    }
<#if relations?has_content>
<#list relations as rel>
<#assign relationField = rel.relationField?uncap_first>
<#assign relationIdType = rel.relationIdType>
    @MutationMapping
    public ${transferObjectClass} add${relationField?cap_first}To${strippedModelName?cap_first}(@Argument final ${idType} id, @Argument final ${relationIdType} ${relationField}Id) {
        return ${mapperClass}.map${modelName?cap_first}To${transferObjectClass}(
            this.${serviceField}.add${relationField?cap_first}(id, ${relationField}Id)
        );
    }

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
</#if>