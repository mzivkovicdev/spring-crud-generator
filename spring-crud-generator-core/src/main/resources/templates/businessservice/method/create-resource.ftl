<#assign transactionalAnnotation = model.transactionalAnnotation>
<#assign modelName = model.modelName>
<#assign serviceClass = model.modelService>
<#assign inputArgs = model.inputArgs>
<#assign fieldNames = model.fieldNames>
<#assign notNullArgs = model.notNullArgs![]>
<#assign notEmptyArgs = model.notEmptyArgs![]>
<#assign notBlankArgs = model.notBlankArgs![]>

    ${transactionalAnnotation}
    public ${modelName} create(${inputArgs}) {
        <#if notNullArgs?has_content>
        ArgumentVerifier.verifyNotNull(${notNullArgs?join(", ")});
        </#if>
        <#if notBlankArgs?has_content>
        ArgumentVerifier.verifyNotBlank(${notBlankArgs?join(", ")});
        </#if>
        <#if notEmptyArgs?has_content>
        ArgumentVerifier.verifyNotEmpty(${notEmptyArgs?join(", ")});
        </#if>

        <#list relations as rel>
        <#assign relationServiceClass = rel.strippedRelationClassName?uncap_first + "Service">
        <#assign relationClassName = rel.relationClassName>
        <#assign relationField = rel.strippedRelationClassName?uncap_first>
        <#if rel.isCollection?? && rel.isCollection>
        final ${rel.collectionType}<${relationClassName}> ${rel.relationClassName?uncap_first}s = <#if rel.collectionType == "Set">new ${rel.collectionImpl}<>(this.${relationServiceClass}.getAllByIds(${relationField}Ids.stream().toList()))<#else>this.${relationServiceClass}.getAllByIds(${relationField}Ids)</#if>;
        <#else>
        final ${relationClassName} ${rel.relationClassName?uncap_first} = ${relationField}Id != null ?
                this.${relationServiceClass}.getById(${relationField}Id) :
                null;
        </#if>
        </#list>

        return this.${serviceClass?uncap_first}.create(${fieldNames});
    }
