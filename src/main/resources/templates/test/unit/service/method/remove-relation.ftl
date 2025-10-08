<#assign modelName = model.modelName>
<#assign idType = model.idType>
<#assign idField = model.idField>
<#assign strippedModelName = model.strippedModelName>
<#list relations as rel>

    @Test
    void ${rel.methodName}() {

        final ${modelName} ${strippedModelName?uncap_first} = PODAM_FACTORY.manufacturePojo(${modelName}.class);
        final ${rel.relationClassName} ${rel.relationClassName?uncap_first} = PODAM_FACTORY.manufacturePojo(${rel.relationClassName}.class);
        final ${idType} ${idField} = ${strippedModelName?uncap_first}.get${idField?cap_first}();
        <#if !rel.isCollection?? || !rel.isCollection>${strippedModelName?uncap_first}.set${rel.relationField?cap_first}(null);</#if>

        when(this.${strippedModelName?uncap_first}Repository.findById(${idField}))
                .thenReturn(Optional.of(${strippedModelName?uncap_first}));
        when(this.${strippedModelName?uncap_first}Repository.saveAndFlush(any()))
                .thenReturn(${strippedModelName?uncap_first});

        final ${modelName} result = this.${strippedModelName?uncap_first}Service.${rel.methodName}(
            ${idField}, ${rel.relationClassName?uncap_first}
        );

        verify${strippedModelName?cap_first}(result, ${strippedModelName?uncap_first});

        verify(this.${strippedModelName?uncap_first}Repository).findById(${idField});
        verify(this.${strippedModelName?uncap_first}Repository).saveAndFlush(any());
    }

    @Test
    void ${rel.methodName}_notFound() {

        final ${idType} ${idField} = PODAM_FACTORY.manufacturePojo(${idType}.class);
        final ${rel.relationClassName} ${rel.relationClassName?uncap_first} = PODAM_FACTORY.manufacturePojo(${rel.relationClassName}.class);

        when(this.${strippedModelName?uncap_first}Repository.findById(${idField}))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.${strippedModelName?uncap_first}Service.${rel.methodName}(${idField}, ${rel.relationClassName?uncap_first}))
                .isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                    String.format("${strippedModelName?cap_first} with id not found: %s", ${idField})
                )
                .hasNoCause();

        verify(this.${strippedModelName?uncap_first}Repository).findById(${idField});
    }

    @Test
    void ${rel.methodName}_invalidResourceState() {

        final ${modelName} ${strippedModelName?uncap_first} = PODAM_FACTORY.manufacturePojo(${modelName}.class);
        <#if !rel.isCollection?? || !rel.isCollection>
        final ${rel.relationClassName} ${rel.relationClassName?uncap_first} = ${strippedModelName?uncap_first}.set${rel.relationField?cap_first}(null);
        <#else>
        final ${rel.relationClassName} ${rel.relationClassName?uncap_first} = PODAM_FACTORY.manufacturePojo(${rel.relationClassName}.class);
        </#if>
        final ${idType} ${idField} = ${strippedModelName?uncap_first}.get${idField?cap_first}();

        when(this.${strippedModelName?uncap_first}Repository.findById(${idField}))
                .thenReturn(Optional.of(${strippedModelName?uncap_first}));

        assertThatThrownBy(() -> this.${strippedModelName?uncap_first}Service.${rel.methodName}(${idField}<#if rel.isCollection?? && rel.isCollection>, ${rel.relationClassName?uncap_first}</#if>))
                .isExactlyInstanceOf(InvalidResourceStateException.class)
                .hasMessage(
                    "Not possible to remove ${rel.elementParam}"
                )
                .hasNoCause();

        verify(this.${strippedModelName?uncap_first}Repository).findById(${idField});
    }
</#list>