    @Test
    void getAll() {
        <#if dataGenerator == "PODAM">
        final List<${modelName}> ${modelName?uncap_first}s = ${generatorFieldName}.${multipleObjectsMethodName}(List.class, ${modelName}.class);
        <#else>
        final List<${modelName}> ${modelName?uncap_first}s = ${generatorFieldName}.${multipleObjectsMethodName}(${modelName}.class)
                        .size(10)
                        .create();

        </#if>
        final Page<${modelName}> page${strippedModelName?cap_first} = new PageImpl<>(${modelName?uncap_first}s);
        final Integer pageNumber = ${generatorFieldName}.${singleObjectMethodName}(Integer.class);
        final Integer pageSize = ${generatorFieldName}.${singleObjectMethodName}(Integer.class);

        when(this.${strippedModelName?uncap_first}Repository.findAll(PageRequest.of(pageNumber, pageSize)))
                .thenReturn(page${strippedModelName?cap_first});

        final Page<${modelName}> results = this.${strippedModelName?uncap_first}Service.getAll(pageNumber, pageSize);

        assertThat(results).isNotNull();

        results.getContent().forEach(result -> {

            final ${modelName} ${modelName?uncap_first} = ${modelName?uncap_first}s.stream()
                    .filter(obj -> obj.get${idField?cap_first}().equals(result.get${idField?cap_first}()))
                    .findFirst()
                    .orElseThrow();
            
            verify${strippedModelName?cap_first}(result, ${modelName?uncap_first});
        });

        verify(this.${strippedModelName?uncap_first}Repository).findAll(PageRequest.of(pageNumber, pageSize));
    }
