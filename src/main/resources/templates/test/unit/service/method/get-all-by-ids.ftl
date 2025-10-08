
    @Test
    void getAllByIds() {

        final List<${modelName}> ${strippedModelName?uncap_first}s = PODAM_FACTORY.manufacturePojo(List.class, ${modelName}.class);
        final List<${idType}> ids = ${strippedModelName?uncap_first}.stream()
                .map(${modelName}::${idField})
                .collect(Collectors.toList());

        when(this.${strippedModelName?uncap_first}Repository.findAllById(ids))
                .thenReturn(${strippedModelName?uncap_first});

        final List<${strippedModelName}> results = this.${strippedModelName}Service.getAllByIds(ids);

        results.forEach(result -> {

            final ${modelName} ${strippedModelName?uncap_first} = ${strippedModelName?uncap_first}s.stream()
                    .filter(${modelName?uncap_first} -> ${modelName?uncap_first}.get${idField?cap_first}().equals(result.get${idField?cap_first}()))
                    .findFirst()
                    .orElseThrow();

            verify${strippedModelName?cap_first}(result, ${strippedModelName?uncap_first});
        });

        verify(this.${strippedModelName?uncap_first}Repository).findAllById(ids);
    }