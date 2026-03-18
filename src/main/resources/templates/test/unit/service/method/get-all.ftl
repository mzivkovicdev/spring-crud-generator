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
        final String sortBy = null;
        final String sortDirection = null;

        when(this.${strippedModelName?uncap_first}Repository.findAll(PageRequest.of(pageNumber, pageSize)))
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

        verify(this.${strippedModelName?uncap_first}Repository).findAll(PageRequest.of(pageNumber, pageSize));
    }
    <#if sortEnabled?? && sortEnabled>

    @Test
    void getAll_withSortByOnly_usesDefaultDirection() {
        final Integer pageNumber = ${generatorFieldName}.${singleObjectMethodName}(Integer.class);
        final Integer pageSize = ${generatorFieldName}.${singleObjectMethodName}(Integer.class);
        final String sortBy = <#if sortAllowedFields?has_content>"${sortAllowedFields[0]}"<#else>"id"</#if>;
        final String sortDirection = null;
        final Sort sort = Sort.by(Direction.fromString("${sortDefaultDirection}"), sortBy);
        final Page<${modelName}> page${strippedModelName?cap_first} = new PageImpl<>(List.of());

        when(this.${strippedModelName?uncap_first}Repository.findAll(PageRequest.of(pageNumber, pageSize, sort)))
                .thenReturn(page${strippedModelName?cap_first});

        final Page<${modelName}> results = this.${strippedModelName?uncap_first}Service.getAll(
                pageNumber, pageSize, sortBy, sortDirection
        );

        assertThat(results).isNotNull();
        verify(this.${strippedModelName?uncap_first}Repository).findAll(PageRequest.of(pageNumber, pageSize, sort));
    }

    @Test
    void getAll_withSortByAndSortDirection() {
        final Integer pageNumber = ${generatorFieldName}.${singleObjectMethodName}(Integer.class);
        final Integer pageSize = ${generatorFieldName}.${singleObjectMethodName}(Integer.class);
        final String sortBy = <#if sortAllowedFields?has_content>"${sortAllowedFields[0]}"<#else>"id"</#if>;
        final String sortDirection = "DESC";
        final Sort sort = Sort.by(Direction.fromString(sortDirection), sortBy);
        final Page<${modelName}> page${strippedModelName?cap_first} = new PageImpl<>(List.of());

        when(this.${strippedModelName?uncap_first}Repository.findAll(PageRequest.of(pageNumber, pageSize, sort)))
                .thenReturn(page${strippedModelName?cap_first});

        final Page<${modelName}> results = this.${strippedModelName?uncap_first}Service.getAll(
                pageNumber, pageSize, sortBy, sortDirection
        );

        assertThat(results).isNotNull();
        verify(this.${strippedModelName?uncap_first}Repository).findAll(PageRequest.of(pageNumber, pageSize, sort));
    }

    @Test
    void getAll_invalidSortBy_throwsIllegalArgumentException() {
        final Integer pageNumber = ${generatorFieldName}.${singleObjectMethodName}(Integer.class);
        final Integer pageSize = ${generatorFieldName}.${singleObjectMethodName}(Integer.class);

        assertThatThrownBy(() -> this.${strippedModelName?uncap_first}Service.getAll(pageNumber, pageSize, "invalidSortField", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sortBy");
    }
    </#if><#t>
