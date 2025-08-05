<#assign transactionalAnnotation = model.transactionalAnnotation>
<#assign modelName = model.modelName>
<#assign idType = model.idType>
<#list relations as rel>
    
    ${transactionalAnnotation}
    public ${modelName} ${rel.methodName}(final ${idType} id, final ${rel.relationClassName} ${rel.elementParam}) {

        final ${modelName} entity = this.getById(id);

        if (!entity.get${rel.collectionField?cap_first}().add(${rel.elementParam})) {
            throw new RuntimeException("Not possible to add ${rel.elementParam}");
        }

        return this.repository.saveAndFlush(entity);
    }
</#list>