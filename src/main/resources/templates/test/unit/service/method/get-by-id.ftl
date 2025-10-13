    @Test
    void getById() {

        final ${modelName} ${modelName?uncap_first} = PODAM_FACTORY.manufacturePojo(${modelName}.class);
        final ${idType} ${idField} = ${modelName?uncap_first}.get${idField?cap_first}();

        when(this.${strippedModelName?uncap_first}Repository.findById(${idField}))
                .thenReturn(Optional.of(${modelName?uncap_first}));

        final ${modelName} result = this.${strippedModelName?uncap_first}Service.getById(${idField});

        verify${strippedModelName?cap_first}(result, ${modelName?uncap_first});

        verify(this.${strippedModelName?uncap_first}Repository).findById(${idField});
    }

    @Test
    void getById_notFound() {

        final ${idType} ${idField} = PODAM_FACTORY.manufacturePojo(${idType}.class);

        when(this.${strippedModelName?uncap_first}Repository.findById(${idField}))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.${strippedModelName?uncap_first}Service.getById(${idField}))
                .isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                    String.format("${strippedModelName?cap_first} with id not found: %s", ${idField})
                )
                .hasNoCause();

        verify(this.${strippedModelName?uncap_first}Repository).findById(${idField});
    }
    