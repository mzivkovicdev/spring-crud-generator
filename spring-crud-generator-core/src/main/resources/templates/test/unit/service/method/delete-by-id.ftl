    
    @Test
    void deleteById() {

        final ${idType} ${idField} = ${generatorFieldName}.${singleObjectMethodName}(${idType}.class);
        <#if softDeleteEnabled?? && softDeleteEnabled>
        final ${modelName} ${modelName?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${modelName}.class);
        when(this.${strippedModelName?uncap_first}Repository.findByIdAndDeletedFalse(${idField}))
                .thenReturn(Optional.of(${modelName?uncap_first}));
        </#if>

        this.${strippedModelName?uncap_first}Service.deleteById(${idField});

        <#if softDeleteEnabled?? && softDeleteEnabled>
        assertThat(${modelName?uncap_first}.getDeleted()).isTrue();
        verify(this.${strippedModelName?uncap_first}Repository).findByIdAndDeletedFalse(${idField});
        verify(this.${strippedModelName?uncap_first}Repository).save(${modelName?uncap_first});
        <#else>
        verify(this.${strippedModelName?uncap_first}Repository).deleteById(${idField});
        </#if>
    }
    <#if softDeleteEnabled?? && softDeleteEnabled>

    @Test
    void deleteById_notFound() {

        final ${idType} ${idField} = ${generatorFieldName}.${singleObjectMethodName}(${idType}.class);

        when(this.${strippedModelName?uncap_first}Repository.findByIdAndDeletedFalse(${idField}))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.${strippedModelName?uncap_first}Service.deleteById(${idField}))
                .isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                    String.format("${strippedModelName?cap_first} with id not found: %s", ${idField})
                )
                .hasNoCause();

        verify(this.${strippedModelName?uncap_first}Repository).findByIdAndDeletedFalse(${idField});
    }
    </#if>
