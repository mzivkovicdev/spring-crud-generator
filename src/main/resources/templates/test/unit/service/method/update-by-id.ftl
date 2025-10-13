
    @Test
    void updateById() {

        final ${modelName} ${strippedModelName?uncap_first} = PODAM_FACTORY.manufacturePojo(${modelName}.class);
        final ${idType} ${idField} = ${strippedModelName?uncap_first}.get${idField?cap_first}();

        when(this.${strippedModelName?uncap_first}Repository.findById(${idField}))
                .thenReturn(Optional.of(${strippedModelName?uncap_first}));
        when(this.${strippedModelName?uncap_first}Repository.saveAndFlush(any()))
                .thenReturn(${strippedModelName?uncap_first});

        final ${modelName} result = this.${strippedModelName?uncap_first}Service.updateById(
            ${idField}, <#list fieldNamesWithoutId as fieldName>${strippedModelName?uncap_first}.get${fieldName?cap_first}()<#if fieldName_has_next>, </#if></#list>
        );

        verify${strippedModelName?cap_first}(result, ${strippedModelName?uncap_first});

        verify(this.${strippedModelName?uncap_first}Repository).findById(${idField});
        verify(this.${strippedModelName?uncap_first}Repository).saveAndFlush(any());
    }

    @Test
    void updateById_notFound() {

        final ${modelName} ${strippedModelName?uncap_first} = PODAM_FACTORY.manufacturePojo(${modelName}.class);
        final ${idType} ${idField} = ${strippedModelName?uncap_first}.get${idField?cap_first}();

        when(this.${strippedModelName?uncap_first}Repository.findById(${idField}))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.${strippedModelName?uncap_first}Service.updateById(${idField}, <#list fieldNamesWithoutId as fieldName>${strippedModelName?uncap_first}.get${fieldName?cap_first}()<#if fieldName_has_next>, </#if></#list>))
                .isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                    String.format("${strippedModelName?cap_first} with id not found: %s", ${idField})
                )
                .hasNoCause();

        verify(this.${strippedModelName?uncap_first}Repository).findById(${idField});
    }