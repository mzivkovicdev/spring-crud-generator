<#assign transactionalAnnotation = model.transactionalAnnotation>
<#assign modelName = model.modelName>
<#assign serviceClass = model.modelService>

    ${transactionalAnnotation}
    public List<${modelName}> bulkCreate(final List<${modelName}> models) {

        final List<${modelName}> resolvedModels = models.stream()
                .map(model -> {
                    <#list relations as rel>
                    <#assign relationServiceClass = rel.strippedRelationClassName?uncap_first + "Service">
                    <#assign relationClassName = rel.relationClassName>
                    <#assign relationField = rel.elementParam>
                    <#assign relationVar = rel.strippedRelationClassName?uncap_first>
                    <#if rel.isCollection?? && rel.isCollection>
                    final ${rel.collectionType}<${rel.relationIdType}> ${relationVar}Ids = (model.get${relationField?cap_first}() != null && !model.get${relationField?cap_first}().isEmpty()) ?
                            model.get${relationField?cap_first}().stream()
                                    .map(${relationClassName}::get${rel.relationIdField?cap_first})
                                    .collect(Collectors.${rel.collectMethod}()) :
                            ${rel.emptyCollection};
                    final ${rel.collectionType}<${relationClassName}> ${relationVar}s = <#if rel.collectionType == "Set">new ${rel.collectionImpl}<>(this.${relationServiceClass}.getAllByIds(${relationVar}Ids.stream().toList()))<#else>this.${relationServiceClass}.getAllByIds(${relationVar}Ids)</#if>;
                    model.set${relationField?cap_first}(${relationVar}s);
                    <#else>
                    final ${rel.relationIdType} ${relationVar}Id = model.get${relationField?cap_first}() != null ?
                            model.get${relationField?cap_first}().get${rel.relationIdField?cap_first}() :
                            null;
                    final ${relationClassName} ${relationVar} = ${relationVar}Id != null ?
                            this.${relationServiceClass}.getById(${relationVar}Id) :
                            null;
                    model.set${relationField?cap_first}(${relationVar});
                    </#if>
                    </#list>
                    return model;
                })
                .toList();

        return this.${serviceClass?uncap_first}.bulkCreate(resolvedModels);
    }
