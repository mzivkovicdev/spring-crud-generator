<#assign modelName = model.modelName>
<#assign strippedModelName = model.strippedModelName>
<#assign idType = model.idType>
<#assign idField = model.idField>
<#assign serviceClass = model.modelService>
<#list relations as rel>
<#assign relationServiceClass = rel.strippedRelationClassName?uncap_first + "Service">
<#assign relationField = rel.strippedRelationClassName?uncap_first>
    <#if rel.isCollection?? && rel.isCollection>
    @Test
    void ${rel.methodName}() {

        final ${modelName} ${modelName?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${modelName}.class);
        final ${idType} ${idField?uncap_first} = ${modelName?uncap_first}.get${idField?cap_first}();
        final ${rel.relationClassName} ${rel.relationClassName?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${rel.relationClassName}.class);
        final ${rel.relationIdType} ${relationField}Id = ${rel.relationClassName?uncap_first}.get${rel.relationIdField?cap_first}();

        when(this.${relationServiceClass}.getReferenceById(${relationField}Id)).thenReturn(${rel.relationClassName?uncap_first});
        when(this.${serviceClass?uncap_first}.${rel.methodName}(${idField?uncap_first}, ${rel.relationClassName?uncap_first})).thenReturn(${modelName?uncap_first});

        final ${modelName} result = this.${strippedModelName}BusinessService.${rel.methodName}(${idField?uncap_first}, ${relationField}Id);

        verify(this.${relationServiceClass}).getReferenceById(${relationField}Id);
        verify(this.${serviceClass?uncap_first}).${rel.methodName}(${idField?uncap_first}, ${rel.relationClassName?uncap_first});

        verify${strippedModelName?cap_first}(result, ${modelName?uncap_first});
    }</#if>
</#list>