
    @Test
    void getAllByIds() {
        <#if dataGenerator == "PODAM">
        final List<${modelName}> ${strippedModelName?uncap_first}s = ${generatorFieldName}.${multipleObjectsMethodName}(List.class, ${modelName}.class);
        <#else>
        final List<${modelName}> ${strippedModelName?uncap_first}s = ${generatorFieldName}.${multipleObjectsMethodName}(${modelName}.class)
                        .size(10)
                        .create();

        </#if>
        final List<${idType}> ids = ${strippedModelName?uncap_first}s.stream()
                .map(${modelName}::get${idField?cap_first})
                .toList();

        when(this.${strippedModelName?uncap_first}Repository.findAllById(ids))
                .thenReturn(${strippedModelName?uncap_first}s);

        final List<${modelName}> results = this.${strippedModelName}Service.getAllByIds(ids);

        results.forEach(result -> {

            final ${modelName} ${strippedModelName?uncap_first} = ${strippedModelName?uncap_first}s.stream()
                    .filter(obj -> obj.get${idField?cap_first}().equals(result.get${idField?cap_first}()))
                    .findFirst()
                    .orElseThrow();

            verify${strippedModelName?cap_first}(result, ${strippedModelName?uncap_first});
        });

        verify(this.${strippedModelName?uncap_first}Repository).findAllById(ids);
    }