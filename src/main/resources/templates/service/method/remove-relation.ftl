<#assign transactionalAnnotation = model.transactionalAnnotation>
<#assign modelName = model.modelName>
<#assign idType = model.idType>
<#assign idField = model.idField>
<#assign strippedModelName = model.strippedModelName>
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
    <#if cache>
    @CachePut(value = "${strippedModelName}", key = "#${idField}")
    </#if><#t>
    public ${modelName} ${rel.methodName}(final ${idType} ${idField}<#if rel.isCollection?? && rel.isCollection>, final ${rel.relationClassName} ${rel.elementParam}</#if>) {

        final ${modelName} entity = this.getById(${idField});

        <#if rel.isCollection?? && rel.isCollection>
        if (!entity.get${rel.relationField?cap_first}().remove(${rel.elementParam})) {
            throw new InvalidResourceStateException("Not possible to remove ${rel.elementParam}");
        }
        <#else>
        if (entity.get${rel.relationField?cap_first}() == null) {
            throw new InvalidResourceStateException("Not possible to remove ${rel.elementParam}");
        }
        entity.set${rel.relationField?cap_first}(null);
        </#if>

        return this.repository.saveAndFlush(entity);
    }
</#list>