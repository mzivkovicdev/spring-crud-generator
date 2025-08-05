<#assign transactionalAnnotation = model.transactionalAnnotation>
<#assign modelName = model.modelName>
<#assign idType = model.idType>
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
    public ${modelName} ${rel.methodName}(final ${idType} id, final ${rel.relationClassName} ${rel.elementParam}) {

        final ${modelName} entity = this.getById(id);
        
        <#if rel.isCollection?? && rel.isCollection>
        if (!entity.get${rel.relationField?cap_first}().add(${rel.elementParam})) {
            throw new RuntimeException("Not possible to add ${rel.elementParam}");
        }
        <#else>
        if (entity.get${rel.relationField?cap_first}() != null) {
            throw new RuntimeException("Not possible to add ${rel.elementParam}");
        }
        entity.set${rel.relationField?cap_first}(${rel.elementParam});
        </#if>

        return this.repository.saveAndFlush(entity);
    }
</#list>