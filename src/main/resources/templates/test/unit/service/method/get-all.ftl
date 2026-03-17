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
        <#if sortEnabled?? && sortEnabled>
        final String sortBy = "${sortDefaultField}";
        final String sortDirection = "${sortDefaultDirection}";
        final Sort sort = Sort.by(
                Direction.fromString(sortDirection), sortBy
        );

        when(this.${strippedModelName?uncap_first}Repository.findAll(PageRequest.of(pageNumber, pageSize, sort)))
                .thenReturn(page${strippedModelName?cap_first});
        <#else>
        when(this.${strippedModelName?uncap_first}Repository.findAll(PageRequest.of(pageNumber, pageSize)))
                .thenReturn(page${strippedModelName?cap_first});
        </#if>

        final Page<${modelName}> results = this.${strippedModelName?uncap_first}Service.getAll(
                pageNumber, pageSize<#if sortEnabled?? && sortEnabled>, sortBy, sortDirection</#if>
        );

        assertThat(results).isNotNull();

        results.getContent().forEach(result -> {

            final ${modelName} ${modelName?uncap_first} = ${modelName?uncap_first}s.stream()
                    .filter(obj -> obj.get${idField?cap_first}().equals(result.get${idField?cap_first}()))
                    .findFirst()
                    .orElseThrow();
            
            verify${strippedModelName?cap_first}(result, ${modelName?uncap_first});
        });

        <#if sortEnabled?? && sortEnabled>
        verify(this.${strippedModelName?uncap_first}Repository).findAll(PageRequest.of(pageNumber, pageSize, sort));
        <#else>
        verify(this.${strippedModelName?uncap_first}Repository).findAll(PageRequest.of(pageNumber, pageSize));
        </#if>
    }
