<#assign modelName = model.modelName>
<#assign strippedModelName = model.strippedModelName>
<#assign serviceClass = model.modelService>
<#assign testInputArgs = model.testInputArgs>
<#assign fieldNames = model.fieldNames>

    @Test
    void create() {

        final ${modelName} ${modelName?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${modelName}.class);
        <#list relations as rel>
        <#assign relationField = rel.strippedRelationClassName?uncap_first>
        <#if rel.isCollection?? && rel.isCollection>
        final List<${rel.relationClassName}> ${rel.relationClassName?uncap_first}s = ${modelName?uncap_first}.get${rel.elementParam?cap_first}();
        final List<${rel.relationIdType}> ${relationField}Ids = ${rel.relationClassName?uncap_first}s.stream()
                .map(${rel.relationClassName}::get${rel.relationIdField?cap_first})
                .toList();
        <#else>
        final ${rel.relationClassName} ${rel.relationClassName?uncap_first} = ${modelName?uncap_first}.get${rel.elementParam?cap_first}();
        final ${rel.relationIdType} ${relationField}Id = ${rel.relationClassName?uncap_first}.get${rel.relationIdField?cap_first}();
        </#if>
        </#list>
        <#list fields as field>
        final ${field.resolvedType} ${field.name?uncap_first} = ${modelName?uncap_first}.get${field.name?cap_first}();
        </#list>

        <#list relations as rel>
        <#assign relationServiceClass = rel.strippedRelationClassName?uncap_first + "Service">
        <#assign relationClassName = rel.relationClassName>
        <#assign relationField = rel.strippedRelationClassName?uncap_first>
        <#if rel.isCollection?? && rel.isCollection>
        when(this.${relationServiceClass}.getAllByIds(${relationField}Ids)).thenReturn(${rel.relationClassName?uncap_first}s);
        <#else>
        when(this.${relationServiceClass}.getReferenceById(${relationField}Id)).thenReturn(${rel.relationClassName?uncap_first});
        </#if>
        </#list>
        when(this.${serviceClass?uncap_first}.create(${fieldNames})).thenReturn(${modelName?uncap_first});

        final ${modelName} result = this.${strippedModelName?uncap_first}BusinessService.create(${testInputArgs});

        <#list relations as rel>
        <#assign relationServiceClass = rel.strippedRelationClassName?uncap_first + "Service">
        <#assign relationClassName = rel.relationClassName>
        <#assign relationField = rel.strippedRelationClassName?uncap_first>
        <#if rel.isCollection?? && rel.isCollection>
        verify(this.${relationServiceClass}).getAllByIds(${relationField}Ids);
        <#else>
        verify(this.${relationServiceClass}).getReferenceById(${relationField}Id);
        </#if>
        </#list>
        verify(this.${serviceClass?uncap_first}).create(${fieldNames});

        verify${strippedModelName?cap_first}(result, ${modelName?uncap_first});
    }