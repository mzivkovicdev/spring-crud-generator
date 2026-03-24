<#assign modelName = model.modelName>
<#assign strippedModelName = model.strippedModelName>
<#assign serviceClass = model.modelService>

    @Test
    void bulkCreate() {

        final ${modelName} ${modelName?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${modelName}.class);
        final List<${modelName}> ${modelName?uncap_first}s = List.of(${modelName?uncap_first});
        <#list relations as rel>
        <#assign relationField = rel.strippedRelationClassName?uncap_first>
        <#if rel.isCollection?? && rel.isCollection>
        final ${rel.collectionType}<${rel.relationClassName}> ${rel.relationClassName?uncap_first}s =
                ${modelName?uncap_first}.get${rel.elementParam?cap_first}() != null ? ${modelName?uncap_first}.get${rel.elementParam?cap_first}() : ${rel.emptyCollection};
        final ${rel.collectionType}<${rel.relationIdType}> ${relationField}Ids = ${rel.relationClassName?uncap_first}s.stream()
                .map(${rel.relationClassName}::get${rel.relationIdField?cap_first})
                .collect(Collectors.${rel.collectMethod}());
        <#if rel.collectionType == "Set">
        final List<${rel.relationClassName}> ${rel.relationClassName?uncap_first}List = ${rel.relationClassName?uncap_first}s.stream().toList();
        </#if>
        <#else>
        final ${rel.relationClassName} ${rel.relationClassName?uncap_first} = ${modelName?uncap_first}.get${rel.elementParam?cap_first}();
        final ${rel.relationIdType} ${relationField}Id = ${rel.relationClassName?uncap_first} != null ? ${rel.relationClassName?uncap_first}.get${rel.relationIdField?cap_first}() : null;
        </#if>
        </#list>

        <#list relations as rel>
        <#assign relationServiceClass = rel.strippedRelationClassName?uncap_first + "Service">
        <#assign relationField = rel.strippedRelationClassName?uncap_first>
        <#if rel.isCollection?? && rel.isCollection>
        when(this.${relationServiceClass}.getAllByIds(<#if rel.collectionType == "Set">${relationField}Ids.stream().toList()<#else>${relationField}Ids</#if>))
                .thenReturn(<#if rel.collectionType == "Set">${rel.relationClassName?uncap_first}List<#else>${rel.relationClassName?uncap_first}s</#if>);
        <#else>
        if (${relationField}Id != null) {
        when(this.${relationServiceClass}.getById(${relationField}Id)).thenReturn(${rel.relationClassName?uncap_first});
        }
        </#if>
        </#list>
        when(this.${serviceClass?uncap_first}.bulkCreate(${modelName?uncap_first}s)).thenReturn(${modelName?uncap_first}s);

        final List<${modelName}> results = this.${strippedModelName?uncap_first}BusinessService.bulkCreate(${modelName?uncap_first}s);

        <#list relations as rel>
        <#assign relationServiceClass = rel.strippedRelationClassName?uncap_first + "Service">
        <#assign relationField = rel.strippedRelationClassName?uncap_first>
        <#if rel.isCollection?? && rel.isCollection>
        verify(this.${relationServiceClass}).getAllByIds(<#if rel.collectionType == "Set">${relationField}Ids.stream().toList()<#else>${relationField}Ids</#if>);
        <#else>
        if (${relationField}Id != null) {
        verify(this.${relationServiceClass}).getById(${relationField}Id);
        }
        </#if>
        </#list>
        verify(this.${serviceClass?uncap_first}).bulkCreate(${modelName?uncap_first}s);

        assertThat(results).isNotNull();
        assertThat(results).hasSize(1);
        verify${strippedModelName?cap_first}(results.get(0), ${modelName?uncap_first});
    }
