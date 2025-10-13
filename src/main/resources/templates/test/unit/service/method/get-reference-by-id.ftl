    @Test
    void getReferenceById() {

        final ${modelName} ${strippedModelName?uncap_first} = PODAM_FACTORY.manufacturePojo(${modelName}.class);
        final ${idType} ${idField} = ${strippedModelName?uncap_first}.get${idField?cap_first}();

        when(this.${strippedModelName?uncap_first}Repository.getReferenceById(${idField}))
                .thenReturn(${strippedModelName?uncap_first});

        final ${modelName} result = this.${strippedModelName?uncap_first}Service.getReferenceById(${idField});

        verify${strippedModelName?cap_first}(result, ${strippedModelName?uncap_first});

        verify(this.${strippedModelName?uncap_first}Repository).getReferenceById(${idField});
    }