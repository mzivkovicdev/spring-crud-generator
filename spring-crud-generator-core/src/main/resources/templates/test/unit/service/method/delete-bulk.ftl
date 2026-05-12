    @Test
    void bulkDelete() {

        final ${idType} firstId = ${generatorFieldName}.${singleObjectMethodName}(${idType}.class);
        final ${idType} secondId = ${generatorFieldName}.${singleObjectMethodName}(${idType}.class);
        final List<${idType}> ids = List.of(firstId, secondId);
        <#if softDeleteEnabled?? && softDeleteEnabled>
        final ${modelName} first${modelName} = ${generatorFieldName}.${singleObjectMethodName}(${modelName}.class);
        final ${modelName} second${modelName} = ${generatorFieldName}.${singleObjectMethodName}(${modelName}.class);
        when(this.${strippedModelName?uncap_first}Repository.findByIdAndDeletedFalse(firstId))
                .thenReturn(Optional.of(first${modelName}));
        when(this.${strippedModelName?uncap_first}Repository.findByIdAndDeletedFalse(secondId))
                .thenReturn(Optional.of(second${modelName}));
        </#if>

        this.${strippedModelName?uncap_first}Service.bulkDelete(ids);

        <#if softDeleteEnabled?? && softDeleteEnabled>
        assertThat(first${modelName}.getDeleted()).isTrue();
        assertThat(second${modelName}.getDeleted()).isTrue();
        verify(this.${strippedModelName?uncap_first}Repository).findByIdAndDeletedFalse(firstId);
        verify(this.${strippedModelName?uncap_first}Repository).findByIdAndDeletedFalse(secondId);
        verify(this.${strippedModelName?uncap_first}Repository).save(first${modelName});
        verify(this.${strippedModelName?uncap_first}Repository).save(second${modelName});
        <#else>
        verify(this.${strippedModelName?uncap_first}Repository).deleteById(firstId);
        verify(this.${strippedModelName?uncap_first}Repository).deleteById(secondId);
        </#if>
    }