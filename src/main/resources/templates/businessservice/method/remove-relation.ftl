<#assign transactionalAnnotation = model.transactionalAnnotation>
<#assign modelName = model.modelName>
<#assign idType = model.idType>
<#assign idField = model.idField>
<#assign serviceClass = model.modelService>
<#list relations as rel>
    <#if rel.isCollection?? && rel.isCollection>
    <#assign javadocFields = rel.javadocFields>
    <#assign relationServiceClass = rel.strippedRelationClassName?uncap_first + "Service">
    <#assign relationField = rel.strippedRelationClassName?uncap_first>
    <#if javadocFields?has_content>
    /**
     * Add {@link ${rel.relationClassName}} to {@link ${modelName}}
     *
     <#list javadocFields as docField>
     * ${docField}
     </#list>
     * @return Added {@link ${rel.relationClassName}} to {@link ${modelName}}
     */</#if>
    ${transactionalAnnotation}
    public ${modelName} ${rel.methodName}(final ${idType} ${idField}, final ${rel.relationIdType} ${relationField}Id) {

        final ${rel.relationClassName} entity = this.${relationServiceClass}.getReferenceById(${relationField}Id);

        LOGGER.info("Removing ${rel.relationClassName} with ID {} from ${modelName} with ID {}", ${relationField}Id, ${idField});

        return this.${serviceClass?uncap_first}.${rel.methodName}(${idField}, entity);
    }
    </#if>
</#list>