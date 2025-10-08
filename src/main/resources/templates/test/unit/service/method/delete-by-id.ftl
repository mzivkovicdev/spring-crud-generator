    @Test
    void deleteById() {

        final ${idType} ${idField} = PODAM_FACTORY.manufacturePojo(${idType}.class);

        this.${strippedModelName?uncap_first}Service.deleteById(${idField});

        verify(this.${strippedModelName?uncap_first}Repository).deleteById(${idField});
    }