<#assign transactionalAnnotation = model.transactionalAnnotation>
<#assign modelName = model.modelName>
<#assign idType = model.idType>
<#assign idField = model.idField>
<#assign strippedModelName = model.strippedModelName>
<#list relations as rel>
    
    <#assign javadocFields = rel.javadocFields>
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
    <#if cache>@CachePut(value = "${strippedModelName}", key = "#${idField}")</#if>
    public ${modelName} ${rel.methodName}(final ${idType} ${idField}, final ${rel.relationClassName} ${rel.elementParam}) {

        final ${modelName} entity = this.getById(${idField});
        
        <#if rel.isCollection?? && rel.isCollection>
        if (!entity.get${rel.relationField?cap_first}().add(${rel.elementParam})) {
            throw new InvalidResourceStateException("Not possible to add ${rel.elementParam}");
        }
        <#else>
        if (entity.get${rel.relationField?cap_first}() != null) {
            throw new InvalidResourceStateException("Not possible to add ${rel.elementParam}");
        }
        entity.set${rel.relationField?cap_first}(${rel.elementParam});
        </#if>

        return this.repository.saveAndFlush(entity);
    }
</#list>