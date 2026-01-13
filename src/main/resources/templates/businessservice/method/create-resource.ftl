<#assign transactionalAnnotation = model.transactionalAnnotation>
<#assign modelName = model.modelName>
<#assign serviceClass = model.modelService>
<#assign inputArgs = model.inputArgs>
<#assign fieldNames = model.fieldNames>

    ${transactionalAnnotation}
    public ${modelName} create(${inputArgs}) {

        <#list relations as rel>
        <#assign relationServiceClass = rel.strippedRelationClassName?uncap_first + "Service">
        <#assign relationClassName = rel.relationClassName>
        <#assign relationField = rel.strippedRelationClassName?uncap_first>
        <#if rel.isCollection?? && rel.isCollection>
        final List<${relationClassName}> ${rel.relationClassName?uncap_first}s = this.${relationServiceClass}.getAllByIds(${relationField}Ids);
        <#else>
        final ${relationClassName} ${rel.relationClassName?uncap_first} = ${relationField}Id != null ?
                this.${relationServiceClass}.getReferenceById(${relationField}Id) :
                null;
        </#if>
        </#list>

        return this.${serviceClass?uncap_first}.create(${fieldNames});
    }