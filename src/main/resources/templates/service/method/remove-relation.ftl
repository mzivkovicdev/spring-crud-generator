<#assign transactionalAnnotation = model.transactionalAnnotation>
<#assign modelName = model.modelName>
<#assign idType = model.idType>
<#list relations as rel>
    
    <#assign javadocFields = rel.javadocFields>
    <#if javadocFields?has_content>
    /**
     * Remove {@link ${rel.relationClassName}} from {@link ${modelName}}
     *
     <#list javadocFields as docField>
     * ${docField}
     </#list>
     * @return Removed {@link ${rel.relationClassName}} from {@link ${modelName}}
     */</#if>
    ${transactionalAnnotation}
    public ${modelName} ${rel.methodName}(final ${idType} id<#if rel.isCollection?? && rel.isCollection>, final ${rel.relationClassName} ${rel.elementParam}</#if>) {

        final ${modelName} entity = this.getById(id);

        <#if rel.isCollection?? && rel.isCollection>
        if (!entity.get${rel.relationField?cap_first}().remove(${rel.elementParam})) {
            throw new RuntimeException("Not possible to remove ${rel.elementParam}");
        }
        <#else>
        if (entity.get${rel.relationField?cap_first}() == null) {
            throw new RuntimeException("Not possible to remove ${rel.elementParam}");
        }
        entity.set${rel.relationField?cap_first}(null);
        </#if>

        return this.repository.saveAndFlush(entity);
    }
</#list>