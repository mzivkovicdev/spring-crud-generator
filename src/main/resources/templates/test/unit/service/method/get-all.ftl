    @Test
    void getAll() {

        final List<${modelName}> ${modelName?uncap_first}s = PODAM_FACTORY.manufacturePojo(
                List.class, ${modelName}.class
        );
        final Page<${modelName}> page${strippedModelName?cap_first} = new PageImpl<>(${modelName?uncap_first}s);
        final Integer pageNumber = PODAM_FACTORY.manufacturePojo(Integer.class);
        final Integer pageSize = PODAM_FACTORY.manufacturePojo(Integer.class);

        when(this.${strippedModelName?uncap_first}Repository.findAll(PageRequest.of(pageNumber, pageSize)))
                .thenReturn(page${strippedModelName?cap_first});

        final Page<${modelName}> results = this.${strippedModelName?uncap_first}Service.getAll(pageNumber, pageSize);

        assertThat(results).isNotNull();

        results.getContent().forEach(result -> {

            final ${modelName} ${modelName?uncap_first} = ${modelName?uncap_first}s.stream()
                    .filter(${strippedModelName?uncap_first} -> ${strippedModelName?uncap_first}.get${idField?cap_first}().equals(result.get${idField?cap_first}()))
                    .findFirst()
                    .orElseThrow();
            
            verify${strippedModelName?cap_first}(result, ${modelName?uncap_first});
        });

        verify(this.${strippedModelName?uncap_first}Repository).findAll(PageRequest.of(pageNumber, pageSize));
    }
