    
    @Test
    void deleteById() {

        final ${idType} ${idField} = ${generatorFieldName}.${singleObjectMethodName}(${idType}.class);

        this.${strippedModelName?uncap_first}Service.deleteById(${idField});

        verify(this.${strippedModelName?uncap_first}Repository).deleteById(${idField});
    }