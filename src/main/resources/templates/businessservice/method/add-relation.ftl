<#assign transactionalAnnotation = model.transactionalAnnotation>
<#assign modelName = model.modelName>
<#assign idType = model.idType>
<#assign serviceClass = model.modelService>
<#list relations as rel>
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
    public ${modelName} ${rel.methodName}(final ${idType} id, final ${rel.relationIdType} ${relationField}Id) {

        final ${rel.relationClassName} entity = this.${relationServiceClass}.getReferenceById(${relationField}Id);

        LOGGER.info("Adding ${rel.relationClassName} with ID {} to ${modelName} with ID {}", ${relationField}Id, id);

        return this.${serviceClass?uncap_first}.${rel.methodName}(id, entity);
    }
</#list>