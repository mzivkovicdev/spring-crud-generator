
    @Test
    void bulkCreate() {

        final ${modelName} ${strippedModelName?uncap_first} = ${generatorFieldName}.${singleObjectMethodName}(${modelName}.class);
        final List<${modelName}> ${strippedModelName?uncap_first}s = List.of(${strippedModelName?uncap_first});

        when(this.${strippedModelName?uncap_first}Repository.saveAllAndFlush(${strippedModelName?uncap_first}s))
                .thenReturn(${strippedModelName?uncap_first}s);

        final List<${modelName}> results = this.${strippedModelName?uncap_first}Service.bulkCreate(${strippedModelName?uncap_first}s);

        assertThat(results).isNotNull();
        assertThat(results).hasSize(1);
        verify${strippedModelName?cap_first}(results.get(0), ${strippedModelName?uncap_first});

        verify(this.${strippedModelName?uncap_first}Repository).saveAllAndFlush(${strippedModelName?uncap_first}s);
    }
